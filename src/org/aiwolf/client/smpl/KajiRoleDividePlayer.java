package org.aiwolf.client.smpl;

import org.aiwolf.client.base.player.RoleBasePlayer;
import org.aiwolf.common.net.GameInfo;


public class KajiRoleDividePlayer extends RoleBasePlayer{

	public KajiRoleDividePlayer(){
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
