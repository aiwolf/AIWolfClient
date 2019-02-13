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

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
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

	/** カミングアウトするターン */
	int comingoutTurn;

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

	/** talk()のターン */
	int talkTurn;

	/** 襲撃投票先候補 */
	Agent attackVoteCandidate;

	/** 宣言済み襲撃投票先候補 */
	Agent declaredAttackVoteCandidate;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		whisperQueue.clear();
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		werewolves = new ArrayList<>(gameInfo.getRoleMap().keySet());
		humans = new ArrayList<>();
		for (Agent a : aliveOthers) {
			if (!werewolves.contains(a)) {
				humans.add(a);
			}
		}
		// ランダムに騙る役職を決める
		List<Role> roles = new ArrayList<>();
		for (Role r : Arrays.asList(Role.VILLAGER, Role.SEER, Role.MEDIUM)) {
			if (gameInfo.getExistingRoles().contains(r)) {
				roles.add(r);
			}
		}
		fakeRole = randomSelect(roles);
		// 1～3日目からランダムにカミングアウトする
		comingoutDay = (int) (Math.random() * 3 + 1);
		// 第0～4ターンからランダムにカミングアウトする
		comingoutTurn = (int) (Math.random() * 5);
		isCameout = false;
		fakeJudgeMap.clear();
		fakeJudgeQueue.clear();
		possessedList.clear();
	}

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);
		// 占い/霊媒結果が嘘の場合，裏切り者候補
		for (Judge j : divinationList) {
			Agent agent = j.getAgent();
			if (!werewolves.contains(agent) && ((humans.contains(j.getTarget()) && j.getResult() == Species.WEREWOLF) || (werewolves.contains(j.getTarget()) && j.getResult() == Species.HUMAN))) {
				if (!possessedList.contains(agent)) {
					possessedList.add(agent);
					enqueueWhisper(new Content(new EstimateContentBuilder(me, agent, Role.POSSESSED)));
				}
			}
		}
		villagers.clear();
		for (Agent agent : aliveOthers) {
			if (!werewolves.contains(agent) && !possessedList.contains(agent)) {
				villagers.add(agent);
			}
		}
	}

	private Judge getFakeJudge() {
		Agent target = null;
		// 占い師騙りの場合
		if (fakeRole == Role.SEER) {
			List<Agent> candidates = new ArrayList<>();
			for (Agent a : aliveOthers) {
				if (!fakeJudgeMap.containsKey(a) && comingoutMap.get(a) != Role.SEER) {
					candidates.add(a);
				}
			}
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
		talkTurn = -1;
		if (day == 0) {
			enqueueWhisper(new Content(new ComingoutContentBuilder(me, me, fakeRole)));
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
			for (Agent a : villagers) {
				if (comingoutMap.get(a) == fakeRole || fakeJudgeMap.get(a) == Species.WEREWOLF) {
					candidates.add(a);
				}
			}
			// 候補がいなければ人間と判定していない村人陣営から
			if (candidates.isEmpty()) {
				for (Agent a : villagers) {
					if (fakeJudgeMap.get(a) != Species.HUMAN) {
						candidates.add(a);
					}
				}
			}
		}
		// 村人騙り，あるいは候補がいない場合ば村人陣営から選ぶ
		if (candidates.isEmpty()) {
			candidates.addAll(villagers);
		}
		// それでも候補がいない場合は裏切り者に投票
		if (candidates.isEmpty()) {
			candidates.addAll(possessedList);
		}
		if (!candidates.isEmpty()) {
			if (!candidates.contains(voteCandidate)) {
				voteCandidate = randomSelect(candidates);
				if (canTalk) {
					enqueueTalk(new Content(new EstimateContentBuilder(me, voteCandidate, Role.WEREWOLF)));
				}
			}
		} else {
			voteCandidate = null;
		}
	}

	@Override
	public String talk() {
		talkTurn++;
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
					enqueueWhisper(new Content(new ComingoutContentBuilder(me, me, fakeRole)));
				} else {
					// 対抗カミングアウトがある場合，今日カミングアウトする
					for (Agent a : humans) {
						if (comingoutMap.get(a) == fakeRole) {
							comingoutDay = day;
						}
					}
					// カミングアウトするタイミングになったらカミングアウト
					if (day >= comingoutDay && talkTurn >= comingoutTurn) {
						isCameout = true;
						enqueueTalk(new Content(new ComingoutContentBuilder(me, me, fakeRole)));
					}
				}
			}
			// カミングアウトしたらこれまでの偽判定結果をすべて公開
			else {
				while (!fakeJudgeQueue.isEmpty()) {
					Judge judge = fakeJudgeQueue.poll();
					if (fakeRole == Role.SEER) {
						enqueueTalk(new Content(new DivinedResultContentBuilder(me, judge.getTarget(), judge.getResult())));
					} else if (fakeRole == Role.MEDIUM) {
						enqueueTalk(new Content(new IdentContentBuilder(me, judge.getTarget(), judge.getResult())));
					}
				}
			}
		}
		return super.talk();
	}

	/** 襲撃先候補を選ぶ */
	void chooseAttackVoteCandidate() {
		// カミングアウトした村人陣営は襲撃先候補
		List<Agent> candidates = new ArrayList<>();
		for (Agent a : villagers) {
			if (isCo(a)) {
				candidates.add(a);
			}
		}
		// 候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			candidates.addAll(villagers);
		}
		// 村人陣営がいない場合は裏切り者を襲う
		if (candidates.isEmpty()) {
			candidates.addAll(possessedList);
		}
		if (!candidates.isEmpty()) {
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
			enqueueWhisper(new Content(new AttackContentBuilder(me, attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return dequeueWhisper();
	}

	void enqueueWhisper(Content content) {
		if (content.getSubject() == Agent.UNSPEC) {
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
