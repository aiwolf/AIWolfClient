package org.aiwolf.client.base.smpl;

import org.aiwolf.client.base.player.RoleBasePlayer;

/**
 * Sampleのみを使って起動するPlayer
 * @author tori
 *
 */
public class SamplePlayer extends RoleBasePlayer {

	public SamplePlayer(){
		setVillagerPlayer(new SampleVillagerPlayer());
		setSeerPlayer(new SampleSeerPlayer());
		setMediumPlayer(new SampleMediumPlayer());
		setBodyGuardPlayer(new SampleBodyGuardPlayer());
		setPossesedPlayer(new SamplePossesedPlayer());
		setWerewolfPlayer(new SampleWereWolfPlayer());
	}

	@Override
	public String getName() {
		return SamplePlayer.class.getSimpleName();
	}	
}
