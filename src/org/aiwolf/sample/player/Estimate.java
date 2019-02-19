/**
 * Estimate.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;

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

	Estimate(Agent estimater, Agent estimated, Role role) {
		this.estimater = estimater;
		this.estimated = estimated;
		addRole(role);
	}

	Estimate(Agent estimater, Agent estimated, Role role, Content reason) {
		this(estimater, estimated, role);
		addReason(reason);
	}

	Estimate(Content content) {
		if (content.getTopic() == Topic.ESTIMATE) {
			estimater = content.getSubject();
			estimated = content.getTarget();
			addRole(content.getRole());
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.ESTIMATE) {
			Content estimate = content.getContentList().get(1);
			Content reason = content.getContentList().get(0);
			estimater = estimate.getSubject();
			estimated = estimate.getTarget();
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
		Content estimate = getEstimateContent();
		if (estimate == null) {
			return null;
		}
		Content reason = getReasonContent();
		if (reason == null) {
			return estimate;
		}
		return new Content(new BecauseContentBuilder(estimater, reason, estimate));
	}

	Agent getEstimater() {
		return estimater;
	}

	Agent getEstimated() {
		return estimated;
	}

	Content getEstimateContent() {
		Content[] estimates = roles.stream().map(r -> new Content(new EstimateContentBuilder(estimater, estimated, r))).toArray(size -> new Content[size]);
		if (estimates.length == 0) {
			return null;
		}
		if (estimates.length == 1) {
			return estimates[0];
		}
		return new Content(new XorContentBuilder(estimater, estimates[0], estimates[1])); // 3つ目以降は無視
	}

	Content getReasonContent() {
		if (reasons.isEmpty()) {
			return null;
		}
		if (reasons.size() == 1) {
			return reasons.get(0);
		}
		return new Content(new AndContentBuilder(estimater, reasons));
	}

	@Override
	public String toString() {
		return toContent().getText();
	}

}
