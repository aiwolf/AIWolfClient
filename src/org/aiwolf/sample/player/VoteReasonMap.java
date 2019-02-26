/**
 * VoteReasonMap.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;

/**
 * 各プレイヤーが宣言した投票先とその理由
 * 
 * @author otsuki
 */
class VoteReasonMap extends HashMap<Agent, Entry<Agent, Content>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5951357442022947347L;

	// 得票数マップ
	private Map<Agent, Integer> voteCountMap = new HashMap<>();

	// 得票数をカウント
	private void countVote() {
		keySet().stream().map(voter -> get(voter).getKey()).distinct().forEach(voted -> {
			voteCountMap.put(voted, (int) keySet().stream().filter(a -> get(a).getKey() == voted).count());
		});
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @param reason
	 * @return
	 */
	boolean put(Agent voter, Agent voted, Content reason) {
		if (voter == null || voted == null) {
			return false;
		}
		put(voter, new SimpleEntry<Agent, Content>(voted, reason));
		countVote();
		return true;
	}

	/**
	 * 
	 * @param vote
	 * @param reason
	 * @return
	 */
	boolean put(Content vote, Content reason) {
		if (vote.getTopic() == Topic.VOTE) {
			Agent voter = vote.getSubject();
			Agent voted = vote.getTarget();
			return put(voter, voted, reason);
		}
		return false;
	}

	/**
	 * 
	 * @param content
	 * @return
	 */
	boolean put(Content content) {
		if (content.getTopic() == Topic.VOTE) {
			return put(content, null);
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.VOTE) {
			return put(content.getContentList().get(1), content.getContentList().get(0));
		}
		return false;
	}

	/**
	 * 
	 * @param voted
	 * @return
	 */
	int getVoteCount(Agent voted) {
		return voteCountMap.get(voted) != null ? voteCountMap.get(voted) : 0;
	}

	/**
	 * 
	 * @return
	 */
	List<Agent> getOrderedList() {
		return voteCountMap.keySet().stream()
				.sorted((a1, a2) -> getVoteCount(a2) - getVoteCount(a1)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param voter
	 * @return
	 */
	Agent getTarget(Agent voter) {
		if (containsKey(voter)) {
			return get(voter).getKey();
		}
		return null;
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @return
	 */
	Content getReason(Agent voter) {
		if (containsKey(voter)) {
			return get(voter).getValue();
		}
		return null;
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @return
	 */
	Content getReason(Agent voter, Agent voted) {
		if (getTarget(voter) == voted) {
			return getReason(voter);
		}
		return null;
	}

	@Override
	public void clear() {
		super.clear();
		voteCountMap.clear();
	}

}
