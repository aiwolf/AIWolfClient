/**
 * AdditionalGameInfo.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.util.Counter;

/**
 * <div lang="ja">追加のゲーム情報</div>
 *
 * <div lang="en">Additional game information.</div>
 */
public class AdditionalGameInfo {

	private List<Agent> executedAgentList = new ArrayList<>();
	private List<Judge> divinationList = new ArrayList<>();
	private List<Judge> inquestList = new ArrayList<>();
	private List<Judge> whisperedDivinationList = new ArrayList<>();
	private List<Judge> whisperedInquestList = new ArrayList<>();
	private List<Agent> killedAgentList = new ArrayList<>();
	private List<Agent> aliveOthers;
	private List<Agent> deadOthers = new ArrayList<>();
	private Map<Agent, Role> comingoutMap = new HashMap<>();
	private Map<Agent, Role> whisperedComingoutMap = new HashMap<>();
	private Map<Agent, List<Judge>> judgeMap = new HashMap<>();
	private Map<Agent, List<Judge>> whisperedJudgeMap = new HashMap<>();
	private Map<Agent, List<Talk>> estimateMap = new HashMap<>();
	private Map<Agent, List<Talk>> whisperedEstimateMap = new HashMap<>();
	private Map<Agent, Agent> voteMap = new HashMap<>();
	private Map<Agent, Agent> whisperedVoteMap = new HashMap<>();
	private Map<Agent, Agent> attackVoteMap = new HashMap<>();
	private Counter<Agent> voteCounter = new Counter<>();
	private Counter<Agent> whisperedVoteCounter = new Counter<>();
	private Counter<Agent> attackVoteCounter = new Counter<>();
	private Agent me;
	private Role myRole;
	private int talkListHead; // talkList読み込みのヘッド
	private int whisperListHead; // whisperList読み込みのヘッド
	private int day;

	/**
	 * <div lang="ja">自分自身を返す</div>
	 *
	 * <div lang="en">Returns myself.</div>
	 * 
	 * @return <div lang="ja">自分自身を表す{@code Agent}</div>
	 *
	 *         <div lang="en">{@code Agent} representing myself.</div>
	 */
	public Agent getMe() {
		return me;
	}

	/**
	 * <div lang="ja">自分の役職を返す</div>
	 *
	 * <div lang="en">Returns my role.</div>
	 * 
	 * @return <div lang="ja">自分の役職を表す{@code Role}</div>
	 *
	 *         <div lang="en">{@code Role} representing my role.</div>
	 */
	public Role getMyRole() {
		return myRole;
	}

	/**
	 * <div lang="ja">GameInfoに基づいて{@code AdditionalGameInfo}を構築する</div>
	 *
	 * <div lang="en">Constructs {@code AdditionalGameInfo} based on GameInfo.</div>
	 */
	public AdditionalGameInfo(GameInfo gameInfo) {
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		aliveOthers = new ArrayList<>(gameInfo.getAgentList());
		aliveOthers.remove(me);
	}

	/**
	 * <div lang="ja">自分以外の生存者を返す</div>
	 *
	 * <div lang="en">Returns the alive agents except me.</div>
	 * 
	 * @return <div lang="ja">生存者のリストを表す{@code List<Agent>}</div>
	 *
	 *         <div lang="en">{@code List<Agent>} representing the list of alive agents.</div>
	 */
	public List<Agent> getAliveOthers() {
		return aliveOthers;
	}

	/**
	 * <div lang="ja">自分以外の死者を返す</div>
	 *
	 * <div lang="en">Returns the dead agents except me.</div>
	 * 
	 * @return <div lang="ja">死者のリストを表す{@code List<Agent>}</div>
	 *
	 *         <div lang="en">{@code List<Agent>} representing the list of dead agents.</div>
	 */
	public List<Agent> getDeadOthers() {
		return deadOthers;
	}

	/**
	 * <div lang="ja">追放されたエージェントのリストを返す</div>
	 *
	 * <div lang="en">Returns the list of executed agents.</div>
	 *
	 * @return <div lang="ja">追放されたエージェントのリストを表す{@code List<Agent>}</div>
	 * 
	 *         <div lang="en">{@code List<Agent>} representing the list of executed agents.</div>
	 */
	public List<Agent> getExecutedAgentList() {
		return executedAgentList;
	}

