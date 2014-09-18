package org.aiwolf.client.lib;

/**
 * Protocolクラスのフィールドであり，一つの発話文を表すクラス．
 * フィールドとして，
 * 文章(String), 発話文のタイプ(SentenceType)，基礎文(Passage)を含む，
 * @author kengo
 *
 */
public class Utterance {
	private String text;


	//発話文のタイプ(Declareとか)
	private SentenceType sentenceType;

	//発話の中身(comingout seerとか)
	private Passage passage;

	/**
	 * 発話の文章(String型)を引数として発話を生成する．
	 *  @param input
	 */
	public Utterance(String input){
		text = input;
		if(input.equals("SKIP")){
			passage = new Passage(input);
			return;
		}else if(input.equals("Over")){
			passage = new Passage(input);
			return;
		}

		if(true){
			passage = new Passage(text);
		}

		String[] textSplit = text.split("\\s+");
		try {
			sentenceType.setUv(UtteranceVerb.fromString(textSplit[0]));
			if(sentenceType.getUv() == UtteranceVerb.declare){
				sentenceType.setRate(Integer.parseInt(textSplit[1]));
			}
		} catch (Exception e) {
			// TODO: handle exception
			//
		}
	}

	/**
	 * 発話の文章を返す
	 * @return
	 */
	public String getText() {
		return text;
	}

	/**
	 * 発話のSentenceTypeを返す
	 * @return
	 */
	public SentenceType getSentenceType() {
		return sentenceType;
	}

	/**
	 * 発話の中の基礎文を返す
	 * @return
	 */
	public Passage getPassage() {
		return passage;
	}

}
