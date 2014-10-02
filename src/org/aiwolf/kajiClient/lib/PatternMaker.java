package org.aiwolf.kajiClient.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * List<Pattern>の更新，拡張に用いる
 * @author kengo
 *
 */
public class PatternMaker {

	GameSetting gameSetting;



	public PatternMaker(GameSetting gameSetting){
		this.gameSetting = gameSetting;
	}

	/**
	 * COの発言を元にパターンを作成，更新する．
	 * @param patternList
	 * @param coAgent
	 * @param coRole
	 * @param gameInfo
	 * @return
	 */
	//必要データ：元のパターンリスト，COしたエージェントと役職，死人リスト，
	public void extendPatternList(List<Pattern> patterns, Agent coAgent, Role coRole, AdvanceGameInfo advanceGameInfo){

		List<Pattern> newPatterns = new ArrayList<Pattern>();

		for(Pattern pattern: patterns){
			boolean isGenuineCO = false;
			switch (coRole) {
			case SEER:
				if(pattern.getSeerAgent() != null){
					isGenuineCO = true;
				}
				break;

			case MEDIUM:
				if(pattern.getMediumAgent() != null){
					isGenuineCO = true;
				}
				break;
			}
			//真能力者がいる場合
			if(isGenuineCO == true){
				Pattern newPattern = pattern.clone();
				//enemyMapにまだ入っていない
				if(!newPattern.getEnemyMap().containsKey(coAgent)){
					//白確Agentに入っている
					if(newPattern.getWhiteAgentSet().contains(coAgent)){
						newPattern.getEnemyMap().put(coAgent, EnemyCase.white);
					}
					//白確Agentに入っていない
					else{
						newPattern.getEnemyMap().put(coAgent, EnemyCase.gray);
					}
				}
				newPatterns.add(newPattern);
			}
			//真能力者がいない
			else{
				//新しいCO者を真とするPattern
				Pattern newPattern1 = pattern.clone();
				//新しいCO者も灰色とするPattern
				Pattern newPattern2 = pattern.clone();

				//newPattern1について
				switch (coRole) {
				case SEER:
					newPattern1.setSeerAgent(coAgent);
					break;
				case MEDIUM:
					newPattern1.setMediumAgent(coAgent);
					break;
				}

				//newPattern2について
				newPattern2.getEnemyMap().put(coAgent, EnemyCase.gray);
			}
		}

		//newPatternsの矛盾ないものをpatternに入れる．
		removeContradictPatterns(newPatterns);
		patterns.clear();
		patterns.addAll(newPatterns);
		return;
	}

	//襲撃によって死んだプレイヤーを白確，真能力者によって白確か黒確
	/**
	 * 襲撃されたプレイヤーを白確にする
	 * @param patternList
	 * @param attackedAgent
	 */
	public void updateAttackedData(List<Pattern> patterns, Agent attackedAgent){
		for(Pattern pattern: patterns){
			pattern.getWhiteAgentSet().add(attackedAgent);
		}
		removeContradictPatterns(patterns);
	}

	/**
	 * 占い，霊能によって得られた情報を付加する
	 * @param patterns
	 * @param judge
	 */
	public void updateJudgeData(List<Pattern> patterns, Judge judge){
		for(Pattern pattern: patterns){
			//Judge者が真能力者とされている場合
			if(judge.getAgent().equals(pattern.getSeerAgent()) || judge.getAgent().equals(pattern.getMediumAgent())){
				switch (judge.getResult()) {
				//白判定の場合
				case HUMAN:
					pattern.getWhiteAgentSet().add(judge.getTarget());
					break;

				//黒判定の場合
				case WEREWOLF:
					pattern.getEnemyMap().put(judge.getTarget(), EnemyCase.black);
					break;
				}
			}
		}
		removeContradictPatterns(patterns);
		return;
	}