	/**
	 * <div lang="ja">エージェントが，追放されたエージェントのリストに無かった場合は追加する。同時に死亡者リストにも追加し，生存者リストから除く</div>
	 *
	 * <div lang="en">Adds the agent to the list of the executed ones if it doesn't contain the agent. At the same time,
	 * the agent is added to the list of the dead ones and removed from the list of the alive ones. </div>
	 *
	 * @param executedAgent
	 *            <div lang="ja">追加するエージェントを表す{@code Agent}</div>
	 * 
	 *            <div lang="en">{@code Agent} representing the agent to be added to the list.</div>
	 */
	public void addExecutedAgent(Agent executedAgent) {
		if (executedAgent == null) {
			return;
		}
		if (!executedAgentList.contains(executedAgent)) {
			executedAgentList.add(executedAgent);
		}
		if (executedAgent != me) {
			aliveOthers.remove(executedAgent);
			if (!deadOthers.contains(executedAgent)) {
				deadOthers.add(executedAgent);
			}
		}
	}

	/**
	 * <div lang="ja">殺された（襲撃，呪殺，ただし追放は含まない）エージェントのリストを返す</div>
	 *
	 * <div lang="en">Returns the list of killed agents. Notice that this list doesn't contain the executed ones. </div>
	 *
	 * @return <div lang="ja">殺されたエージェントのリストを表す{@code List<Agent>}</div>
	 * 
	 *         <div lang="en">{@code List<Agent>} representing the list of killed agents.</div>
	 */
	public List<Agent> getKilledAgentList() {
		return killedAgentList;
	}

	/**
	 * <div lang="ja">エージェントが，殺された（襲撃，呪殺，ただし追放は含まない）エージェントのリストに無かった場合追加する。同時に死亡者リストにも追加し，生存者リストから除く</div>
	 *
	 * <div lang="en">Adds the agent to the list of the killed ones if it doesn't contain the agent. Notice that this
	 * list doesn't contain the executed ones. At the same time, the agent is added to the list of the dead ones and
	 * removed from the list of the alive ones. </div>
	 *
	 * @param killedAgentList
	 *            <div lang="ja">追加するエージェントを表す{@code Agent}</div>
	 * 
	 *            <div lang="en">{@code Agent} representing the agent to be added to the list.</div>
	 */
	public void addKilledAgent(Agent killedAgent) {
		if (killedAgent == null) {
			return;
		}
		if (!killedAgentList.contains(killedAgent)) {
			killedAgentList.add(killedAgent);
		}
		if (killedAgent != me) {
			aliveOthers.remove(killedAgent);
			if (!deadOthers.contains(killedAgent)) {
				deadOthers.add(killedAgent);
			}
		}
	}

	/**
	 * <div lang="ja">カミングアウト状況を返す</div>
	 *
	 * <div lang="en">Returns the situation of comingouts.</div>
	 * 
	 * @return <div lang="ja">カミングアウト状況を表す{@code Map<Agent, Role>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, Role>} representing the situation of comingouts.</div>
	 */
	public Map<Agent, Role> getComingoutMap() {
		return comingoutMap;
	}

	/**
	 * <div lang="ja">新たなカミングアウトをカミングアウト状況に登録する</div>
	 *
	 * <div lang="en">Registers a new comingout on the comingout map.</div>
	 * 
	 * @param agent
	 *            <div lang="ja">カミングアウトしたプレイヤーを表す{@code Agent}</div>
	 *
	 *            <div lang="en">{@code Agent} representing the player who did this comingout.</div>
	 * 
	 * @param role
	 *            <div lang="ja">カミングアウトした役職を表す{@code Role}</div>
	 *
	 *            <div lang="en">{@code Role} representing the role this comingout claims.</div>
	 */
	public void putComingoutMap(Agent agent, Role role) {
		comingoutMap.put(agent, role);
	}

