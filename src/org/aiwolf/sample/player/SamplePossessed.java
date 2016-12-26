/**
 * SamplePossessed.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivineContentBuilder;
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
import org.aiwolf.sample.lib.AbstractPossessed;

/**
 * <div lang="ja">裏切り者プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample possessed agent</div>
 */
public class SamplePossessed extends AbstractPossessed {

	GameInfo currentGameInfo;
	GameSetting gameSetting;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent voteCandidate; // 投票先候補
	Agent declaredVoteCandidate; // 宣言した投票先候補
	Vote lastVote; // 再投票における前回の投票
	Deque<Content> talkQueue = new LinkedList<>();
	List<Agent> seers = new ArrayList<>(); // 占い師候補リスト
	Agent trueSeer; // 真占い師と思われるエージェント
	List<Agent> werewolves = new ArrayList<>(); // 人狼候補リスト

	int comingoutDay; // カミングアウトする日
	List<Integer> comingoutDays = new ArrayList<>(Arrays.asList(1, 2, 3));
	boolean isCameout; // カミングアウト済みか否か
	Deque<Judge> judgeQueue = new LinkedList<>(); // 偽判定結果のFIFO
	List<Agent> judgedAgents = new ArrayList<>(); // 偽判定済みエージェントのリスト
	Role fakeRole; // 騙る役職

	@Override
	public String getName() {
		return "SamplePossessed";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		this.gameSetting = gameSetting;
		day = -1;
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		agi = new AdditionalGameInfo(gameInfo);
		seers.clear();
		trueSeer = null;
		werewolves.clear();

		// List<Role> fakeRoles = new ArrayList<>();
		// for (Role role : gameInfo.getExistingRoles()) {
		// if (role == Role.SEER || role == Role.MEDIUM) {
		// fakeRoles.add(role);
		// }
		// }
		// Collections.shuffle(fakeRoles);
		// fakeRole = fakeRoles.get(0);

		// Collections.shuffle(comingoutDays);
		// comingoutDay = comingoutDays.get(0);

		// 占い師騙りオンリー
		fakeRole = Role.SEER;

		// なるべく早く人狼に気づいてもらうために即カミングアウト
		comingoutDay = 0;

		isCameout = false;
		judgeQueue.clear();
		judgedAgents.clear();
	}

	@Override
	public void dayStart() {
		// このメソッドの前に呼ばれるupdate()に任せて，何もしない
	}

	@Override
	public void update(GameInfo gameInfo) {

		currentGameInfo = gameInfo;

		// 1日の最初のupdate()でdayStart()の機能を代行する
		if (currentGameInfo.getDay() == day + 1) { // 1日の最初のupdate()
			day = currentGameInfo.getDay();
			declaredVoteCandidate = null;
			voteCandidate = null;
			lastVote = null;
			talkQueue.clear();

			// 偽の判定
			if (day > 0) {
				Judge judge = getFakeJudge(fakeRole);
				if (judge != null) {
					judgeQueue.offer(judge);
					judgedAgents.add(judge.getTarget());
				}
			}
		}

		agi.update(currentGameInfo);
	}

