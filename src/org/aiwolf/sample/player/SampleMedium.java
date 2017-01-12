/**
 * SampleMedium.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.RequestContentBuilder;
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
import org.aiwolf.sample.lib.AbstractMedium;

/**
 * <div lang="ja">霊媒師プレイヤーのサンプル</div>
 *
 * <div lang="en">Sample medium agent</div>
 */
public class SampleMedium extends AbstractMedium {

	GameInfo currentGameInfo;
	int day;
	Agent me;
	Role myRole;
	AdditionalGameInfo agi;
	Agent voteCandidate; // 投票先候補
	Agent declaredVoteCandidate; // 宣言した投票先候補
	Vote lastVote; // 再投票における前回の投票
	List<Content> talkList = new ArrayList<>(); // 発話リスト．次のtalkHeadと併せて待ち行列を構成
	int talkHead;
	List<Agent> seers = new ArrayList<>(); // 占い師候補リスト
	Agent trueSeer; // 真占い師と思われるエージェント
	List<Agent> werewolves = new ArrayList<>(); // 人狼候補リスト

	int comingoutDay; // カミングアウトする日
	List<Integer> comingoutDays = new ArrayList<>(Arrays.asList(1, 2, 3));
	boolean isCameout; // カミングアウト済みか否か
	List<Judge> inquestList = new ArrayList<>(); // 霊媒結果リスト．次のinquestHeadと併せて待ち行列を構成
	int inquestHead;

	@Override
	public String getName() {
		return "SampleMedium";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		day = -1;
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		agi = new AdditionalGameInfo(gameInfo);
		seers.clear();
		trueSeer = null;
		werewolves.clear();

		Collections.shuffle(comingoutDays);
		comingoutDay = comingoutDays.get(0); // 1～3日目をランダムで
		isCameout = false;
		inquestList.clear();
		inquestHead = 0;
	}

	@Override
	public void dayStart() {
		declaredVoteCandidate = null;
		voteCandidate = null;
		lastVote = null;
		talkList.clear();
		talkHead = 0;

		// 霊媒結果を待ち行列に入れる
		if (currentGameInfo.getMediumResult() != null) {
			inquestList.add(currentGameInfo.getMediumResult());
		}
	}

