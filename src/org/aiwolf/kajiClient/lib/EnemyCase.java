package org.aiwolf.kajiClient.lib;

/**
 * 敵プレイヤーの状態．白確定or黒確定or灰色
 * @author kengo
 *
 */
public enum EnemyCase {
	/**
	 * 白確の敵．狂人
	 */
	white,

	/**
	 * 黒確の敵．人狼
	 */
	black,

	/**
	 * まだ狂人か人狼か分からない敵
	 */
	gray

}
