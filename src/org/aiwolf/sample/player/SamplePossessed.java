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
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 裏切り者役エージェントクラス
 */
public final class SamplePossessed extends SampleBasePlayer {

	/** CO済みならtrue */
	private boolean isCameout;

	/** 偽占い結果の時系列 */
	private List<Judge> fakeDivinationList = new ArrayList<>();

	/** 偽占い済みエージェントと判定のマップ */
	private Map<Agent, Judge> myFakeDivinationMap = new HashMap<>();

	/** 偽白リスト */
	private List<Agent> fakeWhiteList = new ArrayList<>();

	/** 偽黒リスト */
	private List<Agent> fakeBlackList = new ArrayList<>();

	/** 偽灰リスト */
	private List<Agent> fakeGrayList = new ArrayList<>();

	/** 宣言済みの裏切り者 */
	private Agent declaredFakePossessed;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		isCameout = false;
		fakeDivinationList.clear();
		myFakeDivinationMap.clear();
		fakeWhiteList.clear();
		fakeBlackList.clear();
		fakeGrayList = new ArrayList<>(aliveOthers);
		declaredFakePossessed = null;
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
			}
			Judge fakeDivination = new Judge(day, me, divined, Species.HUMAN);
			fakeDivinationList.add(fakeDivination);
			myFakeDivinationMap.put(divined, fakeDivination);
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
				if (isCameout) {
					Content myDivination = divinedContent(me, voteCandidate, myFakeDivinationMap.get(voteCandidate).getResult());
					Content vote = voteContent(Content.ANY, voteCandidate);
					Content reason = dayContent(me, myFakeDivinationMap.get(voteCandidate).getDay(), myDivination);
					Content request = requestContent(me, Content.ANY, vote);
					enqueueTalk(becauseContent(me, reason, request));
					voteReasonMap.put(me, voteCandidate, reason);
				}
			}
			return;
		}

		// 偽人狼がいない場合は占い師同様の推測
		List<Agent> wolfCandidates = new ArrayList<>();

		// 自称占い師を人狼か裏切り者と推測する
		for (Agent he : aliveOthers) {
			if (comingoutMap.get(he) == Role.SEER) {
				wolfCandidates.add(he);
				// CO後なら理由を述べてよい
				if (isCameout) {
					Content heIs = coContent(he, he, Role.SEER);
					Content reason = andContent(me, iAm, heIs);
					estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
				}
			}
		}
		// 自分の占いと矛盾する自称霊媒師を人狼か裏切り者と推測する
		for (Judge ident : identList) {
			Agent he = ident.getAgent();
			Agent target = ident.getTarget();
			Species result = ident.getResult();
			Content hisIdent = dayContent(me, ident.getDay(), identContent(he, target, result));
			Judge myJudge = myFakeDivinationMap.get(target);
			if ((myJudge != null && result != myJudge.getResult())) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					// CO後なら理由を述べてよい
					if (isCameout) {
						Content myDivination = dayContent(me, myJudge.getDay(), divinedContent(me, myJudge.getTarget(), myJudge.getResult()));
						Content reason = andContent(me, myDivination, hisIdent);
						estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
					}
				}
			}
		}

		// 村人陣営共通の人狼候補決定アルゴリズム
		// 自分を人狼と判定していて，生存している自称占い師を投票先候補に追加
		for (Judge divination : divinationList) {
			Agent he = divination.getAgent();
			Agent target = divination.getTarget();
			Species result = divination.getResult();
			Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
			if (target == me && result == Species.WEREWOLF) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					Content reason = andContent(me, iAm, hisDivination);
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

		// 偽裏切り者関連アルゴリズム
		List<Agent> possessedList = new ArrayList<>();
		for (Agent he : wolfCandidates) {
			// 人狼候補なのに人間と偽判定⇒偽裏切り者
			if (fakeWhiteList.contains(he)) {
				possessedList.add(he);
				// CO後なら理由を述べてよい
				if (isCameout) {
					Content heIs = dayContent(me, myFakeDivinationMap.get(he).getDay(), divinedContent(me, he, Species.HUMAN));
					// 既存の推測理由があれば推測役職を裏切り者にする
					Estimate estimate = estimateReasonMap.getEstimate(me, he);
					if (estimate != null) {
						estimate.resetRole(Role.POSSESSED);
						estimate.addReason(heIs);
					}
				}
			}
		}
		if (!possessedList.isEmpty()) {
			// 偽裏切り者を偽人狼候補から除く
			wolfCandidates.removeAll(possessedList);
			// 裏切り者新発見の場合
			if (declaredFakePossessed == null || !possessedList.contains(declaredFakePossessed)) {
				declaredFakePossessed = randomSelect(possessedList);
				// CO後なら理由を付けてESTIMATE
				if (isCameout) {
					Estimate estimate = estimateReasonMap.getEstimate(me, declaredFakePossessed);
					if (estimate != null) {
						enqueueTalk(estimateReasonMap.getEstimate(me, declaredFakePossessed).toContent());
					}
				}
			}
		}

		if (!wolfCandidates.isEmpty()) {
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
		// 候補がいない場合
		else {
			if (!isRevote) {
				// 初回投票
				// まず灰からランダム
				if (!fakeGrayList.isEmpty()) {
					if (!fakeGrayList.contains(voteCandidate)) {
						voteCandidate = randomSelect(fakeGrayList);
					}
				}
				// 灰もいない場合ランダム
				else {
					if (!aliveOthers.contains(voteCandidate)) {
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
			voteReasonMap.put(me, voteCandidate, null);
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
				fakeGrayList.remove(fakeHuman);
				fakeWhiteList.add(fakeHuman);
			}
			Judge fakeJudge = new Judge(day, me, seer, Species.WEREWOLF);
			fakeDivinationList.add(fakeJudge);
			myFakeDivinationMap.put(seer, fakeJudge);
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