	/**
	 * <div lang="ja">カミングアウト（囁き）状況を返す</div>
	 *
	 * <div lang="en">Returns the situation of whispered comingouts.</div>
	 * 
	 * @return <div lang="ja">カミングアウト（囁き）状況を表す{@code Map<Agent, Role>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, Role>} representing the situation of whispered comingouts.</div>
	 */
	public Map<Agent, Role> getWhisperedComingoutMap() {
		return whisperedComingoutMap;
	}

	/**
	 * <div lang="ja">新たなカミングアウト（囁き）をカミングアウト（囁き）状況に登録する</div>
	 *
	 * <div lang="en">Registers a new whispered comingout on the whiepered comingout map.</div>
	 * 
	 * @param agent
	 *            <div lang="ja">カミングアウトしたプレイヤーを表す{@code Agent}</div>
	 *
	 *            <div lang="en">{@code Agent} representing the player who did this comingout.</div>
	 * 
	 * @param role
	 *            <div lang="ja">カミングアウトした役職を表す{@code Role}</div>
	 *
	 *            <div lang="en">{@code Role} representing the role this comingout claims.</div>
	 */
	public void putWhisperedComingoutMap(Agent agent, Role role) {
		whisperedComingoutMap.put(agent, role);
	}

	/**
	 * <div lang="ja">判定状況マップを返す</div>
	 *
	 * <div lang="en">Returns the situation of judgment.</div>
	 * 
	 * @return <div lang="ja">判定状況を表す{@code Map<Agent, List<Judge>>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, List<Judge>>} representing the situation of judgment.</div>
	 */
	public Map<Agent, List<Judge>> getJudgeMap() {
		return judgeMap;
	}

	/**
	 * <div lang="ja">新たな判定を判定状況マップに登録する</div>
	 *
	 * <div lang="en">Registers a new judgment on the judgment map.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">判定を表す{@code Judge}</div>
	 * 
	 *            <div lang="en">{@code Judge} representing the judgment.</div>
	 */
	public void putJudgeMap(Judge judge) {
		Agent agent = judge.getAgent();
		if (judgeMap.get(agent) == null) {
			judgeMap.put(agent, new ArrayList<Judge>());
		}
		judgeMap.get(agent).add(judge);
	}

	/**
	 * <div lang="ja">判定（囁き）状況マップを返す</div>
	 *
	 * <div lang="en">Returns the situation of whispered judgment.</div>
	 * 
	 * @return <div lang="ja">判定（囁き）状況を表す{@code Map<Agent, List<Judge>>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, List<Judge>>} representing the situation of whispered judgment.</div>
	 */
	public Map<Agent, List<Judge>> getWhisperedJudgeMap() {
		return whisperedJudgeMap;
	}

	/**
	 * <div lang="ja">新たな判定（囁き）を判定（囁き）状況マップに登録する</div>
	 *
	 * <div lang="en">Registers a new whispered judgment on the whispered judgment map.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">判定（囁き）を表す{@code Judge}</div>
	 * 
	 *            <div lang="en">{@code Judge} representing the whispered judgment.</div>
	 */
	public void putWhisperedJudgeMap(Judge judge) {
		Agent agent = judge.getAgent();
		if (whisperedJudgeMap.get(agent) == null) {
			whisperedJudgeMap.put(agent, new ArrayList<Judge>());
		}
		whisperedJudgeMap.get(agent).add(judge);
	}

	/**
	 * <div lang="ja">推測状況マップを返す</div>
	 *
	 * <div lang="en">Returns the situation of estimate.</div>
	 * 
	 * @return <div lang="ja">推測状況を表す{@code Map<Agent, List<Talk>>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, List<Talk>>} representing the situation of estimate.</div>
	 */
	public Map<Agent, List<Talk>> getEstimateMap() {
		return estimateMap;
	}

	/**
	 * <div lang="ja">新たな推測発言を推測状況マップに登録する</div>
	 *
	 * <div lang="en">Registers a new estimating talk on the estimate map.</div>
	 * 
	 * @param content
	 *            <div lang="ja">推測発言を表す{@code Talk}</div>
	 * 
	 *            <div lang="en">{@code Talk} representing the estimating talk.</div>
	 */
	public void putEstimateMap(Talk talk) {
		Content content = new Content(talk.getText());
		if (content.getTopic() != Topic.ESTIMATE) {
			return;
		}
		Agent agent = content.getTarget();
		if (agent != null) {
			if (estimateMap.get(agent) == null) {
				estimateMap.put(agent, new ArrayList<Talk>());
			}
			estimateMap.get(agent).add(talk);
		}
	}

