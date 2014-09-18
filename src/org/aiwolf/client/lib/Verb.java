package org.aiwolf.client.lib;

/**
 * 人狼プロトコルにおける動詞を表す．
 * @author kengo
 *
 */
public enum Verb {
	/**
	 * 「AgentはRoleだと思う．」という発話の動詞．
	 * 人狼プロトコルでは"Agent01 wolf"というように動詞が無い文で表すが，便宜上この文章の動詞を"is"として扱う．
	 * 動詞のカテゴリーはESTIMATE
	 */
	is(Category.ESTIMATE),//"Guen wolf" 等を便宜上"Guen is wolf"という風に扱う．


	/**
	 * 「AgentがRoleだとカミングアウトする．」という発話の動詞．
	 * 例文："Agent01 comingout wolf"
	 * 動詞のカテゴリーはCOMINGOUT
	 */
	comingout(Category.COMINGOUT),

	/**
	 * 「AgentがSpecies(人間or人狼)だと占われた．」という発話の動詞．
	 * 例文："Agent01 inspected Human"
	 * 動詞のカテゴリーはRESULT
	 */
	inspected(Category.RESULT),

	/**
	 * 「AgentがSpecies(人間or人狼)だと霊能された．」という発話の動詞．
	 * 例文："Agent01 medium_telled Werewolf"
	 * 動詞のカテゴリーはRESULT
	 */
	medium_telled(Category.RESULT),

	/**
	 * 「Agentが護衛された．」という発話の動詞．
	 * 例文："Agent01 guarded"
	 * 動詞のカテゴリーはRESULT
	 */
	guarded(Category.RESULT);


	//attack(Category.)

	private Category category;

	private Verb(Category c){
		category = c;
	}

	/**
	 * StringからVerb型へ変換する
	 * @param input
	 * @return
	 */
	public static Verb fromString(String input){
		for(Verb v: Verb.values()){
			if(input.equalsIgnoreCase(v.name())){
				return v;
			}
		}
		return is;
	}

	/**
	 * 対象の動詞のカテゴリーを取得する
	 * @return
	 */
	public Category getCategory(){
		return category;
	}

}
