/**
 * SampleSeer.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.aiwolf.client.base.player.AbstractSeer;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * <div lang="ja">占い師プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample seer agent</div>
 * 
 * @deprecated
 */
public class SampleSeer extends AbstractSeer{

	//COする日にち
	int comingoutDay;

	//CO済みか否か
	boolean isCameout;

	//全体に占い結果を報告済みのプレイヤー
	ArrayList<Judge> declaredJudgedAgentList = new ArrayList<Judge>();

	boolean isSaidAllDivineResult;

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

		isSaidAllDivineResult = false;

		readTalkListNum =0;

	}

	@Override
	public String talk() {
		//CO,霊媒結果，投票先の順に発話の優先度高

		/*
		 * 未CO，かつ設定したCOする日にちを過ぎていたらCO
		 */

		if(!isCameout && getDay() >= comingoutDay){
			String string = new Content(new ComingoutContentBuilder(getMe(), getMe(), getMyRole())).getText();
			isCameout = true;
			return string;
		}
		/*
		 * COしているなら占い結果の報告
		 */
		else if(isCameout && !isSaidAllDivineResult){
			for(Judge judge: getMyJudgeList()){
				if(!declaredJudgedAgentList.contains(judge)){
					String string = new Content(new DivinedContentBuilder(getMe(), judge.getTarget(), judge.getResult())).getText();
					declaredJudgedAgentList.add(judge);
					return string;
				}
			}
			isSaidAllDivineResult = true;
		}

		/*
		 * 今日投票するプレイヤーの報告
		 * 前に報告したプレイヤーと同じ場合は報告なし
		 */
		if(declaredPlanningVoteAgent != planningVoteAgent){
			String string = new Content(new VoteContentBuilder(getMe(), planningVoteAgent)).getText();
			declaredPlanningVoteAgent = planningVoteAgent;
			return string;
		}

		else{
			return Talk.OVER;
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
		List<Agent> nonInspectedAgentList = new ArrayList<Agent>();

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
		 * talkListからCO，占い結果の抽出
		 */
		for(int i = readTalkListNum; i < talkList.size(); i++){
			Talk talk = talkList.get(i);
			Content content = new Content(talk.getAgent(), talk.getText());
			switch (content.getTopic()) {

			//カミングアウトの発話の場合
			case COMINGOUT:
				agi.getComingoutMap().put(talk.getAgent(), content.getRole());
				if (content.getRole() == getMyRole()) {
					setPlanningVoteAgent();
				}
				break;

			default:
				break;
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

		List<Agent> voteAgentCandidate = new ArrayList<Agent>();
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
