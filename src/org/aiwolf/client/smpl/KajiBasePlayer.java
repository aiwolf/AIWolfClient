package org.aiwolf.client.smpl;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.base.player.AbstractPlayer;
import org.aiwolf.client.lib.Passage;
import org.aiwolf.client.lib.Protocol;
import org.aiwolf.client.lib.TemplateTalkFactory;
import org.aiwolf.client.lib.Utterance;
import org.aiwolf.client.lib.Verb;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.server.GameData;

/**
 * 全役職共通部分のアルゴリズム
 * initialize：初期パターン作成
 * update：発話ログからAGI更新，Pattern更新
 * dayStart：AGIの死亡プレイヤーを更新
 * @author kengo
 *
 */
public abstract class KajiBasePlayer extends AbstractPlayer {

	//CO,能力の結果などのデータ集合
	AdvanceGameInfo advanceGameInfo = new AdvanceGameInfo();

	//有りうる役職振り分けのパターン集合
	List<Pattern> patterns = new ArrayList<Pattern>();

	//トークをどこまで読んだか
	int readTalkNumber = 0;

	//今日投票するプレイヤー(暫定)
	Agent voteTarget = null;

	//最新の発話で言った投票先プレイヤー
	Agent toldVoteTarget = null;


	@Override
	public void initialize(GameInfo gameInfo){
		super.initialize(gameInfo);

		//初期パターンの作成
		Pattern initialPattern = new Pattern(null, null, null);
		patterns.add(initialPattern);
	}

	@Override
	public void update(GameInfo gameInfo){
		super.update(gameInfo);

		//発話ログの処理
		List<Talk> talks = gameInfo.getTalkList();
		for(; readTalkNumber < talks.size(); readTalkNumber++){
			Talk talk = talks.get(readTalkNumber);
			Protocol protocol = new Protocol(talk.getContent());
			for(Utterance u: protocol.getUtterances()){
				Passage p = u.getPassage();
				switch (p.getVerb()) {

				//COの発話処理
				case comingout:
					advanceGameInfo.putComingoutMap(talk.getAgent(), p.getObject());
					PatternMaker.extendPatternList(patterns, talk.getAgent(), p.getObject(), advanceGameInfo);
					break;


				//占い結果の発話処理
				case inspected:
					Judge inspectJudge = new Judge(getDay(), talk.getAgent(), p.getSubject(), p.getAttribution());
					advanceGameInfo.addInspectJudges(inspectJudge);
					PatternMaker.updateJudgeData(patterns, inspectJudge);
					break;


				//霊能結果の発話処理
				case medium_telled:
					Judge tellingJudge = new Judge(getDay(), talk.getAgent(), p.getSubject(), p.getAttribution());
					advanceGameInfo.addMediumJudges(tellingJudge);
					PatternMaker.updateJudgeData(patterns, tellingJudge);
					break;

				//上記以外
				default:

					break;
				}
			}
		}

		//投票先を更新(更新する条件などはサブクラスで記載)
		voteTarget = getVoteTarget();

	}

	@Override
	public void dayStart() {
		readTalkNumber = 0;

		//死亡したプレイヤーをAGIに記録
		Agent attackedAgent = getLatestDayGameInfo().getAttackedAgent();
		if(attackedAgent != null){
			DeadCondition attackedAgentCondition = new DeadCondition(attackedAgent, getDay(), CauseOfDeath.attacked);
			advanceGameInfo.addDeadConditions(attackedAgentCondition);
		}

		Agent executedAgent = getLatestDayGameInfo().getExecutedAgent();
		if(executedAgent != null){
			DeadCondition executeddAgentCondition = new DeadCondition(executedAgent, getDay(), CauseOfDeath.executed);
			advanceGameInfo.addDeadConditions(executeddAgentCondition);
		}

		//今日の暫定投票先
		toldVoteTarget = null;
		voteTarget = getVoteTarget();

	}

	@Override
	public String talk() {

		List<Utterance> talkContents = new ArrayList<Utterance>();

		//まだ暫定投票先を発話していない場合
		if(toldVoteTarget != voteTarget && voteTarget != null){
			talkContents.add(TemplateTalkFactory.estimate(voteTarget, Role.WEREWOLF));
			toldVoteTarget = voteTarget;
		}

		//占い，霊能結果の発話
		Utterance judgeReport = getJudgeUtterance();
		if(judgeReport != null){
			talkContents.add(judgeReport);
		}

		//カミングアウトの発話
		Utterance comingoutReport = getComingoutUtterance();
		if(comingoutReport != null){
			talkContents.add(comingoutReport);
		}



		//話すことがあった場合
		if(talkContents.size() != 0){
			Protocol protocol = new Protocol(talkContents);
			return protocol.getText();
		}
		//話すことが無かった場合
		else{
			return TemplateTalkFactory.over().getText();
		}
	}

	/**
	 * 占い or 霊能結果の発話を行う．結果の報告をしない場合はnullを返す
	 * @return
	 */
	public abstract Utterance getJudgeUtterance();

	/**
	 * カミングアウトの発話を行う．COしない場合はnullを返す
	 * @return
	 */
	public abstract Utterance getComingoutUtterance();

	@Override
	public String whisper() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent attack() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent divine() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent guard() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent vote() {
		// TODO 自動生成されたメソッド・スタブ
		//人間側のVoteを書いちゃう？
		return null;
	}

	@Override
	public void finish() {
		// TODO 自動生成されたメソッド・スタブ

	}

	public abstract Agent getVoteTarget();

}
