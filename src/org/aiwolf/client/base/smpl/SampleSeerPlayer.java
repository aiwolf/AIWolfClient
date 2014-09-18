package org.aiwolf.client.base.smpl;

import java.util.*;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractSeerPlayer;
import org.aiwolf.client.lib.*;
import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class SampleSeerPlayer extends AbstractSeerPlayer{

	//COする日にち
	int comingoutDay;

	//CO済みか否か
	boolean isCameout;

	//全体に占い結果を報告済みのプレイヤー
	ArrayList<Judge> declaredJudgedAgentList = new ArrayList<>();

	boolean isSaidInspectResultToday;

	AdvanceGameInfo agi = new AdvanceGameInfo();

	//今日投票しようと思っているプレイヤー
	Agent planningVoteAgent;

	//自分が最後に宣言した「投票しようと思っているプレイヤー」
	Agent declaredPlanningVoteAgent;

	//会話をどこまで読んだか
	int readTalkListNum;



	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		comingoutDay = new Random().nextInt(3)+1;
		isCameout = false;
	}


	@Override
	public void dayStart() {
		super.dayStart();

		//投票するプレイヤーの初期化，設定
		declaredPlanningVoteAgent = null;
		planningVoteAgent = null;
		setPlanningVoteAgent();

		isSaidInspectResultToday = false;

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
			Utterance u = TemplateTalkFactory.estimate(planningVoteAgent, Role.WEREWOLF);
			utterances.add(u);
			declaredPlanningVoteAgent = planningVoteAgent;
		}

		/*
		 * 未CO，かつ設定したCOする日にちを過ぎていたらCO
		 */
		if(!isCameout && getDay() >= comingoutDay){
			Utterance u2 = TemplateTalkFactory.comingout(getMe(), getMyRole());
			utterances.add(u2);
			isCameout = true;
		}

		/*
		 * COしているなら占い結果の報告
		 */
		if(isCameout && !isSaidInspectResultToday){
			for(Judge judge: getMyJudgeList()){
				if(!declaredJudgedAgentList.contains(judge)){
					Utterance u_result = TemplateTalkFactory.inspected(judge.getTarget(), judge.getResult());
					utterances.add(u_result);
					declaredJudgedAgentList.add(judge);
				}
			}
			isSaidInspectResultToday = true;
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
	public Agent divine() {
		/*
		 * まだ占っていないプレイヤーの中からランダムに選択
		 */
		List<Agent> nonInspectedAgentList = new ArrayList<>();

		for(Agent agent: getLatestDayGameInfo().getAliveAgentList()){
			if(!isJudgedAgent(agent)){
				nonInspectedAgentList.add(agent);
			}
		}

		if(nonInspectedAgentList.size() == 0){
			return getMe();
		}else{
			return nonInspectedAgentList.get(new Random().nextInt(nonInspectedAgentList.size()));
		}
	}

	@Override
	public void finish() {
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
					if(p.getObject() == getMyRole() && !talk.getAgent().equals(getMe())){
						setPlanningVoteAgent();
					}
				}

			}
		}
		readTalkListNum =talkList.size();
	}

	public void setPlanningVoteAgent(){
		/*
		 * 自分以外の占い師COのプレイヤー．または自分が黒判定を出したプレイヤー
		 * いなければ，白判定を出したプレイヤー以外でランダム
		 */

		List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
		aliveAgentList.remove(getMe());

		List<Agent> voteAgentCandidate = new ArrayList<>();
		for(Agent agent: aliveAgentList){
			if(agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == getMyRole()){
				voteAgentCandidate.add(agent);
			}else{
				for(Judge judge: getMyJudgeList()){
					if(judge.getTarget().equals(agent) && judge.getResult() == Species.WEREWOLF){
						voteAgentCandidate.add(agent);
					}
				}
			}
		}

		if(voteAgentCandidate.contains(planningVoteAgent)){
			return;
		}


		if(voteAgentCandidate.size() > 0){
			Random rand = new Random();
			planningVoteAgent = voteAgentCandidate.get(rand.nextInt(voteAgentCandidate.size()));
		}else{
			Random rand = new Random();
			planningVoteAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
		}
		return;
	}


}
