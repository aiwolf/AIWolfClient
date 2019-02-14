/**
 * SampleBodyguard.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狩人役エージェントクラス
 */
public final class SampleBodyguard extends SampleVillager {

	/** 人狼候補リスト */
	List<Agent> wolfCandidates = new ArrayList<>();

	/** 護衛したエージェント */
	Agent guardedAgent;

	/** 護衛に成功したエージェントのリスト */
	List<Agent> guardedAgentList = new ArrayList<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		wolfCandidates.clear();
		guardedAgent = null;
		guardedAgentList.clear();
	}

	@Override
	public void dayStart() {
		super.dayStart();
		// 襲撃失敗の場合，護衛したエージェントを登録
		if (currentGameInfo.getLastDeadAgentList().isEmpty()) {
			guardedAgentList.add(guardedAgent);
		}
	}

	@Override
	void chooseVoteCandidate() {
		wolfCandidates.clear();
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
		// 候補がいない場合は護衛に成功した者以外からランダム
		if (wolfCandidates.isEmpty()) {
			List<Agent> candidates = aliveOthers.stream().filter(a -> !guardedAgentList.contains(a)).collect(Collectors.toList());
			if (!candidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(candidates);
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
	public Agent guard() {
		Agent guardCandidate = null;
		// 前日の護衛が成功しているようなら同じエージェントを護衛
		if (guardedAgent != null && isAlive(guardedAgent) && currentGameInfo.getLastDeadAgentList().isEmpty()) {
			guardCandidate = guardedAgent;
		}
		// 新しい護衛先の選定
		else {
			// 占い師をカミングアウトしていて，かつ人狼候補になっていないエージェントを探す
			List<Agent> candidates = new ArrayList<>();
			for (Agent agent : aliveOthers) {
				if (comingoutMap.get(agent) == Role.SEER && !wolfCandidates.contains(agent)) {
					candidates.add(agent);
				}
			}
			// 見つからなければ霊媒師をカミングアウトしていて，かつ人狼候補になっていないエージェントを探す
			if (candidates.isEmpty()) {
				for (Agent agent : aliveOthers) {
					if (comingoutMap.get(agent) == Role.MEDIUM && !wolfCandidates.contains(agent)) {
						candidates.add(agent);
					}
				}
			}
			// それでも見つからなければ自分と人狼候補以外から護衛
			if (candidates.isEmpty()) {
				for (Agent agent : aliveOthers) {
					if (!wolfCandidates.contains(agent)) {
						candidates.add(agent);
					}
				}
			}
			// それでもいなければ自分以外から護衛
			if (candidates.isEmpty()) {
				candidates.addAll(aliveOthers);
			}
			// 護衛候補からランダムに護衛
			guardCandidate = randomSelect(candidates);
		}
		guardedAgent = guardCandidate;
		return guardCandidate;
	}

}
