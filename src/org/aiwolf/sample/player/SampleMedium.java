/**
 * SampleMedium.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * 霊媒師役エージェントクラス
 */
public final class SampleMedium extends SampleBasePlayer {

	/** 人狼候補リスト */
	private List<Agent> wolfCandidates = new ArrayList<>();

	/** COする日 */
	private int comingoutDay;

	/** CO済みならtrue */
	private boolean isCameout;

	/** 自分の霊媒結果の時系列 */
	private List<Judge> myIdentList = new ArrayList<>();

	/** 自分の霊媒結果のマップ */
	private Map<Agent, Judge> myIdentMap = new HashMap<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		myIdentList.clear();
		myIdentMap.clear();
	}

	@Override
	public void dayStart() {
		super.dayStart();

		// 霊媒結果を待ち行列に入れる
		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			myIdentList.add(ident);
			myIdentMap.put(ident.getTarget(), ident);
		}
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = isCameout ? coContent(me, me, Role.MEDIUM) : coContent(me, me, Role.VILLAGER);

		wolfCandidates.clear();
		// 偽霊媒師は人狼か裏切り者
		for (Agent he : aliveOthers) {
			if (comingoutMap.get(he) == Role.MEDIUM) {
				wolfCandidates.add(he);
				// CO後なら理由をつける
				if (isCameout) {
					Content reason = andContent(me, iAm, coContent(he, he, Role.MEDIUM));
					estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
				}
			}
		}

		// 自分の判定と矛盾する偽占い師は人狼か裏切り者
		for (Judge divination : divinationList) {
			Agent he = divination.getAgent();
			Agent target = divination.getTarget();
			Species result = divination.getResult();
			Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
			Judge myJudge = myIdentMap.get(target);
			if (myJudge != null && result != myJudge.getResult()) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					// CO後なら推定理由をつける
					if (isCameout) {
						Content myIdent = dayContent(me, myJudge.getDay(), identContent(me, myJudge.getTarget(), myJudge.getResult()));
						Content reason = andContent(me, myIdent, hisDivination);
						estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
					}
				}
			}
		}

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
				voteCandidate = randomSelect(aliveOthers);
			}
		}
	}

	@Override
	void chooseFinalVoteCandidate() {
		if (!isRevote) {
			// 人狼候補が見つけられなかった場合，初回投票では投票リクエストに応じる
			if (wolfCandidates.isEmpty()) {
				voteCandidate = randomSelect(voteRequestCounter.getRequestMap().values().stream()
						.filter(a -> a != me).collect(Collectors.toList()));
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
	public String talk() {
		// カミングアウトする日になったら，あるいは霊媒結果が人狼だったら
		// あるいは霊媒師カミングアウトが出たらカミングアウト
		if (!isCameout && (day >= comingoutDay || isCo(Role.MEDIUM)
				|| (!myIdentList.isEmpty() && myIdentList.get(myIdentList.size() - 1).getResult() == Species.WEREWOLF))) {
			enqueueTalk(coContent(me, me, Role.MEDIUM));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの霊媒結果をすべて公開
		if (isCameout) {
			Content[] judges = myIdentList.stream().map(j -> dayContent(me, j.getDay(),
					identContent(me, j.getTarget(), j.getResult()))).toArray(size -> new Content[size]);
			if (judges.length == 1) {
				enqueueTalk(judges[0]);
				enqueueTalk(judges[0].getContentList().get(0));
			} else if (judges.length > 1) {
				enqueueTalk(andContent(me, judges));
				for (Content c : judges) {
					enqueueTalk(c.getContentList().get(0));
				}
			}
			myIdentList.clear();
		}
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
