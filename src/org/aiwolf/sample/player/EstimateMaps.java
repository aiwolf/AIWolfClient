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
import org.aiwolf.client.lib.EstimateContentBuilder;
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

	private Map<Agent, Map<Agent, Content>> estimateReasonMaps = new HashMap<>();

	private void putContent(Agent estimater, Agent estimated, Content content) {
		if (!estimateReasonMaps.containsKey(estimater)) {
			estimateReasonMaps.put(estimater, new HashMap<Agent, Content>());
		}
		estimateReasonMaps.get(estimater).put(estimated, content);
	}

	/**
	 * contentが推測発言なら登録する．理由が付されていればそれも登録する．
	 * 
	 * @param content
	 *            発言
	 * @return 登録の成否
	 */
	boolean addEstimateReason(Content content) {
		if (content.getTopic() == Topic.ESTIMATE) {
			putContent(content.getSubject(), content.getTarget(), content);
			return true;
		} else if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.ESTIMATE) {
			putContent(content.getContentList().get(1).getSubject(), content.getContentList().get(1).getTarget(), content);
			return true;
		}
		return false;
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
	 * @return 登録の成否
	 */
	boolean addEstimateReason(Agent estimater, Agent estimated, Role role, Content reason) {
		return addEstimateReason(new Content(new EstimateContentBuilder(estimater, estimated, role)), reason);
	}

	/**
	 * 
	 * @param estimate
	 * @param reason
	 * @return
	 */
	boolean addEstimateReason(Content estimate, Content reason) {
		if (estimate.getTopic() == Topic.ESTIMATE) {
			if (null == reason) {
				return addEstimateReason(estimate);
			}
			return addEstimateReason(new Content(new BecauseContentBuilder(estimate.getSubject(), reason, estimate)));
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
		return estimateReasonMaps.get(estimater) != null ? estimateReasonMaps.get(estimater).get(estimated) : null;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Content getEstimate(Agent estimater, Agent estimated) {
		Content content = getContent(estimater, estimated);
		if (null == content) {
			return null;
		}
		if (content.getTopic() == Topic.ESTIMATE) {
			return content;
		}
		if (content.getOperator() == Operator.BECAUSE && content.getContentList().get(1).getTopic() == Topic.ESTIMATE) {
			return content.getContentList().get(1);
		}
		return null;
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
		estimateReasonMaps.entrySet().stream().forEach(e -> e.getValue().clear());
		estimateReasonMaps.clear();
	}
}
