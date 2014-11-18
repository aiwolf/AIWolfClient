package org.aiwolf.client.lib;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

/**
 * Factory to create template talk contents
 * @author kengo
 *
 */
public class TemplateTalkFactory {

	/**
	 * Talk one's estimation
	 * @param target
	 * @param role
	 * @return
	 */
	public static String estimate(Agent target, Role role){
		String[] split = {Topic.ESTIMATE.toString(), String.valueOf(target.toString()), role.toString()};
		return wordAttachment(split);
	}

	/**
	 * Comingout someone's role
	 * @param target
	 * @param role
	 * @return
	 */
	public static String comingout(Agent target, Role role){
		String[] split = {Topic.COMINGOUT.toString(), String.valueOf(target.toString()), role.toString()};
		return wordAttachment(split);
	}

	/**
	 * Report result of divine
	 * @param target
	 * @param species
	 * @return
	 */
	public static String divined(Agent target, Species species){
		String[] split = {Topic.DIVINED.toString(), String.valueOf(target.toString()), species.toString()};
		return wordAttachment(split);
	}

	
	/**
	 * Report result of inquest
	 * @param target
	 * @param species
	 * @return
	 */
	public static String inquested(Agent target, Species species){
		String[] split = {Topic.INQUESTED.toString(), String.valueOf(target.toString()), species.toString()};
		return wordAttachment(split);
	}

	/**
	 * Report guarded agent
	 * @param target
	 * @return
	 */
	public static String guarded(Agent target){
		String[] split = {Topic.GUARDED.toString(), String.valueOf(target.toString())};
		return wordAttachment(split);
	}

	/**
	 * Declare vote target
	 * @param target
	 * @return
	 */
	public static String vote(Agent target){
		String[] split = {Topic.VOTE.toString(), String.valueOf(target.toString())};
		return wordAttachment(split);
	}

	public enum TalkType{
		TALK(false),
		WHISPER(true);

		private boolean isWhisper;
		private TalkType(boolean isWhisper) {
			this.isWhisper = isWhisper;
		}

		public static TalkType parseTalkType(String input){
			if(input.equalsIgnoreCase("talk")){
				return TALK;
			}else if(input.equalsIgnoreCase("whisper")){
				return WHISPER;
			}else{
				return null;
			}
		}
	}

	public static String agree(TalkType talkType, int day, int id){
		String[] split = {Topic.AGREE.toString(), talkType.toString(), "day"+String.valueOf(day), "ID:"+String.valueOf(id)};
		return wordAttachment(split);
	}

	public static String disagree(TalkType talkType, int day, int id){
		String[] split = {Topic.DISAGREE.toString(), talkType.toString(), "day"+String.valueOf(day), "ID:"+String.valueOf(id)};
		return wordAttachment(split);
	}

	public static String over(){
		return Talk.OVER;
	}

	public static String skip(){
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
