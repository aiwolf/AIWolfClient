/**
 * SampleSeer.java
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
 * 占い師役エージェントクラス
 */
public final class SampleSeer extends SampleBasePlayer {

	/** COする日 */
	private int comingoutDay;

	/** CO済みならtrue */
	private boolean isCameout;

	/** 自分の占い結果の時系列 */
	private List<Judge> myDivinationList = new ArrayList<>();

	/** 自分の占い済みエージェントと判定のマップ */
	private Map<Agent, Judge> myDivinationMap = new HashMap<>();

	/** 生存人狼 */
	private List<Agent> aliveWolves;

	/** 人狼候補 */
	private List<Agent> wolfCandidates = new ArrayList<>();

	/** 白リスト */
	private List<Agent> whiteList = new ArrayList<>();

	/** 黒リスト */
	private List<Agent> blackList = new ArrayList<>();

	/** 灰リスト */
	private List<Agent> grayList = new ArrayList<>();

	/** 宣言済みの裏切り者 */
	private Agent declaredPossessed;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		myDivinationList.clear();
		myDivinationMap.clear();
		wolfCandidates.clear();
		whiteList.clear();
		blackList.clear();
		grayList = new ArrayList<>(aliveOthers);
		declaredPossessed = null;
	}

	@Override
	public void dayStart() {
		super.dayStart();

		// 占い結果を登録し，白黒に振り分ける
		Judge divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			Agent divined = divination.getTarget();
			myDivinationList.add(divination);
			grayList.remove(divined);
			if (divination.getResult() == Species.HUMAN) {
				whiteList.add(divined);
			} else {
				blackList.add(divined);
			}
			myDivinationMap.put(divined, divination);
		}
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = isCameout ? coContent(me, me, Role.SEER) : coContent(me, me, Role.VILLAGER);

		// 生存人狼がいれば当然投票
		aliveWolves = blackList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
		// 既定の投票先が生存人狼でない場合投票先を変える
		if (!aliveWolves.isEmpty()) {
			if (!aliveWolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveWolves);
				// CO後なら投票理由を付ける
				if (isCameout) {
					Content myDivination = divinedContent(me, voteCandidate, myDivinationMap.get(voteCandidate).getResult());
					Content reason = dayContent(me, myDivinationMap.get(voteCandidate).getDay(), myDivination);
					voteReasonMap.put(me, voteCandidate, reason);
				}
			}
			return;
		}

		// これ以降は生存人狼がいない場合
		wolfCandidates.clear();

		// 偽占い師は人狼か裏切り者
		for (Agent he : aliveOthers) {
			if (comingoutMap.get(he) == Role.SEER) {
				wolfCandidates.add(he);
				// CO後なら推定理由をつける
				if (isCameout) {
					Content heIs = coContent(he, he, Role.SEER);
					Content reason = andContent(me, iAm, heIs);
					estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
				}
			}
		}

		// 自分の判定と矛盾する偽霊媒師は人狼か裏切り者
		for (Judge ident : identList) {
			Agent he = ident.getAgent();
			Agent target = ident.getTarget();
			Species result = ident.getResult();
			Content hisIdent = dayContent(me, ident.getDay(), identContent(he, target, result));
			Judge myJudge = myDivinationMap.get(target);
			if ((myJudge != null && result != myJudge.getResult())) {
				if (isAlive(he) && !wolfCandidates.contains(he)) {
					wolfCandidates.add(he);
					// CO後なら推定理由をつける
					if (isCameout) {
						Content myDivination = dayContent(me, myJudge.getDay(), divinedContent(me, myJudge.getTarget(), myJudge.getResult()));
						Content reason = andContent(me, myDivination, hisIdent);
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

		// 裏切り者関連アルゴリズム
		List<Agent> possessedList = new ArrayList<>();
		for (Agent he : wolfCandidates) {
			// 人狼候補なのに人間⇒裏切り者
			if (whiteList.contains(he)) {
				possessedList.add(he);
				// CO後なら推定理由をつける
				if (isCameout) {
					Content heIs = dayContent(me, myDivinationMap.get(he).getDay(), divinedContent(me, he, Species.HUMAN));
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
			// 裏切り者を人狼候補から除く
			wolfCandidates.removeAll(possessedList);
			// 裏切り者新発見の場合
			if (declaredPossessed == null || !possessedList.contains(declaredPossessed)) {
				declaredPossessed = randomSelect(possessedList);
				// CO後なら理由を付けてESTIMATE
				if (isCameout) {
					Estimate estimate = estimateReasonMap.getEstimate(me, declaredPossessed);
					if (estimate != null) {
						enqueueTalk(estimateReasonMap.getEstimate(me, declaredPossessed).toContent());
					}
				}
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
			// 見つからなかった場合
			if (voteCandidate == null || !isAlive(voteCandidate)) {
				if (!grayList.isEmpty()) {
					// 灰がいたら灰からランダム
					voteCandidate = randomSelect(grayList);
				} else {
					voteCandidate = randomSelect(aliveOthers);
				}
			}
		}
	}

	@Override
	void chooseFinalVoteCandidate() {
		if (!isRevote) {
			// 人狼（候補）が見つけられなかった場合，初回投票では投票リクエストに応じる
			if (aliveWolves.isEmpty() && wolfCandidates.isEmpty()) {
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
		// カミングアウトする日になったら，あるいは占い結果が人狼だったら
		// あるいは占い師カミングアウトが出たらカミングアウト
		if (!isCameout && (day >= comingoutDay || isCo(Role.SEER)
				|| (!myDivinationList.isEmpty() && myDivinationList.get(myDivinationList.size() - 1).getResult() == Species.WEREWOLF))) {
			enqueueTalk(coContent(me, me, Role.SEER));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの占い結果をすべて公開
		if (isCameout) {
			Content[] judges = myDivinationList.stream().map(j -> dayContent(me, j.getDay(),
					divinedContent(me, j.getTarget(), j.getResult()))).toArray(size -> new Content[size]);
			if (judges.length == 1) {
				enqueueTalk(judges[0]);
				enqueueTalk(judges[0].getContentList().get(0));
			} else if (judges.length > 1) {
				enqueueTalk(andContent(me, judges));
				for (Content c : judges) {
					enqueueTalk(c.getContentList().get(0));
				}
			}
			myDivinationList.clear();
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
		List<Agent> candidates = grayList.stream().filter(a -> isAlive(a)).collect(Collectors.toList());
		if (candidates.isEmpty()) {
			return null;
		}
		return randomSelect(candidates);
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
	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
