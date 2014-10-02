package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.aiwolf.client.base.player.AbstractPlayer;
import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;
import org.aiwolf.kajiClient.lib.AdvanceGameInfo;
import org.aiwolf.kajiClient.lib.CauseOfDeath;
import org.aiwolf.kajiClient.lib.DeadCondition;
import org.aiwolf.kajiClient.lib.EnemyCase;
import org.aiwolf.kajiClient.lib.Pattern;
import org.aiwolf.kajiClient.lib.PatternMaker;

/**
 * 全役職共通部分のアルゴリズム
 * initialize：初期パターン作成
 * update：発話ログからAGI更新，Pattern更新
 * dayStart：AGIの死亡プレイヤーを更新
 * @author kengo
 *
 */
public abstract class AbstractKajiBasePlayer extends AbstractPlayer {

	//CO,能力の結果などのデータ集合
	AdvanceGameInfo advanceGameInfo = new AdvanceGameInfo();

	//ありうるパターン全て
	List<Pattern> generalPatterns = new ArrayList<Pattern>();

	//自分の役職を入れたパターン
	List<Pattern> myPatterns = new ArrayList<Pattern>();

	//パターンを更新，拡張するときに用いる
	PatternMaker patternMaker;

	//トークをどこまで読んだか
	int readTalkNumber = 0;

	//今日投票するプレイヤー(暫定)
	Agent voteTarget = null;

	//最新の発話で言った投票先プレイヤー
	Agent toldVoteTarget = null;

	//各役職の強さを数値化したもの．暫定版エージェント用
	Map<Role, Double> rolePoint = new HashMap<Role, Double>();



	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		/*
		 * パターン生成
		 */
		super.initialize(gameInfo, gameSetting);
		//初期パターンの作成
		patternMaker = new PatternMaker(gameSetting);
		generalPatterns.add(new Pattern(null, null, new HashMap<Agent, Role>()));
		Pattern initialPattern;
		switch (getMyRole()) {
		case SEER:
			initialPattern = new Pattern(getMe(), null, new HashMap<Agent, Role>());
			break;
		case MEDIUM:
			initialPattern = new Pattern(null, getMe(), new HashMap<Agent, Role>());
		default:
			initialPattern = new Pattern(null, null, new HashMap<Agent, Role>());
			break;
		}
		myPatterns.add(initialPattern);

