package org.aiwolf.client.lib;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

public class TemplateWhisperFactory {

	public static Utterance attack(Agent targetAgent){
		Verb verb =
		return new Utterance(getSVsentence(targetAgent, ))
	}

	/**
	 * 「【対象プレイヤー】は【役職】だと思う．」という発話を生成する
	 * 例："Agent01 Seer" (Agent01は占い師だと思う)
	 * @param targetAgent
	 * @param role
	 * @return
	 */
	public static Utterance estimate(Agent targetAgent, Role role){
		return new Utterance(getSCsentence(targetAgent, State.fromRole(role)));
	}

	/**
	 * 「【対象プレイヤー】が【役職】をカミングアウトする．」という発話を生成する
	 * 例："Agent02 comingout seer" (Agent02が占い師だとカミングアウトする)
	 * @param subjectAgent
	 * @param role
	 * @return
	 */
	public static Utterance comingout(Agent subjectAgent, Role role){
		Verb verb = Verb.comingout;
		return new Utterance(getSVCsentence(subjectAgent, verb, State.fromRole(role)));
	}

	/**
	 * 「【対象プレイヤー】が【人間or人狼】だと占われた．」という発話を生成する
	 * 例："Agent03 inspected Human" (Agent03が人間だと占われた)
	 * @param subjectAgent
	 * @param species
	 * @return
	 */
	public static Utterance inspected(Agent subjectAgent, Species species){
		Verb verb = Verb.inspected;
		State o = State.fromSpecies(species);
		return new Utterance(getSVCsentence(subjectAgent, verb, o));
	}

	/**
	 * 「【対象プレイヤー】が【人間or人狼】だと霊能される．」という発話を生成する
	 * 例："Agent04 medium_telled Human" (Agent04が人間だと霊能された)
	 * @param subjectAgent
	 * @param species
	 * @return
	 */
	public static Utterance medium_telled(Agent subjectAgent, Species species){
		Verb verb = Verb.medium_telled;
		State o = State.fromSpecies(species);
		return new Utterance(getSVCsentence(subjectAgent, verb, o));
	}

	/**
	 * 「【対象プレイヤー】が護衛された．」という発話を生成する
	 * @param subjectAgent
	 * @return
	 */
	public static Utterance guarded(Agent subjectAgent){
		Verb verb = Verb.guarded;
		return new Utterance(getSVsentence(subjectAgent, verb));
	}

	/**
	 * 話すことはないがまだ会話のターンは終わってほしくない時に用いる
	 * @return
	 */
	public static Utterance skip(){
		return new Utterance("SKIP");
	}

	/**
	 * もう話すことはない時に用いる
	 * @return
	 */
	public static Utterance over(){
		return new Utterance("Over");
	}


	private static String toString(SentenceType sentenceType, Passage passage){
		String text = "";
		text = text + sentenceType.getUv() + " " + sentenceType.getRate() + "% " + " ( " + toString(passage) + " )";
		return text;
	}

	private static String toString(Passage passage){

		return null;
	}

	private static String getSCsentence(Agent agent, State complement){
		return String.valueOf(agent.toString()) + " " + complement.name();
	}

	private static String getSVsentence(Agent agent, Verb v){
		return String.valueOf(agent.toString()) + " " + v.name();
	}

	private static String getSVCsentence(Agent agent, Verb v, State complement){
		return String.valueOf(agent.toString()) + " "+ v.name() + " " + complement.name();
	}




}
