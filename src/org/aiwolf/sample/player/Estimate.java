/**
 * Estimate.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.XorContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

/**
 * @author otsuki
 *
 */
class Estimate {

	private Agent estimater;
	private Agent estimated;
	private List<Role> roles = new ArrayList<>();
	private List<Content> reasons = new ArrayList<>();

	public Estimate(Agent estimater, Agent estimated, Role role) {
		this.estimater = estimater;
		this.estimated = Agent.UNSPEC == estimated ? Agent.ANY : estimated;
		roles.add(role);
	}

	public Estimate(Agent estimater, Agent estimated, Role role, Content reason) {
		this(estimater, estimated, role);
		addReason(reason);
	}

	public void addRole(Role role) {
		if (!roles.contains(role)) {
			roles.add(role);
		}
	}

	public void addReason(Content reason) {
		if (!reasons.contains(reason)) {
			reasons.add(reason);
		}
	}

	public boolean hasRole(Role role) {
		return roles.contains(role);
	}

	public boolean hasReason(Content reason) {
		return reasons.contains(reason);
	}

	public Content toContent() {
		List<Content> estimateList = roles.stream().map(r -> new Content(new EstimateContentBuilder(estimater, estimated, r))).collect(Collectors.toList());
		if (estimateList.isEmpty()) {
			return null;
		}
		Content estimate;
		if (estimateList.size() == 1) {
			estimate = estimateList.get(0);
		} else {
			estimate = new Content(new XorContentBuilder(estimater, estimateList.get(0), estimateList.get(1))); // 3つ目以降は無視
		}
		if (reasons.isEmpty()) {
			return estimate;
		}
		Content reason;
		if (reasons.size() == 1) {
			reason = reasons.get(0);
		} else {
			reason = new Content(new AndContentBuilder(estimater, reasons));
		}
		return new Content(new BecauseContentBuilder(estimater, reason, estimate));
	}

}
