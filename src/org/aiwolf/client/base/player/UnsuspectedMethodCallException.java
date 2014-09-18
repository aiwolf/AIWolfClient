/**
 * 
 */
package org.aiwolf.client.base.player;

import org.aiwolf.common.AIWolfRuntimeException;

/**
 * 各種エージェントにおいて呼ばれるべきではないメソッドが呼ばれたときに投げられるException
 * @author tori
 *
 */
public class UnsuspectedMethodCallException extends AIWolfRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public UnsuspectedMethodCallException() {
	}

	/**
	 * @param arg0
	 */
	public UnsuspectedMethodCallException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public UnsuspectedMethodCallException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public UnsuspectedMethodCallException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public UnsuspectedMethodCallException(String arg0, Throwable arg1,
			boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
