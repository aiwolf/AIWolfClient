/**
 * SamplePossessed.java
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
import org.aiwolf.sample.lib.AbstractPossessed;

/**
 * <div lang="ja">裏切り者プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample possessed agent</div>
 */
public class SamplePossessed extends AbstractPossessed {

	GameInfo currentGameInfo;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent planningVoteAgent; // 今日投票しようと思っているプレイヤー
	Agent declaredPlanningVoteAgent; // 自分が最後に宣言した「投票しようと思っているプレイヤー」
	int readTalkListNum; // 会話をどこまで読んだか
	Vote lastVote;

	int comingoutDay; // COする日にち
	boolean isCameout; // CO済みか否か
	List<Judge> declaredFakeJudgedAgentList = new ArrayList<>(); // 全体に偽占い(霊媒)結果を報告済みのJudge
	boolean isSaidAllFakeResult;
	Role fakeRole; // 騙る役職
	List<Judge> fakeJudgeList = new ArrayList<>(); // 偽の占い(or霊媒)結果

	@Override
	public String getName() {
		return "SamplePossessed";
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
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		day = gameInfo.getDay();
		List<Talk> talkList = gameInfo.getTalkList();
		boolean existInspectResult = false;
		boolean existMediumResult = false;

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
			case DIVINED:
				Agent seerAgent = talk.getAgent();
				Agent inspectedAgent = content.getTarget();
				Species inspectResult = content.getResult();
				Judge judge = new Judge(day, seerAgent, inspectedAgent, inspectResult);
				agi.addDivination(judge);
				existInspectResult = true;
				break;

			case INQUESTED:
				agi.addInquestList(new Judge(day, talk.getAgent(), content.getTarget(), content.getResult()));
				existMediumResult = true;
				break;

			default:
				break;
			}
		}
		readTalkListNum = talkList.size();

		// 新しい占い結果があれば投票先を変える．(新たに黒判定が出た，または投票先のプレイヤーに白判定が出た場合)
		if (existInspectResult || existMediumResult) {
			setPlanningVoteAgent();
		}
	}

	@Override
	public String talk() {
		// CO,霊媒結果，投票先の順に発話の優先度高

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
	public Agent vote() {
		return planningVoteAgent;
	}

	@Override
	public void finish() {
	}

	/**
	 * 今日投票予定のプレイヤーを設定する．
	 */
	void setPlanningVoteAgent() {
		// 村人騙りなら自分以外からランダム
		// それ以外→対抗CO，もしくは自分が黒だと占ったプレイヤーからランダム
		// いなければ白判定を出したプレイヤー以外からランダム
		// それもいなければ生存プレイヤーからランダム
		List<Agent> aliveAgentList = new ArrayList<>(currentGameInfo.getAliveAgentList());
		aliveAgentList.remove(me);

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
		Agent fakeGiftTarget = null;

		Species fakeResult = null;

		if (fakeRole == Role.SEER) {
			// 偽占い(or霊媒)の候補．以下，偽占い候補
			List<Agent> fakeGiftTargetCandidateList = new ArrayList<Agent>();

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

			// 30%で黒判定，70%で白判定
			if (Math.random() < 0.3) {
				fakeResult = Species.WEREWOLF;
			} else {
				fakeResult = Species.HUMAN;
			}

		} else if (fakeRole == Role.MEDIUM) {
			fakeGiftTarget = currentGameInfo.getExecutedAgent();
			// 30%で黒判定，70%で白判定
			if (Math.random() < 0.3) {
				fakeResult = Species.WEREWOLF;
			} else {
				fakeResult = Species.HUMAN;
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

}
