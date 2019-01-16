package org.aiwolf.sample.player;

import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

/** 村人役エージェントクラス */
public class SampleVillager extends SampleBasePlayer {

	@Override
	protected void chooseVoteCandidate() {
		werewolves.clear();
		for (Judge j : divinationList) {
			// 自分あるいは殺されたエージェントを人狼と判定していて，生存している自称占い師を投票先候補とする
			if (j.getResult() == Species.WEREWOLF && (j.getTarget() == me || isKilled(j.getTarget()))) {
				Agent candidate = j.getAgent();
				if (isAlive(candidate) && !werewolves.contains(candidate)) {
					werewolves.add(candidate);
					Content reason = new Content(new DivinedResultContentBuilder(candidate, j.getTarget(), Species.WEREWOLF));
					estimateReasonMap.put(candidate, reason);
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
					Content action = new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF));
					if (estimateReasonMap.containsKey(voteCandidate)) {
						talkQueue.offer(new Content(new BecauseContentBuilder(estimateReasonMap.get(voteCandidate), action)));
					} else {
						talkQueue.offer(action);
					}
					voteReasonMap.put(voteCandidate, action);
					Content request = new Content(new RequestContentBuilder(null, new Content(new DivinationContentBuilder(voteCandidate))));
					talkQueue.offer(new Content(new BecauseContentBuilder(action, request)));
				}
			}
		}
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
	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
