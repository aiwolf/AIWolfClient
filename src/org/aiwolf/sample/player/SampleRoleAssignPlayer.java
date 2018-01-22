package org.aiwolf.sample.player;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;
import org.aiwolf.sample.player.*;

/**
 * 役職に実際のプレイヤークラスを割り当てるプレイヤークラス
 */
public class SampleRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public SampleRoleAssignPlayer() {
		setVillagerPlayer(new SampleVillager());
		setBodyguardPlayer(new SampleBodyguard());
		setMediumPlayer(new SampleMedium());
		setSeerPlayer(new SampleSeer());
		setPossessedPlayer(new SamplePossessed());
		setWerewolfPlayer(new SampleWerewolf());
	}

	public String getName() {
		return "SampleRoleAssignPlayer";
	}

}
