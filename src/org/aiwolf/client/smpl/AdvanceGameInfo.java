package org.aiwolf.client.smpl;


import java.util.*;

import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;
/**
 * サーバから送られてくるGameInfoを簡単に集約
 * 占い，霊能結果リスト
 * COマップ
 * 死亡リスト
 * @author kengo
 *
 */
public class AdvanceGameInfo {

	/**
	 * 発話で伝えられた占い結果のリスト．今回のプロトコルでは何日目に占ったのか分からないので，発話日に設定．
	 */
	private List<Judge> inspectJudges = new ArrayList<Judge>();

	/**
	 * 発話で伝えられた霊能結果のリスト．今回のプロトコルでは何日目に霊能したのか分からないので，発話日に設定．
	 */
	private List<Judge> mediumJudges = new ArrayList<Judge>();



	private Map<Agent, Role> comingoutMap = new HashMap<Agent, Role>();


	private List<DeadCondition> deadConditions = new ArrayList<DeadCondition>();


	public Map<Agent, Role> getComingoutMap() {
		return comingoutMap;
	}

	/**
	 * COしたプレイヤーをcomingoutMapに加える．
	 * @param agent
	 * @param role
	 */
	public void putComingoutMap(Agent agent, Role role){
		comingoutMap.put(agent, role);
	}

	public void setComingoutMap(Map<Agent, Role> comingoutMap) {
		this.comingoutMap = comingoutMap;
	}

	public List<Judge> getInspectJudges() {
		return inspectJudges;
	}

	public void setInspectJudges(List<Judge> inspectJudgeList) {
		this.inspectJudges = inspectJudgeList;
	}

	public void addInspectJudges(Judge judge) {
		this.inspectJudges.add(judge);
	}

	public List<Judge> getMediumJudges() {
		return mediumJudges;
	}

	public void setMediumJudges(List<Judge> mediumJudgeList) {
		this.mediumJudges = mediumJudgeList;
	}

	public void addMediumJudges(Judge judge) {
		this.mediumJudges.add(judge);
	}

	public List<DeadCondition> getDeadConditions() {
		return deadConditions;
	}

	public void setDeadConditions(List<DeadCondition> deadConditions) {
		this.deadConditions = deadConditions;
	}

	public void addDeadConditions(DeadCondition deadCondition){
		this.deadConditions.add(deadCondition);
	}


}
