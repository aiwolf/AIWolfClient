package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import org.aiwolf.client.base.player.AbstractVillagerPlayer;
import org.aiwolf.client.lib.Passage;
import org.aiwolf.client.lib.Protocol;
import org.aiwolf.client.lib.State;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.client.lib.Verb;
import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class SampleVillagerPlayer extends AbstractVillagerPlayer{
	/*
	 * 投票アルゴリズム：人狼だと占われたエージェント，いなければ，ランダム
	 * 発話：その日に投票しようとしているエージェントを報告．変化すれば報告．
	 */

	AdvanceGameInfo agi = new AdvanceGameInfo();

	//今日投票しようと思っているプレイヤー
	Agent planningVoteAgent;

	//自分が最後に宣言した「投票しようと思っているプレイヤー」
	Agent declaredPlanningVoteAgent;

	//会話をどこまで読んだか
	int readTalkListNum;

	@Override
	public void initialize(GameInfo gameInfo) {
		super.initialize(gameInfo);
	}

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

			Utterance u = TemplateTalkFactory.estimate(planningVoteAgent, Role.werewolf);
			declaredPlanningVoteAgent = planningVoteAgent;
			return u.getText();
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

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);

		List<Talk> talkList = gameInfo.getTalkList();
		boolean existInspectResult = false;

		/*
		 * talkListから占い結果の抽出
		 */
		for(int i = readTalkListNum; i < talkList.size(); i++){
			Talk talk = talkList.get(i);
			Protocol protocol = new Protocol(talk.getContent());
			for(Utterance u: protocol.getUtterances()){
				Passage p = u.getPassage();
				if(p.getVerb() == Verb.inspected){
					Agent seerAgent = talk.getAgent();
					Agent inspectedAgent = p.getSubject();
					Species inspectResult = p.getAttribution();

					Judge judge = new Judge(getDay(), seerAgent, inspectedAgent, inspectResult);
					agi.addInspectJudgeList(judge);

					/*
					Map<Agent, Map<Agent, Species>> map = agi.getInspectMap();
					Map<Agent, Species> m;
					if(map.get(seerAgent) == null){
						m = new HashMap<Agent, Species>();
					}else{
						m = map.get(seerAgent);
					}
					m.put(inspectedAgent, inspectResult);
					map.put(seerAgent, m);*/

					existInspectResult =true;
				}
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
/*			for(Entry<Agent, Map<Agent, Species>> m: agi.getInspectMap().entrySet()){
				if(m.getValue().get(planningVoteAgent) == Species.Werewolf){
					return;
				}
			}
*/
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
			if(aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.Werewolf){
				voteAgentCandidate.add(judge.getTarget());
			}
		}


/*		for(Entry<Agent, Map<Agent, Species>> m: agi.getInspectMap().entrySet()){
			for(Entry<Agent, Species> m2: m.getValue().entrySet()){
				if(aliveAgentList.contains(m2) && m2.getValue() == Species.Werewolf){
					voteAgentCandidate.add(m2.getKey());
				}
			}
		}
*/
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
