/**
 * SampleSeer.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 占い師役エージェントクラス
 */
public final class SampleSeer extends SampleVillager {
	int comingoutDay;
	boolean isCameout;
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Judge> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	Agent possessed;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		divinationQueue.clear();
		myDivinationMap.clear();
		whiteList.clear();
		blackList.clear();
		grayList = new ArrayList<>();
		semiWolves.clear();
		possessedList.clear();
		possessed = Agent.UNSPEC;
	}

	@Override
	public void dayStart() {
		super.dayStart();
		// 占い結果を待ち行列に入れる
		Judge divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			divinationQueue.offer(divination);
			grayList.remove(divination.getTarget());
			if (divination.getResult() == Species.HUMAN) {
				whiteList.add(divination.getTarget());
			} else {
				blackList.add(divination.getTarget());
			}
			myDivinationMap.put(divination.getTarget(), divination);
		}
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = CoContent(me, me, Role.VILLAGER);
		if (isCameout) {
			iAm = CoContent(me, me, Role.SEER);
		}

		// 生存人狼がいれば当然投票
		List<Agent> aliveWolves = new ArrayList<>();
		for (Agent a : blackList) {
			if (isAlive(a)) {
				aliveWolves.add(a);
			}
		}
		// 既定の投票先が生存人狼でない場合投票先を変える
		if (!aliveWolves.isEmpty()) {
			if (!aliveWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveWolves);
				if (canTalk) {
					Content myDivination = DivinedContent(me, voteCandidate, myDivinationMap.get(voteCandidate).getResult());
					Content reason = DayContent(me, myDivinationMap.get(voteCandidate).getDay(), myDivination);
					Content request = RequestContent(me, Agent.ANY, VoteContent(Agent.ANY, voteCandidate));
					enqueueTalk(BecauseContent(me, reason, request));
				}
			}
			return;
		}
		// 確定人狼がいない場合は推測する
		List<Agent> wolfCandidates = new ArrayList<>();
		// 偽占い師
		for (Agent a : aliveOthers) {
			if (comingoutMap.get(a) == Role.SEER) {
				Content heIs = CoContent(a, a, Role.SEER);
				wolfCandidates.add(a);
				if (isCameout) {
					Content reason = AndContent(me, iAm, heIs);
					estimateMaps.addEstimate(me, a, Role.WEREWOLF, reason);
				}
			}
		}
		// 偽霊媒師
		for (Judge j : identList) {
			Content hisIdent = IdentContent(j.getAgent(), j.getTarget(), j.getResult());
			if ((myDivinationMap.containsKey(j.getTarget()) && j.getResult() != myDivinationMap.get(j.getTarget()).getResult())) {
				Agent candidate = j.getAgent();
				if (isAlive(candidate) && !wolfCandidates.contains(candidate)) {
					wolfCandidates.add(candidate);
					if (isCameout) {
						Content myDivination = DivinedContent(me, j.getTarget(), myDivinationMap.get(j.getTarget()).getResult());
						Content reason = AndContent(me, iAm, myDivination, hisIdent);
						estimateMaps.addEstimate(me, candidate, Role.WEREWOLF, reason);
					}
				}
			}
		}
		possessedList.clear();
		semiWolves.clear();
		for (Agent a : wolfCandidates) {
			// 人狼候補なのに人間⇒裏切り者
			if (whiteList.contains(a)) {
				possessedList.add(a);
			} else {
				semiWolves.add(a);
			}
		}
		if (!possessedList.isEmpty()) {
			if (Agent.UNSPEC == possessed || !possessedList.contains(possessed)) {
				possessed = randomSelect(possessedList);
				Content reason1 = estimateMaps.getReason(me, possessed);
				Content reason2 = DivinedContent(me, possessed, Species.HUMAN);
				Content reason = AndContent(me, reason1, reason2);
				Content estimate = EstimateContent(me, possessed, Role.POSSESSED);
				enqueueTalk(BecauseContent(me, reason, estimate));
			}
		}
		if (!semiWolves.isEmpty()) {
			if (!semiWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(semiWolves);
				Estimate estimate = estimateMaps.getEstimate(me, voteCandidate);
				// 以前の投票先から変わる場合，新たに推測発言をする
				if (canTalk) {
					if (estimate != null) {
						enqueueTalk(estimate.toContent());
					}
					voteMap.addVoteReason(me, voteCandidate, estimate.getEstimateContent());
				}
			}
		}
		// 人狼候補がいない場合はグレイからランダム
		else {
			if (!grayList.isEmpty()) {
				if (!grayList.contains(voteCandidate)) {
					voteCandidate = randomSelect(grayList);
				}
			}
			// グレイもいない場合ランダム
			else {
				if (!aliveOthers.contains(voteCandidate)) {
					voteCandidate = randomSelect(aliveOthers);
				}
			}
		}
	}

	@Override
	public String talk() {
		// カミングアウトする日になったら，あるいは占い結果が人狼だったら
		// あるいは占い師カミングアウトが出たらカミングアウト
		if (!isCameout && (day >= comingoutDay || (!divinationQueue.isEmpty() && divinationQueue.peekLast().getResult() == Species.WEREWOLF) || isCo(Role.SEER))) {
			enqueueTalk(CoContent(me, me, Role.SEER));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの占い結果をすべて公開
		if (isCameout) {
			while (!divinationQueue.isEmpty()) {
				Judge divination = divinationQueue.poll();
				enqueueTalk(DivinedContent(me, divination.getTarget(), divination.getResult()));
			}
		}
		return super.talk();
	}

	@Override
	public Agent divine() {
		// 人狼候補がいればそれらからランダムに占う
		if (!semiWolves.isEmpty()) {
			return randomSelect(semiWolves);
		}
		// 人狼候補がいない場合，まだ占っていない生存者からランダムに占う
		List<Agent> candidates = new ArrayList<>();
		for (Agent a : aliveOthers) {
			if (!myDivinationMap.containsKey(a)) {
				candidates.add(a);
			}
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return randomSelect(candidates);
	}

}
