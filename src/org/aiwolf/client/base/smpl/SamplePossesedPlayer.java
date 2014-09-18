package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractPossessedPlayer;
import org.aiwolf.client.lib.Passage;
import org.aiwolf.client.lib.Protocol;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.client.lib.Verb;
import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class SamplePossesedPlayer extends AbstractPossessedPlayer {

	//COする日にち
	int comingoutDay;

	//CO済みか否か
	boolean isCameout;

	//全体に偽占い(霊能)結果を報告済みのJudge
	ArrayList<Judge> declaredFakeJudgedAgentList = new ArrayList<>();

	//全体に占い結果を報告済みのプレイヤー
//	ArrayList<Agent> declaredFakeResultAgent = new ArrayList<>();
	boolean isSaidFakeResultToday;

	AdvanceGameInfo agi = new AdvanceGameInfo();

	//今日投票しようと思っているプレイヤー
	Agent planningVoteAgent;

	//自分が最後に宣言した「投票しようと思っているプレイヤー」
	Agent declaredPlanningVoteAgent;

	//会話をどこまで読んだか
	int readTalkListNum;

	//騙る役職
	Role fakeRole;

	//偽の占い(or霊能)結果
	List<Judge> fakeJudgeList = new ArrayList<>();
	//Map<Agent, Species> fakeResultMap = new HashMap<Agent, Species>();

	public void initialize(GameInfo gameInfo, GameSetting gameSetting){
		super.initialize(gameInfo, gameSetting);

		List<Role> fakeRoleList = Arrays.asList(Role.seer, Role.medium, Role.villager);
		fakeRole = fakeRoleList.get(new Random().nextInt(fakeRoleList.size()));

		//占い師，or霊能者なら1~3日目からランダムに選択してCO．村人ならCOしない．
		comingoutDay = new Random().nextInt(3)+1;
		if(fakeRole == Role.villager){
			comingoutDay = 1000;
		}
		isCameout = false;
	}


	@Override
	public void dayStart() {
		//投票するプレイヤーの初期化，設定
		declaredPlanningVoteAgent = null;
		planningVoteAgent = null;
		setPlanningVoteAgent();

		if(getDay() >= 1){
			setFakeResult();
		}
		isSaidFakeResultToday = false;

		readTalkListNum =0;
	}

	@Override
	public String talk() {
		ArrayList<Utterance> utterances = new ArrayList<>();

		/*
		 * 今日投票するプレイヤーの報告
		 * 前に報告したプレイヤーと同じ場合は報告なし
		 */
		if(declaredPlanningVoteAgent != planningVoteAgent){
			Utterance u = TemplateTalkFactory.estimate(planningVoteAgent, Role.werewolf);
			utterances.add(u);
			declaredPlanningVoteAgent = planningVoteAgent;
		}

		/*
		 * 未CO，かつ設定したCOする日にちを過ぎていたらCO
		 */
		if(!isCameout && getDay() >= comingoutDay && fakeRole != Role.villager){
			Utterance u2 = TemplateTalkFactory.comingout(getMe(), fakeRole);
			utterances.add(u2);
			isCameout = true;
		}

		/*
		 * COしているなら偽占い(or霊能)結果の報告
		 */
		if(isCameout && !isSaidFakeResultToday){
			for(Judge judge: getMyFakeJudgeList()){
				if(!declaredFakeJudgedAgentList.contains(judge)){
					Utterance u_result = null;
					if(fakeRole == Role.seer){
						u_result = TemplateTalkFactory.inspected(judge.getTarget(), judge.getResult());
					}else if(fakeRole == Role.medium){
						u_result = TemplateTalkFactory.medium_telled(judge.getTarget(), judge.getResult());
					}
					utterances.add(u_result);
					declaredFakeJudgedAgentList.add(judge);
				}
			}
			isSaidFakeResultToday = true;
		}

		if(utterances.size() > 0){
			Protocol p = new Protocol(utterances);
			return p.getText();
		}else{
			return TemplateTalkFactory.over().getText();
		}
	}

	@Override
	public Agent vote() {
		return planningVoteAgent;
	}

	@Override
	public void finish() {
		// TODO 自動生成されたメソッド・スタブ

	}


	/**
	 * 今日投票予定のプレイヤーを設定する．
	 */
	public void setPlanningVoteAgent(){
		/*
		 * 村人騙りなら自分以外からランダム
		 * それ以外↓
		 * 対抗CO，もしくは自分が黒だと占ったプレイヤーからランダム
		 * いなければ白判定を出したプレイヤー以外からランダム
		 * それもいなければ生存プレイヤーからランダム
		 */

		List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
		aliveAgentList.remove(getMe());

		if(fakeRole == Role.villager){
			if(aliveAgentList.contains(planningVoteAgent)){
				return;
			}else{
				Random rand = new Random();
				planningVoteAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
			}
		}

		//偽占いで人間だと判定したプレイヤーのリスト
		List<Agent> fakeHumanList = new ArrayList<>();

		List<Agent> voteAgentCandidate = new ArrayList<>();
		for(Agent a: aliveAgentList){
			if(agi.getComingoutMap().containsKey(a) && agi.getComingoutMap().get(a) == fakeRole){
				voteAgentCandidate.add(a);
			}
		}
		for(Judge judge: getMyFakeJudgeList()){
			if(judge.getResult() == Species.Human){
				fakeHumanList.add(judge.getTarget());
			}else{
				voteAgentCandidate.add(judge.getTarget());
			}
		}

		if(voteAgentCandidate.contains(planningVoteAgent)){
			return;
		}


		if(voteAgentCandidate.size() > 0){
			Random rand = new Random();
			planningVoteAgent = voteAgentCandidate.get(rand.nextInt(voteAgentCandidate.size()));
		}else{
			//自分が白判定を出していないプレイヤーのリスト
			List<Agent> aliveAgentExceptHumanList = getLatestDayGameInfo().getAliveAgentList();
			aliveAgentExceptHumanList.removeAll(fakeHumanList);

			if(aliveAgentExceptHumanList.size() > 0){
				Random rand = new Random();
				planningVoteAgent = aliveAgentExceptHumanList.get(rand.nextInt(aliveAgentExceptHumanList.size()));
			}else{
				Random rand = new Random();
				planningVoteAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
			}
		}
		return;
	}


	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);

		List<Talk> talkList = gameInfo.getTalkList();

		/*
		 * talkListからCO結果を抽出
		 * 自分以外で占い師COするプレイヤーが出たら投票先を変える
		 */
		for(int i = readTalkListNum; i < talkList.size(); i++){
			Talk talk = talkList.get(i);
			Protocol protocol = new Protocol(talk.getContent());
			for(Utterance u: protocol.getUtterances()){
				Passage p = u.getPassage();
				if(p.getVerb() == Verb.comingout){
					agi.getComingoutMap().put(talk.getAgent(), p.getObject());
					if(p.getObject() == fakeRole && !talk.getAgent().equals(getMe())){
						setPlanningVoteAgent();
					}
				}

			}
		}
		readTalkListNum =talkList.size();
	}


	/**
	 * 能力者騙りをする際に，偽の占い(or霊能)結果を作成する．
	 */
	public void setFakeResult(){
		Agent fakeGiftTarget = null;

		Species fakeResult = null;

		if(fakeRole == Role.seer){
			//偽占い(or霊能)の候補．以下，偽占い候補
			List<Agent> fakeGiftTargetCandidateList = new ArrayList<>();

			List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
			aliveAgentList.remove(getMe());

			for(Agent agent: aliveAgentList){
				//まだ偽占いしてないプレイヤー，かつ対抗CO者じゃないプレイヤーは偽占い候補
				if(!isJudgedAgent(agent) && fakeRole != agi.getComingoutMap().get(agent)){
					fakeGiftTargetCandidateList.add(agent);
				}
			}

			if(fakeGiftTargetCandidateList.size() > 0){
				Random rand = new Random();
				fakeGiftTarget = fakeGiftTargetCandidateList.get(rand.nextInt(fakeGiftTargetCandidateList.size()));
			}else{
				aliveAgentList.removeAll(fakeGiftTargetCandidateList);
				Random rand = new Random();
				fakeGiftTarget = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
			}

			//30%で黒判定，70%で白判定
			if(Math.random() < 0.3){
				fakeResult = Species.Werewolf;
			}else{
				fakeResult = Species.Human;
			}

		}
		else if(fakeRole == Role.medium){
			fakeGiftTarget = getLatestDayGameInfo().getExecutedAgent();
			//30%で黒判定，70%で白判定
			if(Math.random() < 0.3){
				fakeResult = Species.Werewolf;
			}else{
				fakeResult = Species.Human;
			}
		}
		else{
			return;
		}

		fakeJudgeList.add(new Judge(getDay(), getMe(), fakeGiftTarget, fakeResult));
	}

	public List<Judge> getMyFakeJudgeList(){
		return fakeJudgeList;
	}


	/**
	 * すでに占い(or霊能)対象にしたプレイヤーならtrue,まだ占っていない(霊能していない)ならばfalseを返す．
	 * @param myJudgeList
	 * @param agent
	 * @return
	 */
	public boolean isJudgedAgent(Agent agent){
		for(Judge judge: getMyFakeJudgeList()){
			if(judge.getAgent() == agent){
				return true;
			}
		}
		return false;
	}

}
