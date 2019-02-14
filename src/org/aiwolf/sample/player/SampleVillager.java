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
public class SampleVillager extends SampleBasePlayer {

	@Override
	void chooseVoteCandidate() {
		List<Agent> wolfCandidates = new ArrayList<>();
		for (Judge j : divinationList) {
			Content fakeDivination = DayContent(me, j.getDay(), DivinedContent(j.getAgent(), j.getTarget(), j.getResult()));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (j.getTarget() == me && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content iAmVillager = CoContent(me, me, Role.VILLAGER);
					Content reason = AndContent(me, iAmVillager, fakeDivination);
					estimateMaps.addEstimate(me, j.getAgent(), Role.WEREWOLF, reason);
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(j.getTarget()) && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content reason = fakeDivination;
					estimateMaps.addEstimate(me, j.getAgent(), Role.WEREWOLF, reason);
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
				// 以前の投票先から変わる場合，新たに推測発言と占い要請をする
				if (canTalk) {
					enqueueTalk(estimate.toContent());
					Content request = RequestContent(me, Agent.ANY, DivinationContent(Agent.ANY, voteCandidate));
					enqueueTalk(BecauseContent(me, estimate.getEstimateContent(), request));
				}
				voteReasonMap.addVoteReason(me, voteCandidate, estimate.getEstimateContent());
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
