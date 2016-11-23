/**
 * AbstractPossessed.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.client.base.player;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

/**
 * <div lang="ja">裏切り者用抽象クラス</div>
 *
 * <div lang="en">Abstract class for possessed</div>
 */
public abstract class AbstractPossessed extends AbstractRole{


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

	public AbstractPossessed(){
		myRole = Role.POSSESSED;
	}

}
