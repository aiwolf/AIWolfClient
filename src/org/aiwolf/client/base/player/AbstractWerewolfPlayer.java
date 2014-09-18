package org.aiwolf.client.base.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

public abstract class AbstractWerewolfPlayer extends AbstractPlayer{

	@Override
	public abstract void dayStart();

	@Override
	public abstract String talk();

	@Override
	public abstract String whisper();

	@Override
	public abstract Agent vote();

	@Override
	public abstract Agent attack();

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

	public AbstractWerewolfPlayer(){
		myRole = Role.werewolf;
	}

	public List<Agent> getWolfList(){
		List<Agent> wolfList = new ArrayList<Agent>();

		Map<Agent, Role> wolfMap = getLatestDayGameInfo().getRoleMap();
		for(Entry<Agent, Role> set: wolfMap.entrySet()){
			if(set.getValue() == Role.werewolf){
				wolfList.add(set.getKey());
			}
		}

		return wolfList;

	}


}