	/**
	 * <div lang="ja">推測（囁き）状況マップを返す</div>
	 *
	 * <div lang="en">Returns the situation of whispered estimate.</div>
	 * 
	 * @return <div lang="ja">推測（囁き）状況を表す{@code Map<Agent, List<Talk>>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, List<Talk>>} representing the situation of whispered estimate.</div>
	 */
	public Map<Agent, List<Talk>> getWhisperedEstimateMap() {
		return whisperedEstimateMap;
	}

	/**
	 * <div lang="ja">新たな推測（囁き）発言を推測（囁き）状況マップに登録する</div>
	 *
	 * <div lang="en">Registers a new estimating whisper on the whispered estimate map.</div>
	 * 
	 * @param content
	 *            <div lang="ja">推測（囁き）発言を表す{@code Talk}</div>
	 * 
	 *            <div lang="en">{@code Talk} representing the estimating whisper.</div>
	 */
	public void putWhisperedEstimateMap(Talk talk) {
		Content content = new Content(talk.getText());
		if (content.getTopic() != Topic.ESTIMATE) {
			return;
		}
		Agent agent = content.getTarget();
		if (agent != null) {
			if (whisperedEstimateMap.get(agent) == null) {
				whisperedEstimateMap.put(agent, new ArrayList<Talk>());
			}
			whisperedEstimateMap.get(agent).add(talk);
		}
	}

	/**
	 * <div lang="ja">占い結果リストを返す</div>
	 *
	 * <div lang="en">Returns the list of divinations.</div>
	 * 
	 * @return <div lang="ja">占い結果リストを表す{@code List<Judge>}</div>
	 *
	 *         <div lang="en">{@code List<Judge>} representing the list of divinations.</div>
	 */
	public List<Judge> getDivinationList() {
		return divinationList;
	}

	/**
	 * <div lang="ja">占い結果リストに新しい占い結果を追加する</div>
	 *
	 * <div lang="en">Add a new divination to the list of divinations.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する占い結果を表す{@code Judge}</div>
	 *
	 *            <div lang="en">{@code Judge} representing the divination to be added.</div>
	 */
	public void addDivination(Judge judge) {
		divinationList.add(judge);
	}

	/**
	 * <div lang="ja">占い結果（囁き）リストを返す</div>
	 *
	 * <div lang="en">Returns the list of whispered divinations.</div>
	 * 
	 * @return <div lang="ja">占い結果（囁き）リストを表す{@code List<Judge>}</div>
	 *
	 *         <div lang="en">{@code List<Judge>} representing the list of whispered divinations.</div>
	 */
	public List<Judge> getWhisperedDivinationList() {
		return whisperedDivinationList;
	}

	/**
	 * <div lang="ja">占い結果（囁き）リストに新しい占い結果（囁き）を追加する</div>
	 *
	 * <div lang="en">Add a new whispered divination to the list of whispered divinations.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する占い結果（囁き）を表す{@code Judge}</div>
	 *
	 *            <div lang="en">{@code Judge} representing the whispered divination to be added.</div>
	 */
	public void addWhisperedDivination(Judge judge) {
		whisperedDivinationList.add(judge);
	}

	/**
	 * <div lang="ja">霊媒結果リストを返す</div>
	 *
	 * <div lang="en">Returns the list of inquests.</div>
	 * 
	 * @return <div lang="ja">霊媒結果リストを表す{@code List<Judge>}</div>
	 *
	 *         <div lang="en">{@code List<Judge>} representing the list of inquests.</div>
	 */
	public List<Judge> getInquestList() {
		return inquestList;
	}

	/**
	 * <div lang="ja">霊媒結果リストに新しい霊媒結果を追加する</div>
	 *
	 * <div lang="en">Add a new inquest to the list of inquests.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する霊媒結果を表す{@code Judge}</div>
	 *
	 *            <div lang="en">{@code Judge} representing the inquest to be added.</div>
	 */
	public void addInquestList(Judge judge) {
		this.inquestList.add(judge);
	}