	@Override
	public String talk() {
		// カミングアウトする日になったらカミングアウト
		if (!isCameout && day >= comingoutDay) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, fakeRole)));
			isCameout = true;
		}

		// カミングアウトしたらこれまでの偽判定結果をすべて公開
		if (isCameout) {
			while (!judgeQueue.isEmpty()) {
				Judge judge = judgeQueue.poll();
				if (fakeRole == Role.SEER) {
					enqueueTalk(new Content(new DivineContentBuilder(judge.getTarget(), judge.getResult())));
				} else if (fakeRole == Role.MEDIUM) {
					enqueueTalk(new Content(new InquestContentBuilder(judge.getTarget(), judge.getResult())));
				}
			}
		}

		chooseVoteCandidate();
		// 以前宣言した（未宣言を含む）投票先と違う投票先を選んだ場合宣言する
		if (voteCandidate != declaredVoteCandidate) {
			enqueueTalk(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}

		return dequeueTalk().getText();
	}

	@Override
	public Agent vote() {
		// 初回投票
		if (lastVote == null) {
			lastVote = new Vote(day, me, voteCandidate);
			return voteCandidate;
		}
		// 再投票：人狼候補以外で前回最多得票
		Counter<Agent> counter = new Counter<>();
		for (Vote vote : currentGameInfo.getLatestVoteList()) {
			if (!werewolves.contains(vote.getTarget())) {
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
		// 候補がいない場合：人狼候補以外から
		if (candidates.isEmpty()) {
			candidates.addAll(agi.getAliveOthers());
			candidates.removeAll(werewolves);
		}
		if (candidates.contains(voteCandidate)) {
			return voteCandidate;
		}
		Collections.shuffle(candidates);
		return candidates.get(0);
	}

	@Override
	public void finish() {
	}

	/**
	 * <div lang="ja">投票先候補を選ぶ</div>
	 *
	 * <div lang="en">Choose a candidate for vote.</div>
	 */
	void chooseVoteCandidate() {

		werewolves.clear();

		for (Judge judge : agi.getDivinationList()) {
			Agent agent = judge.getAgent();
			if (judge.getTarget() == me && judge.getResult() == Species.WEREWOLF) {
				// 自分を人狼と判定している占い師は人狼候補
				if (!werewolves.contains(agent)) {
					werewolves.add(agent);
				}
			} else if (agi.getKilledAgentList().contains(judge.getTarget()) && judge.getResult() == Species.WEREWOLF) {
				// 死亡したエージェントを人狼と判定した占い師は人狼候補
				if (!werewolves.contains(agent)) {
					werewolves.add(agent);
				}
			}
		}

		// 占い師と思われるエージェントに人狼だと占われているエージェントは人狼候補
		seers.clear();
		for (Judge judge : agi.getDivinationList()) {
			Agent agent = judge.getAgent();
			Agent target = judge.getTarget();
			if (!werewolves.contains(agent)) {
				if (!seers.contains(agent)) {
					seers.add(agent);
				}
				if (judge.getResult() == Species.WEREWOLF) {
					if (!werewolves.contains(target)) {
						werewolves.add(target);
					}
				}
			}
		}

		// 占い師候補を生存者に限定
		seers.removeAll(agi.getDeadOthers());

		if (seers.isEmpty()) {
			// 占い師候補なし
			trueSeer = null;
		} else {
			if (trueSeer == null || !seers.contains(trueSeer)) {
				Collections.shuffle(seers);
				trueSeer = seers.get(0);
			}
		}

		List<Agent> villagers = new ArrayList<>(agi.getAliveOthers());
		villagers.removeAll(werewolves);
		List<Agent> candidates = new ArrayList<>();
		// 占い師/霊媒師騙りの場合
		if (fakeRole == Role.SEER || fakeRole == Role.MEDIUM) {
			// 対抗カミングアウトのエージェントは投票先候補
			for (Agent agent : villagers) {
				if (agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == fakeRole) {
					candidates.add(agent);
				}
			}
			// 人狼と判定したエージェントは投票先候補
			List<Agent> fakeHumans = new ArrayList<>();
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

		// 村人騙りなら不必要
		if (role == Role.VILLAGER) {
			return null;
		}
		// 占い師騙りの場合
		else if (fakeRole == Role.SEER) {
			List<Agent> candidates = new ArrayList<>();
			for (Agent agent : agi.getAliveOthers()) {
				if (!judgedAgents.contains(agent) && agi.getComingoutMap().get(agent) != fakeRole) {
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
		}
		// 霊媒師騙りの場合
		else if (role == Role.MEDIUM) {
			target = currentGameInfo.getExecutedAgent();
			if (target == null) {
				return null;
			}
		}
		// 偽人狼に余裕があれば，人狼と人間の割合を勘案して，30%の確率で人狼と判定
		Species result = Species.HUMAN;
		if (SampleWerewolf.countWolfJudge(judgeQueue) < gameSetting.getRoleNum(Role.WEREWOLF) && Math.random() < 0.3) {
			result = Species.WEREWOLF;
		}
		return new Judge(day, me, target, result);
	}

	/**
	 * <div lang="ja">発話を待ち行列に入れる</div>
	 *
	 * <div lang="en">Enqueue a utterance.</div>
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
	 * <div lang="en">Dequeue a utterance.</div>
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

}
