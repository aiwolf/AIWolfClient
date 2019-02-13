/**
 * SampleBasePlayer.java
 * 
 * Copyright (c) 2018 人狼知能プロジェクト
 */

package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * すべての役職のベースとなるクラス
 * 
 * @author otsuki
 */
public class SampleBasePlayer implements Player {

	/**
	 * このエージェント
	 */
	protected Agent me;

	/**
	 * 日付
	 */
	protected int day;

	/**
	 * talk()できるか時間帯か
	 */
	protected boolean canTalk;

	/**
	 * whisper()できるか時間帯か
	 */
	protected boolean canWhisper;

	/**
	 * 最新のゲーム情報
	 */
	protected GameInfo currentGameInfo;

	/**
	 * 自分以外の生存エージェント
	 */
	protected List<Agent> aliveOthers;

	/**
	 * 追放されたエージェント
	 */
	protected List<Agent> executedAgents = new ArrayList<>();

	/**
	 * 殺されたエージェント
	 */
	protected List<Agent> killedAgents = new ArrayList<>();

	/**
	 * 発言された占い結果報告のリスト
	 */
	protected List<Judge> divinationList = new ArrayList<>();

	/**
	 * 発言された霊媒結果報告のリスト
	 */
	protected List<Judge> identList = new ArrayList<>();

	/**
	 * 発言用待ち行列
	 */
	protected Deque<Content> talkQueue = new LinkedList<>();

	/**
	 * 囁き用待ち行列
	 */
	protected Deque<Content> whisperQueue = new LinkedList<>();

	/**
	 * 投票先候補
	 */
	protected Agent voteCandidate;

	/**
	 * 宣言済み投票先候補
	 */
	protected Agent declaredVoteCandidate;

	/**
	 * 襲撃投票先候補
	 */
	protected Agent attackVoteCandidate;

	/**
	 * 宣言済み襲撃投票先候補
	 */
	protected Agent declaredAttackVoteCandidate;

	/**
	 * カミングアウト状況
	 */
	protected Map<Agent, Role> comingoutMap = new HashMap<>();

	/**
	 * GameInfo.talkList読み込みのヘッド
	 */
	protected int talkListHead;

	/**
	 * 人間リスト
	 */
	protected List<Agent> humans = new ArrayList<>();

	/**
	 * 人狼リスト
	 */
	protected List<Agent> werewolves = new ArrayList<>();

	/**
	 * 推測理由マップ
	 */
	protected EstimateReasonMaps estimateReasonMaps = new EstimateReasonMaps();

	/**
	 * 投票理由マップ
	 */
	protected VoteReasonMap voteReasonMap = new VoteReasonMap();

