/**
 * EstimateMaps.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.HashMap;
import java.util.Map;

import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

/**
 * すべてのプレイヤーの推測とその理由
 * 
 * @author otsuki
 */
class EstimateMaps {

	private Map<Agent, Map<Agent, Estimate>> estimateMaps = new HashMap<>();

	boolean addEstimate(Estimate estimate) {
		Agent estimater = estimate.getEstimater();
		Agent estimated = estimate.getEstimated();
		if (estimater == Content.ANY || estimater == Content.UNSPEC || estimated == Content.ANY || estimated == Content.UNSPEC) {
			return false;
		}
		if (!estimateMaps.containsKey(estimater)) {
			estimateMaps.put(estimater, new HashMap<Agent, Estimate>());
		}
		estimateMaps.get(estimater).put(estimated, estimate);
		return true;
	}

	/**
	 * contentが推測発言なら登録する．理由が付されていればそれも登録する．
	 * 
	 * @param content
	 *            発言
	 * @return 登録の成否
	 */
	boolean addEstimate(Content content) {
		return addEstimate(new Estimate(content));
	}

	/**
	 * 推測を理由を付けて登録する
	 * 
	 * @param estimater
	 *            推測者
	 * @param estimated
	 *            被推測者
	 * @param role
	 *            推測される役職
	 * @param reason
	 *            理由
	 */
	boolean addEstimate(Agent estimater, Agent estimated, Role role, Content reason) {
		return addEstimate(new Estimate(estimater, estimated, role, reason));
	}

	/**
	 * 
	 * @param estimate
	 * @param reason
	 * @return
	 */
	boolean addEstimate(Content estimate, Content reason) {
		if (estimate.getTopic() == Topic.ESTIMATE) {
			if (reason == null) {
				return addEstimate(estimate);
			}
			return addEstimate(new Content(new BecauseContentBuilder(estimate.getSubject(), reason, estimate)));
		}
		return false;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Content getContent(Agent estimater, Agent estimated) {
		return estimateMaps.get(estimater) != null ? estimateMaps.get(estimater).get(estimated).toContent() : null;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Estimate getEstimate(Agent estimater, Agent estimated) {
		return estimateMaps.get(estimater) != null ? estimateMaps.get(estimater).get(estimated) : null;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Content getReason(Agent estimater, Agent estimated) {
		Content content = getContent(estimater, estimated);
		return content != null && content.getOperator() == Operator.BECAUSE ? content.getContentList().get(0) : null;
	}

	/**
	 * 
	 */
	void clear() {
		estimateMaps.entrySet().stream().forEach(e -> e.getValue().clear());
		estimateMaps.clear();
	}
}
