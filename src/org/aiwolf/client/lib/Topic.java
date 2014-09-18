package org.aiwolf.client.lib;

/**
 * 文章の動詞，補語を表す．
 * @author kengo
 *
 */
public enum Topic {

	/**
	 * AgentはRoleだと思う
	 * ESTIMATE Agent Role
	 */
	ESTIMATE,

	/**
	 * AgentがRoleをカミングアウトする
	 * COMINGOUT Agent Role
	 */
	COMINGOUT,

	/**
	 * AgentがSpeciesだと占われる
	 * DIVINED Agent Species
	 */
	DIVINED,

	/**
	 * AgentがSpeciesだと霊能される
	 * INQUESTED Agent Species
	 */
	INQUESTED,

	/**
	 * Agentが護衛される
	 * GUARED Agent
	 */
	GUARDED,

	/**
	 * Agentに投票する
	 * VOTE Agent
	 */
	VOTE,

	/**
	 * Agentを襲撃する
	 * ATTACK Agent
	 */
	ATTACK,

	/**
	 * 発話[Day][Number]に同意する
	 * AGREE Day Number
	 */
	AGREE,

	/**
	 * 発話[Day][Number]に同意しない
	 * DISAGREE Day Number
	 */
	DISAGREE,

	/**
	 * もう発話することが無い場合
	 * OVER
	 */
	OVER,

	/**
	 * まだ発話したいことがある場合
	 * SKIP
	 */
	SKIP;

	/**
	 * 引数のStringがTopicに存在するものならTopicを返す．
	 * @param string
	 * @return
	 */
	public static  Topic getTopic(String string){
		for(Topic topic: Topic.values()){
			if(topic.toString().equals(string)){
				return topic;
			}
		}
		return null;
	}

}
