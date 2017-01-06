/**
 * SampleWerewolf.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivineContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.InquestContentBuilder;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.util.Counter;
import org.aiwolf.sample.lib.AbstractWerewolf;

/**
 * <div lang="ja">人狼プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample werewolf agent</div>
 */
public class SampleWerewolf extends AbstractWerewolf {

	GameInfo currentGameInfo;
	GameSetting gameSetting;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent voteCandidate; // 投票先候補
	Agent declaredVoteCandidate; // 宣言した投票先候補
	Agent attackCandidate; // 襲撃先候補
	Agent declaredAttackCandidate; // 宣言した襲撃先候補
	Vote lastVote; // 再投票における前回の投票
	Vote lastAttackVote; // 襲撃再投票における前回の投票
	Deque<Content> talkQueue = new LinkedList<>();
	Deque<Content> whisperQueue = new LinkedList<>();
	Agent possessed; // 裏切り者と思われるプレイヤー
	List<Agent> werewolves; // 人狼リスト
	List<Agent> aliveWerewolves = new ArrayList<>(); // 生存人狼リスト
	List<Agent> humans; // 人間リスト

	int talkTurn; // talk()のターン
	int whisperTurn; // whisper()のターン
	int comingoutDay; // カミングアウトする日
	List<Integer> comingoutDays = new ArrayList<>(Arrays.asList(1, 2, 3));
	int comingoutTurn; // カミングアウトするターン
	List<Integer> comingoutTurns = new ArrayList<>(Arrays.asList(0, 1, 2));
	boolean isCameout; // カミングアウト済みか否か
	Deque<Judge> divinationQueue = new LinkedList<>(); // 偽占い結果のFIFO
	Deque<Judge> inquestQueue = new LinkedList<>(); // 偽霊媒結果のFIFO
	List<Agent> divinedAgents = new ArrayList<>(); // 偽占い済みエージェントのリスト
	Role fakeRole; // 騙る役職
	boolean isFixFakeRole = false; // 騙る役職が決まったかどうか
	List<Role> fakeRoles = new ArrayList<>(Arrays.asList(Role.VILLAGER)); // 騙れる役職のリスト
	Judge lastFakeJudge;

	@Override
	public String getName() {
		return "SampleWerewolf";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		this.gameSetting = gameSetting;
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		agi = new AdditionalGameInfo(gameInfo);
		werewolves = new ArrayList<>(gameInfo.getRoleMap().keySet());
		humans = new ArrayList<>(agi.getAliveOthers());
		humans.removeAll(werewolves);

		for (Role role : gameInfo.getExistingRoles()) {
			if (role == Role.SEER || role == Role.MEDIUM) {
				fakeRoles.add(role);
			}
		}
		// 暫定的に騙る役職を決める
		Collections.shuffle(fakeRoles);
		fakeRole = fakeRoles.get(0);

		// 1～3日目のランダムな日にカミングアウトする．他の人狼との同時カミングアウトを避けるため発話ターンは散らす
		Collections.shuffle(comingoutDays);
		comingoutDay = comingoutDays.get(0);
		Collections.shuffle(comingoutTurns);
		comingoutTurn = comingoutTurns.get(0);

		isCameout = false;
		inquestQueue.clear();
		divinationQueue.clear();
		divinedAgents.clear();
	}

