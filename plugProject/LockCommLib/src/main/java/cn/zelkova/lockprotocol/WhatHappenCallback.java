package cn.zelkova.lockprotocol;

/**
 * 用来跟踪类库内部执行的信息，一般调试时使用
 *
 * @author zp
 */
public interface WhatHappenCallback {

	/**
	 * 当内部输出信息时被调用
	 * @param msg 要显示的信息
     */
	void letMeKnow(String msg);
}
