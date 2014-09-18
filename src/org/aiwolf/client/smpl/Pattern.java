package org.aiwolf.client.smpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.*;
import org.aiwolf.server.GameSetting;

public class Pattern {
	/*
	 * 前提となる情報(プレイヤーと役職のセット)
	 * 確定敵の情報(プレイヤーと狂人かどうか．黒確と白確(他が全員黒確))
	 * 前提から決定する情報(どの前提でも確定する情報はどうするか)
	 * 尤度
	 * 　何日目に各役職が死んでる確率(COタイミング以降は意図的に殺せるから反映無し)
	 * 　何日目に占い，霊能で何人人狼が当たる確率
	 */

	//前提とする占い師と霊能者のエージェント
	Agent seerAgent = null;
	Agent mediumAgent = null;

	//敵サイド確定となるエージェント
	Map<Agent, EnemyCase> enemyMap = new HashMap<Agent, EnemyCase>();

	//白確エージェント．(真能力者から白判定 or 襲撃死)
	List<Agent> whiteAgentList = new ArrayList<Agent>();

	//尤度
	double likelifood;


	public Pattern(Agent seerAgent, Agent mediumAgent, Map<Agent, Role> comingoutMap){
		this.seerAgent = seerAgent;
		this.mediumAgent = mediumAgent;
		for(Entry<Agent, Role> entry: comingoutMap.entrySet()){
			if(entry.getValue() != Role.SEER && entry.getValue() != Role.MEDIUM){
				continue;
			}
			if(!entry.getKey().equals(seerAgent) && !entry.getKey().equals(mediumAgent)){
				enemyMap.put(entry.getKey(), EnemyCase.gray);
			}
		}
	}


	/**
	 * 新しい占い，霊能結果を用いてパターンを更新する．整合性が取れない場合はfalseを返す
	 * @param judge
	 */
	public boolean updatePattern(Judge judge){
		Agent judgment = judge.getAgent();
		if(judgment == seerAgent || judgment == mediumAgent){
			switch (judge.getResult()) {
			case HUMAN:
				Agent target = judge.getTarget();
				whiteAgentList.add(target);
				/**
				 * 敵陣営のプレイヤーなら狂人確定．他の敵を人狼と確定．
				 */
				if(enemyMap.containsKey(target)){
					Map<Agent, EnemyCase> enemyMapNew = new HashMap<Agent, EnemyCase>();
					for(Entry<Agent, EnemyCase> entry: enemyMap.entrySet()){
						if(entry.getKey().equals(target)){
							enemyMapNew.put(entry.getKey(), EnemyCase.white);
						}else{
							enemyMapNew.put(entry.getKey(), EnemyCase.black);
						}
					}
					enemyMap = enemyMapNew;
				}
				break;
			case WEREWOLF:
				enemyMap.put(judge.getTarget(), EnemyCase.black);
				break;
			}
		}

		if(!isPatternMatched()){
			return false;
		}
		/*
		 * 尤度を更新するアルゴリズムも必要か
		 */

		return true;
	}

	public boolean isPatternMatched(){
		/*
		 * 人狼するプレイヤー数によって変化するようにしたい
		 */
		int enmeyNumber = 4;

		/**
		 * 敵の数が過多なら嘘．人狼の数がゲーム設定の人狼数を超えても嘘(狂人は1人設定)．
		 */
		if(enemyMap.size() > enmeyNumber){
			return false;
		}else if(enemyMap.size() == enmeyNumber){
			int blackNumber = 0;
			for(Entry<Agent, EnemyCase> entry: enemyMap.entrySet()){
				if(entry.getValue() == EnemyCase.black){
					blackNumber++;
				}
			}
			if(blackNumber > enmeyNumber - 1){
				return false;
			}
		}

		/**
		 * 白確定かつ黒確定がいれば嘘
		 */
		for(Entry<Agent, EnemyCase> entry: enemyMap.entrySet()){

		}
		return true;
	}


	public Agent getSeerAgent() {
		return seerAgent;
	}

	public void setSeerAgent(Agent seerAgent) {
		this.seerAgent = seerAgent;
	}

	public Agent getMediumAgent() {
		return mediumAgent;
	}

	public void setMediumAgent(Agent mediumAgent) {
		this.mediumAgent = mediumAgent;
	}

	public Map<Agent, EnemyCase> getEnemyMap() {
		return enemyMap;
	}

	public void setEnemyMap(Map<Agent, EnemyCase> enemyMap) {
		this.enemyMap = enemyMap;
	}

	public List<Agent> getWhiteAgentList() {
		return whiteAgentList;
	}

	public void setWhiteAgentList(List<Agent> whiteAgentList) {
		this.whiteAgentList = whiteAgentList;
	}

	public double getLikelifood() {
		return likelifood;
	}

	public void setLikelifood(double likelifood) {
		this.likelifood = likelifood;
	}



}