	@Override
	public void dayStart() {
		declaredVoteCandidate = null;
		voteCandidate = null;
		declaredAttackCandidate = null;
		attackCandidate = null;
		lastVote = null;
		lastAttackVote = null;
		lastFakeJudge = null;
		talkQueue.clear();
		whisperQueue.clear();
		talkTurn = -1;
		whisperTurn = -1;
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		day = currentGameInfo.getDay();
		int lastDivIdx = agi.getDivinationList().size();
		int lastInqIdx = agi.getInquestList().size();
		agi.update(currentGameInfo);
		aliveWerewolves.clear();
		aliveWerewolves.addAll(werewolves);
		aliveWerewolves.removeAll(agi.getDeadOthers());

		// 人狼1の場合whisper()が呼ばれないので0日目はupdate()で担当
		if (aliveWerewolves.size() == 1 && day == 0) {
			// 翌日の偽判定を作成
			if (lastFakeJudge == null) {
				if (fakeRole == Role.SEER) {
					Judge divination = getFakeJudge(Role.SEER);
					if (divination != null) {
						lastFakeJudge = divination;
						divinationQueue.offer(divination);
						divinedAgents.add(divination.getTarget());
					}
				}
				if (fakeRole == Role.MEDIUM) {
					Judge inquest = getFakeJudge(Role.MEDIUM);
					if (inquest != null) {
						lastFakeJudge = inquest;
						inquestQueue.offer(inquest);
					}
				}
			}
		}

		List<Agent> possessedPersons = new ArrayList<>();
		// 占い結果が嘘の場合，裏切り者候補
		for (int i = lastDivIdx; i < agi.getDivinationList().size(); i++) {
			Judge judge = agi.getDivinationList().get(i);
			if ((humans.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF) || (werewolves.contains(judge.getTarget()) && judge.getResult() == Species.HUMAN)) {
				if (!werewolves.contains(judge.getAgent()) && !possessedPersons.contains(judge.getAgent())) {
					possessedPersons.add(judge.getAgent());
				}
			}
		}
		// 霊媒結果が嘘の場合，裏切り者候補
		for (int i = lastInqIdx; i < agi.getInquestList().size(); i++) {
			Judge judge = agi.getInquestList().get(i);
			if ((humans.contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF) || (werewolves.contains(judge.getTarget()) && judge.getResult() == Species.HUMAN)) {
				if (!werewolves.contains(judge.getAgent()) && !possessedPersons.contains(judge.getAgent())) {
					possessedPersons.add(judge.getAgent());
				}
			}
		}
		if (!possessedPersons.isEmpty()) {
			if (!possessedPersons.contains(possessed)) {
				Collections.shuffle(possessedPersons);
				possessed = possessedPersons.get(0);
				enqueueWhisper(new Content(new EstimateContentBuilder(possessed, Role.POSSESSED)));
			}
		}
	}

