package org.aiwolf.client.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aiwolf.client.lib.TemplateTalkFactory.TalkType;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

/*
 * 発話をパースしたもの
 */
public class Utterance {
	//原文
	String text = null;

	//文章のトピック
	Topic topic = null;

	//文章上の対象プレイヤー
	Agent target = null;

	//カミングアウト結果や占い結果等
	State state = null;

	//TopicがAGREE,DISAGREEの時の対象発話のログの種類（囁きかどうか）
	@Deprecated
	TalkType talkType = null;

	//TopicがAGREE,DISAGREEの時の対象発話の日にち
	int talkDay = -1;

	//TopicがAGREE,DISAGREEの時の対象発話のID
	int talkID = -1;


	/**
	 * 発話全体のStringを返す
	 * @return
	 */
	public String getText(){
		return text;
	}

	/**
	 * 発話のトピックを返す
	 * @return
	 */
	public Topic getTopic(){
		return topic;
	}

	/**
	 * 発話の対象を返す．対象のない発話の場合はnull
	 * @return
	 */
	public Agent getTarget(){
		if(target != null){
			return target;
		}else{
			return null;
		}
	}

	/**
	 * TopicがESTIMATE,COMINGOUTの場合，そのRoleを返す．それ以外はnull
	 * @return
	 */
	public Role getRole(){
		if(state != null){
			return state.toRole();
		}else{
			return null;
		}
	}

	/**
	 * TopicがDIVINED,INQUESTEDの場合，そのRoleを返す．それ以外はnull
	 * @return
	 */
	public Species getResult(){
		if(state != null){
			return state.toSpecies();
		}else{
			return null;
		}
	}

	public TalkType getTalkType(){
		return talkType;
	}

	/**
	 * TopicがAGREE,DISAGREEの時，対象発話の発話日を返す．それ以外は-1
	 * @return
	 */
	public int getTalkDay(){
		return talkDay;
	}

	/**
	 * TopicがAGREE,DISAGREEの時，対象発話の発話IDを返す．それ以外は-1
	 * 発話日と発話IDでTalkとの一意性が取れる
	 * @return
	 */
	public int getTalkID(){
		return talkID;
	}

	/**
	 *
	 * @param input
	 */
	public Utterance(String input){
		text = input;

		//原文を単語に分割
		String[] split = input.split("\\s+");// 一つ以上の空白で区切る

		//Topicの取得．Topicに存在しない者ならばnullが入る
		topic = Topic.getTopic(split[0]);

		int agentId = -1;

		if(split.length >= 2 && split[1].startsWith("Agent")){
//			int startNum = split[1].indexOf("[") + 1;
//			int endNum = split[1].indexOf("]");
//			agentId = Integer.parseInt(split[1].substring(startNum, endNum));
			agentId = getInt(split[1]);
		}

		switch (topic) {
			//話すこと無し
		case SKIP:
		case OVER:
			break;

			//同意系
		case AGREE:
		case DISAGREE:
			//Talk day4 ID38 みたいな形でくるので数字だけ取得
			talkType = TalkType.parseTalkType(split[1]);
//			talkDay = Integer.parseInt(split[2].substring(3));
//			talkID = Integer.parseInt(split[3].substring(3));
			talkDay = getInt(split[2]);
			talkID = getInt(split[3]);
			break;

			//"Topic Agent Role"
		case ESTIMATE:
		case COMINGOUT:
			target = Agent.getAgent(agentId);
			state = State.parseState(split[2]);
			break;

			//RESULT系
		case DIVINED:
		case INQUESTED:
			state = State.parseState(split[2]);
		case GUARDED:
			target = Agent.getAgent(agentId);
			break;

			//投票系
		case ATTACK:
		case VOTE:
			target = Agent.getAgent(agentId);
			break;

		default:
			return;
		}


		return;
	}

	static final private Pattern intPattern = Pattern.compile("-?[\\d]+");

	protected int getInt(String text){
		Matcher m = intPattern.matcher(text);
		if(m.find()){
			return Integer.parseInt(m.group());
		}
		return -1;
	}
}
