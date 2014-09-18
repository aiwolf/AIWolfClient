package org.aiwolf.client.base.player;

import java.util.ArrayList;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;

public abstract class AbstractSeerPlayer extends AbstractPlayer{

	//占い結果のリスト
	ArrayList<Judge> myJudgeList = new ArrayList<Judge>();


	@Override
	public  void dayStart(){
		//占い結果をjudgeListに格納
		if(gameInfoMap.get(getDay()).getDivineResult() != null){
			myJudgeList.add(getLatestDayGameInfo().getDivineResult());
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
	public abstract Agent divine();

	@Override
	final public Agent guard(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract void finish();

	public AbstractSeerPlayer(){
		super();
		myRole = Role.SEER;
	}

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
