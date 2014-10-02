package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class KajiMediumPlayer extends AbstractGiftedPlayer {

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		//カミングアウトする日数をランダムに設定(0なら日数経過ではカミングアウトしない)
		comingoutDay = new Random().nextInt(4);
	}

	@Override
	public void dayStart() {
		super.dayStart();
		if(getLatestDayGameInfo().getMediumResult() != null){
			notToldjudges.add(getLatestDayGameInfo().getMediumResult());
		}
	}

	@Override
	public String getJudgeText() {
		if(isComingout && notToldjudges.size() != 0){
			String talk = TemplateTalkFactory.inquested(notToldjudges.get(0).getTarget(), notToldjudges.get(0).getResult());
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
				if((double)voteToMe * 4 > votes.size()){
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
}
