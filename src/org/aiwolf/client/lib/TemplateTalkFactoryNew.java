package org.aiwolf.client.lib;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

/**
 * 発話を生成するクラス
 * @author kengo
 *
 */
public class TemplateTalkFactoryNew {

	public String estimate(Agent target, Role role){
		String[] split = {Topic.ESTIMATE.toString(), String.valueOf(target.getAgentIdx()), role.toString()};
		return wordAttachment(split);
	}

	public String comingout(Agent target, Role role){
		String[] split = {Topic.COMINGOUT.toString(), String.valueOf(target.getAgentIdx()), role.toString()};
		return wordAttachment(split);
	}

	public String divined(Agent target, Species species){
		String[] split = {Topic.DIVINED.toString(), String.valueOf(target.getAgentIdx()), species.toString()};
		return wordAttachment(split);
	}

	public String inquested(Agent target, Species species){
		String[] split = {Topic.INQUESTED.toString(), String.valueOf(target.getAgentIdx()), species.toString()};
		return wordAttachment(split);
	}

	public String guarded(Agent target){
		String[] split = {Topic.GUARDED.toString(), String.valueOf(target.getAgentIdx())};
		return wordAttachment(split);
	}

	public String vote(Agent target){
		String[] split = {Topic.VOTE.toString(), String.valueOf(target.getAgentIdx())};
		return wordAttachment(split);
	}

	public String agree(int day, int id){
		String[] split = {Topic.AGREE.toString(), String.valueOf(day), String.valueOf(id)};
		return wordAttachment(split);
	}

	public String disagree(int day, int id){
		String[] split = {Topic.DISAGREE.toString(), String.valueOf(day), String.valueOf(id)};
		return wordAttachment(split);
	}

	public String over(){
		return Talk.OVER;
	}

	public String skip(){
		return Talk.SKIP;
	}

	private static String wordAttachment(String[] split){
		String answer = "";
		for(int i = 0; i < split.length; i++){
			answer += split[i] + " ";
		}
		return answer.trim();
	}
}
