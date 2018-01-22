package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

/**
 * 裏切り者役エージェントクラス
 */
public class SamplePossessed extends SampleVillager {
	int numWolves;
	boolean isCameout;
	List<Judge> fakeDivinationList = new ArrayList<>();
	Deque<Judge> fakeDivinationQueue = new LinkedList<>();
	List<Agent> divinedAgents = new ArrayList<>();

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		isCameout = false;
		fakeDivinationList.clear();
		fakeDivinationQueue.clear();
		divinedAgents.clear();
	}

	private Judge getFakeDivination() {
		Agent target = null;
		List<Agent> candidates = new ArrayList<>();
		for (Agent a : aliveOthers) {
			if (!divinedAgents.contains(a) && comingoutMap.get(a) != Role.SEER) {
				candidates.add(a);
			}
		}
		if (!candidates.isEmpty()) {
			target = randomSelect(candidates);
		} else {
			target = randomSelect(aliveOthers);
		}
		// 偽人狼に余裕があれば，人狼と人間の割合を勘案して，30%の確率で人狼と判定
		Species result = Species.HUMAN;
		int nFakeWolves = 0;
		for (Judge j : fakeDivinationList) {
			if (j.getResult() == Species.WEREWOLF) {
				nFakeWolves++;
			}
		}
		if (nFakeWolves < numWolves && Math.random() < 0.3) {
			result = Species.WEREWOLF;
		}
		return new Judge(day, me, target, result);
	}

	public void dayStart() {
		super.dayStart();
		// 偽の判定
		if (day > 0) {
			Judge judge = getFakeDivination();
			if (judge != null) {
				fakeDivinationList.add(judge);
				fakeDivinationQueue.offer(judge);
				divinedAgents.add(judge.getTarget());
			}
		}
	}

	protected void chooseVoteCandidate() {
		werewolves.clear();
		List<Agent> candidates = new ArrayList<>();
		// 自分や殺されたエージェントを人狼と判定している占い師は人狼候補
		for (Judge j : divinationList) {
			if (j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))) {
				if (!werewolves.contains(j.getAgent())) {
					werewolves.add(j.getAgent());
				}
			}
		}
		// 対抗カミングアウトのエージェントは投票先候補
		for (Agent a : aliveOthers) {
			if (!werewolves.contains(a) && comingoutMap.get(a) == Role.SEER) {
				candidates.add(a);
			}
		}
		// 人狼と判定したエージェントは投票先候補
		List<Agent> fakeHumans = new ArrayList<>();
		for (Judge j : fakeDivinationList) {
			if (j.getResult() == Species.HUMAN) {
				if (!fakeHumans.contains(j.getTarget())) {
					fakeHumans.add(j.getTarget());
				}
			} else {
				if (!candidates.contains(j.getTarget())) {
					candidates.add(j.getTarget());
				}
			}
		}
		// 候補がいなければ人間と判定していない村人陣営から
		if (candidates.isEmpty()) {
			for (Agent a : aliveOthers) {
				if (!werewolves.contains(a) && !fakeHumans.contains(a)) {
					candidates.add(a);
				}
			}
		}
		// それでも候補がいなければ村人陣営から
		if (candidates.isEmpty()) {
			for (Agent a : aliveOthers) {
				if (!werewolves.contains(a)) {
					candidates.add(a);
				}
			}
		}
		if (!candidates.contains(voteCandidate)) {
			voteCandidate = randomSelect(candidates);
			// 以前の投票先から変わる場合，新たに推測発言と占い要請をする
			if (canTalk) {
				talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
				talkQueue.offer(new Content(new RequestContentBuilder(null, new Content(new DivinationContentBuilder(voteCandidate)))));
			}
		}
	}

	public String talk() {
		// 即占い師カミングアウト
		if (!isCameout) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
			isCameout = true;
		}
		// カミングアウトしたらこれまでの偽占い結果をすべて公開
		if (isCameout) {
			while (!fakeDivinationQueue.isEmpty()) {
				Judge divination = fakeDivinationQueue.poll();
				talkQueue.offer(new Content(new DivinedResultContentBuilder(divination.getTarget(), divination.getResult())));
			}
		}
		return super.talk();
	}

}
