package org.aiwolf.kajiClient.player;

import org.aiwolf.client.base.player.AbstractRoleAssignPlayer;

import org.aiwolf.client.base.smpl.*;
import org.aiwolf.common.net.GameInfo;


public class KajiRoleAssignPlayer extends AbstractRoleAssignPlayer{

	public KajiRoleAssignPlayer(){
		setVillagerPlayer(new KajiVillager());
		setBodyguardPlayer(new KajiBodygurad());
		setMediumPlayer(new KajiMedium());
		setPossessedPlayer(new KajiPossessed());
		setSeerPlayer(new KajiSeer());
		setWerewolfPlayer(new KajiWerewolf());
	}

	@Override
	public String getName() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
