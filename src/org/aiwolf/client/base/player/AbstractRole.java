/**
 * AbstractRole.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.player;

import java.util.HashMap;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * <div lang="ja">各役職用抽象クラス共通のスーパークラス</div>
 *
 * <div lang="en">The common super class for each role's abstract class</div>
 */
public abstract class AbstractRole {

	Map<Integer, GameInfo> gameInfoMap = new HashMap<Integer, GameInfo>();
	int day;
	Agent me;
	Role myRole;
	GameSetting gameSetting;

	/**
	 * <div lang="ja">このプレイヤーの名前を返す．</div>
	 * 
	 * <div lang="en">Returns this player's name.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーの名前</div>
	 * 
	 *         <div lang="en">this player's name</div>
	 */
	public String getName() {
		return myRole.name() + "Player:ID=" + me.getAgentIdx();
	}

	/**
	 * <div lang="ja">ゲームの情報が更新されたときに呼び出される．</div>
	 * 
	 * <div lang="en">Called when the game information updated.</div>
	 * 
	 * @param gameInfo
	 *            <div lang="ja">ゲームの情報</div>
	 * 
	 *            <div lang="en">information of game</div>
	 */
	public void update(GameInfo gameInfo) {
		day = gameInfo.getDay();

		gameInfoMap.put(day, gameInfo);
	}

	/**
	 * <div lang="ja">今日のゲーム情報を返す．</div>
	 *
	 * <div lang="en">Returns today's game information.</div>
	 * 
	 * @return <div lang="ja">今日のゲーム情報</div>
	 *
	 *         <div lang="en">today's game information</div>
	 */
	public GameInfo getLatestDayGameInfo() {
		return gameInfoMap.get(day);
	}

