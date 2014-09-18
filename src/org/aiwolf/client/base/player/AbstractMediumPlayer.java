package org.aiwolf.client.base.player;

import java.util.ArrayList;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;

public abstract class AbstractMediumPlayer extends AbstractPlayer{

	//占い結果のリスト
	ArrayList<Judge> myJudgeList = new ArrayList<Judge>();


//	HashMap<Agent, Species> resultMap = new HashMap<>();

	@Override
	public void dayStart(){
		//霊能結果をjudgeListに格納
		if(gameInfoMap.get(getDay()).getMediumResult() != null){
			myJudgeList.add(getLatestDayGameInfo().getMediumResult());
		}
	}

	@Override
	public abstract String talk();

	@Override
	final public String whisper(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract Agent vote();

	@Override
	final public Agent attack(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	final public Agent divine(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	final public Agent guard(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract void finish();

	public AbstractMediumPlayer(){
		myRole = Role.medium;
	}

	/**
	 * 自分の霊能結果のリストを返す．
	 * @return
	 */
	public ArrayList<Judge> getMyJudgeList() {
		return myJudgeList;
	}


	/**
	 * すでに占い(or霊能)対象にしたプレイヤーならtrue,まだ占っていない(霊能していない)ならばfalseを返す．
	 * @param myJudgeList
	 * @param agent
	 * @return
	 */
	public boolean isJudgedAgent(Agent agent){
		for(Judge judge: myJudgeList){
			if(judge.getAgent() == agent){
				return true;
			}
		}
		return false;
	}

}