	@Override
	public String talk() {
		talkTurn++;

		// まず他の人狼のカミングアウト状況を調べる
		int fakeSeerCO = 0;
		int fakeMediumCO = 0;
		for (Agent wolf : werewolves) {
			if (agi.getComingoutMap().get(wolf) == Role.SEER) {
				fakeSeerCO++;
			} else if (agi.getComingoutMap().get(wolf) == Role.MEDIUM) {
				fakeMediumCO++;
			}
		}

		// 直前の調整の結果，潜伏人狼と決定した場合，すぐに空のカミングアウトをしたと考える
		if (!isCameout && fakeRole == Role.VILLAGER) {
			isCameout = true;
			removeWhisper(Topic.DIVINED);
			removeWhisper(Topic.INQUESTED);
		} else if (fakeRole == Role.SEER) {
			// カミングアウトする日になったら，あるいは対抗カミングアウトがあればカミングアウト
			if (!isCameout && (day >= comingoutDay || agi.getComingoutMap().containsValue(Role.SEER)) && talkTurn >= comingoutTurn) {
				isCameout = true;
				// 既に偽占い師カミングアウトありの場合潜伏
				if (fakeSeerCO > 0) {
					fakeRole = Role.VILLAGER;
					removeWhisper(Topic.DIVINED);
					removeWhisper(Topic.INQUESTED);
				}
				// 占い師を騙る
				else {
					enqueueTalk(new Content(new ComingoutContentBuilder(me, fakeRole)));
					removeWhisper(Topic.INQUESTED);
				}
			}
		} else if (fakeRole == Role.MEDIUM) {
			// カミングアウトする日になったら，あるいは対抗カミングアウトがあればカミングアウト
			if (!isCameout && (day >= comingoutDay || agi.getComingoutMap().containsValue(Role.MEDIUM)) && talkTurn >= comingoutTurn) {
				isCameout = true;
				// 既に偽霊媒師カミングアウトありの場合潜伏
				if (fakeMediumCO > 0) {
					fakeRole = Role.VILLAGER;
					removeWhisper(Topic.DIVINED);
					removeWhisper(Topic.INQUESTED);
				}
				// 霊媒師を騙る
				else {
					enqueueTalk(new Content(new ComingoutContentBuilder(me, fakeRole)));
					removeWhisper(Topic.DIVINED);
				}
			}
		}

		// カミングアウトしたら，これまでの偽判定結果をすべて公開
		if (isCameout) {
			if (fakeRole == Role.SEER) {
				while (!divinationQueue.isEmpty()) {
					Judge divination = divinationQueue.poll();
					enqueueTalk(new Content(new DivineContentBuilder(divination.getTarget(), divination.getResult())));
				}
			} else if (fakeRole == Role.MEDIUM) {
				while (!inquestQueue.isEmpty()) {
					Judge inquest = inquestQueue.poll();
					enqueueTalk(new Content(new InquestContentBuilder(inquest.getTarget(), inquest.getResult())));
				}
			}
		}

		chooseVoteCandidate();
		// 以前宣言した（未宣言を含む）投票先と違う投票先を選んだ場合宣言する
		if (voteCandidate != declaredVoteCandidate)

		{
			enqueueTalk(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}

		return dequeueTalk().getText();
	}

	@Override
	public String whisper() {
		whisperTurn++;

		// 初日(Day0)は，騙る役職の調整をする
		if (day == 0) {
			if (!isFixFakeRole) {
				// 最初に騙る役職を宣言
				if (whisperTurn == 0) {
					enqueueWhisper(new Content(new ComingoutContentBuilder(me, fakeRole)));
				} else {
					Role newFakeRole = coordinateFakeRole();
					if (newFakeRole == null) {
						isFixFakeRole = true;
					} else if (newFakeRole != fakeRole) {
						fakeRole = newFakeRole;
						enqueueWhisper(new Content(new ComingoutContentBuilder(me, fakeRole)));
					}
				}
			}
		} else {
			chooseAttackCandidate();
			// 以前宣言した（未宣言を含む）襲撃先と違う襲撃先を選んだ場合宣言する
			if (attackCandidate != declaredAttackCandidate) {
				enqueueWhisper(new Content(new AttackContentBuilder(attackCandidate)));
				declaredAttackCandidate = attackCandidate;
			}
		}

		if (isFixFakeRole && lastFakeJudge == null) {
			// 追放結果判明後なので翌日の偽判定を作成
			if (fakeRole == Role.SEER) {
				Judge divination = getFakeJudge(Role.SEER);
				if (divination != null) {
					lastFakeJudge = divination;
					divinationQueue.offer(divination);
					divinedAgents.add(divination.getTarget());
					enqueueWhisper(new Content(new DivineContentBuilder(divination.getTarget(), divination.getResult())));
				}
			}
			if (fakeRole == Role.MEDIUM) {
				Judge inquest = getFakeJudge(Role.MEDIUM);
				if (inquest != null) {
					lastFakeJudge = inquest;
					inquestQueue.offer(inquest);
					enqueueWhisper(new Content(new InquestContentBuilder(inquest.getTarget(), inquest.getResult())));
				}
			}
		}

		return dequeueWhisper().getText();
	}

	@Override
	public Agent vote() {
		// 初回投票
		if (lastVote == null) {
			lastVote = new Vote(day, me, voteCandidate);
			return voteCandidate;
		}
		// 再投票：前回最多得票の人間
		Counter<Agent> counter = new Counter<>();
		for (Vote vote : currentGameInfo.getLatestVoteList()) {
			if (humans.contains(vote.getTarget())) {
				counter.add(vote.getTarget());
			}
		}
		int max = counter.get(counter.getLargest());
		List<Agent> candidates = new ArrayList<>();
		for (Agent agent : counter) {
			if (counter.get(agent) == max) {
				candidates.add(agent);
			}
		}
		// 候補がいない場合：村人陣営から
		if (candidates.isEmpty()) {
			candidates.addAll(agi.getAliveOthers());
			candidates.removeAll(werewolves);
			candidates.remove(possessed);
		}
		if (candidates.contains(voteCandidate)) {
			return voteCandidate;
		}
		Collections.shuffle(candidates);
		return candidates.get(0);
	}

	@Override
	public Agent attack() {

		// 人狼1の場合whisper()が呼ばれないので1日目以降はattack()で担当
		if (aliveWerewolves.size() == 1) {
			chooseAttackCandidate();

			// 追放結果判明後なので翌日の偽判定を作成
			if (lastFakeJudge == null) {
				if (fakeRole == Role.SEER) {
					Judge divination = getFakeJudge(Role.SEER);
					if (divination != null) {
						lastFakeJudge = divination;
						divinationQueue.offer(divination);
						divinedAgents.add(divination.getTarget());
					}
				}
				if (fakeRole == Role.MEDIUM) {
					Judge inquest = getFakeJudge(Role.MEDIUM);
					if (inquest != null) {
						lastFakeJudge = inquest;
						inquestQueue.offer(inquest);
					}
				}
			}
		}

		// 初回投票
		if (lastAttackVote == null) {
			lastAttackVote = new Vote(day, me, attackCandidate);
			return attackCandidate;
		}
		// 再投票：前回最多得票数の人間
		List<Agent> candidates = new ArrayList<>();
		Counter<Agent> counter = new Counter<>();
		for (Vote vote : currentGameInfo.getLatestAttackVoteList()) {
			if (humans.contains(vote.getTarget())) {
				counter.add(vote.getTarget());
			}
		}
		int max = counter.get(counter.getLargest());
		for (Agent agent : counter) {
			if (counter.get(agent) == max) {
				candidates.add(agent);
			}
		}
		// 候補がいない場合：村人陣営から
		if (candidates.isEmpty()) {
			candidates.addAll(agi.getAliveOthers());
			candidates.removeAll(werewolves);
			candidates.remove(possessed);
		}
		// 候補がいない場合：人間から
		if (candidates.isEmpty()) {
			candidates.addAll(humans);
		}
		if (candidates.contains(attackCandidate)) {
			return attackCandidate;
		}
		Collections.shuffle(candidates);
		return candidates.get(0);
	}

	@Override
	public void finish() {
	}

	/**
	 * <div lang="ja">騙る役職について，他の人狼との間で調整をする</div>
	 *
	 * <div lang="en">Coordinates with other werewolves on the fake role.</div>
	 */
	Role coordinateFakeRole() {
		// 騙る役職の希望調査
		Map<Role, Integer> roleDesireMap = new HashMap<>();
		for (Role role : fakeRoles) {
			roleDesireMap.put(role, 0);
		}
		for (Role role : agi.getWhisperedComingoutMap().values()) {
			if (fakeRoles.contains(role)) {
				roleDesireMap.put(role, roleDesireMap.get(role) + 1);
			}
		}

		// 騙る役職が確定したか判定
		boolean isFixed = true;
		// 人手が足りている場合（普通のケース），村人以外への希望がすべて1なら確定
		if (fakeRoles.size() <= werewolves.size()) {
			for (Role role : fakeRoles) {
				if (role != Role.VILLAGER && roleDesireMap.get(role) != 1) {
					isFixed = false;
					break;
				}
			}
		}
		// 人手不足の場合，村人希望が0で村人以外への希望がすべて1以下なら確定
		else {
			for (Role role : fakeRoles) {
				if ((role == Role.VILLAGER && roleDesireMap.get(role) > 0) || roleDesireMap.get(role) > 1) {
					isFixed = false;
					break;
				}
			}
		}
		if (isFixed) {
			return null;
		}

		switch (fakeRole) {
		case VILLAGER:
			// 村人騙り希望でも，空き役職がある場合，確率0.5でそちらに転向
			for (Role role : fakeRoles) {
				if (role != Role.VILLAGER && roleDesireMap.get(role) == 0 && Math.random() < 0.5) {
					return role;
				}
			}
			break;

		case SEER:
			// 自分の他に占い師希望者がいた場合，確率0.5で村人に転向
			if (roleDesireMap.get(Role.SEER) > 1 && Math.random() < 0.5) {
				return Role.VILLAGER;
			}
			break;

		case MEDIUM:
			// 自分の他に霊媒師希望者がいた場合，確率0.5で村人に転向
			if (roleDesireMap.get(Role.MEDIUM) > 1 && Math.random() < 0.5) {
				return Role.VILLAGER;
			}
			break;

		default:
			break;
		}
		return fakeRole;
	}

	/**
	 * <div lang="ja">投票先候補を選ぶ</div>
	 *
	 * <div lang="en">Chooses a candidate for vote.</div>
	 */
	void chooseVoteCandidate() {
		List<Agent> villagers = new ArrayList<>(agi.getAliveOthers());
		villagers.removeAll(werewolves);
		villagers.remove(possessed);
		List<Agent> candidates = villagers; // 村人騙りの場合は村人陣営から

		// 占い師/霊媒師騙りの場合
		if (fakeRole == Role.SEER || fakeRole == Role.MEDIUM) {
			candidates = new ArrayList<>();
			// 対抗カミングアウトのエージェントは投票先候補
			for (Agent agent : villagers) {
				if (agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == fakeRole) {
					candidates.add(agent);
				}
			}
			// 人狼と判定したエージェントは投票先候補
			List<Agent> fakeHumans = new ArrayList<>();
			Deque<Judge> judgeQueue = null;
			if (fakeRole == Role.SEER) {
				judgeQueue = divinationQueue;
			} else if (fakeRole == Role.MEDIUM) {
				judgeQueue = inquestQueue;
			}
			for (Judge judge : judgeQueue) {
				if (judge.getResult() == Species.HUMAN) {
					fakeHumans.add(judge.getTarget());
				} else if (!candidates.contains(judge.getTarget())) {
					candidates.add(judge.getTarget());
				}
			}
			// 候補がいなければ人間と判定していない村人陣営から
			if (candidates.isEmpty()) {
				candidates.addAll(villagers);
				candidates.removeAll(fakeHumans);
				// それでも候補がいなければ村人陣営から
				if (candidates.isEmpty()) {
					candidates.addAll(villagers);
				}
			}
		}
		if (candidates.contains(voteCandidate)) {
			return;
		} else {
			Collections.shuffle(candidates);
			voteCandidate = candidates.get(0);
		}
	}

	/**
	 * <div lang="ja">襲撃先候補を選ぶ</div>
	 *
	 * <div lang="en">Chooses a candidate for attack.</div>
	 */
	void chooseAttackCandidate() {
		// カミングアウトした村人陣営は襲撃先候補
		List<Agent> villagers = new ArrayList<>(agi.getAliveOthers());
		villagers.removeAll(werewolves);
		villagers.remove(possessed);
		List<Agent> candidates = new ArrayList<>();
		for (Agent agent : villagers) {
			if (agi.getComingoutMap().containsKey(agent)) {
				candidates.add(agent);
			}
		}
		// 候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			candidates.addAll(villagers);
		}
		if (candidates.contains(attackCandidate)) {
			return;
		}
		Collections.shuffle(candidates);
		attackCandidate = candidates.get(0);
	}