	/**
	 * <div lang="ja">指定日のゲーム情報を返す．</div>
	 *
	 * <div lang="en">Returns the game information of the specified day.</div>
	 * 
	 * @param day
	 *            <div lang="ja">指定日</div>
	 *
	 *            <div lang="en">the specified day</div>
	 * 
	 * @return <div lang="ja">指定日のゲーム情報</div>
	 *
	 *         <div lang="en">the game information of the specified day</div>
	 */
	public GameInfo getGameInfo(int day) {
		try {
			return gameInfoMap.get(day);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * <div lang="ja">日ごとのゲーム情報を格納したMapオブジェクトを返す．</div>
	 *
	 * <div lang="en">Returns the map object contains the game information of each day.</div>
	 * 
	 * @return <div lang="ja">日ごとのゲーム情報を格納したMapオブジェクト</div>
	 *
	 *         <div lang="en">the map object contains the game information of each day</div>
	 */
	public Map<Integer, GameInfo> getGameInfoMap() {
		return gameInfoMap;
	}

	/**
	 * <div lang="ja">このプレイヤーの役職を返す．</div>
	 *
	 * <div lang="en">Returns this player's role.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーの役職</div>
	 *
	 *         <div lang="en">this player's role</div>
	 */
	public Role getMyRole() {
		return myRole;
	}

	/**
	 * <div lang="ja">このプレイヤーのAgentオブジェクトを返す．</div>
	 *
	 * <div lang="en">Returns this player's Agent object.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーのAgentオブジェクト</div>
	 *
	 *         <div lang="en">this player's Agent object</div>
	 */
	public Agent getMe() {
		return me;
	}

	/**
	 * <div lang="ja">今日が何日目かを返す．</div>
	 *
	 * <div lang="en">Returns what day today is.</div>
	 * 
	 * @return <div lang="ja">今日が何日目か</div>
	 *
	 *         <div lang="en">what day today is</div>
	 */
	public int getDay() {
		return day;
	}

	/**
	 * <div lang="ja">このプレイヤーのAgentオブジェクトをセットする．</div>
	 *
	 * <div lang="en">Sets this player's Agent object.</div>
	 * 
	 * @param agent
	 *            <div lang="ja">このプレイヤーのAgentオブジェクト</div>
	 *
	 *            <div lang="en">this player's Agent object</div>
	 */
	public void setAgent(Agent agent) {
		me = agent;
	}

	/**
	 * <div lang="ja">このプレイヤーがプレイしているゲームの設定を返す．</div>
	 *
	 * <div lang="en">Returns the settings of game this player is playing.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーがプレイしているゲームの設定</div>
	 * 
	 *         <div lang="en">the settings of game this player is playing</div>
	 */
	public GameSetting getGameSetting() {
		return gameSetting;
	}

	/**
	 * <div lang="ja">ゲーム開始時に呼び出される．</div>
	 * 
	 * <div lang="en">Called when the game started.</div>
	 * 
	 * @param gameInfo
	 *            <div lang="ja">ゲームの情報</div> <div lang="en">game information</div>
	 * @param gameSetting
	 *            <div lang="ja">ゲームの設定</div> <div lang="en">game settings</div>
	 */
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		gameInfoMap.clear();
		this.gameSetting = gameSetting;
		day = gameInfo.getDay();
		gameInfoMap.put(day, gameInfo);
		myRole = gameInfo.getRole();
		me = gameInfo.getAgent();
		return;
	}

	/**
	 * <div lang="ja">1日の始まりに呼び出される．</div>
	 * 
	 * <div lang="en">Called when the day started.</div>
	 */
	public abstract void dayStart();

	/**
	 * <div lang="ja">このプレイヤーの発言のテキストを返す． </div>
	 * 
	 * <div lang="en">Returns the text of this player's talk.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーの発言のテキスト</div>
	 * 
	 *         <div lang="en">the text of this player's talk</div>
	 */
	public abstract String talk();

	/**
	 * <div lang="ja">この人狼の囁きのテキストを返す． </div>
	 * 
	 * <div lang="en">Returns the text of this werewolf's whisper. </div>
	 * 
	 * @return <div lang="ja">この人狼の囁きのテキスト</div>
	 * 
	 *         <div lang="en">the text of this werewolf's whisper</div>
	 */
	public abstract String whisper();

	/**
	 * <div lang="ja">このプレイヤーが追放したいプレイヤーを返す．</div>
	 * 
	 * <div lang="en">Returns the player this player wants to execute.</div>
	 * 
	 * @return <div lang="ja">このプレイヤーが追放したいプレイヤー</div>
	 * 
	 *         <div lang="en">the player this player wants to execute</div>
	 */
	public abstract Agent vote();

	/**
	 * <div lang="ja">この人狼が襲撃したいプレイヤーを返す．</div>
	 * 
	 * <div lang="en">Returns the player this werewolf wants to attack.</div>
	 * 
	 * @return <div lang="ja">この人狼が襲撃したいプレイヤー</div>
	 * 
	 *         <div lang="en">the player this werewolf wants to attack</div>
	 */
	public abstract Agent attack();

	/**
	 * <div lang="ja">この占い師が占いたいプレイヤーを返す．</div>
	 * 
	 * <div lang="en">Returns the player whose species this seer wants to divine.</div>
	 * 
	 * @return <div lang="ja">この占い師が占いたいプレイヤー</div>
	 * 
	 *         <div lang="en">the player whose species this seer wants to divine</div>
	 */
	public abstract Agent divine();

	/**
	 * <div lang="ja">この狩人が護衛したいプレイヤーを返す．</div>
	 * 
	 * <div lang="en">Returns the player this bodyguard wants to guard.</div>
	 * 
	 * @return <div lang="ja">この狩人が護衛したいプレイヤー</div>
	 * 
	 *         <div lang="en">the player this bodyguard wants to guard</div>
	 */
	public abstract Agent guard();

	/**
	 * <div lang="ja">ゲーム終了時に呼び出される．<br>
	 * このメソッドが呼び出される前に，ゲームの情報のすべての情報は更新される． </div>
	 * 
	 * <div lang="en">Called when the game finished.<br>
	 * Before this method is called, gameinfo is updated with all information. </div>
	 */
	public abstract void finish();
}
