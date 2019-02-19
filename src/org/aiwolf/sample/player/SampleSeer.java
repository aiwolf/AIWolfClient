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
import java.util.stream.Collectors;

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
public final class SampleSeer extends SampleBasePlayer {
	int comingoutDay;
	boolean isCameout;
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Judge> myDivinationMap = new HashMap<>();
	List<Agent> wolfCandidates = new ArrayList<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	Agent possessed;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		divinationQueue.clear();
		myDivinationMap.clear();
		wolfCandidates.clear();
		whiteList.clear();
		blackList.clear();
		grayList.clear();
		possessedList.clear();
		possessed = Content.UNSPEC;
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
		Content iAm = isCameout ? coContent(me, me, Role.SEER) : coContent(me, me, Role.VILLAGER);

		// 生存人狼がいれば当然投票
		List<Agent> aliveWolves = blackList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
		// 既定の投票先が生存人狼でない場合投票先を変える
		if (!aliveWolves.isEmpty()) {
			if (!aliveWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveWolves);
				if (canTalk) {
					Content myDivination = divinedContent(me, voteCandidate, myDivinationMap.get(voteCandidate).getResult());
					Content vote = voteContent(Content.ANY, voteCandidate);
					Content reason = dayContent(me, myDivinationMap.get(voteCandidate).getDay(), myDivination);
					Content request = requestContent(me, Content.ANY, vote);
					enqueueTalk(becauseContent(me, reason, request));
					enqueueTalk(inquiryContent(me, Content.ANY, vote));
				}
			}
			return;
		}

		// 確定人狼がいない場合は推測する
		wolfCandidates.clear();
		;
		// 偽占い師
		for (Agent a : aliveOthers) {
			if (comingoutMap.get(a) == Role.SEER) {
				wolfCandidates.add(a);
				Estimate estimate = new Estimate(me, a, Role.WEREWOLF);
				estimate.addRole(Role.POSSESSED);
				if (isCameout) {
					Content heIs = coContent(a, a, Role.SEER);
					Content reason = andContent(me, iAm, heIs);
					estimate.addReason(reason);
				}
				estimateMaps.addEstimate(estimate);
			}
		}
		// 偽霊媒師
		for (Judge j : identList) {
			Agent he = j.getAgent();
			Agent target = j.getTarget();
			Species result = j.getResult();
			Content hisDayIdent = dayContent(me, j.getDay(), identContent(he, target, result));
			Judge myJudge = myDivinationMap.get(target);
			if ((myJudge != null && result != myJudge.getResult())) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Estimate estimate = new Estimate(me, he, Role.WEREWOLF);
					estimate.addRole(Role.POSSESSED);
					if (isCameout) {
						Content myDayDivination = dayContent(me, myJudge.getDay(), divinedContent(me, myJudge.getTarget(), myJudge.getResult()));
						Content reason = andContent(me, myDayDivination, hisDayIdent);
						estimate.addReason(reason);
					}
					estimateMaps.addEstimate(estimate);
				}
			}
		}
		// 裏切り者
		possessedList.clear();
		for (Agent a : wolfCandidates) {
			// 人狼候補なのに人間⇒裏切り者
			if (whiteList.contains(a)) {
				possessedList.add(a);
			} else {
			}
		}
		wolfCandidates = wolfCandidates.stream().filter(a -> !possessedList.contains(a)).collect(Collectors.toList());
		if (!possessedList.isEmpty()) {
			if (possessed == Content.UNSPEC || !possessedList.contains(possessed)) {
				possessed = randomSelect(possessedList);
				Content reason1 = estimateMaps.getReason(me, possessed);
				Content reason2 = divinedContent(me, possessed, Species.HUMAN);
				Content reason = andContent(me, reason1, reason2);
				Content estimate = estimateContent(me, possessed, Role.POSSESSED);
				enqueueTalk(becauseContent(me, reason, estimate));
			}
		}
		if (!wolfCandidates.isEmpty()) {
			if (!wolfCandidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(wolfCandidates);
				Estimate estimate = estimateMaps.getEstimate(me, voteCandidate);
				// 以前の投票先から変わる場合，新たに推測発言をする
				if (canTalk) {
					if (estimate != null) {
						enqueueTalk(estimate.toContent());
						voteMap.addVoteReason(me, voteCandidate, estimate.getEstimateContent());
					} else {
						voteMap.addVoteReason(me, voteCandidate, null);
					}
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
			enqueueTalk(coContent(me, me, Role.SEER));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの占い結果をすべて公開
		if (isCameout) {
			Content[] judges = divinationQueue.stream().map(j -> dayContent(me, j.getDay(), divinedContent(me, j.getTarget(), j.getResult()))).toArray(size -> new Content[size]);
			if (judges.length == 1) {
				enqueueTalk(judges[0]);

			} else if (judges.length > 1) {
				enqueueTalk(andContent(me, judges));
			}
			divinationQueue.clear();
		}
		return super.talk();
	}

	@Override
	public Agent divine() {
		// 人狼候補がいればそれらからランダムに占う
		if (!wolfCandidates.isEmpty()) {
			return randomSelect(wolfCandidates);
		}
		// 人狼候補がいない場合，まだ占っていない生存者からランダムに占う
		List<Agent> candidates = aliveOthers.stream().filter(a -> !myDivinationMap.containsKey(a)).collect(Collectors.toList());
		if (candidates.isEmpty()) {
			return null;
		}
		return randomSelect(candidates);
	}

}
