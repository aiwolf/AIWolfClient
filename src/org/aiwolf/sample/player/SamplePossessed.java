/**
 * SamplePossessed.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 * 裏切り者役エージェントクラス
 */
public final class SamplePossessed extends SampleBasePlayer {
	int numWolves;
	boolean isCameout;
	List<Judge> fakeDivinationList = new ArrayList<>();
	List<Agent> divinedAgents = new ArrayList<>();
	Map<Agent, Judge> myFakeDivinationMap = new HashMap<>();
	List<Agent> fakeWhiteList = new ArrayList<>();
	List<Agent> fakeBlackList = new ArrayList<>();
	List<Agent> fakeGrayList = new ArrayList<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		isCameout = false;
		fakeDivinationList.clear();
		divinedAgents.clear();
		myFakeDivinationMap.clear();
		fakeWhiteList.clear();
		fakeBlackList.clear();
		fakeGrayList = new ArrayList<>(aliveOthers);
	}

	@Override
	public void dayStart() {
		super.dayStart();
		// CO後は毎日偽の白判定
		if (isCameout) {
			Agent divined = randomSelect(fakeGrayList.stream()
					.filter(a -> isAlive(a)).collect(Collectors.toList()));
			if (divined == null) {
				divined = me;
				System.out.println();
			}
			Judge fakeDivination = new Judge(day, me, divined, Species.HUMAN);
			fakeDivinationList.add(fakeDivination);
			myFakeDivinationMap.put(divined, fakeDivination);
			divinedAgents.add(divined);
			fakeGrayList.remove(divined);
			fakeWhiteList.add(divined);
		}
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = isCameout ? coContent(me, me, Role.SEER) : coContent(me, me, Role.VILLAGER);

		// 生存偽人狼がいれば当然投票
		List<Agent> aliveFakeWolves = fakeBlackList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
		// 既定の投票先が生存偽人狼でない場合投票先を変える
		if (!aliveFakeWolves.isEmpty()) {
			if (!aliveFakeWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveFakeWolves);
				if (canTalk) {
					Content myDivination = divinedContent(me, voteCandidate, myFakeDivinationMap.get(voteCandidate).getResult());
					Content vote = voteContent(Content.ANY, voteCandidate);
					Content reason = dayContent(me, myFakeDivinationMap.get(voteCandidate).getDay(), myDivination);
					Content request = requestContent(me, Content.ANY, vote);
					enqueueTalk(becauseContent(me, reason, request));
					enqueueTalk(inquiryContent(me, Content.ANY, vote));
					voteMap.addVoteReason(me, voteCandidate, divinedContent(me, voteCandidate, Species.WEREWOLF));
				}
			}
			return;
		}

		// 偽人狼がいない場合は占い師同様の推測
		List<Agent> wolfCandidates = new ArrayList<>();

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
			Judge myJudge = myFakeDivinationMap.get(target);
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

		for (Judge j : divinationList) {
			Content dayDivination = dayContent(me, j.getDay(), divinedContent(j.getAgent(), j.getTarget(), j.getResult()));
			// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (j.getTarget() == me && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content reason = andContent(me, iAm, dayDivination);
					Estimate estimate = new Estimate(me, j.getAgent(), Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
				}
			}
			// 殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補に追加
			if (isKilled(j.getTarget()) && j.getResult() == Species.WEREWOLF) {
				if (isAlive(j.getAgent()) && !wolfCandidates.contains(j.getAgent())) {
					wolfCandidates.add(j.getAgent());
					Content reason = andContent(me, attackedContent(Content.ANY, j.getTarget()), dayDivination);
					Estimate estimate = new Estimate(me, j.getAgent(), Role.WEREWOLF, reason);
					estimate.addRole(Role.POSSESSED);
					estimateMaps.addEstimate(estimate);
				}
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
			if (!fakeGrayList.isEmpty()) {
				if (!fakeGrayList.contains(voteCandidate)) {
					voteCandidate = randomSelect(fakeGrayList);
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
		// 占い師が出たら対抗カミングアウト
		if (!isCameout && isCo(Role.SEER)) {
			enqueueTalk(coContent(me, me, Role.SEER));
			isCameout = true;
			Agent seer = null;
			for (Agent agent : aliveOthers) {
				if (comingoutMap.get(agent) == Role.SEER) {
					seer = agent;
					break;
				}
			}
			// 前日までの偽占いをでっち上げる
			List<Agent> fakeHumans = new ArrayList<>(currentGameInfo.getAgentList());
			fakeHumans.remove(me);
			fakeHumans.remove(seer);
			Collections.shuffle(fakeHumans);
			for (int d = 1; d < day; d++) {
				Agent fakeHuman = fakeHumans.get(d - 1);
				Judge fakeJudge = new Judge(d, me, fakeHuman, Species.HUMAN);
				fakeDivinationList.add(fakeJudge);
				myFakeDivinationMap.put(fakeHuman, fakeJudge);
				divinedAgents.add(fakeHuman);
				fakeGrayList.remove(fakeHuman);
				fakeWhiteList.add(fakeHuman);
			}
			Judge fakeJudge = new Judge(day, me, seer, Species.WEREWOLF);
			fakeDivinationList.add(fakeJudge);
			myFakeDivinationMap.put(seer, fakeJudge);
			divinedAgents.add(seer);
			fakeGrayList.remove(seer);
			fakeBlackList.add(seer);
		}
		// カミングアウトしたらこれまでの偽占い結果をまとめて報告
		if (isCameout) {
			Content[] judges = fakeDivinationList.stream().map(j -> dayContent(me, j.getDay(),
					divinedContent(me, j.getTarget(), j.getResult()))).toArray(size -> new Content[size]);
			if (judges.length == 1) {
				enqueueTalk(judges[0]);

			} else if (judges.length > 1) {
				enqueueTalk(andContent(me, judges));
			}
			fakeDivinationList.clear();
		}
		return super.talk();
	}

}
