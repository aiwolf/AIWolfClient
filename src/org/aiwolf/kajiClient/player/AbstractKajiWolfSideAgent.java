package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.kajiClient.lib.Pattern;
import org.aiwolf.kajiClient.lib.PatternMaker;

public abstract class AbstractKajiWolfSideAgent extends AbstractGiftedPlayer {


	//騙る役職
	Role fakeRole = null;

	//fakeRoleに沿ったPatterns
	List<Pattern> fakePatterns = new ArrayList<Pattern>();


	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		//fakeRoleをランダムで選択
		Role[] fakeRoles = {Role.VILLAGER, Role.SEER, Role.MEDIUM};
		fakeRole = fakeRoles[new Random().nextInt(fakeRoles.length)];

		fakePatterns.add(new Pattern(null, null, new HashMap<Agent, Role>()));
		patternMaker.settleAgentRole(myPatterns, getMe(), getMyRole());
		patternMaker.settleAgentRole(fakePatterns, getMe(), fakeRole);
	}

	@Override
	public void dayStart() {
		super.dayStart();

		patternMaker.updateAttackedData(fakePatterns, getLatestDayGameInfo().getAttackedAgent());
		switch (fakeRole) {
		//占い師騙りの場合，2日目以降fakeJudgeをいれる
		case SEER:
			if(getDay() >= 2){
				setFakeDivineJudge();
			}
			break;

		//霊能者騙りの場合，襲撃されたAgentがいればfakeJudgeをいれる
		case MEDIUM:
			if(getLatestDayGameInfo().getExecutedAgent() != null){
				setFakeInquestJudge(getLatestDayGameInfo().getExecutedAgent());
			}
			break;

		//村人騙りの場合，何もしない
		case VILLAGER:
			break;
		}
	}

	/**
	 * 2日目以降のdayStartで呼ばれる
	 * 偽占い結果を作る
	 */
	abstract protected void setFakeDivineJudge();

	/**
	 * 処刑されたプレイヤーがいた時に呼ばれる
	 * 偽霊能結果を作る
	 * @param executedAgent
	 */
	abstract protected void setFakeInquestJudge(Agent executedAgent);


	@Override
	public void comingoutTalkDealing(Talk talk, Utterance utterance){
		super.comingoutTalkDealing(talk, utterance);
		patternMaker.extendPatternList(fakePatterns, talk.getAgent(), utterance.getRole(), advanceGameInfo);
	}

	@Override
	public void divinedTalkDealing(Talk talk, Utterance utterance){
		super.divinedTalkDealing(talk, utterance);
		Judge inspectJudge = new Judge(getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
		patternMaker.updateJudgeData(fakePatterns, inspectJudge);
	}

	@Override
	public void inquestedTalkDealing(Talk talk, Utterance utterance){
		super.inquestedTalkDealing(talk, utterance);
		Judge tellingJudge = new Judge(getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
		patternMaker.updateJudgeData(fakePatterns, tellingJudge);
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
		 * 投票先に選ばれそう（全体の2/3が投票かつ全投票中で1/4以上が自分に投票）
		 */
		if(isComingout || fakeRole == Role.VILLAGER){
			return null;
		}else{
			//日数によるカミングアウト
			if(getDay() == comingoutDay){
				return comingoutFakeRole();
			}

			//偽CO出現
			Map<Agent, Role> comingoutMap = advanceGameInfo.getComingoutMap();
			for(Entry<Agent, Role> set: comingoutMap.entrySet()){
				if(set.getValue() == fakeRole && !set.getKey().equals(getMe())){
					return comingoutFakeRole();
				}
			}

			//人狼見つける
			for(Judge judge: notToldjudges){
				if(judge.getResult() == Species.WEREWOLF){
					return comingoutFakeRole();
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
					return comingoutFakeRole();
				}
			}
		}
		return null;
	}

	/**
	 * fakeRoleをカミングアウトする
	 * @return
	 */
	private String comingoutFakeRole(){
		isComingout = true;
		return TemplateTalkFactory.comingout(getMe(), fakeRole);
	}

	/**
	 * 今までに出した黒判定の数を返す
	 * @return
	 */
	protected int getBlackJudgeNum(){
		int blackJudgeNum = 0;
		for(Judge judge: toldjudges){
			if(judge.getResult() == Species.WEREWOLF){
				blackJudgeNum++;
			}
		}
		for(Judge judge: notToldjudges){
			if(judge.getResult() == Species.WEREWOLF){
				blackJudgeNum++;
			}
		}
		return blackJudgeNum;
	}




}
