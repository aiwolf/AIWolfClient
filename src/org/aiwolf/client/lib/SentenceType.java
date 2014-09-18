package org.aiwolf.client.lib;

/**
 * 発話文における発話動詞の情報を表す．Utteranceクラスのフィールドの一つ
 * @author kengo
 *
 */
public class SentenceType {
	private UtteranceVerb uv;
	private int rate;

	/**
	 * 発話動詞を返す
	 * @return
	 */
	public UtteranceVerb getUv() {
		return uv;
	}

	protected void setUv(UtteranceVerb uv) {
		this.uv = uv;
	}

	/**
	 * 発話動詞の確証度(%)を返す
	 * @return
	 */
	public int getRate() {
		return rate;
	}
	public void setRate(int rate) {
		this.rate = rate;
	}



}
