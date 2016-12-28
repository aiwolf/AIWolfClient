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
 * 
 * @deprecated
 */
public abstract class AbstractPossessed extends AbstractRole{


	@Override
	public abstract String talk();

	@Override
	public final String whisper(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract Agent vote();

	@Override
	public final Agent attack(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent divine(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent guard(){
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public abstract void finish();

	/**
	 * <div lang="ja">このクラスの新しいインスタンスを初期化する．</div>
	 *
	 * <div lang="en">Initializes a new instance of this class.</div>
	 */
	public AbstractPossessed(){
		myRole = Role.POSSESSED;
	}

}