	/**
	 * PatternのListから矛盾したPatternを除外する
	 * @param patterns
	 */
	public void removeContradictPatterns(List<Pattern> patterns){

		List<Pattern> subPatterns = new ArrayList<Pattern>();
		for(Pattern pattern: patterns){
			boolean isContradict = false;
			Map<Agent, EnemyCase> enemyMap = pattern.getEnemyMap();

			//真占い師が真霊媒師
			if(pattern.getSeerAgent() == pattern.getMediumAgent()){
				isContradict = true;
			}
			//enemyMapに真占い師，霊媒師が含まれている
			else if(enemyMap.containsKey(pattern.getSeerAgent()) || enemyMap.containsKey(pattern.getMediumAgent())){
				isContradict = true;
			}
			else{
				//白確エージェントかつ，enemyMapで黒確
				for(Agent agent: pattern.getWhiteAgentSet()){
					if(enemyMap.containsKey(agent) && enemyMap.get(agent) == EnemyCase.black){
						isContradict = true;
					}
				}
				//enemyMapが限界数を超えている，人狼と狂人がそれぞれ多すぎる．
				if(enemyMap.size() > gameSetting.getRoleNum(Role.POSSESSED) + gameSetting.getRoleNum(Role.WEREWOLF)){
					isContradict = true;
				}else{
					int werewolfNum = 0;
					int possessedNum = 0;
					for(Entry<Agent, EnemyCase> set: enemyMap.entrySet()){
						switch (set.getValue()) {
						case black:
							werewolfNum++;
							break;
						case white:
							possessedNum++;
							break;
						}
					}
					if(werewolfNum > gameSetting.getRoleNum(Role.WEREWOLF) || possessedNum > gameSetting.getRoleNum(Role.POSSESSED)){
						isContradict = true;
					}
				}
			}

			if(isContradict){
				subPatterns.add(pattern);
			}
		}
		patterns.removeAll(subPatterns);
	}


	/**
	 * agentが設定されたroleとならないPatternを除外する．
	 * 例えば自分が真占い師の時に，自分を人狼，狂人とするパターン，他のAgentが真占い師となるパターンを除外
	 * @param patterns
	 * @param agent
	 * @param role
	 */
	public static void settleAgentRole(List<Pattern> patterns, Agent agent, Role role){

		//除外するパターン
		List<Pattern> subPatterns = new ArrayList<Pattern>();

		switch (role) {
		case VILLAGER:
		case BODYGUARD:
			for(Pattern pattern: patterns){
				if(pattern.getEnemyMap().containsKey(agent)){
					subPatterns.add(pattern);
				}else{
					pattern.getWhiteAgentSet().add(agent);
				}
			}
			break;

		case SEER:
			/*
			 * enemyMapに自分が含まれている場合
			 * 他のAgentがSeerとなっている場合(nullなら書き換え)
			 * TODO
			 * （真霊能者が自分の占い結果と異なる結果を出しているとき）もともとなってる説
			 */
			for(Pattern pattern: patterns){
				if(pattern.getEnemyMap().containsKey(agent) || (pattern.getSeerAgent() != null && !pattern.getSeerAgent().equals(agent))){
					subPatterns.add(pattern);
				}else{
					pattern.setSeerAgent(agent);
				}
			}
			break;

		case MEDIUM:
			for(Pattern pattern: patterns){
				if(pattern.getEnemyMap().containsKey(agent) ||  (pattern.getMediumAgent() != null && !pattern.getMediumAgent().equals(agent))){
					subPatterns.add(pattern);
				}else{
					pattern.setMediumAgent(agent);
				}
			}
			break;

		case POSSESSED:
			/*
			 * 自分が真の能力者に含まれている時
			 * 確定黒になっている時
			 */
			for(Pattern pattern: patterns){
				if(agent.equals(pattern.getSeerAgent()) || agent.equals(pattern.getMediumAgent())){
					subPatterns.add(pattern);
				}
				else if(pattern.getEnemyMap().containsKey(agent) && pattern.getEnemyMap().get(agent) == EnemyCase.black){
					subPatterns.add(pattern);
				}else{
					pattern.getEnemyMap().put(agent, EnemyCase.white);
					pattern.getWhiteAgentSet().add(agent);
				}
			}
			break;

		case WEREWOLF:
			/*
			 * 自分が真の能力者に含まれている時
			 * 確定白になっている時
			 */
			for(Pattern pattern: patterns){
				if(agent.equals(pattern.getSeerAgent()) || agent.equals(pattern.getMediumAgent())){
					subPatterns.add(pattern);
				}
				else if(pattern.getEnemyMap().containsKey(agent) && pattern.getEnemyMap().get(agent) == EnemyCase.white){
					subPatterns.add(pattern);
				}else{
					pattern.getEnemyMap().put(agent, EnemyCase.black);
				}
			}
			break;

		default:
			break;
		}

		patterns.removeAll(subPatterns);
		return;
	}

	public List<Pattern> clonePatterns(List<Pattern> patterns){
		List<Pattern> newPatterns = new ArrayList<Pattern>();
		for(Pattern pattern: patterns){
			newPatterns.add(pattern.clone());
		}
		return newPatterns;
	}

}
