package org.aiwolf.client.base.player;

import org.aiwolf.client.base.smpl.SampleBodyguardPlayer;
import org.aiwolf.client.base.smpl.SampleMediumPlayer;
import org.aiwolf.client.base.smpl.SamplePossessedPlayer;
import org.aiwolf.client.base.smpl.SampleSeerPlayer;
import org.aiwolf.client.base.smpl.SampleVillagerPlayer;
import org.aiwolf.client.base.smpl.SampleWereWolfPlayer;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 各プレイヤーに使用したいプレイヤーのインスタンスを生成して下さい． 例えば，村人のエージェントだけ自作のエージェントにしたい場合は， <br>
 * Player villagerPlayer = new SampleVillagerPlayer();<br>
 * ↓ <br>
 * Player villagerPlayer = new [自作プレイヤーのクラス名のコンストラクタ];<br>
 * と変更すれば，村人の役職が割り振られた時は自作のエージェント，それ以外の役職になった時はサンプルエージェントでプレイします．
 *
 * @author tori
 *
 */
abstract public class AbstractRoleAssignPlayer implements Player {

	private AbstractPlayer villagerPlayer = new SampleVillagerPlayer();
	private AbstractPlayer seerPlayer = new SampleSeerPlayer();
	private AbstractPlayer mediumPlayer = new SampleMediumPlayer();
	private AbstractPlayer bodyguardPlayer = new SampleBodyguardPlayer();
	private AbstractPlayer possessedPlayer = new SamplePossessedPlayer();
	private AbstractPlayer werewolfPlayer = new SampleWereWolfPlayer();

	private AbstractPlayer rolePlayer;

	/**
	 * @return villagerPlayer
	 */
	final public AbstractPlayer getVillagerPlayer() {
		return villagerPlayer;
	}

	/**
	 * @param villagerPlayer セットする villagerPlayer
	 */
	final public void setVillagerPlayer(AbstractPlayer villagerPlayer) {
		this.villagerPlayer = villagerPlayer;
	}

	/**
	 * @return seerPlayer
	 */
	final public AbstractPlayer getSeerPlayer() {
		return seerPlayer;
	}

	/**
	 * @param seerPlayer セットする seerPlayer
	 */
	final public void setSeerPlayer(AbstractPlayer seerPlayer) {
		this.seerPlayer = seerPlayer;
	}

	/**
	 * @return mediumPlayer
	 */
	final public AbstractPlayer getMediumPlayer() {
		return mediumPlayer;
	}

	/**
	 * @param mediumPlayer セットする mediumPlayer
	 */
	final public void setMediumPlayer(AbstractPlayer mediumPlayer) {
		this.mediumPlayer = mediumPlayer;
	}

	/**
	 * @return bodyGuardPlayer
	 */
	final public AbstractPlayer getBodyguardPlayer() {
		return bodyguardPlayer;
	}

	/**
	 * @param bodyGuardPlayer セットする bodyGuardPlayer
	 */
	final public void setBodyguardPlayer(AbstractPlayer bodyGuardPlayer) {
		this.bodyguardPlayer = bodyGuardPlayer;
	}

	/**
	 * @return possesedPlayer
	 */
	final public AbstractPlayer getPossessedPlayer() {
		return possessedPlayer;
	}

	/**
	 * @param possesedPlayer セットする possesedPlayer
	 */
	final public void setPossessedPlayer(AbstractPlayer possesedPlayer) {
		this.possessedPlayer = possesedPlayer;
	}

	/**
	 * @return werewolfPlayer
	 */
	final public AbstractPlayer getWerewolfPlayer() {
		return werewolfPlayer;
	}

	/**
	 * @param werewolfPlayer セットする werewolfPlayer
	 */
	final public void setWerewolfPlayer(AbstractPlayer werewolfPlayer) {
		this.werewolfPlayer = werewolfPlayer;
	}

	@Override
	abstract public String getName();

	@Override
	final public void update(GameInfo gameInfo) {
		rolePlayer.update(gameInfo);
	}

	@Override
	final public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		Role myRole = gameInfo.getRole();
		switch (myRole) {
		case VILLAGER:
			rolePlayer = villagerPlayer;
			break;
		case SEER:
			rolePlayer = seerPlayer;
			break;
		case MEDIUM:
			rolePlayer = mediumPlayer;
			break;
		case BODYGUARD:
			rolePlayer = bodyguardPlayer;
			break;
		case POSSESSED:
			rolePlayer = possessedPlayer;
			break;
		case WEREWOLF:
			rolePlayer = werewolfPlayer;
			break;
		default:
			rolePlayer = villagerPlayer;
			break;

		}
		rolePlayer.initialize(gameInfo, gameSetting);

	}

	@Override
	final public void dayStart() {
		rolePlayer.dayStart();
	}

	@Override
	final public String talk() {
		return rolePlayer.talk();
	}

	@Override
	final public String whisper() {
		return rolePlayer.whisper();
	}

	@Override
	final public Agent vote() {
		return rolePlayer.vote();
	}

	@Override
	final public Agent attack() {
		return rolePlayer.attack();
	}

	@Override
	final public Agent divine() {
		return rolePlayer.divine();
	}

	@Override
	final public Agent guard() {
		return rolePlayer.guard();
	}

	@Override
	final public void finish() {
		rolePlayer.finish();
	}

}
