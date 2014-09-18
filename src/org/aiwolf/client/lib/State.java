package org.aiwolf.client.lib;


import org.aiwolf.common.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;

public enum State {
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
		case bodyguard:
			return bodyguard;
		case freemason:
			return freemason;
		case medium:
			return medium;
		case possessed:
			return possessed;
		case seer:
			return seer;
		case villager:
			return villager;
		case werewolf:
			return werewolf;
		default:
			return null;
		}
	}

	public Role toRole(){
		switch (this) {
		case bodyguard:
			return Role.bodyguard;
		case freemason:
			return Role.freemason;
		case medium:
			return Role.medium;
		case possessed:
			return Role.possessed;
		case seer:
			return Role.seer;
		case villager:
			return Role.villager;
		case werewolf:
			return Role.werewolf;
		default:
			return null;
		}
	}

	public static State fromSpecies(Species species){
		switch (species) {
		case Human:
			return HUMAN;
		case Werewolf:
			return werewolf;
		default:
			return null;
		}
	}

	public Species toSpecies(){
		switch (this) {
		case HUMAN:
			return Species.Human;
		case werewolf:
			return Species.Werewolf;
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
