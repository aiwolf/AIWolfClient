/**
 * SampleBodyguard.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.List;
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

	/** 護衛に成功したエージェントのリスト */
	private List<Agent> guardedAgentList = new ArrayList<>();

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
			if (!guardedAgentList.contains(guardedAgent)) {
				guardedAgentList.add(guardedAgent);
			}
		}
	}

	@Override
	void chooseVoteCandidate() {
		wolfCandidates.clear();
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
				// 初回は護衛に成功したエージェント以外からランダム
				List<Agent> candidates = aliveOthers.stream().filter(a -> !guardedAgentList.contains(a)).collect(Collectors.toList());
				if (!candidates.isEmpty()) {
					if (!candidates.contains(voteCandidate)) {
						voteCandidate = randomSelect(candidates);
					}
				}
			} else {
				// 再投票の場合は護衛に成功したエージェント以外の前回最多得票に入れる
				VoteReasonMap vrmap = new VoteReasonMap();
				for (Vote v : currentGameInfo.getLatestVoteList()) {
					vrmap.put(v.getAgent(), v.getTarget(), null);
				}
				List<Agent> candidates = vrmap.getOrderedList();
				candidates.remove(me);
				candidates.removeAll(guardedAgentList);
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
