package org.aiwolf.sample.player;

import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;

/** 村人役エージェントクラス */
public class SampleVillager extends SampleBasePlayer {

	protected void chooseVoteCandidate() {
		werewolves.clear();
		for (Judge j : divinationList) {
			// 自分あるいは殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補とする
			if (j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))) {
				Agent candidate = j.getAgent();
				if (isAlive(candidate) && !werewolves.contains(candidate)) {
					werewolves.add(candidate);
				}
			}
		}
		// 候補がいない場合はランダム
		if (werewolves.isEmpty()) {
			if (!aliveOthers.contains(voteCandidate)) {
				voteCandidate = randomSelect(aliveOthers);
			}
		} else {
			if (!werewolves.contains(voteCandidate)) {
				voteCandidate = randomSelect(werewolves);
				// 以前の投票先から変わる場合，新たに推測発言と占い要請をする
				if (canTalk) {
					talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
					talkQueue.offer(new Content(new RequestContentBuilder(null, new Content(new DivinationContentBuilder(voteCandidate)))));
				}
			}
		}
	}

	public String whisper() {
		throw new UnsupportedOperationException();
	}

	public Agent attack() {
		throw new UnsupportedOperationException();
	}

	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
