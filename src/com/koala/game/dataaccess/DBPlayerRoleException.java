/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.dataaccess;

/**
 * <p>创建新角色时对应的异常情况<br>
 * @author zhaizl
 */
public class DBPlayerRoleException extends KGameDataAccessException {

	/** 角色名称验证：太短 */
	public static final int ROLENAME_VALIDITY_TOOSHORT = 1001;
	/** 角色名称验证：太长 */
	public static final int ROLENAME_VALIDITY_TOOLONG = 1002;
	/** 角色名称验证：角色名称已存在 */
	public static final int ROLENAME_VALIDITY_DUPLICATED = 1003;
	/** 创建角色失败 */
	public static final int CREATE_NEW_ROLE_FAILED = 1005;

	public DBPlayerRoleException(String message, Throwable cause,
			int errorCode, String desc) {
		super(message, cause, errorCode, desc);
	}
}
