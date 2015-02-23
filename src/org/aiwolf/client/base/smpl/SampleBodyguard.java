package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractBodyguard;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class SampleBodyguard extends AbstractBodyguard {

	AdvanceGameInfo agi = new AdvanceGameInfo();

	//今日投票しようと思っているプレイヤー
	Agent planningVoteAgent;

	//自分が最後に宣言した「投票しようと思っているプレイヤー」
	Agent declaredPlanningVoteAgent;

	//会話をどこまで読んだか
	int readTalkListNum;


	@Override
	public void dayStart() {
		declaredPlanningVoteAgent = null;
		planningVoteAgent = null;
		setPlanningVoteAgent();

		readTalkListNum =0;

	}

	@Override
	public String talk() {

		if(declaredPlanningVoteAgent != planningVoteAgent){

			String string = TemplateTalkFactory.vote(planningVoteAgent);
			declaredPlanningVoteAgent = planningVoteAgent;
			return string;
		}else{
			return TemplateTalkFactory.over();
		}
	}

	@Override
	public Agent vote() {
		return planningVoteAgent;
	}

	@Override
	public Agent guard() {
		//占い師，もしくは霊能者COしているプレイヤーからランダムに選択(20%の確率で生存プレイヤーの中からランダムに変更)

		List<Agent> guardAgentCandidate = new ArrayList<Agent>();

		List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
		aliveAgentList.remove(getMe());

		for(Agent agent: aliveAgentList){
			if(agi.getComingoutMap().containsKey(agent)){
				List<Role> guardRoleList = Arrays.asList(Role.SEER, Role.MEDIUM);
				if(guardRoleList.contains(agi.getComingoutMap().get(agent))){
					guardAgentCandidate.add(agent);
				}
			}
		}

		Agent guardAgent;

		if(guardAgentCandidate.size() > 0 && Math.random() < 0.8){
			Random rand = new Random();
			guardAgent = guardAgentCandidate.get(rand.nextInt(guardAgentCandidate.size()));
		}else{
			Random rand = new Random();
			guardAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
		}
		return guardAgent;
	}

	@Override
	public void finish() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);

		List<Talk> talkList = gameInfo.getTalkList();
		boolean existInspectResult = false;

		/*
		 * talkListからCO，占い結果の抽出
		 */
		for(int i = readTalkListNum; i < talkList.size(); i++){
			Talk talk = talkList.get(i);
			Utterance utterance = new Utterance(talk.getContent());
			switch (utterance.getTopic()) {

			//カミングアウトの発話の場合
			case COMINGOUT:
				agi.getComingoutMap().put(talk.getAgent(), utterance.getRole());
				break;

			//占い結果の発話の場合
			case DIVINED:
				//AGIのJudgeListに結果を加える
				Agent seerAgent = talk.getAgent();
				Agent inspectedAgent = utterance.getTarget();
				Species inspectResult = utterance.getResult();
				Judge judge = new Judge(getDay(), seerAgent, inspectedAgent, inspectResult);
				agi.addInspectJudgeList(judge);

				existInspectResult =true;
				break;
			}
		}
		readTalkListNum =talkList.size();

		/*
		 * 新しい占い結果があれば投票先を変える．(新たに黒判定が出た，または投票先のプレイヤーに白判定が出た場合)
		 */
		if(existInspectResult){
			setPlanningVoteAgent();
		}
	}

	public void setPlanningVoteAgent(){
		/*
		 * 人狼だと占われたプレイヤーを指定している場合はそのまま
		 */
		if(planningVoteAgent != null){
			for(Judge judge: agi.getInspectJudgeList()){
				if(judge.getTarget().equals(planningVoteAgent)){
					return;
				}
			}
		}

		/*
		 * 投票先を未設定，または人狼だと占われたプレイヤー以外を投票先にしている場合
		 * 人狼だと占われたプレイヤーがいれば，投票先をそのプレイヤーに設定
		 * いなければ生存プレイヤーからランダムに選択
		 */
		List<Agent> voteAgentCandidate = new ArrayList<Agent>();

		List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
		aliveAgentList.remove(getMe());

		for(Judge judge: agi.getInspectJudgeList()){
			if(aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF){
				voteAgentCandidate.add(judge.getTarget());
			}
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