	/**
	 * <div lang="ja">偽判定を返す</div>
	 *
	 * <div lang="en">Returns the fake judge.</div>
	 * 
	 * @param role
	 *            <div lang="ja">騙る役職を表す{@code Role}</div>
	 *
	 *            <div lang="en">{@code Role} representing the fake role.</div>
	 * 
	 * @return <div lang="ja">偽判定を表す{@code Judge}</div>
	 *
	 *         <div lang="en">{@code Judge} representing the fake judge.</div>
	 */
	Judge getFakeJudge(Role role) {
		Agent target = null;
		Species result = null;

		// 村人騙りなら不必要
		if (role == Role.VILLAGER) {
			return null;
		}
		// 占い師騙りの場合
		else if (role == Role.SEER) {
			List<Agent> candidates = new ArrayList<>();
			for (Agent agent : agi.getAliveOthers()) {
				if (!divinedAgents.contains(agent) && agi.getComingoutMap().get(agent) != fakeRole) {
					candidates.add(agent);
				}
			}

			if (!candidates.isEmpty()) {
				Collections.shuffle(candidates);
				target = candidates.get(0);
			} else {
				candidates.clear();
				candidates.addAll(agi.getAliveOthers());
				Collections.shuffle(candidates);
				target = candidates.get(0);
			}

			result = Species.HUMAN;
			// 人間が偽占い対象の場合
			if (humans.contains(target)) {
				// 偽人狼に余裕があれば
				if (countWolfJudge(divinationQueue) < gameSetting.getRoleNum(Role.WEREWOLF)) {
					// 裏切り者，あるいはまだカミングアウトしていないエージェントの場合，判定は五分五分
					if ((target == possessed || !agi.getComingoutMap().containsKey(target)) && Math.random() < 0.5) {
						result = Species.WEREWOLF;
					}
					// それ以外は人狼判定
					else {
						result = Species.WEREWOLF;
					}
				}
			}
		}
		// 霊媒師騙りの場合
		else if (role == Role.MEDIUM) {
			if (currentGameInfo.getLatestExecutedAgent() != null) {
				target = currentGameInfo.getLatestExecutedAgent();
			} else {
				target = currentGameInfo.getExecutedAgent();
			}
			if (target == null) {
				return null;
			}

			result = Species.HUMAN;
			// 人間が霊媒対象の場合
			if (humans.contains(target)) {
				// 偽人狼に余裕があれば
				if (countWolfJudge(inquestQueue) < gameSetting.getRoleNum(Role.WEREWOLF)) {
					// 裏切り者，あるいはまだカミングアウトしていないエージェントの場合，判定は五分五分
					if ((target == possessed || !agi.getComingoutMap().containsKey(target)) && Math.random() < 0.5) {
						result = Species.WEREWOLF;
					}
					// それ以外は人狼判定
					else {
						result = Species.WEREWOLF;
					}
				}
			}
		}
		return new Judge(day, me, target, result);
	}

