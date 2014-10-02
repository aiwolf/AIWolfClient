package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Random;

import javax.print.attribute.standard.Media;

import org.aiwolf.client.base.player.AbstractVillagerPlayer;
import org.aiwolf.client.base.player.AbstractWerewolfPlayer;
import org.aiwolf.client.lib.TemplateWhisperFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.kajiClient.lib.Pattern;
import org.aiwolf.kajiClient.lib.Strategies;

public class KajiWereWolfPlayer extends AbstractKajiWolfSideAgent {

	//狂人のAgent．不確定の時はnull
	Agent possessedAgent = null;

	//Whisperで自分の騙る役職を伝えたか
	boolean hasWhisperedFakeRole = false;

	//人狼達のfakeRoleに矛盾が起こらないPatterns
	List<Pattern> wolfsPatterns;

	//仲間人狼のfakeRole
	Map<Agent, Role> wolfsFakeRoleMap = new HashMap<Agent, Role>();

	//Whisperをどこまで読んだか
	int readWhisperNumber = 0;

	//今日の嘘JudgeをWhisperで伝えたか
	boolean hasWhisperTodaysFakeJudge;

	//今日の嘘Judge
	Judge todaysFakeJudge;

	//WhisperされたJudgeのリスト
	List<Judge> whisperedJudges = new ArrayList<Judge>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		//myPatternsに仲間の人狼をセットする
		for(Entry<Agent, Role> set: gameInfo.getRoleMap().entrySet()){
			if(!set.getKey().equals(getMe())){
				patternMaker.settleAgentRole(myPatterns, set.getKey(), Role.WEREWOLF);
			}
		}
		wolfsPatterns = new ArrayList<Pattern>(fakePatterns);
	}


	@Override
	public void update(GameInfo gameInfo){
		super.update(gameInfo);

		//whisperの処理

		List<Talk> whisperList = gameInfo.getWhisperList();

		/*
		 * 各発話についての処理
		 * カミングアウトについてはパターンの拡張
		 * 能力結果の発話についてはパターン情報の更新
		 */
		for(; readTalkNumber < whisperList.size(); readTalkNumber++){
			Talk talk = whisperList.get(readTalkNumber);
			Utterance utterance = new Utterance(talk.getContent());
			switch (utterance.getTopic()) {
			case COMINGOUT:
				comingoutWhisperDealing(talk, utterance);
				break;

			case DIVINED:
				divinedWhisperDealing(talk, utterance);
				break;

			case INQUESTED:
				inquestedWhisperDealing(talk, utterance);
				break;

			case VOTE:
				voteWhisperDealing(talk, utterance);
				break;
			//上記以外
			default:
				break;
			}
		}
	}

	@Override
	public void dayStart(){
		super.dayStart();
		readWhisperNumber = 0;
		hasWhisperTodaysFakeJudge = false;
	}


	@Override
	public String whisper(){
		/*
		 * 自分の騙り役職を最初に報告する
		 * fakeJudgeの結果を報告する
		 */
		if(!hasWhisperedFakeRole){
			hasWhisperedFakeRole = true;
			return TemplateWhisperFactory.comingout(getMe(), fakeRole);
		}else{
			if(fakeRole == Role.SEER || fakeRole == Role.MEDIUM){
				if(!hasWhisperTodaysFakeJudge && todaysFakeJudge != null){
					switch (fakeRole) {
					case SEER:
						hasWhisperTodaysFakeJudge = true;
						return TemplateWhisperFactory.divined(todaysFakeJudge.getTarget(), todaysFakeJudge.getResult());
					case MEDIUM:
						hasWhisperTodaysFakeJudge = true;
						return TemplateWhisperFactory.inquested(todaysFakeJudge.getTarget(), todaysFakeJudge.getResult());
					}
				}
			}
		}
		return TemplateWhisperFactory.over();
	}

	private void comingoutWhisperDealing(Talk talk, Utterance utterance){
		/*
		 * wolfsPatternsを更新する
		 */
		wolfsFakeRoleMap.put(talk.getAgent(), utterance.getRole());
		patternMaker.settleAgentRole(wolfsPatterns, talk.getAgent(), utterance.getRole());
	}

	private void divinedWhisperDealing(Talk talk, Utterance utterance){
		judgeWhisperDealing(talk, utterance);
	}

	private void inquestedWhisperDealing(Talk talk, Utterance utterance){
		judgeWhisperDealing(talk, utterance);
	}


	private void judgeWhisperDealing(Talk talk, Utterance utterance){
		/*
		 * 人狼同士が協調可能Patternがあるときに，それが消えたら自分のJudgeを書き換え
		 * 村人騙りなら何もしない
		 */
		Judge judge = new Judge(getDay(), talk.getAgent(), utterance.getTarget(), utterance.getResult());
		whisperedJudges.add(judge);
		patternMaker.updateJudgeData(wolfsPatterns, judge);

		if(fakeRole != Role.SEER && fakeRole != Role.MEDIUM ){
			return;
		}
		if(wolfsPatterns.size() != 0){
			List<Pattern> hypotheticPatterns = patternMaker.clonePatterns(wolfsPatterns);

			patternMaker.updateJudgeData(hypotheticPatterns, todaysFakeJudge);
			if(hypotheticPatterns.size() == 0){
				switch (fakeRole) {
				case SEER:
					setFakeDivineJudge();
					break;
				case MEDIUM:
					setFakeInquestJudge(getLatestDayGameInfo().getExecutedAgent());
				}
			}
		}
	}



	private void voteWhisperDealing(Talk talk, Utterance utterance) {
		// TODO 自動生成されたメソッド・スタブ
	}


	@Override
	public void divinedTalkDealing(Talk talk, Utterance utterance){
		super.divinedTalkDealing(talk, utterance);
		confirmPossessedAgent();
	}

	@Override
	public void inquestedTalkDealing(Talk talk, Utterance utterance){
		super.inquestedTalkDealing(talk, utterance);
		confirmPossessedAgent();
	}

	/**
	 * 狂人確定のAgentがいるか確かめる
	 * いた場合はpossessedAgentにいれる
	 */
	private void confirmPossessedAgent(){
		loop1:for(Entry<Agent, Role> set: advanceGameInfo.getComingoutMap().entrySet()){
			if(set.getValue() == Role.SEER || set.getValue() == Role.MEDIUM){
				for(Pattern pattern: myPatterns){
					if(set.getKey().equals(pattern.getSeerAgent()) || set.getKey().equals(pattern.getMediumAgent())){
						continue loop1;
					}
				}
			}
			//全てのPatternにおいて真能力者とされていないカミングアウトしたプレイヤー＝狂人
			possessedAgent = set.getKey();
		}
	}

	/**
	 * 狂人が分かっているかを返す
	 * @return
	 */
	private boolean knowsPossessed(){
		if(possessedAgent == null){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * 狂人が分かっている場合はFakeRoleを返す
	 * 分かっていない場合はnull
	 * @return
	 */
	private Role possessedFakeRole(){
		if(!knowsPossessed()){
			return null;
		}else{
			return advanceGameInfo.getComingoutMap().get(possessedAgent);
		}
	}

	private List<Agent> getWolfList(){

		List<Agent> wolfList = new ArrayList<Agent>();
		for(Entry<Agent, Role> set: getLatestDayGameInfo().getRoleMap().entrySet()){
			if(set.getValue() == Role.WEREWOLF){
				wolfList.add(set.getKey());
			}
		}
		return wolfList;
	}


	@Override
	public Agent attack(){
		/*
		 * myPatternで各役職の強さのルーレット
		 * ただし，人狼側から黒判定を出しいているプレイヤーは襲わない
		 * 襲う相手がいない場合は人間からランダム
		 */
		Map<Agent, Double> riskValues = new HashMap<Agent, Double>();

		//生存Agentに対して
		loop1:for(Agent agent: getLatestDayGameInfo().getAliveAgentList()){
			//人狼だったら候補からはずす
			if(getWolfList().contains(agent)){
				continue loop1;
			}
			//人狼，確定狂人が黒判定を出していたら襲撃候補からはずす
			for(Judge judge: whisperedJudges){
				if(judge.getTarget().equals(agent) && judge.getResult() == Species.WEREWOLF){
					continue loop1;
				}
			}
			if(possessedFakeRole() == Role.SEER){
				for(Judge judge: advanceGameInfo.getInspectJudges()){
					if(judge.getResult() == Species.WEREWOLF){
						continue loop1;
					}
				}
			}else if(possessedFakeRole() == Role.MEDIUM){
				for(Judge judge: advanceGameInfo.getMediumJudges()){
					if(judge.getResult() == Species.WEREWOLF){
						continue loop1;
					}
				}
			}

			//人狼側から黒判定が出ていない場合
			//agentが死んだときに失われる役職値(正ならば人間側が死にやすいということ)
			double riskValue =0.0;
			for(Pattern pattern: myPatterns){
				riskValue += getRiskValue(pattern, agent, getLatestDayGameInfo().getAliveAgentList());
			}
			riskValues.put(agent, riskValue);
		}

		if(riskValues.size() != 0){
			return Strategies.rouletSelect(riskValues);
		}
		//襲撃候補がいなくなってしまったら
		else{
			List<Agent> aliveAgents = new ArrayList<Agent>(getLatestDayGameInfo().getAliveAgentList());
			Collections.shuffle(aliveAgents);
			for(Agent agent: aliveAgents){
				if(!getWolfList().contains(agent)){
					return agent;
				}
			}
		}
		//ここまでくることは恐らく無い
		return null;
	}


	@Override
	protected void setFakeDivineJudge() {

		Judge fakeJudge;

		//人狼の協調が可能な組み合わせの場合
		if(wolfsPatterns.size() != 0){
			fakeJudge = getMaxEntropyDivineJudge(wolfsPatterns);

		}
		//人狼の協調が不可能な組み合わせの場合
		else{
			fakeJudge = getMaxEntropyDivineJudge(generalPatterns);
		}
		notToldjudges.add(fakeJudge);
		todaysFakeJudge = fakeJudge;
	}

	private Judge getMaxEntropyDivineJudge(List<Pattern> patterns){
		Map<Judge, Integer> remainPatternNumMap = new HashMap<Judge, Integer>();

		for(Agent agent: getLatestDayGameInfo().getAliveAgentList()){
			//すでに占っている，または自分ならば候補からはずす
			if(agent.equals(getMe()) || isJudged(agent)){
				continue;
			}else{
				for(Species species: Species.values()){
					Judge judge = new Judge(getDay(), getMe(), agent, species);
					List<Pattern> hypotheticalPatterns = getHypotheticalPatterns(patterns, judge);
					remainPatternNumMap.put(judge, hypotheticalPatterns.size());
				}

			}
		}
		if(remainPatternNumMap.size() != 0){
			return getMaxIntValueKey(remainPatternNumMap);
		}
		//候補がなくなってしまったとき
		else{
			return new Judge(getDay(), getMe(), getMe(), Species.HUMAN);
		}
	}






	@Override
	protected void setFakeInquestJudge(Agent executedAgent) {
		Judge fakeJudge;

		// 人狼の協調が可能な組み合わせの場合
		if (wolfsPatterns.size() != 0) {
			fakeJudge = getMaxEntropyInquestJudge(wolfsPatterns);

		}
		// 人狼の協調が不可能な組み合わせの場合
		else {
			fakeJudge = getMaxEntropyInquestJudge(generalPatterns);
		}
	}

	private Judge getMaxEntropyInquestJudge(List<Pattern> patterns){
		Map<Judge, Integer> remainPatternNumMap = new HashMap<Judge, Integer>();

		for(Species species: Species.values()){
			Judge judge = new Judge(getDay(), getMe(), getLatestDayGameInfo().getExecutedAgent(), species);
			List<Pattern> hypotheticalPatterns = getHypotheticalPatterns(patterns, judge);
			remainPatternNumMap.put(judge, hypotheticalPatterns.size());
		}
		return getMaxIntValueKey(remainPatternNumMap);
	}






	@Override
	public void setVoteTarget() {
		//initialize時
		if(wolfsPatterns == null){
			return;
		}
		if (wolfsPatterns.size() != 0) {
			setVoteTargetTemplate(wolfsPatterns);
		}
		// 人狼の協調が不可能な組み合わせの場合
		else {
			setVoteTargetTemplate(fakePatterns);
		}

	}

}