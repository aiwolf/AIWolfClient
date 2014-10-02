package org.aiwolf.kajiClient.lib;

import org.aiwolf.common.data.Agent;

/**
 * 死んだプレイヤーの詳細．何日目に(襲撃or処刑)によって死んだか．
 * @author kengo
 *
 */
public class DeadCondition {
	private Agent deadAgent;
	private int dateOfDeath;
	private CauseOfDeath cause;


	public DeadCondition(Agent deadAgent, int dateOfDeath, CauseOfDeath cause){
		this.deadAgent = deadAgent;
		this.dateOfDeath = dateOfDeath;
		this.cause = cause;
	}




	public Agent getDeadAgent() {
		return deadAgent;
	}


	public int getDateOfDeath() {
		return dateOfDeath;
	}


	public CauseOfDeath getCause() {
		return cause;
	}


}
