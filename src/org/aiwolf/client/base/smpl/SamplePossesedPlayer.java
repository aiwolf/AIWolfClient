package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.aiwolf.client.base.player.AbstractPossessedPlayer;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class SamplePossesedPlayer extends AbstractPossessedPlayer {

	//COする日にち
	int comingoutDay;

	//CO済みか否か
	boolean isCameout;

	//全体に偽占い(霊能)結果を報告済みのJudge
	ArrayList<Judge> declaredFakeJudgedAgentList = new ArrayList<Judge>();

	//全体に占い結果を報告済みのプレイヤー
//	ArrayList<Agent> declaredFakeResultAgent = new ArrayList<>();
	boolean isSaidAllFakeResult;

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
	List<Judge> fakeJudgeList = new ArrayList<Judge>();
	//Map<Agent, Species> fakeResultMap = new HashMap<Agent, Species>();

	public void initialize(GameInfo gameInfo, GameSetting gameSetting){
		super.initialize(gameInfo, gameSetting);

//		List<Role> fakeRoleList = Arrays.asList(Role.SEER, Role.MEDIUM, Role.VILLAGER);
		List<Role> fakeRoles = new ArrayList(gameSetting.getRoleNumMap().keySet());
		List<Role> nonFakeRoleList = Arrays.asList(Role.BODYGUARD, Role.FREEMASON, Role.POSSESSED, Role.WEREWOLF);
		fakeRoles.removeAll(nonFakeRoleList);

		fakeRole = fakeRoles.get(new Random().nextInt(fakeRoles.size()));

		//占い師，or霊能者なら1~3日目からランダムに選択してCO．村人ならCOしない．
		comingoutDay = new Random().nextInt(3)+1;
		if(fakeRole == Role.VILLAGER){
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
		isSaidAllFakeResult = false;

		readTalkListNum =0;
	}

	@Override
	public String talk() {
		//CO,霊能結果，投票先の順に発話の優先度高

		/*
		 * 未CO，かつ設定したCOする日にちを過ぎていたらCO
		 */

		if(!isCameout && getDay() >= comingoutDay){
			String string = TemplateTalkFactory.comingout(getMe(), fakeRole);
			isCameout = true;
			return string;
		}
		/*
		 * COしているなら偽占い，霊能結果の報告
		 */
		else if(isCameout && !isSaidAllFakeResult){
			for(Judge judge: getMyFakeJudgeList()){
				if(!declaredFakeJudgedAgentList.contains(judge)){
					if(fakeRole == Role.SEER){
						String string = TemplateTalkFactory.divined(judge.getTarget(), judge.getResult());
						declaredFakeJudgedAgentList.add(judge);
						return string;
					}else if(fakeRole == Role.MEDIUM){
						String string = TemplateTalkFactory.inquested(judge.getTarget(), judge.getResult());
						declaredFakeJudgedAgentList.add(judge);
						return string;
					}
				}
			}
			isSaidAllFakeResult = true;
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

		if(fakeRole == Role.VILLAGER){
			if(aliveAgentList.contains(planningVoteAgent)){
				return;
			}else{
				Random rand = new Random();
				planningVoteAgent = aliveAgentList.get(rand.nextInt(aliveAgentList.size()));
			}
		}

		//偽占いで人間だと判定したプレイヤーのリスト
		List<Agent> fakeHumanList = new ArrayList<Agent>();

		List<Agent> voteAgentCandidate = new ArrayList<Agent>();
		for(Agent a: aliveAgentList){
			if(agi.getComingoutMap().containsKey(a) && agi.getComingoutMap().get(a) == fakeRole){
				voteAgentCandidate.add(a);
			}
		}
		for(Judge judge: getMyFakeJudgeList()){
			if(judge.getResult() == Species.HUMAN){
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
		boolean existInspectResult = false;

		/*
		 * talkListからCO，占い結果の抽出
		 */
		for(int i = readTalkListNum; i < talkList.size(); i++){
			Talk talk = talkList.get(i);
			Utterance utterance = new Utterance(talk.getContent());
			switch (utterance.getTopic()) {

			/*
			 * カミングアウトの発話の場合
			 * 自分以外で占い師COするプレイヤーが出たら投票先を変える
			 */
			case COMINGOUT:
				agi.getComingoutMap().put(talk.getAgent(), utterance.getRole());
				if(utterance.getRole() == fakeRole){
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

	/**
	 * 能力者騙りをする際に，偽の占い(or霊能)結果を作成する．
	 */
	public void setFakeResult(){
		Agent fakeGiftTarget = null;

		Species fakeResult = null;

		if(fakeRole == Role.SEER){
			//偽占い(or霊能)の候補．以下，偽占い候補
			List<Agent> fakeGiftTargetCandidateList = new ArrayList<Agent>();

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
				fakeResult = Species.WEREWOLF;
			}else{
				fakeResult = Species.HUMAN;
			}

		}
		else if(fakeRole == Role.MEDIUM){
			fakeGiftTarget = getLatestDayGameInfo().getExecutedAgent();
			//30%で黒判定，70%で白判定
			if(Math.random() < 0.3){
				fakeResult = Species.WEREWOLF;
			}else{
				fakeResult = Species.HUMAN;
			}
		}
		else{
			return;
		}

		if(fakeGiftTarget != null){
			fakeJudgeList.add(new Judge(getDay(), getMe(), fakeGiftTarget, fakeResult));
		}
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
