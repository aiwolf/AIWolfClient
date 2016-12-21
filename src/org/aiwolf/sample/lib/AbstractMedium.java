/**
 * AbstractMedium.java
 * 
 * Copyright (c) 2016 人狼知能プロジェクト
 */
package org.aiwolf.sample.lib;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;

/**
 * <div lang= "ja">霊媒師用抽象クラス。 呼ばれるはずのないメソッドが呼ばれると例外を投げる</div>
 * 
 * <div lang="en">Abstract class for medium. When the invalid method is called, it throws an exception.</div>
 */
public abstract class AbstractMedium implements Player {

	@Override
	public final String whisper() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent attack() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent divine() {
		throw new UnsuspectedMethodCallException();
	}

	@Override
	public final Agent guard() {
		throw new UnsuspectedMethodCallException();
	}

}
