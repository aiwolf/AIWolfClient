package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractMediumPlayer;
import org.aiwolf.client.lib.Passage;
import org.aiwolf.client.lib.Protocol;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.client.lib.Verb;
import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class SampleMediumPlayer extends AbstractMediumPlayer {

	//COする日にち
	int comingoutDay;

	//CO済みか否か
	boolean isCameout;

	//全体に霊能結果を報告済みのJudge
	ArrayList<Judge> declaredJudgedAgentList = new ArrayList<Judge>();

//	ArrayList<Agent> declaredMediumTellResultAgent = new ArrayList<>();
	boolean isSaidMediumTellResultToday;

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

		isSaidMediumTellResultToday = false;

		readTalkListNum =0;

	}

	@Override
	public String talk() {

		ArrayList<Utterance> utterances = new ArrayList<Utterance>();

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
		if(isCameout && !isSaidMediumTellResultToday){
			for(Judge judge: getMyJudgeList()){
				if(!declaredJudgedAgentList.contains(judge)){
					Utterance u_result = TemplateTalkFactory.medium_telled(judge.getTarget(), judge.getResult());
					utterances.add(u_result);
					declaredJudgedAgentList.add(judge);
				}
			}
			isSaidMediumTellResultToday = true;
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
				if(p.getVerb() == Verb.comingout){
					agi.getComingoutMap().put(talk.getAgent(), p.getObject());
					if(p.getObject() == getMyRole() && !talk.getAgent().equals(getMe())){
						setPlanningVoteAgent();
					}
				}

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
					map.put(seerAgent, m);
*/
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
		 * 投票先を未設定，または人狼だと占われたプレイヤー以外を投票先にしている場合
		 * 人狼だと占われたプレイヤーがいれば，投票先をそのプレイヤーに設定
		 * いなければ生存プレイヤーからランダムに選択
		 */

		List<Agent> voteAgentCandidate = new ArrayList<Agent>();

		List<Agent> aliveAgentList = getLatestDayGameInfo().getAliveAgentList();
		aliveAgentList.remove(getMe());

		for(Agent agent: aliveAgentList){
			/*
			 * 自分以外に霊能COしているプレイヤーがいれば投票候補
			 */
			if(agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == Role.MEDIUM){
				voteAgentCandidate.add(agent);
			}
/*
			else{

				 * 自分の霊能結果と違う占い結果を出している占い師がいれば投票候補


				Map<Agent, Map<Agent, Species>> inspectMap = agi.getInspectMap();
				if(!inspectMap.containsKey(agent)){
					continue;
				}else{
					Map<Agent, Species> tellResultMap = getMediumTellinResultMap();

					 * 占い師の占い結果Listを参照

					for(Entry<Agent, Species> set: inspectMap.get(agent).entrySet()){
						//霊能したプレイヤーが占われており，かつ霊能結果と占い結果が違うとき投票候補
						if(tellResultMap.containsKey(set.getKey()) && !tellResultMap.get(set.getKey()).equals(set.getValue())){
							voteAgentCandidate.add(agent);
						}
					}
				}
			}
*/
		}

		for(Judge myJudge: getMyJudgeList()){
			for(Judge otherJudge: agi.getInspectJudgeList()){

				if(!aliveAgentList.contains(otherJudge.getAgent())){
					continue;
				}
				/*
				 * 自分と同じ相手について占っている場合
				 */
				if(myJudge.getTarget().equals(otherJudge.getTarget())){
					/*
					 * 自分の占い(霊能)結果と異なる結果を出していたら投票候補
					 */
					if(myJudge.getResult() != otherJudge.getResult()){
						voteAgentCandidate.add(otherJudge.getAgent());
					}
				}
			}
		}


		/*
		 * すでに投票先に指定しているプレイヤーが投票候補内に含まれていたらそのまま
		 */
		if(planningVoteAgent != null && voteAgentCandidate.contains(planningVoteAgent)){
			return;
		}else{
			if (voteAgentCandidate.size() > 0) {
				Random rand = new Random();
				planningVoteAgent = voteAgentCandidate.get(rand.nextInt(voteAgentCandidate.size()));
			} else {

				/*
				 * 投票候補がいない場合は占いで黒判定されているプレイヤーからランダムに選択
				 */
				ArrayList<Agent> subVoteAgentCandidate = new ArrayList<Agent>();

				for(Judge judge: agi.getInspectJudgeList()){
					if(aliveAgentList.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF){
						subVoteAgentCandidate.add(judge.getTarget());
					}
				}

/*				for(Entry<Agent, Map<Agent, Species>> m: agi.getInspectMap().entrySet()){
					for(Entry<Agent, Species> m2: m.getValue().entrySet()){
						if(aliveAgentList.contains(m2) && m2.getValue() == Species.Werewolf){
							subVoteAgentCandidate.add(m2.getKey());
						}
					}
				}
*/
				if(subVoteAgentCandidate.size() > 0){
					Random rand = new Random();
					planningVoteAgent = subVoteAgentCandidate.get(rand.nextInt(subVoteAgentCandidate.size()));
				}else{
					/*
					 * 黒判定されているプレイヤーもいなければ生存プレイヤーからランダムに選択
					 */
					Random rand = new Random();
					planningVoteAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
				}
			}
		}





	}

}
