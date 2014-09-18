package org.aiwolf.client.lib;

import java.util.List;

import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public class Passage {
	String text;

	String[] split;

	Agent subject;

	Category category;

	Verb verb;


	//comingoutのROLE
	State object = null;

	//estimateのROLE
	State state = null;

	//resultのSpecies(guard以外)
	State species = null;

	public Passage(String input) {
		text = input;
		split = input.split("\\s+");// 一つ以上の空白で区切る

		if(split[0].equals("SKIP")){
			category = Category.SKIP;
		}else if(split[0].equals("Over")){
			category = Category.OVER;
		}else{
			verb = Verb.fromString(split[1]);
			category = verb.getCategory();

			switch (category) {
			case COMINGOUT:
				object = State.fromString(split[2]);
				break;
			case ESTIMATE:
				state = State.fromString(split[1]);
				break;
			case RESULT:
				if(verb != Verb.guarded){
					species = State.fromString(split[2]);
				}
			case SKIP:
			case OVER:
			default:
				break;
			}
		}
	}
/*
	public Agent getSubject(List<Agent> agentList) {
		if (category == Category.SKIP) {
			return null;
		} else {
			int idx = Integer.parseInt(split[0]);
			for (Agent agent : agentList) {
				if (agent.getAgentIdx() == idx) {
					return agent;
				}
			}
		}
		return null;
	}
	*/

	/**
	 * 文の名詞を返す．
	 * 例："Agent01 inspected Werewolf" ⇒ "Agent01"
	 * @param agentList
	 * @return
	 * @author tori
	 */
	public Agent getSubject() {
		if (category == Category.SKIP || category == Category.OVER) {
			return null;
		} else {
			String subject = (String) split[0].subSequence(split[0].indexOf("[")+1, split[0].indexOf("]"));
			int idx = Integer.parseInt(subject);
			return Agent.getAgent(idx);
		}
	}


	/**
	 * 文の動詞のカテゴリーを返す．
	 * 例："Agent01 medium_telled Human" ⇒ "RESULT"
	 * @return
	 */
	public Category getCategory() {
		return category;
	}

	/**
	 * 文の動詞を返す．
	 * 例："Agent01 medium_telled Human" ⇒ "medium_telled"
	 * @return
	 */
	public Verb getVerb() {
		return verb;
	}

	/**
	 * 動詞のカテゴリーがCOMINGOUTである文の目的語を返す．(Role型)
	 * 例："Agent01 comingout Seer" ⇒ "Seer"
	 * @return
	 */
	public Role getObject() {
		if(category == Category.COMINGOUT){
			return object.toRole();
		}
		return null;
	}

	/**
	 * 動詞のカテゴリーがESTIMATEである文の目的語を返す．(Role型)
	 * 例："Agent01 Hunter" ⇒ "Hunter"
	 * @return
	 */
	public Role getState(){
		if(category == Category.ESTIMATE){
			return state.toRole();
		}else{
			return null;
		}
	}

	/**
	 * 動詞のカテゴリーがRESULTである文の動詞を返す．(Verb型)
	 * 例："Agent01 inspected Human" ⇒ "inspected"
	 * @return
	 */
	public Verb getAction(){
		if(category == Category.RESULT){
			return verb;
		}else{
			return null;
		}
	}

	/**
	 * 動詞のカテゴリーがRESULTである文の目的語を返す．(Species型)
	 * 例："Agent01 inspected Human" ⇒ "Human"
	 * @return
	 */
	public Species getAttribution(){
		if(category == Category.RESULT){
			return species.toSpecies();
		}else{
			return null;
		}
	}

}