	@Override
	public void update(GameInfo gameInfo) {

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

		// 霊媒結果が人狼だったらカミングアウト
		if (!isCameout && !inquestList.isEmpty() && inquestList.get(inquestList.size() - 1).getResult() == Species.WEREWOLF) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, myRole)));
			isCameout = true;
		}

		// 霊媒師カミングアウトが出たらカミングアウト
		if (!isCameout && agi.getComingoutMap().containsValue(Role.MEDIUM)) {
			enqueueTalk(new Content(new ComingoutContentBuilder(me, myRole)));
			isCameout = true;
		}

		// カミングアウトしたらこれまでの霊媒結果をすべて公開
		if (isCameout) {
			while (inquestHead < inquestList.size()) {
				Judge inquest = inquestList.get(inquestHead++);
				enqueueTalk(new Content(new IdentContentBuilder(inquest.getTarget(), inquest.getResult())));
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
	public Agent vote() { // TODO 投票発言にもとづく投票（追放されそうになった場合の回避）
		// 初回投票
		if (lastVote == null) {
			lastVote = new Vote(day, me, voteCandidate);
			return voteCandidate;
		}
		// 再投票
		List<Agent> candidates = new ArrayList<>();
		Counter<Agent> counter = new Counter<>();
		if (!werewolves.isEmpty()) {
			// 人狼候補がいる場合：前回最多得票数の人狼候補
			for (Vote vote : currentGameInfo.getLatestVoteList()) {
				if (werewolves.contains(vote.getTarget())) {
					counter.add(vote.getTarget());
				}
			}
			int max = counter.get(counter.getLargest());
			for (Agent agent : counter) {
				if (counter.get(agent) == max) {
					candidates.add(agent);
				}
			}
		}
		// 候補がいない場合：前回最多得票のエージェント
		if (candidates.isEmpty()) {
			counter.clear();
			for (Vote vote : currentGameInfo.getLatestVoteList()) {
				counter.add(vote.getTarget());
			}
			int max = counter.get(counter.getLargest());
			for (Agent agent : counter) {
				if (counter.get(agent) == max) {
					candidates.add(agent);
				}
			}
		}
		// 候補がいない場合：自分以外
		if (candidates.isEmpty()) {
			candidates.addAll(agi.getAliveOthers());
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

		// 霊媒師をカミングアウトしている他のエージェントは人狼候補
		List<Agent> others = new ArrayList<>(currentGameInfo.getAgentList());
		others.remove(me);
		for (Agent agent : others) {
			if (agi.getComingoutMap().containsKey(agent) && agi.getComingoutMap().get(agent) == Role.MEDIUM) {
				if (!werewolves.contains(agent)) {
					werewolves.add(agent);
				}
			}
		}

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

		// 自分の霊媒結果と異なる判定の占い師は人狼候補
		for (Judge judge : agi.getDivinationList()) {
			for (Judge myJudge : inquestList) {
				if (judge.getTarget() == myJudge.getTarget() && judge.getResult() != myJudge.getResult()) {
					Agent agent = judge.getAgent();
					if (!werewolves.contains(agent)) {
						werewolves.add(agent);
					}
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
				// これまで真占い師なしか，これまでの真占い師が間違いだった場合，新たな占い師推定発言をする
				Collections.shuffle(seers);
				trueSeer = seers.get(0);
				enqueueTalk(new Content(new EstimateContentBuilder(trueSeer, Role.SEER)));
			}
		}

		// 生存している人狼候補が投票先候補
		List<Agent> candidates = new ArrayList<>(werewolves);
		candidates.removeAll(agi.getDeadOthers());

		// 投票先候補がある場合
		if (!candidates.isEmpty()) {
			// 投票先が未定，あるいは既定の投票先が投票先候補に含まれない場合，新たに投票先を選ぶ
			if (voteCandidate == null || !candidates.contains(voteCandidate)) {
				// 投票発言がある場合，投票先候補から被投票宣言数最大のエージェントを選ぶ
				if (!agi.getVoteCounter().isEmpty()) {
					for (Agent agent : agi.getVoteCounter().getReverseList()) {
						if (candidates.contains(agent)) {
							voteCandidate = agent;
							// 投票先が変わったので人狼推定発言をする
							enqueueTalk(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
							// 占いを要請
							if (trueSeer != null) {
								int i = 0;
								for (; i < agi.getDivinationList().size(); i++) {
									Judge judge = agi.getDivinationList().get(i);
									if (judge.getAgent() == trueSeer && judge.getTarget() == voteCandidate) {
										break;
									}
								}
								if (i == agi.getDivinationList().size()) {
									enqueueTalk(new Content(new RequestContentBuilder(trueSeer, new Content(new DivinationContentBuilder(voteCandidate)))));
								}
							}
							return;
						}
					}
				}
				// 投票発言に該当なし，あるいは投票発言がない場合，投票先候補からランダムに選ぶ
				Collections.shuffle(candidates);
				voteCandidate = candidates.get(0);
				// 投票先が変わったので人狼推定発言をする
				enqueueTalk(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
				// 占いを要請
				if (trueSeer != null) {
					int i = 0;
					for (; i < agi.getDivinationList().size(); i++) {
						Judge judge = agi.getDivinationList().get(i);
						if (judge.getAgent() == trueSeer && judge.getTarget() == voteCandidate) {
							break;
						}
					}
					if (i == agi.getDivinationList().size()) {
						enqueueTalk(new Content(new RequestContentBuilder(trueSeer, new Content(new DivinationContentBuilder(voteCandidate)))));
					}
				}
				return;
			}
			// 既定の投票先が投票先候補に含まれる場合，投票先はそのまま
			else {
				return;
			}
		}
		// 投票先候補がない場合
		else {
			// 投票先未定の場合，自分以外の生存者から投票先を選ぶ
			if (voteCandidate == null) {
				List<Agent> aliveOthers = new ArrayList<>(agi.getAliveOthers());
				// これまでの投票発言がある場合，自分以外の生存者で被投票宣言数最大のエージェントを選ぶ
				if (!agi.getVoteCounter().isEmpty()) {
					for (Agent agent : agi.getVoteCounter().getReverseList()) {
						if (aliveOthers.contains(agent)) {
							voteCandidate = agent;
							return;
						}
					}
				}
				// 投票発言に該当なし，あるいは投票発言がない場合，自分以外の生存者からランダムに選ぶ
				Collections.shuffle(aliveOthers);
				voteCandidate = aliveOthers.get(0);
				return;
			}
			// 既定の投票先があれば，投票先はそのまま
			else {
				return;
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
		boolean isEnqueue = true;

		if (newContent.getOperator() == Operator.REQUEST) {
			for (Content content : talkList) {
				if (content.equals(newContent)) {
					isEnqueue = false;
					break;
				}
			}
			if (isEnqueue) {
				talkList.add(newContent);
			}
			return;
		}

		Iterator<Content> it = talkList.iterator();
		Topic newTopic = newContent.getTopic();

		switch (newTopic) {
		case AGREE:
		case DISAGREE:
			// 同一のものが待ち行列になければ入れる
			while (it.hasNext()) {
				if (it.next().equals(newContent)) {
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

		case IDENTIFIED:
			// 真霊なのでそのまま待ち行列に入れる
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
			talkList.add(newContent);
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
		if (talkHead == talkList.size()) {
			return Content.SKIP;
		}
		return talkList.get(talkHead++);
	}

}
