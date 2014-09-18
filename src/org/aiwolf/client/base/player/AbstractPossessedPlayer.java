package org.aiwolf.client.base.player;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

public abstract class AbstractPossessedPlayer extends AbstractPlayer{


	@Override
	public abstract String talk();

	@Override
	final public String whisper(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract Agent vote();

	@Override
	final public Agent attack(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	final public Agent divine(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	final public Agent guard(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract void finish();

	public AbstractPossessedPlayer(){
		myRole = Role.possessed;
	}



}
