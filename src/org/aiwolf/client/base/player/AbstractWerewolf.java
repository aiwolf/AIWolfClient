/**
 * AbstractWerewolf.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

/**
 * <div lang="ja">人狼用抽象クラス</div>
 *
 * <div lang="en">Abstract class for werewolf</div>
 * 
 * @deprecated
 */
public abstract class AbstractWerewolf extends AbstractRole {

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
	public AbstractWerewolf() {
		myRole = Role.WEREWOLF;
	}

	/**
	 * <div lang="ja">人狼のリストを返す．</div>
	 *
	 * <div lang="en">Returns the list of werewolves.</div>
	 * 
	 * @return <div lang="ja">人狼のリスト</div>
	 *
	 *         <div lang="en">the list of werewolves</div>
	 */
	public List<Agent> getWolfList() {
		List<Agent> wolfList = new ArrayList<Agent>();

		Map<Agent, Role> wolfMap = getLatestDayGameInfo().getRoleMap();
		for (Entry<Agent, Role> set : wolfMap.entrySet()) {
			if (set.getValue() == Role.WEREWOLF) {
				wolfList.add(set.getKey());
			}
		}
		return wolfList;
	}

}