	/**
	 * エージェントが生きているかどうかを返す
	 * 
	 * @param agent
	 * @return
	 */
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	/**
	 * エージェントが殺されたかどうかを返す
	 * 
	 * @param agent
	 * @return
	 */
	protected boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}

	/**
	 * エージェントがカミングアウトしたかどうかを返す
	 * 
	 * @param agent
	 * @return
	 */
	protected boolean isCo(Agent agent) {
		return comingoutMap.containsKey(agent);
	}

	/**
	 * 役職がカミングアウトされたかどうかを返す
	 * 
	 * @param role
	 * @return
	 */
	protected boolean isCo(Role role) {
		return comingoutMap.containsValue(role);
	}

	/**
	 * エージェントが人間かどうかを返す
	 */
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}

	/**
	 * エージェントが人狼かどうかを返す
	 * 
	 * @param agent
	 * @return
	 */
	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}

	/**
	 * リストからランダムに選んで返す
	 * 
	 * @param list
	 * @return
	 */
	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	@Override
	public String getName() {
		return "SampleBasePlayer";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		day = -1;
		me = gameInfo.getAgent();
		aliveOthers = new ArrayList<>(gameInfo.getAliveAgentList());
		aliveOthers.remove(me);
		executedAgents.clear();
		killedAgents.clear();
		divinationList.clear();
		identList.clear();
		comingoutMap.clear();
		humans.clear();
		werewolves.clear();
		estimateReasonMaps.clear();
		voteReasonMap.clear();
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// 1日の最初の呼び出しはdayStart()の前なので何もしない
		if (currentGameInfo.getDay() == day + 1) {
			day = currentGameInfo.getDay();
			return;
		}
		// 2回目の呼び出し以降
		// （夜限定）追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {
				continue;
			}
			Content content = new Content(talk.getText());

			// subjectがUNSPECの場合は発話者に入れ替える
			if (content.getSubject() == Agent.UNSPEC) {
				content = replaceSubject(content, talker);
			}

			// 推測・投票発言があれば登録
			if (!estimateReasonMaps.addEstimateReason(content) && !voteReasonMap.addVoteReason(content)) {
				// それ以外の発言の処理
				switch (content.getTopic()) {
				case COMINGOUT:
					comingoutMap.put(content.getTarget(), content.getRole());
					break;
				case DIVINED:
					divinationList.add(new Judge(day, content.getSubject(), content.getTarget(), content.getResult()));
					break;
				case IDENTIFIED:
					identList.add(new Judge(day, content.getSubject(), content.getTarget(), content.getResult()));
					break;
				default:
					break;
				}
			}
		}
		talkListHead = currentGameInfo.getTalkList().size();
	}

	@Override
	public void dayStart() {
		canTalk = true;
		canWhisper = false;
		if (currentGameInfo.getRole() == Role.WEREWOLF) {
			canWhisper = true;
		}
		talkQueue.clear();
		whisperQueue.clear();
		declaredVoteCandidate = null;
		voteCandidate = null;
		declaredAttackVoteCandidate = null;
		attackVoteCandidate = null;
		talkListHead = 0;
		// 前日に追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getExecutedAgent());
		// 昨夜死亡した（襲撃された）エージェントを登録
		if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
		}
	}

	private void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			aliveOthers.remove(executedAgent);
			if (!executedAgents.contains(executedAgent)) {
				executedAgents.add(executedAgent);
			}
		}
	}

	private void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			aliveOthers.remove(killedAgent);
			if (!killedAgents.contains(killedAgent)) {
				killedAgents.add(killedAgent);
			}
		}
	}

	/**
	 * 投票先候補を選びvoteCandidateにセットする
	 */
	protected void chooseVoteCandidate() {
	}

	@Override
	public String talk() {
		chooseVoteCandidate();
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			Content vote = new Content(new VoteContentBuilder(me, voteCandidate));
			Content reason = voteReasonMap.getReason(me, voteCandidate);
			if (reason != null) {
				enqueueTalk(new Content(new BecauseContentBuilder(me, reason, vote)));
			} else {
				enqueueTalk(vote);
			}
			declaredVoteCandidate = voteCandidate;
		}
		return dequeueTalk();
	}

	protected void enqueueTalk(Content content) {
		if (content.getSubject() == Agent.UNSPEC) {
			talkQueue.offer(replaceSubject(content, me));
		} else {
			talkQueue.offer(content);
		}
	}

	protected String dequeueTalk() {
		if (talkQueue.isEmpty()) {
			return Talk.SKIP;
		}
		Content content = talkQueue.poll();
		if (content.getSubject() == me) {
			return Content.stripSubject(content.getText());
		}
		return content.getText();
	}

	/**
	 * 襲撃先候補を選びattackVoteCandidateにセットする
	 */
	protected void chooseAttackVoteCandidate() {
	}

	@Override
	public String whisper() {
		chooseAttackVoteCandidate();
		if (attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	@Override
	public Agent vote() {
		canTalk = false;
		chooseVoteCandidate();
		return voteCandidate;
	}

	@Override
	public Agent attack() {
		canWhisper = false;
		chooseAttackVoteCandidate();
		canWhisper = true;
		return attackVoteCandidate;
	}

	@Override
	public Agent divine() {
		return null;
	}

	@Override
	public Agent guard() {
		return null;
	}

	@Override
	public void finish() {
	}

	static Content replaceSubject(Content content, Agent newSubject) {
		if (content.getTopic() == Topic.SKIP || content.getTopic() == Topic.OVER) {
			return content;
		}
		if (Agent.UNSPEC == newSubject) {
			return new Content(Content.stripSubject(content.getText()));
		} else {
			return new Content(newSubject + " " + Content.stripSubject(content.getText()));
		}
	}
}
