package org.aiwolf.client.base.smpl;


import java.util.*;

import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class AdvanceGameInfo {

	/**
	 * 発話で伝えられた占い結果のリスト．今回のプロトコルでは何日目に占ったのか分からないので，発話日に設定．
	 */
	private List<Judge> inspectJudgeList = new ArrayList<Judge>();

	/**
	 * 発話で伝えられた霊能結果のリスト．今回のプロトコルでは何日目に霊能したのか分からないので，発話日に設定．
	 */
	private List<Judge> mediumJudgeList = new ArrayList<Judge>();

	//Map<占い師, Map<占われたプレイヤー, 占い結果> >
/*	private Map<Agent, Map<Agent, Species>> inspectMap = new HashMap<Agent, Map<Agent,Species>>();

	private Map<Agent, Map<Agent, Species>> mediumTellingMap = new HashMap<Agent, Map<Agent,Species>>();
*/
	private Map<Agent, Role> comingoutMap = new HashMap<Agent, Role>();

/*	public Map<Agent, Map<Agent, Species>> getInspectMap() {
		return inspectMap;
	}

	public void putInspectMap(Agent seerAgent, Agent inspectedAgent, Species species){
		if(inspectMap.containsKey(seerAgent)){
			inspectMap.get(seerAgent).put(inspectedAgent, species);
		}else{
			Map<Agent, Species> newMap = new HashMap<Agent, Species>();
			newMap.put(inspectedAgent, species);
			inspectMap.put(seerAgent, newMap);
		}
	}

	public void setInspectMap(Map<Agent, Map<Agent, Species>> inspectMap) {
		this.inspectMap = inspectMap;
	}

	public Map<Agent, Map<Agent, Species>> getMediumTellingMap() {
		return mediumTellingMap;
	}

	public void putMediumTellingMap(Agent MediumAgent, Agent TelledAgent, Species species){
		if(mediumTellingMap.containsKey(MediumAgent)){
			mediumTellingMap.get(MediumAgent).put(TelledAgent, species);
		}else{
			Map<Agent, Species> newMap = new HashMap<Agent, Species>();
			newMap.put(TelledAgent, species);
			mediumTellingMap.put(MediumAgent, newMap);
		}
	}

	public void setMediumTellingMap(Map<Agent, Map<Agent, Species>> mediumTellingMap) {
		this.mediumTellingMap = mediumTellingMap;
	}
*/
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

	public List<Judge> getInspectJudgeList() {
		return inspectJudgeList;
	}

	public void setInspectJudgeList(List<Judge> inspectJudgeList) {
		this.inspectJudgeList = inspectJudgeList;
	}

	public void addInspectJudgeList(Judge judge) {
		this.inspectJudgeList.add(judge);
	}

	public List<Judge> getMediumJudgeList() {
		return mediumJudgeList;
	}

	public void setMediumJudgeList(List<Judge> mediumJudgeList) {
		this.mediumJudgeList = mediumJudgeList;
	}

	public void addMediumJudgeList(Judge judge) {
		this.mediumJudgeList.add(judge);
	}


}
