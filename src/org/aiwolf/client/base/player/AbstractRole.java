package org.aiwolf.client.base.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public abstract class AbstractRole{

	//Index:day, content:GameInfo MApで
	Map<Integer, GameInfo> gameInfoMap = new HashMap<Integer, GameInfo>();

	int day;

	Agent me;

	Role myRole;

	GameSetting gameSetting;



	public String getName() {
		return myRole.name() + "Player:ID=" + me.getAgentIdx();
	}


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


	public void initialize(GameInfo gameInfo, GameSetting gameSetting){
		gameInfoMap.clear();
		this.gameSetting = gameSetting;
		day = gameInfo.getDay();
		gameInfoMap.put(day, gameInfo);
		myRole = gameInfo.getRole();
		me = gameInfo.getAgent();
		return;
	}

	public abstract void dayStart();


	public abstract String talk();


	public abstract String whisper();


	public abstract Agent vote();


	public abstract Agent attack();


	public abstract Agent divine();


	public abstract Agent guard();


	public abstract void finish();
}
