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
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 人狼役エージェントクラス
 */
public final class SampleWerewolf extends SampleBasePlayer {

	/** 規定人狼数 */
	int numWolves;

	/** 騙る役職 */
	Role fakeRole;

	/** カミングアウトする日 */
	int comingoutDay;

	/** カミングアウト済みか */
	boolean isCameout;

	/** whisper()できるか時間帯か */
	boolean canWhisper;

	/** 囁き用待ち行列 */
	Deque<Content> whisperQueue = new LinkedList<>();

	/** 偽判定マップ */
	Map<Agent, Species> fakeJudgeMap = new HashMap<>();

	/** 未公表偽判定の待ち行列 */
	Deque<Judge> fakeJudgeQueue = new LinkedList<>();

	/** 裏切り者リスト */
	List<Agent> possessedList = new ArrayList<>();

	/** 人狼リスト */
	List<Agent> werewolves;

	/** 人間リスト */
	List<Agent> humans;

	/** 村人リスト */
	List<Agent> villagers = new ArrayList<>();

	/** 襲撃投票先候補 */
	Agent attackVoteCandidate;

	/** 宣言済み襲撃投票先候補 */
	Agent declaredAttackVoteCandidate;

	/** 襲撃理由マップ */
	Map<Agent, Content> attackReasonMap = new HashMap<>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		whisperQueue.clear();
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		werewolves = new ArrayList<>(gameInfo.getRoleMap().keySet());
		humans = aliveOthers.stream().filter(a -> !werewolves.contains(a)).collect(Collectors.toList());
		// ランダムに騙る役職を決める
		fakeRole = randomSelect(Arrays.asList(Role.VILLAGER, Role.SEER, Role.MEDIUM).stream().filter(r -> gameInfo.getExistingRoles().contains(r)).collect(Collectors.toList()));
		// 1～3日目からランダムにカミングアウトする
		comingoutDay = (int) (Math.random() * 3 + 1);
		isCameout = false;
		fakeJudgeMap.clear();
		fakeJudgeQueue.clear();
		possessedList.clear();
		attackReasonMap.clear();
	}

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);
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
				Estimate estimate = new Estimate(me, he, Role.POSSESSED, reason);
				estimateMaps.addEstimate(estimate);
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
				Estimate estimate = new Estimate(me, he, Role.POSSESSED, reason);
				estimateMaps.addEstimate(estimate);
				enqueueWhisper(estimate.toContent());
			}
		}
		villagers = aliveOthers.stream().filter(a -> !werewolves.contains(a) && !possessedList.contains(a)).collect(Collectors.toList());
	}

	private Judge getFakeJudge() {
		Agent target = null;
		// 占い師騙りの場合
		if (fakeRole == Role.SEER) {
			List<Agent> candidates = aliveOthers.stream()
					.filter(a -> !fakeJudgeMap.containsKey(a) && comingoutMap.get(a) != Role.SEER).collect(Collectors.toList());
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
				// 偽人狼に余裕があれば
				int nFakeWolves = 0;
				for (Agent a : fakeJudgeMap.keySet()) {
					if (fakeJudgeMap.get(a) == Species.WEREWOLF) {
						nFakeWolves++;
					}
				}
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
	public void dayStart() {
		super.dayStart();
		canWhisper = true;
		declaredAttackVoteCandidate = null;
		attackVoteCandidate = null;
		if (day == 0) {
			enqueueWhisper(coContent(me, me, fakeRole));
		}
		// 偽の判定
		else {
			Judge judge = getFakeJudge();
			if (judge != null) {
				fakeJudgeQueue.offer(judge);
				fakeJudgeMap.put(judge.getTarget(), judge.getResult());
			}
		}
	}

	/** 投票先候補を選ぶ */
	@Override
	void chooseVoteCandidate() {
		List<Agent> candidates = new ArrayList<>();
		// 占い師/霊媒師騙りの場合
		if (fakeRole != Role.VILLAGER) {
			// 対抗カミングアウトした，あるいは人狼と判定した村人は投票先候補
			candidates = villagers.stream()
					.filter(a -> comingoutMap.get(a) == fakeRole || fakeJudgeMap.get(a) == Species.WEREWOLF).collect(Collectors.toList());
			// 候補がいなければ人間と判定していない村人陣営から
			if (candidates.isEmpty()) {
				candidates = villagers.stream()
						.filter(a -> fakeJudgeMap.get(a) != Species.HUMAN).collect(Collectors.toList());
			}
		}
		// 村人騙り，あるいは候補がいない場合ば村人陣営から選ぶ
		if (candidates.isEmpty()) {
			candidates = villagers;
		}
		// それでも候補がいない場合は裏切り者に投票
		if (candidates.isEmpty()) {
			candidates = possessedList;
		}
		if (!candidates.isEmpty()) {
			if (!candidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(candidates);
				if (canTalk) {
					enqueueTalk(estimateContent(me, voteCandidate, Role.WEREWOLF));
				}
			}
		} else {
			voteCandidate = null;
		}
	}

	@Override
	public String talk() {
		if (fakeRole != Role.VILLAGER) {
			if (!isCameout) {
				// 他の人狼のカミングアウト状況を調べて騙る役職が重複しないようにする
				int fakeSeerCo = 0;
				int fakeMediumCo = 0;
				for (Agent a : werewolves) {
					if (comingoutMap.get(a) == Role.SEER) {
						fakeSeerCo++;
					} else if (comingoutMap.get(a) == Role.MEDIUM) {
						fakeMediumCo++;
					}
				}
				if (fakeRole == Role.SEER && fakeSeerCo > 0 || fakeRole == Role.MEDIUM && fakeMediumCo > 0) {
					fakeRole = Role.VILLAGER; // 潜伏人狼
					enqueueWhisper(coContent(me, me, fakeRole));
				} else {
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
			}
			// カミングアウトしたらこれまでの偽判定結果をすべて公開
			else {
				while (!fakeJudgeQueue.isEmpty()) {
					Judge judge = fakeJudgeQueue.poll();
					if (fakeRole == Role.SEER) {
						enqueueTalk(divinedContent(me, judge.getTarget(), judge.getResult()));
					} else if (fakeRole == Role.MEDIUM) {
						enqueueTalk(identContent(me, judge.getTarget(), judge.getResult()));
					}
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
			attackReasonMap.put(a, coContent(a, a, comingoutMap.get(a)));
		}
		// 候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			candidates = villagers;
		}
		// 村人陣営がいない場合は裏切り者を襲う
		if (candidates.isEmpty()) {
			candidates = possessedList;
		}
		if (!candidates.isEmpty() && !candidates.contains(declaredAttackVoteCandidate)) {
			attackVoteCandidate = randomSelect(candidates);
		} else {
			attackVoteCandidate = null;
		}
	}

	@Override
	public Agent attack() {
		canWhisper = false;
		chooseAttackVoteCandidate();
		canWhisper = true;
		return attackVoteCandidate;
	}

	@Override
	public String whisper() {
		chooseAttackVoteCandidate();
		if (attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			Content reason = attackReasonMap.get(attackVoteCandidate);
			if (reason != null) {
				enqueueWhisper(becauseContent(me, reason, attackContent(me, attackVoteCandidate)));
			} else {
				enqueueWhisper(attackContent(me, attackVoteCandidate));
			}
			declaredAttackVoteCandidate = attackVoteCandidate;
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

}