	/**
	 * <div lang="ja">{@code Deque<Judge>}の中で，{@code result}が{@code WEREWOLF}であるものの数を返す</div>
	 *
	 * <div lang="en">Returns the number of {@code Judge}s whose {@code result} is {@code WEREWOLF} in the
	 * {@code Deque<Judge>}.</div>
	 * 
	 * @param judges
	 *            {@code Deque<Judge>}
	 */
	static int countWolfJudge(Deque<Judge> judges) {
		int count = 0;
		for (Judge judge : judges) {
			if (judge.getResult() == Species.WEREWOLF) {
				count++;
			}
		}
		return count;
	}

	/**
	 * <div lang="ja">発話を待ち行列に入れる</div>
	 *
	 * <div lang="en">Enqueues a utterance.</div>
	 * 
	 * @param newContent
	 *            <div lang="ja">発話を表す{@code Content}</div>
	 *
	 *            <div lang="en">{@code Content} representing the utterance.</div>
	 */
	void enqueueTalk(Content newContent) {
		String newText = newContent.getText();
		Topic newTopic = newContent.getTopic();
		Iterator<Content> it = talkQueue.iterator();
		boolean isEnqueue = true;

		switch (newTopic) {
		case AGREE:
		case DISAGREE:
			// 同一のものが待ち行列になければ入れる
			while (it.hasNext()) {
				if (it.next().getText().equals(newText)) {
					isEnqueue = false;
					break;
				}
			}
			break;

		case COMINGOUT:
			// 同じエージェントについての異なる役職のカミングアウトが待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.COMINGOUT && content.getTarget() == newContent.getTarget()) {
					if (content.getRole() == newContent.getRole()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case ESTIMATE:
			// 同じエージェントについての推測役職が異なる推測発言が待ち行列に残っていればそちらを取り下げ新しい方を待ち行列に入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.ESTIMATE && content.getTarget() == newContent.getTarget()) {
					if (content.getRole() == newContent.getRole()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case DIVINED:
			// 同じエージェントについての異なる占い結果が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.DIVINED && content.getTarget() == newContent.getTarget()) {
					if (content.getResult() == newContent.getResult()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case INQUESTED:
			// 同じエージェントについての異なる霊媒結果が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.INQUESTED && content.getTarget() == newContent.getTarget()) {
					if (content.getResult() == newContent.getResult()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case VOTE:
			// 異なる投票先宣言が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.VOTE) {
					if (content.getTarget() == newContent.getTarget()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		default:
			break;
		}

		if (isEnqueue) {
			if (newContent.getTopic() == Topic.ESTIMATE) {
				// 過去の推測発言で同一のものには同意発言，相反するものには不同意発言
				if (agi.getEstimateMap().containsKey(newContent.getTarget())) {
					for (Talk talk : agi.getEstimateMap().get(newContent.getTarget())) {
						Content pastContent = new Content(talk.getText());
						if (pastContent.getRole() == newContent.getRole()) {
							enqueueTalk(new Content(new AgreeContentBuilder(TalkType.TALK, talk.getDay(), talk.getIdx())));
						} else {
							enqueueTalk(new Content(new DisagreeContentBuilder(TalkType.TALK, talk.getDay(), talk.getIdx())));
						}
					}
				}
			}
			talkQueue.offer(newContent);
		}
	}

	/**
	 * <div lang="ja">発話を待ち行列から取り出す</div>
	 *
	 * <div lang="en">Dequeues a utterance.</div>
	 * 
	 * @return <div lang="ja">発話を表す{@code Content}</div>
	 *
	 *         <div lang="en">{@code Content} representing the utterance.</div>
	 */
	Content dequeueTalk() {
		if (talkQueue.isEmpty()) {
			return Content.SKIP;
		}
		return talkQueue.poll();
	}

	/**
	 * <div lang="ja">囁きを待ち行列に入れる</div>
	 *
	 * <div lang="en">Enqueues a whispered utterance.</div>
	 * 
	 * @param newContent
	 *            <div lang="ja">発話を表す{@code Content}</div>
	 *
	 *            <div lang="en">{@code Content} representing the utterance.</div>
	 */
	void enqueueWhisper(Content newContent) {
		String newText = newContent.getText();
		Topic newTopic = newContent.getTopic();
		Iterator<Content> it = whisperQueue.iterator();
		boolean isEnqueue = true;

		switch (newTopic) {
		case AGREE:
		case DISAGREE:
			// 同一のものが待ち行列になければ入れる
			while (it.hasNext()) {
				if (it.next().getText().equals(newText)) {
					isEnqueue = false;
					break;
				}
			}
			break;

		case COMINGOUT:
			// 同じエージェントについての異なる役職のカミングアウトが待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.COMINGOUT && content.getTarget() == newContent.getTarget()) {
					if (content.getRole() == newContent.getRole()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case ESTIMATE:
			// 同じエージェントについての推測役職が異なる推測発言が待ち行列に残っていればそちらを取り下げ新しい方を待ち行列に入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.ESTIMATE && content.getTarget() == newContent.getTarget()) {
					if (content.getRole() == newContent.getRole()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case DIVINED:
			// 同じエージェントについての異なる占い結果が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.DIVINED && content.getTarget() == newContent.getTarget()) {
					if (content.getResult() == newContent.getResult()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case INQUESTED:
			// 同じエージェントについての異なる霊媒結果が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.INQUESTED && content.getTarget() == newContent.getTarget()) {
					if (content.getResult() == newContent.getResult()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case VOTE:
			// 異なる投票先宣言が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.VOTE) {
					if (content.getTarget() == newContent.getTarget()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		case ATTACK:
			// 異なる襲撃先宣言が待ち行列に残っていればそれを取り下げて新しい方を入れる
			while (it.hasNext()) {
				Content content = it.next();
				if (content.getTopic() == Topic.ATTACK) {
					if (content.getTarget() == newContent.getTarget()) {
						isEnqueue = false;
						break;
					} else {
						it.remove();
					}
				}
			}
			break;

		default:
			break;
		}

		if (isEnqueue) {
			whisperQueue.offer(newContent);
		}
	}

	/**
	 * <div lang="ja">囁きを待ち行列から取り出す</div>
	 *
	 * <div lang="en">Dequeues a whispered utterance.</div>
	 * 
	 * @return <div lang="ja">発話を表す{@code Content}</div>
	 *
	 *         <div lang="en">{@code Content} representing the utterance.</div>
	 */
	Content dequeueWhisper() {
		if (whisperQueue.isEmpty()) {
			return Content.SKIP;
		}
		return whisperQueue.poll();
	}

	/**
	 * <div lang="ja">指定したトピックの囁きを取り除く</div>
	 *
	 * <div lang="en">Removes the whispers of given topic.</div>
	 * 
	 * @param topic
	 *            <div lang="ja">取り除くトピックを表す{@code Topic}</div>
	 *
	 *            <div lang="en">{@code Topic} representing the topic to be removed from the queue.</div>
	 */
	void removeWhisper(Topic topic) {
		Iterator<Content> it = whisperQueue.iterator();
		while (it.hasNext()) {
			Content content = it.next();
			if (content.getTopic() == topic) {
				it.remove();
			}
		}
	}

}
