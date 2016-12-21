/**
 * SampleWerewolf.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivineContentBuilder;
import org.aiwolf.client.lib.InquestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.lib.AbstractWerewolf;

/**
 * <div lang="ja">人狼プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample werewolf agent</div>
 */
public class SampleWerewolf extends AbstractWerewolf {

	GameInfo currentGameInfo;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent planningVoteAgent; // 今日投票しようと思っているプレイヤー
	Agent declaredPlanningVoteAgent; // 自分が最後に宣言した「投票しようと思っているプレイヤー」
	int readTalkListNum; // 会話をどこまで読んだか
	Vote lastVote;
	Vote lastAttackVote;

	int comingoutDay; // COする日にち
	boolean isCameout; // CO済みか否か
	List<Judge> declaredFakeJudgedAgentList = new ArrayList<>(); // 全体に偽占い(霊媒)結果を報告済みのJudge
	boolean isSaidAllFakeResult;
	Role fakeRole; // 騙る役職
	List<Judge> fakeJudgeList = new ArrayList<>(); // 偽の占い(or霊媒)結果
	Agent maybePossesedAgent = null; // 裏切り者だと思うプレイヤー
	List<Agent> wolfList;

	@Override
	public String getName() {
		return "SampleWerewolf";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		currentGameInfo = gameInfo;
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		agi = new AdditionalGameInfo(gameInfo);

		List<Role> fakeRoles = new ArrayList<>(gameSetting.getRoleNumMap().keySet());
		List<Role> nonFakeRoleList = Arrays.asList(Role.BODYGUARD, Role.FREEMASON, Role.POSSESSED, Role.WEREWOLF, Role.FOX);
		fakeRoles.removeAll(nonFakeRoleList);
		fakeRole = fakeRoles.get(new Random().nextInt(fakeRoles.size()));

		wolfList = new ArrayList<Agent>();
		for (Agent agent : currentGameInfo.getAgentList()) {
			if (currentGameInfo.getRoleMap().get(agent) == Role.WEREWOLF) {
				wolfList.add(agent);
			}
		}

		// 占い師，or霊媒師なら1~3日目からランダムに選択してCO．村人ならCOしない．
		comingoutDay = new Random().nextInt(3) + 1;
		if (fakeRole == Role.VILLAGER) {
			comingoutDay = 1000;
		}
		isCameout = false;
	}

	@Override
	public void dayStart() {
		declaredPlanningVoteAgent = null;
		planningVoteAgent = null;
		setPlanningVoteAgent();

		if (day >= 1) {
			setFakeResult();
		}
		isSaidAllFakeResult = false;

		readTalkListNum = 0;
		lastVote = null;
		lastAttackVote = null;
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		day = gameInfo.getDay();
		List<Talk> talkList = gameInfo.getTalkList();
		// talkListからCO，占い結果，霊媒結果の抽出
		for (int i = readTalkListNum; i < talkList.size(); i++) {
			Talk talk = talkList.get(i);
			Content content = new Content(talk.getText());
			switch (content.getTopic()) {

			// カミングアウトの発話の場合 自分以外で占い師COするプレイヤーが出たら投票先を変える
			case COMINGOUT:
				agi.getComingoutMap().put(talk.getAgent(), content.getRole());
				if (content.getRole() == fakeRole) {
					setPlanningVoteAgent();
				}
				break;

			// 占い結果の発話の場合
			// 人狼以外の占い，霊媒結果で嘘だった場合は狂人だと判断
			case DIVINED:
				Agent seerAgent = talk.getAgent();
				Agent inspectedAgent = content.getTarget();
				Species inspectResult = content.getResult();
				Judge judge = new Judge(day, seerAgent, inspectedAgent, inspectResult);
				agi.addDivination(judge);

				// ジャッジしたのが人狼以外の場合
				if (!wolfList.contains(judge.getAgent())) {
					Species judgeSpecies = judge.getResult();
					Species realSpecies;
					if (wolfList.contains(judge.getTarget())) {
						realSpecies = Species.WEREWOLF;
					} else {
						realSpecies = Species.HUMAN;
					}
					if (judgeSpecies != realSpecies) {
						maybePossesedAgent = judge.getAgent();
						setPlanningVoteAgent();
					}
				}
				break;

			case INQUESTED:
				agi.addInquestList(new Judge(day, talk.getAgent(), content.getTarget(), content.getResult()));
				break;

			default:
				break;
			}
		}
		readTalkListNum = talkList.size();
	}

