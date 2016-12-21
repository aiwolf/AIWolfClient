/**
 * SampleSeer.java
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

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivineContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
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
import org.aiwolf.sample.lib.AbstractSeer;

/**
 * <div lang="ja">占い師プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample seer agent</div>
 */
public class SampleSeer extends AbstractSeer {

	GameInfo currentGameInfo;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent voteCandidate; // 投票先候補
	Agent declaredVoteCandidate; // 宣言した投票先候補
	Vote lastVote; // 再投票における前回の投票
	Deque<Content> talkQueue = new LinkedList<>();
	List<Agent> humans = new ArrayList<>(); // 人間リスト
	List<Agent> werewolves = new ArrayList<>(); // 人狼リスト
	List<Agent> semiwolves = new ArrayList<>(); // 人狼かもリスト

	int comingoutDay; // カミングアウトする日
	List<Integer> comingoutDays = new ArrayList<>(Arrays.asList(1, 2, 3));
	boolean isCameout; // カミングアウト済みか否か
	Deque<Judge> divinationQueue = new LinkedList<>(); // 占い結果のFIFO

	@Override
	public String getName() {
		return "SampleSeer";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		agi = new AdditionalGameInfo(gameInfo);
		humans.clear();
		werewolves.clear();
		semiwolves.clear();

		Collections.shuffle(comingoutDays);
		comingoutDay = comingoutDays.get(0); // 1～3日目をランダムで
		isCameout = false;
		divinationQueue.clear();
	}

	@Override
	public void dayStart() {
		// このメソッドの前に呼ばれるupdate()に任せて，何もしない
	}

	@Override
	public void update(GameInfo gameInfo) {

		// 1日の最初のupdate()でdayStart()の機能を代行する
		if (gameInfo.getDay() == day + 1) { // 1日の最初のupdate()
			declaredVoteCandidate = null;
			voteCandidate = null;
			lastVote = null;
			talkQueue.clear();

			// 占い結果をFIFOに入れる
			Judge divination = currentGameInfo.getDivineResult();
			if (divination != null) {
				divinationQueue.offer(divination);
				if (divination.getResult() == Species.HUMAN) {
					humans.add(divination.getTarget());
				} else {
					werewolves.add(divination.getTarget());
				}
			}
		}

		currentGameInfo = gameInfo;
		day = currentGameInfo.getDay();
		agi.update(currentGameInfo);
	}

