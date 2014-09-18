package org.aiwolf.client.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * 発話文(Utterance)の集合．
 * @author kengo
 *
 */
public class Protocol {
	private String text;

	private List<Utterance> utterances = new ArrayList<Utterance>();

	private Conjection c = null;

	/**
	 * String型で表された発話のパースを行うコンストラクタ
	 * @param input
	 */
	public Protocol(String input){
		text = input;
		if(!text.startsWith("(")){
			utterances.add(new Utterance(text));
		}else{
			LogicalStracture split = LogicalStracture.splitStracture(text);
			c = split.getC();
			ArrayList<String> splitString = split.getElement();
			for(String s: splitString){
				utterances.add(new Utterance(s));
			}
		}
	}

	/**
	 * 発話文の集合をまとめて一つの発話を生成する
	 * @param inputU
	 */
	public Protocol(List<Utterance> inputU){
		if(inputU.size() == 1){
			Utterance u = inputU.get(0);
			text = u.getText();
			utterances.add(u);
			return;
		}

		String conjectionString = "and ";
		utterances = inputU;
		ArrayList<String> textSplit = new ArrayList<String>();
		for(Utterance u: utterances){
			textSplit.add( "( " + u.getText() + " ) " );
			textSplit.add(conjectionString);
		}
		textSplit.remove(textSplit.size()-1);
		text = LogicalStracture.bondStrings(textSplit, "");
	}

	/**
	 * 一つの発話文から発話を生成する
	 * @param inputU
	 */
	public Protocol(Utterance inputU){
		text = inputU.getText();
		utterances.add(inputU);
	}

	/**
	 * 発話をString型で返す
	 * @return
	 */
	public String getText() {
		return text;
	}

	protected void setText(String text) {
		this.text = text;
	}

	/**
	 * 発話に含まれる発話文を返す
	 * @return
	 */
	public List<Utterance> getUtterances() {
		return utterances;
	}

	protected void setUtterances(List<Utterance> utterances) {
		this.utterances = utterances;
	}

	public Conjection getC() {
		return c;
	}

	protected void setC(Conjection c) {
		this.c = c;
	}

}
