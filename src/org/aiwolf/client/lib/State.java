package org.aiwolf.client.lib;


import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

/**
 * 役職，陣営等を表すenum．内部の処理で用いる
 * @author kengo
 *
 */
enum State {
	/**
	 * bodyguard
	 */
	bodyguard(EnumType.ROLE),
	/**
	 * freemason
	 */
	freemason(EnumType.ROLE),
	/**
	 * medium
	 */
	medium(EnumType.ROLE),
	/**
	 * Possessed
	 */
	possessed(EnumType.ROLE),
	/**
	 * Seer
	 */
	seer(EnumType.ROLE),

	/**
	 * Villager
	 */
	villager(EnumType.ROLE),

	/**
	 * WereWolf
	 */
	werewolf(EnumType.ROLE),

	villagerSide(EnumType.TEAM),

	werewolfSide(EnumType.TEAM),

	HUMAN(EnumType.SPECIES),

	//Wolf(EnumType.SPECIES),

	GIFTED(EnumType.GIFTED);

	private EnumType enumType;

	private State(EnumType input){
		enumType = input;
	}

	public EnumType getEnumType(){
		return enumType;
	}

	public static State fromRole(Role role){
		switch (role) {
		case BODYGUARD:
			return bodyguard;
		case FREEMASON:
			return freemason;
		case MEDIUM:
			return medium;
		case POSSESSED:
			return possessed;
		case SEER:
			return seer;
		case VILLAGER:
			return villager;
		case WEREWOLF:
			return werewolf;
		default:
			return null;
		}
	}

	public Role toRole(){
		switch (this) {
		case bodyguard:
			return Role.BODYGUARD;
		case freemason:
			return Role.FREEMASON;
		case medium:
			return Role.MEDIUM;
		case possessed:
			return Role.POSSESSED;
		case seer:
			return Role.SEER;
		case villager:
			return Role.VILLAGER;
		case werewolf:
			return Role.WEREWOLF;
		default:
			return null;
		}
	}

	public static State fromSpecies(Species species){
		switch (species) {
		case HUMAN:
			return HUMAN;
		case WEREWOLF:
			return werewolf;
		default:
			return null;
		}
	}

	public Species toSpecies(){
		switch (this) {
		case HUMAN:
			return Species.HUMAN;
		case werewolf:
			return Species.WEREWOLF;
		default:
			return null;
		}
	}

	public static State fromString(String input){
		for(State noun: State.values()){
			if(noun.name().equals(input)){
				return noun;
			}
		}
		return null;
	}

}
