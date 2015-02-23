package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.base.player.AbstractSeer;
import org.aiwolf.client.base.player.AbstractVillager;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class KajiSeerPlayer extends AbstractGiftedPlayer {

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		//カミングアウトする日数をランダムに設定(0なら日数経過ではカミングアウトしない)
		super.initialize(gameInfo, gameSetting);
		comingoutDay = new Random().nextInt(4);
	}

	@Override
	public void dayStart() {
		super.dayStart();
		if(getLatestDayGameInfo().getDivineResult() != null){
			notToldjudges.add(getLatestDayGameInfo().getDivineResult());
		}
	}

	@Override
	public String getJudgeText() {
		if(isComingout && notToldjudges.size() != 0){
			String talk = TemplateTalkFactory.divined(notToldjudges.get(0).getTarget(), notToldjudges.get(0).getResult());
			toldjudges.add(notToldjudges.get(0));
			notToldjudges.remove(0);
			return talk;
		}
		return null;
	}

	@Override
	public String getComingoutText() {
		/*
		 * カミングアウトする日数になる
		 * 他に同じ能力者COが出る
		 * 人狼を見つける
		 * 投票先に選ばれそう（全体の2/3が投票かつ全投票中でマックスが自分）
		 */
		if(isComingout){
			return null;
		}else{
			//日数によるカミングアウト
			if(getDay() == comingoutDay){
				isComingout = true;
				return TemplateTalkFactory.comingout(getMe(), getMyRole());
			}

			//偽CO出現
			Map<Agent, Role> comingoutMap = advanceGameInfo.getComingoutMap();
			for(Entry<Agent, Role> set: comingoutMap.entrySet()){
				if(set.getValue() == getMyRole() && !set.getKey().equals(getMe())){
					isComingout = true;
					return TemplateTalkFactory.comingout(getMe(), getMyRole());
				}
			}

			//人狼見つける
			for(Judge judge: notToldjudges){
				if(judge.getResult() == Species.WEREWOLF){
					isComingout = true;
					return TemplateTalkFactory.comingout(getMe(), getMyRole());
				}
			}

			//投票先に選ばれそう
			List<Vote> votes = advanceGameInfo.getVoteList(getDay());
			if((double)votes.size() * 1.5 > getLatestDayGameInfo().getAliveAgentList().size()){
				int voteToMe = 0;
				for(Vote vote: votes){
					if(vote.getTarget().equals(getMe())){
						voteToMe++;
					}
				}
				if((double)voteToMe * 3 > votes.size()){
					isComingout = true;
					return TemplateTalkFactory.comingout(getMe(), getMyRole());
				}
			}
		}
		return null;

	}

	@Override
	public void setVoteTarget() {
		setVoteTargetTemplate(myPatterns);
	}

	@Override
	public Agent divine() {
		/*
		 * 生きているプレイヤーでまだ占っていないプレイヤー
		 */

		List<Agent> aliveAgents = new ArrayList<Agent>(getLatestDayGameInfo().getAliveAgentList());

		//既に占ったプレイヤーと自分を除外
		aliveAgents.remove(getMe());
		for(Judge judge: toldjudges){
			if(aliveAgents.contains(judge.getTarget())){
				aliveAgents.remove(judge.getTarget());
			}
		}
		for(Judge judge: notToldjudges){
			if(aliveAgents.contains(judge.getTarget())){
				aliveAgents.remove(judge.getTarget());
			}
		}

		if(aliveAgents.size() == 0){
			return getMe();
		}
		Collections.shuffle(aliveAgents);
		return aliveAgents.get(0);
	}
}
