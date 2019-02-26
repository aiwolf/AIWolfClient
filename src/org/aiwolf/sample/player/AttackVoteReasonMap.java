/**
 * AttackVoteReasonMap.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;

/**
 * 各プレイヤーが宣言した襲撃投票先とその理由
 * 
 * @author otsuki
 */
class AttackVoteReasonMap extends VoteReasonMap {

	/**
	 * 
	 */
	private static final long serialVersionUID = 749687176879894738L;

	@Override
	boolean put(Content vote, Content reason) {
		if (vote.getTopic() == Topic.ATTACK) {
			Agent voter = vote.getSubject();
			Agent voted = vote.getTarget();
			return put(voter, voted, reason);
		}
		return false;
	}

	@Override
	boolean put(Content content) {
		if (content.getTopic() == Topic.ATTACK) {
			return put(content, null);
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.ATTACK) {
			return put(content.getContentList().get(1), content.getContentList().get(0));
		}
		return false;
	}

}
