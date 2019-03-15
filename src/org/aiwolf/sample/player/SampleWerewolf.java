/**
 * SampleWerewolf.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 人狼役エージェントクラス
 */
public final class SampleWerewolf extends SampleBasePlayer {

	/** 規定人狼数 */
	private int numWolves;

	/** 騙る役職 */
	private Role fakeRole;

	/** カミングアウトする日 */
	private int comingoutDay;

	/** カミングアウト済みか */
	private boolean isCameout;

	/** 襲撃投票先候補 */
	private Agent attackVoteCandidate;

	/** 宣言済み襲撃投票先候補 */
	private Agent declaredAttackVoteCandidate;

	/** 囁きリスト読み込みのヘッド */
	private int whisperListHead;

	/** 囁き用待ち行列 */
	private Deque<Content> whisperQueue = new LinkedList<>();

	/** 偽判定リスト */
	private List<Judge> myFakeJudgeList = new ArrayList<>();

	/** 偽判定マップ */
	private Map<Agent, Judge> myFakeJudgeMap = new HashMap<>();

	/** 未公表偽判定の待ち行列 */
	private Deque<Judge> myFakeJudgeQueue = new LinkedList<>();

	/** FCO宣言状況 */
	private Map<Agent, Role> fakeComingoutMap = new HashMap<>();

	/** 裏切り者リスト */
	private List<Agent> possessedList = new ArrayList<>();

	/** 人狼リスト */
	private List<Agent> werewolves = new ArrayList<>();

	/** 人間リスト */
	private List<Agent> humans = new ArrayList<>();

	/** 村人リスト */
	private List<Agent> villagers = new ArrayList<>();

	/** 偽白のリスト */
	private List<Agent> fakeWhiteList = new ArrayList<>();

	/** 偽黒のリスト */
	private List<Agent> fakeBlackList = new ArrayList<>();

	/** 偽灰のリスト */
	private List<Agent> fakeGrayList = new ArrayList<>();

