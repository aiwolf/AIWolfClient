package org.aiwolf.kajiClient.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.kajiClient.lib.Pattern;

public abstract class AbstractGifted extends AbstractKajiBase{
	//まだ報告していないjudge
	List<Judge> notToldjudges = new ArrayList<Judge>();

	//既に報告したjudge
	List<Judge> toldjudges = new ArrayList<Judge>();

	//カミングアウトしたか
	boolean isComingout = false;

	//カミングアウトする日数
	int comingoutDay = -1;

	public boolean isJudged(Agent agent){

		Set<Agent> judgedAgents = new HashSet<Agent>();
		for(Judge judge: toldjudges){
			judgedAgents.add(judge.getTarget());
		}
		for(Judge judge: notToldjudges){
			judgedAgents.add(judge.getTarget());
		}

		if(judgedAgents.contains(agent)){
			return true;
		}else{
			return false;
		}

	}

	public List<Pattern> getHypotheticalPatterns(List<Pattern> originPatterns, Judge judge){
		List<Pattern> hypotheticalPatterns = patternMaker.clonePatterns(originPatterns);
		patternMaker.updateJudgeData(hypotheticalPatterns, judge);
		return hypotheticalPatterns;
	}

}