	/**
	 * <div lang="ja">霊媒結果（囁き）リストを返す</div>
	 *
	 * <div lang="en">Returns the list of whispered inquests.</div>
	 * 
	 * @return <div lang="ja">霊媒結果（囁き）リストを表す{@code List<Judge>}</div>
	 *
	 *         <div lang="en">{@code List<Judge>} representing the list of whispered inquests.</div>
	 */
	public List<Judge> getWhisperedInquestList() {
		return whisperedInquestList;
	}

	/**
	 * <div lang="ja">霊媒結果（囁き）リストに新しい霊媒結果（囁き）を追加する</div>
	 *
	 * <div lang="en">Add a new whispered inquest to the list of whispered inquests.</div>
	 * 
	 * @param judge
	 *            <div lang="ja">追加する霊媒結果（囁き）を表す{@code Judge}</div>
	 *
	 *            <div lang="en">{@code Judge} representing the whispered inquest to be added.</div>
	 */
	public void addWhisperedInquestList(Judge judge) {
		whisperedInquestList.add(judge);
	}

	/**
	 * <div lang="ja">投票宣言マップを返す</div>
	 *
	 * <div lang="en">Returns the map of the declaration of vote.</div>
	 * 
	 * @return <div lang="ja">投票宣言マップを表す{@code Map<Agent, Agent>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, Agent>} representing the map of the declaration of vote.</div>
	 */
	public Map<Agent, Agent> getVoteMap() {
		return voteMap;
	}

	/**
	 * <div lang="ja">投票宣言カウンタを返す</div>
	 *
	 * <div lang="en">Returns the counter of the declaration of vote.</div>
	 * 
	 * @return <div lang="ja">投票宣言カウンタを表す{@code Counter<Agent>}</div>
	 *
	 *         <div lang="en">{@code Counter<Agent>} representing the counter of the declaration of vote.</div>
	 */
	public Counter<Agent> getVoteCounter() {
		return voteCounter;
	}

	/**
	 * <div lang="ja">投票宣言（囁き）マップを返す</div>
	 *
	 * <div lang="en">Returns the map of the whispered declaration of vote.</div>
	 * 
	 * @return <div lang="ja">投票宣言（囁き）マップを表す{@code Map<Agent, Agent>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, Agent>} representing the map of the whispered declaration of vote.</div>
	 */
	public Map<Agent, Agent> getWhisperedVoteMap() {
		return whisperedVoteMap;
	}

	/**
	 * <div lang="ja">投票宣言（囁き）カウンタを返す</div>
	 *
	 * <div lang="en">Returns the counter of the whispered declaration of vote.</div>
	 * 
	 * @return <div lang="ja">投票宣言（囁き）カウンタを表す{@code Counter<Agent>}</div>
	 *
	 *         <div lang="en">{@code Counter<Agent>} representing the counter of the whispered declaration of
	 *         vote.</div>
	 */
	public Counter<Agent> getWhisperedVoteCounter() {
		return whisperedVoteCounter;
	}

	/**
	 * <div lang="ja">襲撃投票宣言マップを返す</div>
	 *
	 * <div lang="en">Returns the map of the declaration of vote for attack.</div>
	 * 
	 * @return <div lang="ja">襲撃投票宣言マップを表す{@code Map<Agent, Agent>}</div>
	 *
	 *         <div lang="en">{@code Map<Agent, Agent>} representing the map of the declaration of vote for
	 *         attack.</div>
	 */
	public Map<Agent, Agent> getAttackVoteMap() {
		return attackVoteMap;
	}

	/**
	 * <div lang="ja">襲撃投票宣言カウンタを返す</div>
	 *
	 * <div lang="en">Returns the counter of the declaration of vote for attack.</div>
	 * 
	 * @return <div lang="ja">襲撃投票宣言カウンタを表す{@code Counter<Agent>}</div>
	 *
	 *         <div lang="en">{@code Counter<Agent>} representing the counter of the declaration of vote for
	 *         attack.</div>
	 */
	public Counter<Agent> getAttackVoteCounter() {
		return attackVoteCounter;
	}