	@Override
	public String talk() {
		// 未CO，かつ設定したCOする日にちを過ぎていたらCO
		if (!isCameout && day >= comingoutDay) {
			String string = new Content(new ComingoutContentBuilder(me, fakeRole)).getText();
			isCameout = true;
			return string;
		}
		// COしているなら偽占い，霊媒結果の報告
		else if (isCameout && !isSaidAllFakeResult) {
			for (Judge judge : fakeJudgeList) {
				if (!declaredFakeJudgedAgentList.contains(judge)) {
					if (fakeRole == Role.SEER) {
						String string = new Content(new DivineContentBuilder(judge.getTarget(), judge.getResult())).getText();
						declaredFakeJudgedAgentList.add(judge);
						return string;
					} else if (fakeRole == Role.MEDIUM) {
						String string = new Content(new InquestContentBuilder(judge.getTarget(), judge.getResult())).getText();
						declaredFakeJudgedAgentList.add(judge);
						return string;
					}
				}
			}
			isSaidAllFakeResult = true;
		}

		// 今日投票するプレイヤーの報告（前に報告したプレイヤーと同じ場合は報告なし）
		if (declaredPlanningVoteAgent != planningVoteAgent) {
			String string = new Content(new VoteContentBuilder(planningVoteAgent)).getText();
			declaredPlanningVoteAgent = planningVoteAgent;
			return string;
		} else {
			return Talk.OVER;
		}

	}

	@Override
	public String whisper() {
		// TODO なんかしようよ
		return Talk.OVER;
	}

	@Override
	public Agent vote() {
		if (lastVote == null) { // この日の初投票
			lastVote = new Vote(day, me, planningVoteAgent);
			return planningVoteAgent;
		} else { // 再投票
			System.err.println("Revote occured");
			lastVote = new Vote(day, me, planningVoteAgent);
			return planningVoteAgent;
		}
	}

	@Override
	public Agent attack() {
		// 能力者COしているプレイヤーは襲撃候補
		// 襲撃候補がいればその中からランダムに選択(20%で全体からランダムに変更)
		// 襲撃候補がいなければ全体からランダム （ただし，いずれの場合も人狼と狂人(暫定)は襲撃対象から除く）

		List<Agent> aliveAgentList = new ArrayList<>(currentGameInfo.getAliveAgentList());
		aliveAgentList.removeAll(wolfList);
		aliveAgentList.remove(maybePossesedAgent);

		List<Agent> attackCandidatePlayer = new ArrayList<Agent>();
		for (Agent agent : aliveAgentList) {
			if (agi.getComingoutMap().containsKey(agent)) {
				attackCandidatePlayer.add(agent);
			}
		}

		Agent attackAgent;
		if (!attackCandidatePlayer.isEmpty() && Math.random() < 0.8) {
			Collections.shuffle(attackCandidatePlayer);
			attackAgent = attackCandidatePlayer.get(0);
		} else {
			Collections.shuffle(aliveAgentList);
			attackAgent = aliveAgentList.get(0);
		}

		if (lastAttackVote == null) {
			lastAttackVote = new Vote(day, me, attackAgent);
		} else {
			lastAttackVote = new Vote(day, me, attackAgent);
			System.err.println("Attack revote occured");
		}

		return attackAgent;
	}

	@Override
	public void finish() {
	}

	/**
	 * 今日投票予定のプレイヤーを設定する．
	 */
	void setPlanningVoteAgent() {
		// 下記のいずれの場合も人狼は投票候補に入れない
		// 狂人が分かれば狂人も除く
		// 村人騙りなら，自分以外からランダム
		// それ以外の場合，対抗CO，もしくは自分が黒だと判定したプレイヤーからランダム
		// いなければ白判定を出したプレイヤー以外からランダム
		// それもいなければ生存プレイヤーからランダム
		List<Agent> aliveAgentList = new ArrayList<>(currentGameInfo.getAliveAgentList());
		aliveAgentList.removeAll(wolfList);
		aliveAgentList.remove(maybePossesedAgent);

		if (fakeRole == Role.VILLAGER) {
			if (aliveAgentList.contains(planningVoteAgent)) {
				return;
			} else {
				Collections.shuffle(aliveAgentList);
				planningVoteAgent = aliveAgentList.get(0);
			}
		}

		// 偽占いで人間だと判定したプレイヤーのリスト
		List<Agent> fakeHumanList = new ArrayList<Agent>();

		List<Agent> voteAgentCandidate = new ArrayList<Agent>();
		for (Agent a : aliveAgentList) {
			if (agi.getComingoutMap().containsKey(a) && agi.getComingoutMap().get(a) == fakeRole) {
				voteAgentCandidate.add(a);
			}
		}
		for (Judge judge : fakeJudgeList) {
			if (judge.getResult() == Species.HUMAN) {
				fakeHumanList.add(judge.getTarget());
			} else {
				voteAgentCandidate.add(judge.getTarget());
			}
		}

		if (voteAgentCandidate.contains(planningVoteAgent)) {
			return;
		}

		if (!voteAgentCandidate.isEmpty()) {
			Collections.shuffle(voteAgentCandidate);
			planningVoteAgent = voteAgentCandidate.get(0);
		} else {
			// 自分が白判定を出していないプレイヤーのリスト
			List<Agent> aliveAgentExceptHumanList = new ArrayList<>(currentGameInfo.getAliveAgentList());
			aliveAgentExceptHumanList.removeAll(fakeHumanList);

			if (!aliveAgentExceptHumanList.isEmpty()) {
				Collections.shuffle(aliveAgentExceptHumanList);
				planningVoteAgent = aliveAgentExceptHumanList.get(0);
			} else {
				Collections.shuffle(aliveAgentList);
				planningVoteAgent = aliveAgentList.get(0);
			}
		}
		return;
	}

