package org.aiwolf.kajiClient.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.base.player.AbstractBodyguard;
import org.aiwolf.client.base.player.AbstractVillager;
import org.aiwolf.common.data.Agent;
import org.aiwolf.kajiClient.lib.Pattern;

public class KajiBodygurad extends AbstractKajiBase {

	@Override
	public String getJudgeText() {
		return null;
	}

	@Override
	public String getComingoutText() {
		return null;
	}

	@Override
	public void setVoteTarget() {
		setVoteTargetTemplate(myPatterns);
	}

	@Override
	public Agent guard() {
		Map<Agent, Double> riskValues = new HashMap<Agent, Double>();

		List<Agent> aliveAgents = getLatestDayGameInfo().getAliveAgentList();
		for(Agent agent: aliveAgents){
			if(agent.equals(getMe())){
				continue;
			}
			//agentが死んだときに失われる役職値(正ならば人間側が死にやすいということ)
			double riskValue =0.0;
			for(Pattern pattern: myPatterns){
				riskValue += getRiskValue(pattern, agent, aliveAgents);
			}
			riskValues.put(agent, riskValue);
		}

		Agent riskMaxAgent = getMaxDoubleValueKey(riskValues);
		return riskMaxAgent;
	}

}
