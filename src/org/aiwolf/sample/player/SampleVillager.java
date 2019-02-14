/**
 * SampleVillager.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DayContentBuilder;
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

	@Override
	void chooseVoteCandidate() {
		List<Agent> wolfCandidates = new ArrayList<>();
		for (Judge j : divinationList) {
			Content fakeDivination = new Content(new DayContentBuilder(me, j.getDay(),
					new Content(new DivinedResultContentBuilder(j.getAgent(), j.getTarget(), j.getResult()))));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (j.getTarget() == me && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content iAmVillager = new Content(new ComingoutContentBuilder(me, Role.VILLAGER));
					Content reason = new Content(new AndContentBuilder(me, iAmVillager, fakeDivination));
					estimateMaps.addEstimateReason(me, j.getAgent(), Role.WEREWOLF, reason);
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(j.getTarget()) && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content reason = fakeDivination;
					estimateMaps.addEstimateReason(me, j.getAgent(), Role.WEREWOLF, reason);
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
					Content estimate = estimateMaps.getEstimate(me, voteCandidate);
					if (estimate != null) {
						Content reason = estimateMaps.getReason(me, voteCandidate);
						if (reason != null) {
							enqueueTalk(new Content(new BecauseContentBuilder(me, reason, estimate)));
						} else {
							enqueueTalk(estimate);
						}
						Content request = new Content(new RequestContentBuilder(me, Agent.ANY, new Content(new DivinationContentBuilder(voteCandidate))));
						enqueueTalk(new Content(new BecauseContentBuilder(me, estimate, request)));
					}
					voteReasonMap.addVoteReason(me, voteCandidate, estimate);
				}
			}
		}
	}

	@Override
	public String talk() {
		// TODO 自分への投票を表明しているプレイヤーにやめるように頼む
		// List<Content> contentsList = aliveOthers.stream().filter(a -> voteReasonMap.getTarget(a) == me)
		// .map(a -> {
		// Content notVote = new Content(new NotContentBuilder(new Content(new VoteContentBuilder(me))));
		// return new Content(new RequestContentBuilder(a, notVote));
		// }).collect(Collectors.toList());
		// enqueueTalk(new Content(new AndContentBuilder(contentsList)));
		return super.talk();
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
