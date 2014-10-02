package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import org.aiwolf.client.base.player.AbstractPossessedPlayer;
import org.aiwolf.client.base.player.AbstractVillagerPlayer;
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

public class KajiPossessedPlayer extends AbstractKajiWolfSideAgent {

	//黒判定を出す確率
	private static final double BLACK_DIVINEJUDGE_PROBABILITY = 0.25;
	private static final double BLACK_INQUESTJUDGE_PROBABILITY = 0.25;


	@Override
	protected void setFakeDivineJudge() {
		/*
		 * 2日目以降に呼ばれる
		 * 25%で黒判定を出す（すでに全人狼数の黒判定を出していたら必ず白判定）
		 * 対象プレイヤーは，そのプレイヤーにその判定を出したときに自分が真占い師となるパターンが最も多くなるプレイヤーで
		 */
		Judge fakeJudge;

		Agent fakeJudgeTarget;
		Species fakeJudgeSpecies;
		if(getBlackJudgeNum() < getGameSetting().getRoleNum(Role.WEREWOLF) && new Random().nextDouble() < BLACK_DIVINEJUDGE_PROBABILITY){
			fakeJudgeSpecies = Species.WEREWOLF;
		}else{
			fakeJudgeSpecies = Species.HUMAN;
		}

		//各Agentを占いの対象としたときに，自分が真占い師となるパターンの残る数のMap
		Map<Agent, Integer> remainPatternNumMap = new HashMap<Agent, Integer>();
		Set<Agent> candidate = new HashSet<Agent>(getLatestDayGameInfo().getAliveAgentList());
		if(getLatestDayGameInfo().getAttackedAgent() != null){
			candidate.add(getLatestDayGameInfo().getAttackedAgent());
		}

		for(Agent agent: candidate){
			Judge judge = new Judge(getDay(), getMe(), agent, fakeJudgeSpecies);
			List<Pattern> hypotheticalPatterns = getHypotheticalPatterns(fakePatterns, judge);
			remainPatternNumMap.put(agent, hypotheticalPatterns.size());
		}

		fakeJudgeTarget = getMaxIntValueKey(remainPatternNumMap);

		fakeJudge = new Judge(getDay(), getMe(), fakeJudgeTarget, fakeJudgeSpecies);
		notToldjudges.add(fakeJudge);
		return;
	}



	@Override
	protected void setFakeInquestJudge(Agent executedAgent) {
		/*
		 * 白判定or黒判定のみ決める
		 * その判定を出したときにfakePatternsが最大となる方
		 * 同じ場合は，上記定数確率にしたがって黒判定
		 */

		Judge fakeJudge;

		Species fakeJudgeSpecies;

		Map<Species, Integer> remainPatternNumMap = new HashMap<Species, Integer>();
		for(Species species: Species.values()){
			Judge judge = new Judge(getDay(), getMe(), executedAgent, species);
			List<Pattern> hypotheticalPatterns = getHypotheticalPatterns(fakePatterns, judge);
			remainPatternNumMap.put(species, hypotheticalPatterns.size());
		}

		if(remainPatternNumMap.get(Species.HUMAN) != remainPatternNumMap.get(Species.WEREWOLF)){
			fakeJudgeSpecies = getMaxIntValueKey(remainPatternNumMap);
		}else{
			if(new Random().nextDouble() < BLACK_INQUESTJUDGE_PROBABILITY){
				fakeJudgeSpecies = Species.WEREWOLF;
			}else{
				fakeJudgeSpecies = Species.HUMAN;
			}
		}

		fakeJudge = new Judge(getDay(), getMe(), executedAgent, fakeJudgeSpecies);
		notToldjudges.add(fakeJudge);
		return;
	}

	@Override
	public void setVoteTarget() {
		setVoteTargetTemplate(fakePatterns);
	}
}
