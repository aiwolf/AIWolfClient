/**
 * SampleVillager.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

/**
 * 村人役エージェントクラス
 * 
 * @author otsuki
 */
public class SampleVillager extends SampleBasePlayer {

	/** 人狼候補リスト */
	List<Agent> wolfCandidates = new ArrayList<>();

	@Override
	void chooseVoteCandidate() {
		wolfCandidates.clear();
		for (Judge j : divinationList) {
			// 自分あるいは殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補とする
			if (j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))) {
				Agent candidate = j.getAgent();
				if (isAlive(candidate) && !wolfCandidates.contains(candidate)) {
					wolfCandidates.add(candidate);
					Content reason = new Content(new DivinedResultContentBuilder(candidate, j.getTarget(), Species.WEREWOLF));
					estimateReasonMaps.addEstimateReason(me, candidate, Role.WEREWOLF, reason);
				}
			}
		}
		// 候補がいない場合はランダム
		if (wolfCandidates.isEmpty()) {
			if (!aliveOthers.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
			}
		} else {
			if (!wolfCandidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(wolfCandidates);
				// 以前の投票先から変わる場合，新たに推測発言と占い要請をする
				if (canTalk) {
					Content estimate = estimateReasonMaps.getEstimate(me, voteCandidate);
					if (estimate != null) {
						Content reason = estimateReasonMaps.getReason(me, voteCandidate);
						if (reason != null) {
							enqueueTalk(new Content(new BecauseContentBuilder(reason, estimate)));
						} else {
							enqueueTalk(estimate);
						}
						Content request = new Content(new RequestContentBuilder(Agent.ANY, new Content(new DivinationContentBuilder(voteCandidate))));
						enqueueTalk(new Content(new BecauseContentBuilder(estimate, request)));
					}
					voteReasonMap.addVoteReason(me, voteCandidate, estimate);
				}
			}
		}
	}

	@Override
	public String whisper() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Agent attack() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
