/**
 * AdvanceGameInfo.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.smpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;

/**
 * <div lang="ja">アドバンストゲーム情報</div>
 *
 * <div lang="en">Advanced game information</div>
 */
public class AdvanceGameInfo {

	/**
	 * <div lang="ja">発話で伝えられた占い結果のリスト</div>
	 *
	 * <div lang="en">list of divinations extracted from talks</div>
	 */
	private List<Judge> inspectJudgeList = new ArrayList<Judge>();

	/**
	 * <div lang="ja">発話で伝えられた霊媒結果のリスト</div>
	 *
	 * <div lang="en">list of inquests extracted from talks</div>
	 */
	private List<Judge> mediumJudgeList = new ArrayList<Judge>();

	private Map<Agent, Role> comingoutMap = new HashMap<Agent, Role>();

	/**
	 * <div lang="ja">カミングアウト状況マップを返す．</div>
	 *
	 * <div lang="en">Returns the map which shows the state of comingouts.</div>
	 * 
	 * @return <div lang="ja">カミングアウト状況マップ</div>
	 *
	 *         <div lang="en">the map which shows the state of comingouts</div>
	 */
	public Map<Agent, Role> getComingoutMap() {
		return comingoutMap;
	}

	/**
	 * <div lang="ja">新たなカミングアウトをカミングアウト状況マップに登録する．</div>
	 *
	 * <div lang="en">Registers a new comingout on the comingout map.</div>
	 * 
	 * @param agent
	 *            <div lang="ja">カミングアウトしたプレイヤー</div>
	 *
	 *            <div lang="en">the player who did this comingout</div>
	 * 
	 * @param role
	 *            <div lang="ja">カミングアウトした役職</div>
	 *
	 *            <div lang="en">the role this comingout claims</div>
	 */
	public void putComingoutMap(Agent agent, Role role) {
		comingoutMap.put(agent, role);
	}

	/**
	 * <div lang="ja">カミングアウト状況マップをセットする．</div>
	 *
	 * <div lang="en">Sets the comingout map.</div>
	 * 
	 * @param comingoutMap
	 *            <div lang="ja">カミングアウト状況マップ</div>
	 *
	 *            <div lang="en">the comingout map</div>
	 */
	public void setComingoutMap(Map<Agent, Role> comingoutMap) {
		this.comingoutMap = comingoutMap;
	}

	/**
	 * <div lang="ja">占い結果リストを返す．</div>
	 *
	 * <div lang="en">Returns the list of divinations.</div>
	 * 
	 * @return <div lang="ja">占い結果リスト</div>
	 *
	 *         <div lang="en">the list of divinations</div>
	 */
	public List<Judge> getInspectJudgeList() {
		return inspectJudgeList;
	}

	/**
	 * <div lang="ja">占い結果リストをセットする．</div>
	 *
	 * <div lang="en">Sets the list of divinations.</div>
	 * 
	 * @param inspectJudgeList
	 *            <div lang="ja">占い結果リスト</div>
	 *
	 *            <div lang="en">the list of divinations</div>
	 */
	public void setInspectJudgeList(List<Judge> inspectJudgeList) {
		this.inspectJudgeList = inspectJudgeList;
	}

	/**
	 * <div lang="ja">占い結果リストに新しい占い結果を追加する．</div>
	 *
	 * <div lang="en">Add a new divination to the list of divinations.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する占い結果</div>
	 *
	 *            <div lang="en">the divination to be added</div>
	 */
	public void addInspectJudgeList(Judge judge) {
		this.inspectJudgeList.add(judge);
	}

	/**
	 * <div lang="ja">霊媒結果リストを返す．</div>
	 *
	 * <div lang="en">Returns the list of inquests.</div>
	 * 
	 * @return <div lang="ja">霊媒結果リスト</div>
	 *
	 *         <div lang="en">the list of inquests</div>
	 */
	public List<Judge> getMediumJudgeList() {
		return mediumJudgeList;
	}

	/**
	 * <div lang="ja">霊媒結果リストをセットする．</div>
	 *
	 * <div lang="en">Sets the list of inquests.</div>
	 * 
	 * @param mediumJudgeList
	 *            <div lang="ja">霊媒結果リスト</div>
	 *
	 *            <div lang="en">the list of inquests</div>
	 */
	public void setMediumJudgeList(List<Judge> mediumJudgeList) {
		this.mediumJudgeList = mediumJudgeList;
	}

	/**
	 * <div lang="ja">霊媒結果リストに新しい霊媒結果を追加する．</div>
	 *
	 * <div lang="en">Add a new inquest to the list of inquests.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する霊媒結果</div>
	 *
	 *            <div lang="en">the inquest to be added</div>
	 */
	public void addMediumJudgeList(Judge judge) {
		this.mediumJudgeList.add(judge);
	}

}
