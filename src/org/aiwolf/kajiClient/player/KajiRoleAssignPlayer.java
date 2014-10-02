package org.aiwolf.kajiClient.player;

import org.aiwolf.client.base.player.AbstarctRoleAssignPlayer;
import org.aiwolf.client.base.smpl.*;
import org.aiwolf.common.net.GameInfo;


public class KajiRoleAssignPlayer extends AbstarctRoleAssignPlayer{

	public KajiRoleAssignPlayer(){
		setVillagerPlayer(new KajiVillagerPlayer());
		setBodyGuardPlayer(new KajiBodyGuradPlayer());
		setMediumPlayer(new KajiMediumPlayer());
		setPossesedPlayer(new KajiPossessedPlayer());
		setSeerPlayer(new KajiSeerPlayer());
		setWerewolfPlayer(new KajiWereWolfPlayer());
	}

	@Override
	public String getName() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
