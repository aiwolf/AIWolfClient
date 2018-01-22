package org.aiwolf.sample.player;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

/** すべての役職のベースとなるクラス */
public class SampleBasePlayer implements Player {
	/** このエージェント */
	Agent me;
	/** 日付 */
	int day;
	/** talk()できるか時間帯か */
	boolean canTalk;
	/** whisper()できるか時間帯か */
	boolean canWhisper;
	/** 最新のゲーム情報 */
	GameInfo currentGameInfo;
	/** 自分以外の生存エージェント */
	List<Agent> aliveOthers;
	/** 追放されたエージェント */
	List<Agent> executedAgents = new ArrayList<>();
	/** 殺されたエージェント */
	List<Agent> killedAgents = new ArrayList<>();
	/** 発言された占い結果報告のリスト */
	List<Judge> divinationList = new ArrayList<>();
	/** 発言された霊媒結果報告のリスト */
	List<Judge> identList = new ArrayList<>();
	/** 発言用待ち行列 */
	Deque<Content> talkQueue = new LinkedList<>();
	/** 囁き用待ち行列 */
	Deque<Content> whisperQueue = new LinkedList<>();
	/** 投票先候補 */
	Agent voteCandidate;
	/** 宣言済み投票先候補 */
	Agent declaredVoteCandidate;
	/** 襲撃投票先候補 */
	Agent attackVoteCandidate;
	/** 宣言済み襲撃投票先候補 */
	Agent declaredAttackVoteCandidate;
	/** カミングアウト状況 */
	Map<Agent, Role> comingoutMap = new HashMap<>();
	/** GameInfo.talkList読み込みのヘッド */
	int talkListHead;
	/** 人間リスト */
	List<Agent> humans = new ArrayList<>();
	/** 人狼リスト */
	List<Agent> werewolves = new ArrayList<>();

	/** エージェントが生きているかどうかを返す */
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	/** エージェントが殺されたかどうかを返す */
	protected boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}

	/** エージェントがカミングアウトしたかどうかを返す */
	protected boolean isCo(Agent agent) {
		return comingoutMap.containsKey(agent);
	}

	/** 役職がカミングアウトされたかどうかを返す */
	protected boolean isCo(Role role) {
		return comingoutMap.containsValue(role);
	}

	/** エージェントが人間かどうかを返す */
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}

	/** エージェントが人狼かどうかを返す */
	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}

	/** リストからランダムに選んで返す */
	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	public String getName() {
		return "MyBasePlayer";
	}

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
	}

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
			switch (content.getTopic()) {
			case COMINGOUT:
				comingoutMap.put(talker, content.getRole());
				break;
			case DIVINED:
				divinationList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
			case IDENTIFIED:
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
			default:
				break;
			}
		}
		talkListHead = currentGameInfo.getTalkList().size();
	}

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

	/** 投票先候補を選びvoteCandidateにセットする */
	protected void chooseVoteCandidate() {
	}

	public String talk() {
		chooseVoteCandidate();
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
		return talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
	}

	/** 襲撃先候補を選びattackVoteCandidateにセットする */
	protected void chooseAttackVoteCandidate() {
	}

	public String whisper() {
		chooseAttackVoteCandidate();
		if (attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	public Agent vote() {
		canTalk = false;
		chooseVoteCandidate();
		return voteCandidate;
	}

	public Agent attack() {
		canWhisper = false;
		chooseAttackVoteCandidate();
		canWhisper = true;
		return attackVoteCandidate;
	}

	public Agent divine() {
		return null;
	}

	public Agent guard() {
		return null;
	}

	public void finish() {
	}

}
