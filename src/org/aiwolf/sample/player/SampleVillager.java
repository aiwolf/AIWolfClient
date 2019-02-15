/**
 * SampleVillager.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

/**
 * 村人役エージェントクラス
 * 
 * @author otsuki
 */
public final class SampleVillager extends SampleBasePlayer {

	@Override
	void chooseVoteCandidate() {
		List<Agent> wolfCandidates = new ArrayList<>();
		for (Judge j : divinationList) {
			Content dayDivination = dayContent(me, j.getDay(), divinedContent(j.getAgent(), j.getTarget(), j.getResult()));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (j.getTarget() == me && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content iAmVillager = coContent(me, me, Role.VILLAGER);
					Content reason = andContent(me, iAmVillager, dayDivination);
					Estimate estimate = new Estimate(me, j.getAgent(), Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(j.getTarget()) && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content reason = andContent(me, attackedContent(Agent.ANY, j.getTarget()), dayDivination);
					Estimate estimate = new Estimate(me, j.getAgent(), Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
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
				Estimate estimate = estimateMaps.getEstimate(me, voteCandidate);
				// 以前の投票先から変わる場合，新たに推測発言をする
				if (canTalk) {
					enqueueTalk(estimate.toContent());
				}
				voteMap.addVoteReason(me, voteCandidate, estimate.getEstimateContent());
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
