package org.aiwolf.client.lib;

/**
 * 動詞のカテゴリー．
 * 例として，占う，霊能する，護衛する という3つの動詞はRESULTというカテゴリーに分類される．
 * @author kengo
 *
 */
public enum Category {

	/**
	 * 役職のカミングアウトに関連する動詞を含むカテゴリー
	 * comingout
	 */
	COMINGOUT,

	/**
	 * 役職の推測に関連する動詞を含むカテゴリー
	 * is
	 */
	ESTIMATE,

	/**
	 * 能力を使った結果に関する動詞を含むカテゴリー
	 * inspected, medium_telled, guarded
	 */
	RESULT,

	/**
	 * 様子見を表す動詞のカテゴリー
	 * skip
	 */
	SKIP,

	/**
	 * もう話すことはないという動詞のカテゴリー
	 * Over
	 */
	OVER

}
