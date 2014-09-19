package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractMediumPlayer;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
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
	boolean isSaidAllInquestResult;

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

		isSaidAllInquestResult = false;

		readTalkListNum =0;

	}

	@Override
	public String talk() {
		//CO,霊能結果，投票先の順に発話の優先度高

		/*
		 * 未CO，かつ設定したCOする日にちを過ぎていたらCO
		 */

		if(!isCameout && getDay() >= comingoutDay){
			String string = TemplateTalkFactory.comingout(getMe(), getMyRole());
			isCameout = true;
			return string;
		}
		/*
		 * COしているなら占い結果の報告
		 */
		else if(isCameout && !isSaidAllInquestResult){
			for(Judge judge: getMyJudgeList()){
				if(!declaredJudgedAgentList.contains(judge)){
					String string = TemplateTalkFactory.inquested(judge.getTarget(), judge.getResult());
					declaredJudgedAgentList.add(judge);
					return string;
				}
			}
			isSaidAllInquestResult = true;
		}

		/*
		 * 今日投票するプレイヤーの報告
		 * 前に報告したプレイヤーと同じ場合は報告なし
		 */
		if(declaredPlanningVoteAgent != planningVoteAgent){
			String string = TemplateTalkFactory.vote(planningVoteAgent);
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
				if(utterance.getRole() == getMyRole()){
					setPlanningVoteAgent();
				}
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