		setRolePoint();

	}

	/**
	 * 各役職の強さを入力
	 */
	private void setRolePoint(){
		rolePoint.put(Role.BODYGUARD, 0.3);
		rolePoint.put(Role.MEDIUM, 0.7);
		rolePoint.put(Role.POSSESSED, -0.3);
		rolePoint.put(Role.SEER, 1.2);
		rolePoint.put(Role.VILLAGER, 0.1);
		rolePoint.put(Role.WEREWOLF, -0.1);
		rolePoint.put(Role.FREEMASON, 0.0);
	}

	/**
	 * patternにおいてagentが死亡した時に失われる役職値を返す
	 * @param pattern
	 * @param agent
	 * @param aliveAgents
	 * @return
	 */
	public double getRiskValue(Pattern pattern, Agent agent, List<Agent> aliveAgents){
		double riskValue = 0.0;

		Map<Role, Double> roleProbabilitys = getRoleProbabilitys(pattern, agent, aliveAgents);
		for(Entry<Role, Double> set: roleProbabilitys.entrySet()){
			riskValue += rolePoint.get(set.getKey()) * set.getValue();
		}
		return riskValue;
	}

	/**
	 * patternにおけるagentが各役職に何パーセントでなっているか返す
	 * @param pattern
	 * @param agent
	 * @param aliveAgents
	 * @return
	 */
	public Map<Role, Double> getRoleProbabilitys(Pattern pattern, Agent agent, List<Agent> aliveAgents){
		Map<Role, Double> roleProbabilitys = new HashMap<Role, Double>();

		Map<Role, Integer> roleNumMap = new HashMap<Role, Integer>(getGameSetting().getRoleNumMap());

		if(pattern.getSeerAgent() != null){
			if(pattern.getSeerAgent().equals(agent)){
				roleProbabilitys.put(Role.SEER, 1.0);
				return roleProbabilitys;
			}
			roleNumMap.put(Role.SEER, 0);
		}
		if(pattern.getMediumAgent() != null){
			if(pattern.getMediumAgent().equals(agent)){
				roleProbabilitys.put(Role.MEDIUM, 1.0);
				return roleProbabilitys;
			}
			roleNumMap.put(Role.MEDIUM, 0);
		}

		Map<Agent, EnemyCase> enemyMap = pattern.getEnemyMap();
		if(enemyMap.size() != 0){

			int restBlackNum = roleNumMap.get(Role.WEREWOLF);
			int restWhiteNum = roleNumMap.get(Role.POSSESSED);
			for(Entry<Agent, EnemyCase> set: enemyMap.entrySet()){
				if(set.getValue() == EnemyCase.black){
					restBlackNum--;
				}else if(set.getValue() == EnemyCase.white){
					restWhiteNum--;
				}
			}
			roleNumMap.put(Role.WEREWOLF, restBlackNum);
			roleNumMap.put(Role.POSSESSED, restWhiteNum);

			if(enemyMap.containsKey(agent)){
				switch (enemyMap.get(agent)) {
				case black:
					roleProbabilitys.put(Role.WEREWOLF, 1.0);
					break;
				case white:
					roleProbabilitys.put(Role.POSSESSED, 1.0);
					break;
				case gray:
					roleProbabilitys.put(Role.WEREWOLF, (double)restBlackNum/((double)restBlackNum + (double)restWhiteNum));
					roleProbabilitys.put(Role.POSSESSED, (double)restWhiteNum/((double)restBlackNum + (double)restWhiteNum));
					break;
				}
				return roleProbabilitys;
			}
		}
		int restRoleNum = 0;
		for(Entry<Role, Integer> set: roleNumMap.entrySet()){
			restRoleNum += set.getValue();
		}

		//白確リストに入っている場合
		if(pattern.getWhiteAgentSet().contains(agent)){
			for(Entry<Role, Integer> set: roleNumMap.entrySet()){
				if(set.getKey() != Role.WEREWOLF){
					roleProbabilitys.put(set.getKey(), (double)roleNumMap.get(set.getKey())/((double)restRoleNum - (double)roleNumMap.get(Role.WEREWOLF)));
				}
			}
		}
		//白確リストにも入っていない場合
		else{
			for(Entry<Role, Integer> set: roleNumMap.entrySet()){
				roleProbabilitys.put(set.getKey(), (double)roleNumMap.get(set.getKey())/((double)restRoleNum));
			}
		}
		return roleProbabilitys;
	}

	@Override
	public void update(GameInfo gameInfo){
		/*
		 * 会話の処理
		 * 暫定投票先の更新
		 */
		super.update(gameInfo);

		List<Talk> talkList = gameInfo.getTalkList();

		/*
		 * 各発話についての処理
		 * カミングアウトについてはパターンの拡張
		 * 能力結果の発話についてはパターン情報の更新
		 */
		for(; readTalkNumber < talkList.size(); readTalkNumber++){
			Talk talk = talkList.get(readTalkNumber);
			Utterance utterance = new Utterance(talk.getContent());
			switch (utterance.getTopic()) {
			case COMINGOUT:
				comingoutTalkDealing(talk, utterance);
				break;

			case DIVINED:
				divinedTalkDealing(talk, utterance);
				break;


			case INQUESTED:
				inquestedTalkDealing(talk, utterance);
				break;

			case VOTE:
				voteTalkDealing(talk, utterance);
				break;
			//上記以外
			default:
				break;
			}
		}
		//投票先を更新(更新する条件などはサブクラスで記載)
		setVoteTarget();
	}

	/**
	 * カミングアウトの発話の処理
	 * @param talk
	 * @param utterance
	 */
	public void comingoutTalkDealing(Talk talk, Utterance utterance){
		advanceGameInfo.putComingoutMap(talk.getAgent(), utterance.getRole());
		patternMaker.extendPatternList(generalPatterns, talk.getAgent(), utterance.getRole(), advanceGameInfo);
		patternMaker.extendPatternList(myPatterns, talk.getAgent(), utterance.getRole(), advanceGameInfo);
	}

	/**
	 * 占い結果の発話の処理
	 * @param talk
	 * @param utterance
	 */
	public void divinedTalkDealing(Talk talk, Utterance utterance){
		Judge inspectJudge = new Judge(getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
		advanceGameInfo.addInspectJudges(inspectJudge);
		patternMaker.updateJudgeData(generalPatterns, inspectJudge);
		patternMaker.updateJudgeData(myPatterns, inspectJudge);
	}

	/**
	 * 霊能結果の発話の処理
	 * @param talk
	 * @param utterance
	 */
	public void inquestedTalkDealing(Talk talk, Utterance utterance){
		Judge tellingJudge = new Judge(getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
		advanceGameInfo.addMediumJudges(tellingJudge);
		patternMaker.updateJudgeData(generalPatterns, tellingJudge);
		patternMaker.updateJudgeData(myPatterns, tellingJudge);
	}

	/**
	 * 投票意思の発話の処理
	 * @param talk
	 * @param utterance
	 */
	public void voteTalkDealing(Talk talk, Utterance utterance){
		Vote vote = new Vote(getDay(), talk.getAgent(), utterance.getTarget());
		advanceGameInfo.addVote(getDay(), vote);
	}

	@Override
	public void dayStart() {
		/*
		 * 死亡プレイヤー情報の更新
		 * 暫定投票先の更新
		 */
		readTalkNumber = 0;
		//死亡したプレイヤーをAGIに記録
		Agent attackedAgent = getLatestDayGameInfo().getAttackedAgent();
		if(attackedAgent != null){
			DeadCondition attackedAgentCondition = new DeadCondition(attackedAgent, getDay(), CauseOfDeath.attacked);
			advanceGameInfo.addDeadConditions(attackedAgentCondition);
			patternMaker.updateAttackedData(generalPatterns, attackedAgent);
			patternMaker.updateAttackedData(myPatterns, attackedAgent);
		}

		Agent executedAgent = getLatestDayGameInfo().getExecutedAgent();
		if(executedAgent != null){
			DeadCondition executeddAgentCondition = new DeadCondition(executedAgent, getDay(), CauseOfDeath.executed);
			advanceGameInfo.addDeadConditions(executeddAgentCondition);
		}

		//今日の暫定投票先
		toldVoteTarget = null;
		voteTarget = null;

	}

	@Override
	public String talk() {
		/*
		 * 発話順序の優先度
		 * カミングアウト＞能力結果の発話＞投票先の発話
		 */

		//カミングアウトの発話
		String comingoutReport = getComingoutText();
		if(comingoutReport != null){
			return comingoutReport;
		}

		//占い，霊能結果の発話
		String judgeReport = getJudgeText();
		if(judgeReport != null){
			return judgeReport;
		}


		//投票先の発話
		if(toldVoteTarget != voteTarget && voteTarget != null){
			String voteReport = TemplateTalkFactory.vote(voteTarget);
			toldVoteTarget = voteTarget;
			return voteReport;
		}

		//話すことが何もなければ
		return Talk.OVER;
	}

	/**
	 * 占い or 霊能結果の発話を行う．結果の報告をしない場合はnullを返す
	 * @return
	 */
	public abstract String getJudgeText();

	/**
	 * カミングアウトの発話を行う．COしない場合はnullを返す
	 * @return
	 */
	public abstract String getComingoutText();

	/**
	 * 今日投票予定のプレイヤーを決定する
	 * updateとdayStartの最後によばれる
	 * @return
	 */
	public abstract void setVoteTarget();

	/**
	 * 各プレイヤーについて，そのプレイヤーが死亡した際の損害の期待値を出す
	 * 損害が一番低いプレイヤーに投票先を移す
	 */
	public void setVoteTargetTemplate(List<Pattern> patterns){
		/*
		 * for各プレイヤー
		 * for全パターン
		 * +そのプレイヤーを殺した時の損害（役職値）
		 */

		Map<Agent, Double> riskValues = new HashMap<Agent, Double>();

		List<Agent> aliveAgents = getLatestDayGameInfo().getAliveAgentList();
		for(Agent agent: aliveAgents){
			if(agent.equals(getMe())){
				continue;
			}
			//agentが死んだときに失われる役職値(正ならば人間側が死にやすいということ)
			double riskValue =0.0;
			for(Pattern pattern: patterns){
				riskValue += getRiskValue(pattern, agent, aliveAgents);
			}
			riskValues.put(agent, riskValue);
		}

		Agent riskMinAgent = getMinDoubleValueKey(riskValues);

		//もしリスク最小が今の投票対象と違っていれば，投票先変更
		if(voteTarget == null || !aliveAgents.contains(voteTarget) || riskValues.get(riskMinAgent) - riskValues.get(voteTarget) != 0){
			voteTarget = riskMinAgent;
		}
		return;
	}

	/**
	 * ValueがDoubleであるMapについて，その値が最大となるKeyを返す
	 * @param map
	 * @return
	 */
	public static <T>T getMaxDoubleValueKey(Map<T, Double> map){
		T maxValueT = null;
		double maxValue = -10000.0;

		double randValue = new Random().nextDouble();
		for(Entry<T, Double> set: map.entrySet()){
			if(set.getValue() > maxValue){
				maxValueT = set.getKey();
				maxValue = set.getValue();
			}else if(set.getValue() == maxValue){
				double newRand = new Random().nextDouble();
				if(randValue < newRand){
					maxValueT = set.getKey();
					maxValue = set.getValue();
					randValue = newRand;
				}
			}
		}
		return maxValueT;
	}

	public static <T>T getMaxIntValueKey(Map<T, Integer> map){
		Map<T, Double> parsedDoubleMap = new HashMap<T, Double>();

		for(Entry<T, Integer> set: map.entrySet()){
			parsedDoubleMap.put(set.getKey(), (double)set.getValue());
		}
		return getMaxDoubleValueKey(parsedDoubleMap);
	}

	/**
	 * ValueがDoubleであるMapについて，その値が最小となるKeyを返す
	 * @param map
	 * @return
	 */
	public static <T>T getMinDoubleValueKey(Map<T, Double> map){
		T minValueT = null;
		double minValue = 10000.0;

		double randValue = new Random().nextDouble();
		for(Entry<T, Double> set: map.entrySet()){
			if(set.getValue() < minValue){
				minValueT = set.getKey();
				minValue = set.getValue();
			}else if(set.getValue() == minValue){
				double newRand = new Random().nextDouble();
				if(randValue > newRand){
					minValueT = set.getKey();
					minValue = set.getValue();
					randValue = newRand;
				}
			}
		}
		return minValueT;
	}

	public static <T>T getMinIntValueKey(Map<T, Integer> map){
		Map<T, Double> parsedDoubleMap = new HashMap<T, Double>();

		for(Entry<T, Integer> set: map.entrySet()){
			parsedDoubleMap.put(set.getKey(), (double)set.getValue());
		}
		return getMinDoubleValueKey(parsedDoubleMap);
	}

	@Override
	public String whisper() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent attack() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent divine() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent guard() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent vote() {
		return voteTarget;
	}

	@Override
	public void finish() {
		// TODO 自動生成されたメソッド・スタブ

	}

}
