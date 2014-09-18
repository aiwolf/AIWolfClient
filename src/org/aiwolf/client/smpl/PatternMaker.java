package org.aiwolf.client.smpl;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;

public class PatternMaker {
	/**
	 * COの発言を元にパターンを作成，更新する．
	 * @param patternList
	 * @param coAgent
	 * @param coRole
	 * @param gameInfo
	 * @return
	 */
	//必要データ：元のパターンリスト，COしたエージェントと役職，死人リスト，
	public static void extendPatternList(List<Pattern> patterns, Agent coAgent, Role coRole, AdvanceGameInfo advanceGameInfo){
		List<Pattern> newPatterns = new ArrayList<Pattern>();

		/**
		 * todo
		 */

		return;
	}

	//襲撃によって死んだプレイヤーを白確，真能力者によって白確か黒確
	/**
	 * 襲撃されたプレイヤーを白確にする
	 * @param patternList
	 * @param attackedAgent
	 */
	public static void updateAttackedData(List<Pattern> patterns, Agent attackedAgent){

		/**
		 * todo
		 */

	}

	/**
	 * 占い，霊能によって得られた情報を付加する
	 * @param patterns
	 * @param judge
	 */
	public static void updateJudgeData(List<Pattern> patterns, Judge judge){

		/**
		 * todo
		 */

	}
}