	/**
	 * <div lang="ja">追加ゲーム情報を更新する</div>
	 *
	 * <div lang="en">Updates the additional game information.</div>
	 * 
	 * @param gameInfo
	 *            <div lang="ja">ゲーム情報を表す{@code GameInfo}</div>
	 *
	 *            <div lang="en">{@code GameInfo} representing the game information.</div>
	 */
	public void update(GameInfo gameInfo) {

		// 1日の最初の呼び出しで，日ごとの初期化などを行う
		if (gameInfo.getDay() == day + 1) {
			day = gameInfo.getDay();
			talkListHead = 0;
			whisperListHead = 0;

			// その日の投票宣言マップをクリア
			voteMap.clear();
			attackVoteMap.clear();

			// 前日に追放されたエージェントを登録
			addExecutedAgent(gameInfo.getExecutedAgent());

			// 前日に襲撃死あるいは呪殺されたエージェントを登録
			for (Agent agent : gameInfo.getLastDeadAgentList()) {
				addKilledAgent(agent);
			}
		}

		// （追放直後に呼ばれた場合限定）追放されたエージェントを登録
		addExecutedAgent(gameInfo.getLatestExecutedAgent());

		// talkListからカミングアウト，占い結果，霊媒結果，投票宣言を抽出
		List<Talk> talkList = gameInfo.getTalkList();
		for (int i = talkListHead; i < talkList.size(); i++) {
			Talk talk = talkList.get(i);
			Agent talker = talk.getAgent();
			Content content = new Content(talk.getText());
			switch (content.getTopic()) {
			case COMINGOUT:
				putComingoutMap(talker, content.getRole());
				break;

			case DIVINED:
				Judge divination = new Judge(day, talker, content.getTarget(), content.getResult());
				addDivination(divination);
				putJudgeMap(divination);
				break;

			case INQUESTED:
				Judge inquest = new Judge(day, talker, content.getTarget(), content.getResult());
				addInquestList(inquest);
				putJudgeMap(inquest);
				break;

			case VOTE:
				getVoteMap().put(talker, content.getTarget());
				break;

			case ESTIMATE:
				putEstimateMap(talk);
				break;

			default:
				break;
			}
		}
		talkListHead = talkList.size();

		// 投票宣言の集計
		voteCounter.clear();
		for (Agent agent : voteMap.keySet()) {
			voteCounter.add(voteMap.get(agent));
		}

		// whisperListからカミングアウト予告，占い結果予告，霊媒結果予告，投票宣言，襲撃投票宣言を抽出
		List<Talk> whisperList = gameInfo.getWhisperList();
		for (int i = whisperListHead; i < whisperList.size(); i++) {
			Talk whisper = whisperList.get(i);
			Agent whisperer = whisper.getAgent();
			Content content = new Content(whisper.getText());
			switch (content.getTopic()) {
			case COMINGOUT:
				putWhisperedComingoutMap(whisperer, content.getRole());
				break;

			case DIVINED:
				Judge whisperedDivination = new Judge(day, whisperer, content.getTarget(), content.getResult());
				addWhisperedDivination(whisperedDivination);
				putWhisperedJudgeMap(whisperedDivination);
				break;

			case INQUESTED:
				Judge whisperedInquest = new Judge(day, whisperer, content.getTarget(), content.getResult());
				addWhisperedInquestList(whisperedInquest);
				putWhisperedJudgeMap(whisperedInquest);
				break;

			case VOTE:
				getWhisperedVoteMap().put(whisperer, content.getTarget());
				break;

			case ESTIMATE:
				putWhisperedEstimateMap(whisper);
				break;

			case ATTACK:
				getAttackVoteMap().put(whisperer, content.getTarget());
				break;

			default:
				break;
			}
		}
		whisperListHead = whisperList.size();

		// 投票宣言の集計
		whisperedVoteCounter.clear();
		for (Agent agent : whisperedVoteMap.keySet()) {
			whisperedVoteCounter.add(whisperedVoteMap.get(agent));
		}

		// 襲撃投票宣言の集計
		attackVoteCounter.clear();
		for (Agent agent : attackVoteMap.keySet()) {
			attackVoteCounter.add(attackVoteMap.get(agent));
		}
	}

}
