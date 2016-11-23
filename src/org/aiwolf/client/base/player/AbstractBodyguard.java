/**
 * AbstractBodyguard.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.player;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

/**
 * <div lang="ja">狩人用抽象クラス</div>
 * 
 * <div lang="en">Abstract class for bodyguard</div>
 */
public abstract class AbstractBodyguard extends AbstractRole{

	@Override
	public abstract void dayStart();

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
	public abstract Agent guard();

	@Override
	public abstract void finish();

	public AbstractBodyguard(){
		myRole = Role.BODYGUARD;
	}

}