	/**
	 * 能力者騙りをする際に，偽の占い(or霊媒)結果を作成する．
	 */
	void setFakeResult() {
		// 村人騙りなら不必要

		// 偽占い(or霊媒)の候補．以下，偽占い候補
		List<Agent> fakeGiftTargetCandidateList = new ArrayList<Agent>();

		Agent fakeGiftTarget;

		Species fakeResult;

		if (fakeRole == Role.VILLAGER) {
			return;
		} else if (fakeRole == Role.SEER) {

			List<Agent> aliveAgentList = new ArrayList<>(currentGameInfo.getAliveAgentList());
			aliveAgentList.remove(me);

			for (Agent agent : aliveAgentList) {
				// まだ偽占いしてないプレイヤー，かつ対抗CO者じゃないプレイヤーは偽占い候補
				if (!isJudgedAgent(agent) && fakeRole != agi.getComingoutMap().get(agent)) {
					fakeGiftTargetCandidateList.add(agent);
				}
			}

			if (!fakeGiftTargetCandidateList.isEmpty()) {
				Collections.shuffle(fakeGiftTargetCandidateList);
				fakeGiftTarget = fakeGiftTargetCandidateList.get(0);
			} else {
				aliveAgentList.removeAll(fakeGiftTargetCandidateList);
				Collections.shuffle(aliveAgentList);
				fakeGiftTarget = aliveAgentList.get(0);
			}
			// 人狼が偽占い対象の場合
			if (wolfList.contains(fakeGiftTarget)) {
				fakeResult = Species.HUMAN;
			}
			// 人間が偽占い対象の場合
			else {
				// 狂人(暫定)，または非COプレイヤー
				if (fakeGiftTarget == maybePossesedAgent || !agi.getComingoutMap().containsKey(fakeGiftTarget)) {
					if (Math.random() < 0.5) {
						fakeResult = Species.WEREWOLF;
					} else {
						fakeResult = Species.HUMAN;
					}
				}
				// 能力者CO，かつ人間，非狂人(暫定)
				else {
					fakeResult = Species.WEREWOLF;
				}
			}
		}

		else if (fakeRole == Role.MEDIUM) {
			fakeGiftTarget = currentGameInfo.getExecutedAgent();
			if (fakeGiftTarget == null) {
				return;
			}
			// 人狼が偽占い対象の場合
			if (wolfList.contains(fakeGiftTarget)) {
				fakeResult = Species.HUMAN;
			}
			// 人間が偽占い対象の場合
			else {
				// 狂人(暫定)，または非COプレイヤー
				if (fakeGiftTarget == maybePossesedAgent || !agi.getComingoutMap().containsKey(fakeGiftTarget)) {
					if (Math.random() < 0.5) {
						fakeResult = Species.WEREWOLF;
					} else {
						fakeResult = Species.HUMAN;
					}
				}
				// 能力者CO，かつ人間，非狂人(暫定)
				else {
					fakeResult = Species.WEREWOLF;
				}
			}
		} else {
			return;
		}

		if (fakeGiftTarget != null) {
			fakeJudgeList.add(new Judge(day, me, fakeGiftTarget, fakeResult));
		}
	}

	/**
	 * すでに占い(or霊媒)対象にしたプレイヤーならtrue,まだ占っていない(霊媒していない)ならばfalseを返す．
	 * 
	 * @param myJudgeList
	 * @param agent
	 * @return
	 */
	boolean isJudgedAgent(Agent agent) {
		for (Judge judge : fakeJudgeList) {
			if (judge.getAgent() == agent) {
				return true;
			}
		}
		return false;
	}

	List<Agent> newAliveAgentList() {
		return new ArrayList<Agent>(currentGameInfo.getAliveAgentList());
	}

}
