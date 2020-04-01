/**
 * SampleBodyguard.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狩人役エージェントクラス
 */
public final class SampleBodyguard extends SampleBasePlayer {

	/** 人狼候補リスト */
	private List<Agent> wolfCandidates = new ArrayList<>();

	/** 護衛したエージェント */
	private Agent guardedAgent;

	/** 護衛に成功したエージェントのセット */
	private Set<Agent> guardedAgentSet = new HashSet<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		wolfCandidates.clear();
		guardedAgent = null;
		guardedAgentSet.clear();
	}

	@Override
	public void dayStart() {
		super.dayStart();
		// 襲撃失敗の場合，護衛したエージェントを登録
		if (currentGameInfo.getLastDeadAgentList().isEmpty()) {
			guardedAgentSet.add(guardedAgent);
		}
	}

	@Override
	void chooseVoteCandidate() {
		wolfCandidates.clear();

		// 村人目線での人狼候補決定アルゴリズム
		for (Judge divination : divinationList) {
			// まず占い結果から人狼候補を見つける
			Agent he = divination.getAgent();
			Species result = divination.getResult();
			if (!isAlive(he) || wolfCandidates.contains(he) || result == Species.HUMAN) {
				continue;
			}
			Agent target = divination.getTarget();
			if (target == me) {
				// 自分を人狼と判定した自称占い師は人狼か裏切り者なので投票先候補に追加
				wolfCandidates.add(he);
				Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
				Content reason = andContent(me, coContent(me, me, Role.VILLAGER), hisDivination);
				estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
			} else if (isKilled(target)) {
				// 殺されたエージェントを人狼と判定した自称占い師は人狼か裏切り者なので投票先候補に追加
				wolfCandidates.add(he);
				Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
				Content reason = andContent(me, attackedContent(Content.ANY, target), hisDivination);
				estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
			}
		}

		if (!wolfCandidates.isEmpty()) {
			// 見つかった場合
			if (!wolfCandidates.contains(voteCandidate)) {
				// 新しい投票先の場合，推測発言をする
				voteCandidate = randomSelect(wolfCandidates);
				Estimate estimate = estimateReasonMap.getEstimate(me, voteCandidate);
				if (estimate != null) {
					enqueueTalk(estimate.toContent());
					voteReasonMap.put(me, voteCandidate, estimate.getEstimateContent());
				}
			}
		} else {
			// 見つからなかった場合ランダム
			if (voteCandidate == null || !isAlive(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers.stream()
						.filter(a -> !guardedAgentSet.contains(a)).collect(Collectors.toList()));
			}
		}
	}

	@Override
	void chooseFinalVoteCandidate() {
		if (!isRevote) {
			// 人狼候補が見つけられなかった場合，初回投票では投票リクエストに応じる
			if (wolfCandidates.isEmpty()) {
				voteCandidate = randomSelect(voteRequestCounter.getRequestMap().values().stream()
						.filter(a -> a != me && !guardedAgentSet.contains(a)).collect(Collectors.toList()));
				if (voteCandidate == null || !isAlive(voteCandidate)) {
					voteCandidate = randomSelect(aliveOthers);
				}
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

}