	/** 襲撃投票理由マップ */
	private AttackVoteReasonMap attackVoteReasonMap = new AttackVoteReasonMap();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);

		// ランダムに騙る役職を決める
		fakeRole = randomFakeRole();

		// 1～3日目からランダムにカミングアウトする
		comingoutDay = (int) (Math.random() * 3 + 1);

		isCameout = false;
		attackVoteCandidate = null;
		declaredAttackVoteCandidate = null;
		whisperListHead = 0;
		whisperQueue.clear();
		myFakeJudgeList.clear();
		myFakeJudgeMap.clear();
		myFakeJudgeQueue.clear();
		fakeComingoutMap.clear();
		possessedList.clear();
		werewolves = new ArrayList<>(gameInfo.getRoleMap().keySet());
		humans = aliveOthers.stream().filter(a -> !werewolves.contains(a)).collect(Collectors.toList());
		villagers.clear();
		fakeWhiteList.clear();
		fakeBlackList.clear();
		fakeGrayList = new ArrayList<>(aliveOthers);
		attackVoteReasonMap.clear();
	}

	private Role randomFakeRole() {
		return randomSelect(Arrays.asList(Role.VILLAGER, Role.SEER, Role.MEDIUM).stream()
				.filter(r -> currentGameInfo.getExistingRoles().contains(r)).collect(Collectors.toList()));
	}

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);

		// GameInfo.whisperListからFCO宣言を抽出
		for (int i = whisperListHead; i < currentGameInfo.getWhisperList().size(); i++) {
			Talk whisper = currentGameInfo.getWhisperList().get(i);
			Agent whisperer = whisper.getAgent();
			if (whisperer == me) {
				continue;
			}
			Content content = new Content(whisper.getText());

			// subjectがUNSPECの場合は発話者に入れ替える
			if (content.getSubject() == Content.UNSPEC) {
				content = replaceSubject(content, whisperer);
			}

			parseWhisper(content);
		}
		whisperListHead = currentGameInfo.getWhisperList().size();

		// 占い結果が嘘の場合，裏切り者候補
		for (Judge j : divinationList) {
			Agent he = j.getAgent();
			Agent target = j.getTarget();
			Species result = j.getResult();
			if (!werewolves.contains(he) && !possessedList.contains(he)
					&& ((humans.contains(target) && result == Species.WEREWOLF) || (werewolves.contains(target) && result == Species.HUMAN))) {
				possessedList.add(he);
				Content heIs = notContent(me, coContent(me, he, Role.WEREWOLF));
				Content hisDayDivination = dayContent(me, j.getDay(), divinedContent(he, target, result));
				Content targetIs;
				if (humans.contains(target)) {
					targetIs = notContent(me, coContent(me, target, Role.WEREWOLF));
				} else {
					targetIs = coContent(me, target, Role.WEREWOLF);
				}
				Content reason = andContent(me, heIs, targetIs, hisDayDivination);
				Estimate estimate = new Estimate(me, he, reason, Role.POSSESSED);
				estimateReasonMap.put(estimate);
				enqueueWhisper(estimate.toContent());
			}
		}

		// 霊媒結果が嘘の場合，裏切り者候補
		for (Judge j : identList) {
			Agent he = j.getAgent();
			Agent target = j.getTarget();
			Species result = j.getResult();
			if (!werewolves.contains(he) && !possessedList.contains(he)
					&& ((humans.contains(target) && result == Species.WEREWOLF) || (werewolves.contains(target) && result == Species.HUMAN))) {
				possessedList.add(he);
				Content heIs = notContent(me, coContent(me, he, Role.WEREWOLF));
				Content hisDayIdent = dayContent(me, j.getDay(), identContent(he, target, result));
				Content targetIs;
				if (humans.contains(target)) {
					targetIs = notContent(me, coContent(me, target, Role.WEREWOLF));
				} else {
					targetIs = coContent(me, target, Role.WEREWOLF);
				}
				Content reason = andContent(me, heIs, targetIs, hisDayIdent);
				Estimate estimate = new Estimate(me, he, reason, Role.POSSESSED);
				estimateReasonMap.put(estimate);
				enqueueWhisper(estimate.toContent());
			}
		}

		villagers = aliveOthers.stream()
				.filter(a -> !werewolves.contains(a) && !possessedList.contains(a)).collect(Collectors.toList());
	}

	private void parseWhisper(Content content) {
		if (estimateReasonMap.put(content)) {
			return; // 推測文と解析
		}
		if (attackVoteReasonMap.put(content)) {
			return; // 襲撃投票宣言と解析
		}
		switch (content.getTopic()) {
		case COMINGOUT: // Declaration of FCO
			fakeComingoutMap.put(content.getSubject(), content.getRole());
			return;
		default:
			break;
		}
	}

	@Override
	public void dayStart() {
		super.dayStart();

		attackVoteCandidate = null;
		declaredAttackVoteCandidate = null;
		whisperListHead = 0;

		if (day == 0) {
			enqueueWhisper(coContent(me, me, fakeRole));
		}
		// 偽の判定
		else {
			if (fakeRole != Role.VILLAGER) {
				Judge judge = getFakeJudge();
				if (judge != null) {
					myFakeJudgeList.add(judge);
					myFakeJudgeMap.put(judge.getTarget(), judge);
					myFakeJudgeQueue.offer(judge);
					if (judge.getResult() == Species.WEREWOLF) {
						fakeBlackList.add(judge.getTarget());
					} else {
						fakeWhiteList.add(judge.getTarget());
					}
					fakeGrayList.remove(judge.getTarget());
				}
			}
		}
	}

	private Judge getFakeJudge() {
		Agent target = null;

		// 占い師騙りの場合
		if (fakeRole == Role.SEER) {
			List<Agent> candidates = aliveOthers.stream()
					.filter(a -> !myFakeJudgeMap.containsKey(a) && comingoutMap.get(a) != Role.SEER).collect(Collectors.toList());
			if (candidates.isEmpty()) {
				target = randomSelect(aliveOthers);
			} else {
				target = randomSelect(candidates);
			}
		}
		// 霊媒師騙りの場合
		else if (fakeRole == Role.MEDIUM) {
			target = currentGameInfo.getExecutedAgent();
		}

		if (target != null) {
			Species result = Species.HUMAN;
			// 人間が偽占い対象の場合
			if (humans.contains(target)) {
				int nFakeWolves = (int) myFakeJudgeMap.keySet().stream()
						.filter(a -> myFakeJudgeMap.get(a).getResult() == Species.WEREWOLF).count();
				// 偽人狼に余裕があれば
				if (nFakeWolves < numWolves) {
					// 裏切り者，あるいはまだカミングアウトしていないエージェントの場合，判定は五分五分
					if (possessedList.contains(target) || !isCo(target)) {
						if (Math.random() < 0.5) {
							result = Species.WEREWOLF;
						}
					}
					// それ以外は人狼判定
					else {
						result = Species.WEREWOLF;
					}
				}
			}
			return new Judge(day, me, target, result);
		}
		return null;
	}

	@Override
	void chooseVoteCandidate() {
		Content iAm = isCameout ? coContent(me, me, fakeRole) : coContent(me, me, Role.VILLAGER);
		List<Agent> fakeWolfCandidates = new ArrayList<>();

		if (fakeRole == Role.SEER) {
			// 生存偽人狼がいれば当然投票（できれば裏切り者は除く）
			List<Agent> aliveFakeWolves = fakeBlackList.stream()
					.filter(a -> isAlive(a) && !possessedList.contains(a)).collect(Collectors.toList());
			if (aliveFakeWolves.isEmpty()) {
				aliveFakeWolves = fakeBlackList.stream()
						.filter(a -> isAlive(a)).collect(Collectors.toList());
			}
			// 既定の投票先が生存偽人狼でない場合投票先を変える
			if (!aliveFakeWolves.isEmpty()) {
				if (!aliveFakeWolves.contains(voteCandidate)) {
					voteCandidate = randomSelect(aliveFakeWolves);
					if (isCameout) {
						Content myDivination = divinedContent(me, voteCandidate, myFakeJudgeMap.get(voteCandidate).getResult());
						Content vote = voteContent(Content.ANY, voteCandidate);
						Content reason = dayContent(me, myFakeJudgeMap.get(voteCandidate).getDay(), myDivination);
						Content request = requestContent(me, Content.ANY, vote);
						enqueueTalk(becauseContent(me, reason, request));
						voteReasonMap.put(me, voteCandidate, reason);
					}
				}
				return;
			}

			// これ以降は生存偽人狼がいない場合
			// 自称占い師を人狼か裏切り者と推測する
			for (Agent he : aliveOthers) {
				if (comingoutMap.get(he) == Role.SEER) {
					fakeWolfCandidates.add(he);
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
				Judge myJudge = myFakeJudgeMap.get(target);
				if ((myJudge != null && result != myJudge.getResult())) {
					if (isAlive(he) && !fakeWolfCandidates.contains(he)) {
						fakeWolfCandidates.add(he);
						if (isCameout) {
							Content myDivination = dayContent(me, myJudge.getDay(), divinedContent(me, myJudge.getTarget(), myJudge.getResult()));
							Content reason = andContent(me, myDivination, hisIdent);
							estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
						}
					}
				}
			}
		} else if (fakeRole == Role.MEDIUM) {
			// 自称霊媒師を人狼か裏切り者と推測する
			for (Agent he : aliveOthers) {
				if (comingoutMap.get(he) == Role.MEDIUM) {
					fakeWolfCandidates.add(he);
					if (isCameout) {
						Content heIs = coContent(he, he, Role.MEDIUM);
						Content reason = andContent(me, iAm, heIs);
						estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
					}
				}
			}
			// 自分の霊媒結果と矛盾する自称占い師を人狼か裏切り者と推測する
			for (Judge divination : divinationList) {
				Agent he = divination.getAgent();
				Agent target = divination.getTarget();
				Species result = divination.getResult();
				Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
				Judge myJudge = myFakeJudgeMap.get(target);
				if ((myJudge != null && result != myJudge.getResult())) {
					if (isAlive(he) && !fakeWolfCandidates.contains(he)) {
						fakeWolfCandidates.add(he);
						if (isCameout) {
							Content myIdent = dayContent(me, myJudge.getDay(), identContent(me, myJudge.getTarget(), myJudge.getResult()));
							Content reason = andContent(me, myIdent, hisDivination);
							estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
						}
					}
				}
			}
		}

		// 村人目線での人狼候補決定アルゴリズム
		for (Judge divination : divinationList) {
			// まず占い結果から人狼候補を見つける
			Agent he = divination.getAgent();
			Species result = divination.getResult();
			if (!isAlive(he) || fakeWolfCandidates.contains(he) || result == Species.HUMAN) {
				continue;
			}
			Agent target = divination.getTarget();
			if (target == me) {
				// 自分を人狼と判定した自称占い師は人狼か裏切り者なので投票先候補に追加
				fakeWolfCandidates.add(he);
				Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
				Content reason = andContent(me, coContent(me, me, Role.VILLAGER), hisDivination);
				estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
			} else if (isKilled(target)) {
				// 殺されたエージェントを人狼と判定した自称占い師は人狼か裏切り者なので投票先候補に追加
				fakeWolfCandidates.add(he);
				Content hisDivination = dayContent(me, divination.getDay(), divinedContent(he, target, result));
				Content reason = andContent(me, attackedContent(Content.ANY, target), hisDivination);
				estimateReasonMap.put(new Estimate(me, he, reason, Role.WEREWOLF, Role.POSSESSED));
			}
		}

		// できれば仲間は除く
		List<Agent> fakeWolfCandidates0 = fakeWolfCandidates.stream()
				.filter(a -> !werewolves.contains(a)).collect(Collectors.toList());
		if (!fakeWolfCandidates0.isEmpty()) {
			fakeWolfCandidates = fakeWolfCandidates0;
			// できれば裏切り者は除く
			List<Agent> fakeWolfCandidates1 = fakeWolfCandidates.stream()
					.filter(a -> !possessedList.contains(a)).collect(Collectors.toList());
			if (!fakeWolfCandidates1.isEmpty()) {
				fakeWolfCandidates = fakeWolfCandidates1;
			}
		}

		if (!fakeWolfCandidates.isEmpty()) {
			// 見つかった場合
			if (!fakeWolfCandidates.contains(voteCandidate)) {
				// 新しい投票先の場合，推測発言をする
				voteCandidate = randomSelect(fakeWolfCandidates);
				Estimate estimate = estimateReasonMap.getEstimate(me, voteCandidate);
				if (estimate != null) {
					enqueueTalk(estimate.toContent());
					voteReasonMap.put(me, voteCandidate, estimate.getEstimateContent());
				}
			}
		} else {
			// 見つからなかった場合
			if (!isRevote) {
				// 初回投票
				// まず灰からランダム
				// できれば仲間は除く
				List<Agent> fakeGrayList0 = fakeGrayList.stream()
						.filter(a -> isAlive(a) && !werewolves.contains(a)).collect(Collectors.toList());
				if (!fakeGrayList0.isEmpty()) {
					fakeGrayList = fakeGrayList0;
					// できれば裏切り者は除く
					List<Agent> fakeGrayList1 = fakeGrayList.stream()
							.filter(a -> !possessedList.contains(a)).collect(Collectors.toList());
					if (!fakeGrayList1.isEmpty()) {
						fakeGrayList = fakeGrayList1;
					}
				}
				if (!fakeGrayList.isEmpty()) {
					if (!fakeGrayList.contains(voteCandidate)) {
						voteCandidate = randomSelect(fakeGrayList);
					}
				} else {
					// 灰もいない場合は投票リクエストに応じた投票
					List<Agent> candidates = null;
					if (voteRequestCounter.isChanged()) {
						candidates = voteRequestCounter.getRequestMap().values().stream()
								.filter(a -> !werewolves.contains(a)).collect(Collectors.toList());
					}
					if (candidates != null && !candidates.isEmpty()) {
						voteCandidate = randomSelect(candidates);
					} else {
						candidates = aliveOthers;
						List<Agent> candidates0 = candidates.stream()
								.filter(a -> !werewolves.contains(a)).collect(Collectors.toList());
						if (!candidates0.isEmpty()) {
							candidates = candidates0;
							List<Agent> candidates1 = candidates.stream()
									.filter(a -> !possessedList.contains(a)).collect(Collectors.toList());
							if (!candidates1.isEmpty()) {
								candidates = candidates1;
							}
						}
						voteCandidate = randomSelect(candidates);
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
	}

	@Override
	public String talk() {
		if (fakeRole != Role.VILLAGER) {
			if (!isCameout) {
				// 対抗カミングアウトがある場合，今日カミングアウトする
				for (Agent a : humans) {
					if (comingoutMap.get(a) == fakeRole) {
						comingoutDay = day;
					}
				}
				// カミングアウトするタイミングになったらカミングアウト
				if (day >= comingoutDay) {
					isCameout = true;
					enqueueTalk(coContent(me, me, fakeRole));
				}
			}
			// カミングアウトしたらこれまでの偽判定結果をすべて公開
			else {
				List<Content> judges = new ArrayList<>();
				while (!myFakeJudgeQueue.isEmpty()) {
					Judge judge = myFakeJudgeQueue.poll();
					if (fakeRole == Role.SEER) {
						judges.add(dayContent(me, judge.getDay(),
								divinedContent(me, judge.getTarget(), judge.getResult())));
					} else if (fakeRole == Role.MEDIUM) {
						judges.add(dayContent(me, judge.getDay(),
								identContent(me, judge.getTarget(), judge.getResult())));
					}
				}
				if (judges.size() == 1) {
					enqueueTalk(judges.get(0));
				} else if (judges.size() > 1) {
					enqueueTalk(andContent(me, judges.toArray(new Content[0])));
				}
			}
		}
		return super.talk();
	}

	/** 襲撃先候補を選ぶ */
	void chooseAttackVoteCandidate() {
		// カミングアウトした村人陣営は襲撃先候補
		List<Agent> candidates = villagers.stream().filter(a -> isCo(a)).collect(Collectors.toList());
		for (Agent a : candidates) {
			attackVoteReasonMap.put(me, a, coContent(a, a, comingoutMap.get(a)));
		}
		// 候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			candidates = villagers;
		}
		// 村人陣営がいない場合は裏切り者を襲う
		if (candidates.isEmpty()) {
			candidates = possessedList;
		}
		if (candidates.isEmpty()) {
			attackVoteCandidate = null;
		} else if (!candidates.contains(declaredAttackVoteCandidate)) {
			attackVoteCandidate = randomSelect(candidates);
		}
	}

	@Override
	public Agent attack() {
		chooseAttackVoteCandidate();
		return attackVoteCandidate;
	}

	@Override
	public String whisper() {
		if (day == 0) {
			// 騙る役職が重複した場合，選び直す
			if (fakeRole != Role.VILLAGER) {
				for (Agent a : fakeComingoutMap.keySet()) {
					if (fakeComingoutMap.get(a) == fakeRole) {
						Role newFakeRole = randomFakeRole();
						if (newFakeRole != fakeRole) {
							fakeRole = newFakeRole;
							enqueueWhisper(coContent(me, me, fakeRole));
						}
						break;
					}
				}
			}
		} else {
			chooseAttackVoteCandidate();
			if (attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
				Content reason = attackVoteReasonMap.getReason(me, attackVoteCandidate);
				if (reason != null) {
					enqueueWhisper(becauseContent(me, reason, attackContent(me, attackVoteCandidate)));
				} else {
					enqueueWhisper(attackContent(me, attackVoteCandidate));
				}
				declaredAttackVoteCandidate = attackVoteCandidate;
			}
		}
		return dequeueWhisper();
	}

	void enqueueWhisper(Content content) {
		if (content.getSubject() == Content.UNSPEC) {
			whisperQueue.offer(replaceSubject(content, me));
		} else {
			whisperQueue.offer(content);
		}
	}

	String dequeueWhisper() {
		if (whisperQueue.isEmpty()) {
			return Talk.SKIP;
		}
		Content content = whisperQueue.poll();
		if (content.getSubject() == me) {
			return Content.stripSubject(content.getText());
		}
		return content.getText();
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
