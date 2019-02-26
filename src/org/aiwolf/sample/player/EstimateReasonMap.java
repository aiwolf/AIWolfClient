/**
 * EstimateReasonMap.java
 * 
 * Copyright (c) 2019 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.common.data.Agent;

/**
 * 各プレイヤーの推測とその理由
 * 
 * @author otsuki
 */
class EstimateReasonMap extends HashMap<Agent, Map<Agent, Estimate>> {

	private static final long serialVersionUID = -7390734706630424321L;

	boolean put(Estimate estimate) {
		if (estimate == null) {
			return false;
		}
		Agent estimater = estimate.getEstimater();
		Agent estimated = estimate.getEstimated();
		if (estimater == null || estimated == null) {
			return false;
		}
		if (!containsKey(estimater)) {
			put(estimater, new HashMap<Agent, Estimate>() {
				private static final long serialVersionUID = 1L;
				{
					put(estimated, estimate);
				}
			});
		}
		return true;
	}

	/**
	 * contentが推測発言なら登録する．理由が付されていればそれも登録する．
	 * 
	 * @param content
	 *            発言
	 * @return 登録の成否
	 */
	boolean put(Content content) {
		List<Estimate> estimates = Estimate.parseContent(content);
		if (estimates == null || estimates.isEmpty()) {
			return false;
		}
		for (Estimate e : estimates) {
			if (!put(e)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Content getContent(Agent estimater, Agent estimated) {
		Estimate estimate = getEstimate(estimater, estimated);
		return estimate != null ? estimate.toContent() : null;
	}

	/**
	 * 
	 * @param estimater
	 * @param estimated
	 * @return
	 */
	Estimate getEstimate(Agent estimater, Agent estimated) {
		return get(estimater) != null ? get(estimater).get(estimated) : null;
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

}
