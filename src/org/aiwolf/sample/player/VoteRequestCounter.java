/**
 * VoteRequestCounter.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;

/**
 * 投票リクエストカウンタ
 * 
 * @author otsuki
 */
class VoteRequestCounter extends ArrayList<Content> {

	private static final long serialVersionUID = 3810063151532925245L;

	/** リクエスト元と投票先のマップ */
	private Map<Agent, Agent> agentRequestMap = new HashMap<>();

	/** エージェントとそれに対する投票リクエスト数のマップ */
	private Map<Agent, Integer> agentCountMap = new HashMap<>();

	/** リクエスト数の降順に並べたエージェントのリスト */
	private List<Agent> orderedAgentList;

	/** 状態が変化した場合true */
	private boolean isChanged = false;

	// リクエスト数をカウント
	private void count() {
		// 指定されなくなる場合があるので毎回クリア
		agentCountMap.clear();
		List<Agent> votedAgents = agentRequestMap.values().stream().distinct().collect(Collectors.toList());
		for (Agent agent : votedAgents) {
			int count = (int) agentRequestMap.values().stream().filter(a -> a == agent).count();
			agentCountMap.put(agent, count);
		}
	}

	@Override
	public boolean add(Content content) {
		Agent requester = content.getSubject();
		if (content.getOperator() == Operator.REQUEST) {
			Content c = content.getContentList().get(0);
			if (c != null && c.getTopic() == Topic.VOTE) {
				super.add(content);
				Agent voted = c.getTarget();
				if (!agentRequestMap.containsKey(requester) || agentRequestMap.get(requester) != voted) {
					isChanged = true;
					agentRequestMap.put(requester, voted);
					count();
					orderedAgentList = agentCountMap.keySet().stream()
							.sorted((a1, a2) -> getCount(a2) - getCount(a1)).collect(Collectors.toList());
				}
				return true;
			}
		}
		return false;
	}

	Map<Agent, Agent> getRequestMap() {
		return agentRequestMap;
	}

	int getCount(Agent agent) {
		return agentCountMap.containsKey(agent) ? agentCountMap.get(agent) : 0;
	}

	List<Agent> getOrderedAgentList() {
		return orderedAgentList;
	}

	List<Agent> getTopAgentList() {
		if (orderedAgentList != null && !orderedAgentList.isEmpty()) {
			int max = getCount(orderedAgentList.get(0));
			return orderedAgentList.stream().filter(a -> getCount(a) == max).collect(Collectors.toList());
		}
		return null;
	}

	/** 以前呼んだ時から変更があればtrue */
	boolean isChanged() {
		return isChanged && !(isChanged = false);
	}

	@Override
	public void clear() {
		super.clear();
		agentRequestMap.clear();
		agentCountMap.clear();
		if (orderedAgentList != null) {
			orderedAgentList.clear();
		}
	}

}
