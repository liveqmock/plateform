/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess;

/**
 * 
 * @author Administrator
 */
public class KGameDataAccessException extends Throwable {	

	private int errorCode;
	private String desc;

	public KGameDataAccessException(String message, Throwable cause,
			int errorCode, String desc) {
		super(message, cause);
		this.errorCode = errorCode;
		this.desc = desc;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getDesc() {
		return desc;
	}
}
