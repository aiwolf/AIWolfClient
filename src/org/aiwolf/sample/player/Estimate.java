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
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
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

	private void setEstimater(Agent estimater) {
		this.estimater = estimater;
	}

	private void setEstimated(Agent estimated) {
		this.estimated = Agent.UNSPEC == estimated ? Agent.ANY : estimated;
	}

	Estimate(Agent estimater, Agent estimated, Role role) {
		setEstimater(estimater);
		setEstimated(estimated);
		roles.add(role);
	}

	Estimate(Agent estimater, Agent estimated, Role role, Content reason) {
		this(estimater, estimated, role);
		addReason(reason);
	}

	Estimate(Content content) {
		if (content.getTopic() == Topic.ESTIMATE) {
			setEstimater(content.getSubject());
			setEstimated(content.getTarget());
			addRole(content.getRole());
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.ESTIMATE) {
			Content estimate = content.getContentList().get(1);
			Content reason = content.getContentList().get(0);
			setEstimater(estimate.getSubject());
			setEstimated(estimate.getTarget());
			addRole(estimate.getRole());
			addReason(reason);
		}

	}

	void addRole(Role role) {
		if (!roles.contains(role)) {
			roles.add(role);
		}
	}

	void addReason(Content reason) {
		if (!reasons.contains(reason)) {
			reasons.add(reason);
		}
	}

	boolean hasRole(Role role) {
		return roles.contains(role);
	}

	boolean hasReason(Content reason) {
		return reasons.contains(reason);
	}

	Content toContent() {
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

	Agent getEstimater() {
		return estimater;
	}

	Agent getEstimated() {
		return estimated;
	}

}
