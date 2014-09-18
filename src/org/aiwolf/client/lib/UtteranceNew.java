package org.aiwolf.client.lib;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;

public class UtteranceNew {
	//原文
	String text = null;

	//文章のトピック
	Topic topic = null;

	//文章上の対象プレイヤー
	Agent target = null;

	//カミングアウト結果や占い結果等
	State state = null;

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
		return target;
	}

	/**
	 * TopicがESTIMATE,COMINGOUTの場合，そのRoleを返す．それ以外はnull
	 * @return
	 */
	public Role getRole(){
		return state.toRole();
	}

	/**
	 * TopicがDIVINED,INQUESTEDの場合，そのRoleを返す．それ以外はnull
	 * @return
	 */
	public Species getResult(){
		return state.toSpecies();
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
	public UtteranceNew(String input){
		text = input;

		//原文を単語に分割
		String[] split = input.split("\\s+");// 一つ以上の空白で区切る

		//Topicの取得．Topicに存在しない者ならばnullが入る
		topic = Topic.getTopic(split[0]);

		switch (topic) {
			//話すこと無し
		case SKIP:
		case OVER:
			break;

			//同意系
		case AGREE:
		case DISAGREE:
			talkDay = Integer.parseInt(split[1]);
			talkID = Integer.parseInt(split[2]);
			break;

			//"Topic Agent Role"
		case ESTIMATE:
		case COMINGOUT:
			target = Agent.getAgent(Integer.parseInt(split[1]));
			state = State.fromString(split[2]);
			break;

			//RESULT系
		case DIVINED:
		case INQUESTED:
			state = State.fromString(split[2]);
		case GUARDED:
			target = Agent.getAgent(Integer.parseInt(split[1]));
			break;

			//投票系
		case ATTACK:
		case VOTE:
			target = Agent.getAgent(Integer.parseInt(split[1]));
			break;

		default:
			break;
		}


	}


}
