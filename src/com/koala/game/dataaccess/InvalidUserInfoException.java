package com.koala.game.dataaccess;

/**
 * <p>当要注册的新用户所提供的用户信息不符合要求时抛出该异常。主要有以下几种情况：<br>
 * 1. 用户名长度不符合要求（4 - 20 个字符）或已经被使用<br>
 * 2. 密码长度不符合要求（6 - 14 个字符）或密码字段和密码确认字段不匹配。<br>
 * 3. 手机号码不符合要求（只能为数字，长度为 11 位）<br>
 * <br>
 * @author zhaizl
 */
public class InvalidUserInfoException extends KGameDataAccessException {
	
	/**
     * 代表用户名已经被使用
     */
    public static final int PLAYER_NAME_INVALID = 5001;

    /**
     * 代表用户名长度不符合要求（4 - 20 个字符）
     */
    public static final int PLAYER_NAME_INVALID_LENGTH = 5002;
    /**
     * 代表密码长度不符合要求（6 - 20 个字符）或密码字段和密码确认字段不匹配。
     */
    public static final int PLAYER_PASSWORD_INVALID_LENGTH = 5003;

    /**
     * 代表手机号码不符合要求（只能为数字，长度为 11 位）
     */
    public static final int PLAYER_MOBILE_INVALID_LENGTH = 5004;


	public InvalidUserInfoException(String message, Throwable cause,
			int errorCode, String desc) {
		super(message, cause, errorCode, desc);
	}

}
