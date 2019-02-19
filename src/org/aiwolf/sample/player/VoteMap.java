/**
 * VoteMap.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;

/**
 * すべてのプレイヤーが宣言した投票先とその理由
 * 
 * @author otsuki
 */
class VoteMap {

	// 投票先マップ
	private Map<Agent, Agent> voteMap = new HashMap<>();

	// 理由マップ
	private Map<Agent, Content> voteReasonMap = new HashMap<>();

	// 得票数マップ
	private Map<Agent, Integer> voteCountMap = new HashMap<>();

	// 得票数をカウント
	private void countVote() {
		voteMap.keySet().stream().map(voter -> getTarget(voter)).distinct().forEach(voted -> {
			voteCountMap.put(voted, (int) voteMap.keySet().stream().filter(a -> getTarget(a) == voted).count());
		});
	}

	boolean isEmpty() {
		return voteMap.isEmpty();
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @param reason
	 * @return
	 */
	boolean addVoteReason(Agent voter, Agent voted, Content reason) {
		if (voter == Content.ANY || voter == Content.UNSPEC || voted == Content.ANY || voted == Content.UNSPEC) {
			return false;
		}
		voteMap.put(voter, voted);
		countVote();
		if (reason != null) {
			voteReasonMap.put(voter, reason);
		}
		return true;
	}

	/**
	 * 
	 * @param vote
	 * @param reason
	 * @return
	 */
	boolean addVoteReason(Content vote, Content reason) {
		if (vote.getTopic() == Topic.VOTE) {
			Agent voter = vote.getSubject();
			Agent voted = vote.getTarget();
			return addVoteReason(voter, voted, reason);
		}
		return false;
	}

	/**
	 * 
	 * @param content
	 * @return
	 */
	boolean addVoteReason(Content content) {
		if (content.getTopic() == Topic.VOTE) {
			return addVoteReason(content, null);
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.VOTE) {
			return addVoteReason(content.getContentList().get(1), content.getContentList().get(0));
		}
		return false;
	}

	/**
	 * 
	 * @param voted
	 * @return
	 */
	int getCount(Agent voted) {
		return voteCountMap.get(voted) != null ? voteCountMap.get(voted) : 0;
	}

	/**
	 * 
	 * @return
	 */
	List<Agent> getOrderedList() {
		return voteCountMap.keySet().stream().map(voter -> getTarget(voter)).distinct().sorted((a1, a2) -> getCount(a2) - getCount(a1)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param voter
	 * @return
	 */
	Agent getTarget(Agent voter) {
		if (voteMap.containsKey(voter)) {
			return voteMap.get(voter);
		}
		return Content.UNSPEC;
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @return
	 */
	Content getReason(Agent voter) {
		if (voteReasonMap.get(voter) != null) {
			return voteReasonMap.get(voter);
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
		if (getTarget(voter) == voted && voteReasonMap.get(voter) != null) {
			return voteReasonMap.get(voter);
		}
		return null;
	}

	/**
	 * 
	 */
	void clear() {
		voteMap.clear();
		voteReasonMap.clear();
		voteCountMap.clear();
	}

}
