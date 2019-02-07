/**
 * VoteReasonMap.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;

/**
 * すべてのプレイヤーの投票先とその理由
 * 
 * @author otsuki
 */
class VoteReasonMap {

	// 理由マップ
	private Map<Agent, Content> voteReasonMap = new HashMap<>();
	// 得票数マップ
	private Map<Agent, Integer> voteCountMap = new HashMap<>();

	// 得票数をカウント
	private void countVote() {
		voteReasonMap.keySet().stream().map(voter -> getTarget(voter)).distinct().forEach(voted -> {
			voteCountMap.put(voted, (int) voteReasonMap.keySet().stream().filter(a -> getTarget(a) == voted).count());
		});
	}

	/**
	 * 
	 * @return
	 */
	List<Agent> getOrderedList() {
		return voteReasonMap.keySet().stream().map(voter -> getTarget(voter)).distinct().sorted((a1, a2) -> getCount(a2) - getCount(a1)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param voted
	 * @return
	 */
	int getCount(Agent voted) {
		countVote();
		return voteCountMap.get(voted) != null ? voteCountMap.get(voted) : 0;
	}

	/**
	 * 
	 * @param content
	 * @return
	 */
	boolean addVoteReason(Content content) {
		if (content.getTopic() == Topic.VOTE) {
			voteReasonMap.put(content.getSubject(), content);
			return true;
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.VOTE) {
			voteReasonMap.put(content.getContentList().get(1).getSubject(), content);
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param voter
	 * @param voted
	 * @param reason
	 * @return
	 */
	boolean addVoteReason(Agent voter, Agent voted, Content reason) {
		return addVoteReason(new Content(new VoteContentBuilder(voter, voted)), reason);
	}

	/**
	 * 
	 * @param vote
	 * @param reason
	 * @return
	 */
	boolean addVoteReason(Content vote, Content reason) {
		if (vote.getTopic() == Topic.VOTE) {
			if (null == reason) {
				return addVoteReason(vote);
			}
			return addVoteReason(new Content(new BecauseContentBuilder(vote.getSubject(), reason, vote)));
		}
		return false;
	}

	/**
	 * 
	 * @param voter
	 * @return
	 */
	Agent getTarget(Agent voter) {
		Content vote = getVote(voter);
		return vote != null ? vote.getTarget() : Agent.UNSPEC;
	}

	/**
	 * 
	 * @param voter
	 * @return
	 */
	Content getContent(Agent voter) {
		return voteReasonMap.get(voter);
	}

	/**
	 * 
	 * @param voter
	 * @return
	 */
	Content getVote(Agent voter) {
		Content content = getContent(voter);
		if (null == content) {
			return null;
		}
		if (content.getTopic() == Topic.VOTE) {
			return content;
		}
		if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.VOTE) {
			return content.getContentList().get(1);
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
		Content content = getContent(voter);
		if (null == content) {
			return null;
		}
		if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.VOTE && content.getContentList().get(1).getTarget() == voted) {
			return content.getContentList().get(0);
		}
		return null;
	}

	/**
	 * 
	 */
	void clear() {
		voteReasonMap.clear();
	}

}
