/**
 * AbstractMedium.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.player;

import java.util.ArrayList;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * <div lang="ja">霊媒師用抽象クラス</div>
 *
 * <div lang="en">Abstract class for medium</div>
 * 
 * @deprecated
 */
public abstract class AbstractMedium extends AbstractRole {

	ArrayList<Judge> myJudgeList = new ArrayList<Judge>();

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		myJudgeList.clear();
		super.initialize(gameInfo, gameSetting);
	}

	@Override
	public void dayStart() {
		if (gameInfoMap.get(getDay()).getMediumResult() != null) {
			myJudgeList.add(getLatestDayGameInfo().getMediumResult());
		}
	}

	@Override
	public abstract String talk();

	@Override
	public final String whisper() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract Agent vote();

	@Override
	public final Agent attack() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent divine() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent guard() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract void finish();

	/**
	 * <div lang="ja">このクラスの新しいインスタンスを初期化する．</div>
	 *
	 * <div lang="en">Initializes a new instance of this class.</div>
	 */
	public AbstractMedium() {
		myRole = Role.MEDIUM;
	}

	/**
	 * <div lang="ja">この霊媒師のこれまでの霊媒結果のリストを返す．</div>
	 *
	 * <div lang="en">Returns the list of inquests this medium done until now.</div>
	 * 
	 * @return <div lang="ja">この霊媒師のこれまでの霊媒結果のリスト</div>
	 *
	 *         <div lang="en">the list of inquests this medium done until now</div>
	 */
	public ArrayList<Judge> getMyJudgeList() {
		return myJudgeList;
	}

	/**
	 * <div lang="ja">与えられたプレイヤーが霊媒判定済みかどうかを返す．</div>
	 *
	 * <div lang="en">Returns whether or not the given player is judged by this medium.</div>
	 * 
	 * @param agent
	 *            <div lang="ja">プレイヤー</div>
	 *
	 *            <div lang="en">player</div>
	 * 
	 * @return <div lang="ja">与えられたプレイヤーが霊媒判定済みかどうか</div>
	 *
	 *         <div lang="en">whether or not the given player is judged by this medium</div>
	 */
	public boolean isJudgedAgent(Agent agent) {
		for (Judge judge : myJudgeList) {
			if (judge.getTarget() == agent) {
				return true;
			}
		}
		return false;
	}

}