	@Override
	public String talk() {
		// TODO 投票発言結果にもとづくカミングアウト（追放されそうになった場合の回避）
		// カミングアウトする日になったらカミングアウト
		if (!isCameout && day >= comingoutDay) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, myRole)));
			isCameout = true;
		}

		// 人狼を占ったらカミングアウト
		if (!isCameout && !divinationQueue.isEmpty() && divinationQueue.peekLast().getResult() == Species.WEREWOLF) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, myRole)));
			isCameout = true;
		}

		// 占い師カミングアウトが出たらカミングアウト
		if (!isCameout && agi.getComingoutMap().containsValue(Role.SEER)) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, myRole)));
			isCameout = true;
		}

		// カミングアウトしたらこれまでの占い結果をすべて公開
		if (isCameout) {
			while (!divinationQueue.isEmpty()) {
				Judge divination = divinationQueue.poll();
				enqueueTalk(new Content(new DivineContentBuilder(divination.getTarget(), divination.getResult())));
			}
		}

		chooseVoteCandidate();
		// 以前宣言した（未宣言を含む）投票先と違う投票先を選んだ場合宣言する
		if (voteCandidate != declaredVoteCandidate) {
			enqueueTalk(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}

		if (talkQueue.isEmpty()) {
			return Talk.SKIP;
		} else {
			return dequeueTalk().getText();
		}
	}

	@Override
	public Agent vote() {
		// TODO 投票発言にもとづく投票（追放されそうになった場合の回避）
		if (lastVote == null) { // この日の初投票
			lastVote = new Vote(day, me, voteCandidate);
			return voteCandidate;
		} else {
			// 再投票
			if (!werewolves.isEmpty()) {
				// 人狼がいる場合前回最多得票数の人狼に投票
				Counter<Agent> counter = new Counter<>();
				for (Vote vote : currentGameInfo.getLatestVoteList()) {
					if (werewolves.contains(vote.getTarget())) {
						counter.add(vote.getTarget());
					}
				}
				if (!counter.isEmpty()) {
					int max = counter.get(counter.getLargest());
					List<Agent> candidates = new ArrayList<>();
					for (Agent agent : counter) {
						if (counter.get(agent) == max) {
							candidates.add(agent);
						}
					}
					Collections.shuffle(candidates);
					return candidates.get(0);
				}
			}

			if (!semiwolves.isEmpty()) {
				// 人狼候補がいる場合前回最多得票数の人狼候補に投票
				Counter<Agent> counter = new Counter<>();
				for (Vote vote : currentGameInfo.getLatestVoteList()) {
					if (semiwolves.contains(vote.getTarget())) {
						counter.add(vote.getTarget());
					}
				}
				if (!counter.isEmpty()) {
					int max = counter.get(counter.getLargest());
					List<Agent> candidates = new ArrayList<>();
					for (Agent agent : counter) {
						if (counter.get(agent) == max) {
							candidates.add(agent);
						}
					}
					Collections.shuffle(candidates);
					return candidates.get(0);
				}
			}

			// 人狼（候補）がいない場合前回最多得票数のエージェントに投票
			Counter<Agent> counter = new Counter<>();
			for (Vote vote : currentGameInfo.getLatestVoteList()) {
				counter.add(vote.getTarget());
			}
			int max = counter.get(counter.getLargest());
			List<Agent> candidates = new ArrayList<>();
			for (Agent agent : counter) {
				if (counter.get(agent) == max) {
					candidates.add(agent);
				}
			}
			Collections.shuffle(candidates);
			return candidates.get(0);
		}
	}

	@Override
	public Agent divine() {
		// 人狼候補がいれば，そこからランダムに占う
		if (!semiwolves.isEmpty()) {
			Collections.shuffle(semiwolves);
			return semiwolves.get(0);
		}

		// 人狼候補がいない場合，まだ占っていない生存者からランダムに占う
		List<Agent> candidates = new ArrayList<>(agi.getAliveOthers());
		candidates.removeAll(werewolves);
		candidates.removeAll(humans);
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

		List<Agent> aliveWolves = new ArrayList<>(werewolves);
		aliveWolves.removeAll(agi.getDeadOthers());
		if (!aliveWolves.isEmpty()) {
			// 生存人狼がいれば当然投票
			if (voteCandidate != null) {
				if (aliveWolves.contains(voteCandidate)) {
					// 既定の投票先が生存人狼ならば，投票先を変えない
					return;
				} else {
					// 既定の投票先が生存人狼でないならば，投票先を変える
					Collections.shuffle(aliveWolves);
					voteCandidate = aliveWolves.get(0);
					return;
				}
			} else {
				// 既定の投票先がなければ，新たな投票先を設定
				Collections.shuffle(aliveWolves);
				voteCandidate = aliveWolves.get(0);
				return;
			}
		}

		// 以後は推測になる
		semiwolves.clear();
		// 占い師をカミングアウトしている他のエージェントは人狼候補
		List<Agent> others = new ArrayList<>(currentGameInfo.getAgentList());
		others.remove(me);
		for (Agent agent : others) {
			if (agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == Role.SEER) {
				if (!semiwolves.contains(agent)) {
					semiwolves.add(agent);
				}
			}
		}

		// 自分の占い結果と異なる判定の霊媒師は人狼候補
		for (Judge judge : agi.getInquestList()) {
			for (Judge myJudge : divinationQueue) {
				if (judge.getTarget() == myJudge.getTarget() && judge.getResult() != myJudge.getResult()) {
					Agent agent = judge.getAgent();
					if (!semiwolves.contains(agent)) {
						semiwolves.add(agent);
					}
				}
			}
		}

		// 生存している灰にしぼる
		semiwolves.removeAll(humans);
		semiwolves.removeAll(agi.getDeadOthers());

		if (semiwolves.isEmpty()) {
			if (voteCandidate != null) {
				// 投票先既定で投票先候補なしの場合，投票先を変えない
				return;
			} else {
				// 投票先未定で投票先候補なしの場合，自分以外の生存者からランダムに選ぶ
				List<Agent> aliveOthers = new ArrayList<>(agi.getAliveOthers());
				Collections.shuffle(aliveOthers);
				voteCandidate = aliveOthers.get(0);
			}
		} else {
			if (voteCandidate != null) {
				// 投票先既定で投票先候補ありの場合
				if (semiwolves.contains(voteCandidate)) {
					// 現在の投票先が投票先候補に含まれている場合，投票先を変えない
					return;
				} else {
					// 現在の投票先が投票先候補に含まれていない場合，新たに投票先候補からランダムに選び，人狼と推定する発言をする
					Collections.shuffle(semiwolves);
					voteCandidate = semiwolves.get(0);
					enqueueTalk(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
				}
			} else {
				// 投票先未定で投票先候補ありの場合，投票先候補からランダムに選び，人狼と推定する発言をする
				Collections.shuffle(semiwolves);
				voteCandidate = semiwolves.get(0);
				enqueueTalk(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
			}
		}
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
			// 真占なのでそのまま待ち行列に入れる
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
		return talkQueue.poll();
	}

}
