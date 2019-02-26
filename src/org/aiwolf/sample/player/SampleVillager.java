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
import org.aiwolf.common.data.Vote;

/**
 * 村人役エージェントクラス
 * 
 * @author otsuki
 */
public final class SampleVillager extends SampleBasePlayer {

	@Override
	void chooseVoteCandidate() {
		List<Agent> wolfCandidates = new ArrayList<>();
		// 村人陣営共通の人狼候補決定アルゴリズム
		for (Judge divination : divinationList) {
			Agent he = divination.getAgent();
			Agent target = divination.getTarget();
			Species result = divination.getResult();
			Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (target == me && result == Species.WEREWOLF) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Content reason = andContent(me, coContent(me, me, Role.VILLAGER), hisDivination);
					estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(target) && result == Species.WEREWOLF) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Content reason = andContent(me, attackedContent(Content.ANY, target), hisDivination);
					estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
				}
			}
		}
		// 候補がいない場合
		if (wolfCandidates.isEmpty()) {
			if (!isRevote) {
				// 初回投票はランダム
				if (!aliveOthers.contains(voteCandidate)) {
					voteCandidate = randomSelect(aliveOthers);
				}
			} else {
				// 再投票の場合は自分以外の前回最多得票に入れる
				VoteReasonMap vrmap = new VoteReasonMap();
				for (Vote v : currentGameInfo.getLatestVoteList()) {
					vrmap.put(v.getAgent(), v.getTarget(), null);
				}
				List<Agent> candidates = vrmap.getOrderedList();
				candidates.remove(me);
				if (candidates.isEmpty()) {
					voteCandidate = randomSelect(aliveOthers);
				} else {
					voteCandidate = candidates.get(0);
				}
			}
		} else {
			if (!wolfCandidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(wolfCandidates);
				Estimate estimate = estimateReasonMap.getEstimate(me, voteCandidate);
				// 以前の投票先から変わる場合，新たに推測発言をする
				if (estimate != null) {
					enqueueTalk(estimate.toContent());
					voteReasonMap.put(me, voteCandidate, estimate.getEstimateContent());
				} else {
					voteReasonMap.put(me, voteCandidate, null);
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
