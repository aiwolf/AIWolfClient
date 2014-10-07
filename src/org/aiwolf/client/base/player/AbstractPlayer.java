package org.aiwolf.client.base.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public abstract class AbstractPlayer implements Player{

	//Index:day, content:GameInfo MApで
	Map<Integer, GameInfo> gameInfoMap = new HashMap<Integer, GameInfo>();

	int day;

	Agent me;

	Role myRole;

	GameSetting gameSetting;


	@Override
	public String getName() {
		return myRole.name() + "Player:ID=" + me.getAgentIdx();
	}

	@Override
	public void update(GameInfo gameInfo) {
		day = gameInfo.getDay();

		gameInfoMap.put(day, gameInfo);
	}

	public GameInfo getLatestDayGameInfo(){
		return gameInfoMap.get(day);
	}

	public GameInfo getGameInfo(int day){
		try {
			return gameInfoMap.get(day);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Map<Integer, GameInfo> getGameInfoMap(){
		return gameInfoMap;
	}

	public Role getMyRole(){
		return myRole;
	}

	public Agent getMe(){
		return me;
	}

	public int getDay(){
		return day;
	}

	public void setAgent(Agent agent){
		me = agent;
	}

	public GameSetting getGameSetting(){
		return gameSetting;
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting){
		gameInfoMap.clear();
		this.gameSetting = gameSetting;
		day = gameInfo.getDay();
		gameInfoMap.put(day, gameInfo);
		myRole = gameInfo.getRole();
		me = gameInfo.getAgent();
		return;
	}
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
	public abstract Agent divine();

	@Override
	public abstract Agent guard();

	@Override
	public abstract void finish();
}
