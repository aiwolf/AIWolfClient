/**
 * SampleMedium.java
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
 * 霊媒師役エージェントクラス
 */
public final class SampleMedium extends SampleBasePlayer {
	int comingoutDay;
	boolean isCameout;
	Deque<Judge> identQueue = new LinkedList<>();
	Map<Agent, Judge> myIdentMap = new HashMap<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		identQueue.clear();
		myIdentMap.clear();
	}

	@Override
	public void dayStart() {
		super.dayStart();
		// 霊媒結果を待ち行列に入れる
		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			identQueue.offer(ident);
			myIdentMap.put(ident.getTarget(), ident);
		}
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = isCameout ? coContent(me, me, Role.MEDIUM) : coContent(me, me, Role.VILLAGER);
		List<Agent> wolfCandidates = new ArrayList<>();
		// 霊媒師をカミングアウトしている他のエージェントは人狼候補
		for (Agent agent : aliveOthers) {
			if (comingoutMap.get(agent) == Role.MEDIUM) {
				wolfCandidates.add(agent);
				Estimate estimate = new Estimate(me, agent, Role.WEREWOLF);
				estimate.addRole(Role.POSSESSED);
				if (isCameout) {
					Content heIs = coContent(agent, agent, Role.MEDIUM);
					Content reason = andContent(me, iAm, heIs);
					estimate.addReason(reason);
				}
				estimateMaps.addEstimate(estimate);
			}
		}
		for (Judge j : divinationList) {
			Agent he = j.getAgent();
			Agent target = j.getTarget();
			Species result = j.getResult();
			Content hisDayDivination = dayContent(me, j.getDay(), divinedContent(he, target, result));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (target == me && result == Species.WEREWOLF) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Content reason = andContent(me, iAm, hisDayDivination);
					Estimate estimate = new Estimate(me, he, Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(target) && result == Species.WEREWOLF) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Content reason = andContent(me, attackedContent(Content.ANY, target), hisDayDivination);
					Estimate estimate = new Estimate(me, he, Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
				}
			}
			Judge myJudge = myIdentMap.get(target);
			// 偽占い師
			if (myJudge != null && result != myJudge.getResult()) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Estimate estimate = new Estimate(me, he, Role.WEREWOLF);
					estimate.addRole(Role.POSSESSED);
					if (isCameout) {
						Content myDayIdent = dayContent(me, myJudge.getDay(), identContent(me, myJudge.getTarget(), myJudge.getResult()));
						Content reason = andContent(me, myDayIdent, hisDayDivination);
						estimate.addReason(reason);
					}
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
					if (estimate != null) {
						enqueueTalk(estimate.toContent());
						voteMap.addVoteReason(me, voteCandidate, estimate.getEstimateContent());
					} else {
						voteMap.addVoteReason(me, voteCandidate, null);
					}
				}
			}
		}
	}

	@Override
	public String talk() {
		// カミングアウトする日になったら，あるいは霊媒結果が人狼だったら
		// あるいは霊媒師カミングアウトが出たらカミングアウト
		if (!isCameout && (day >= comingoutDay || (!identQueue.isEmpty() && identQueue.peekLast().getResult() == Species.WEREWOLF) || isCo(Role.MEDIUM))) {
			enqueueTalk(coContent(me, me, Role.MEDIUM));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの霊媒結果をすべて公開
		if (isCameout) {
			Content[] judges = identQueue.stream().map(j -> dayContent(me, j.getDay(), identContent(me, j.getTarget(), j.getResult()))).toArray(size -> new Content[size]);
			if (judges.length == 1) {
				enqueueTalk(judges[0]);

			} else if (judges.length > 1) {
				enqueueTalk(andContent(me, judges));
			}
			identQueue.clear();
		}
		return super.talk();
	}

}
