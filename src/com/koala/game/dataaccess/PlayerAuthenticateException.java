package com.koala.game.dataaccess;

/**
 * <p>处理用户登录、修改密码、取回密码时的异常情况。</p>
 * <br>
 * @author zhaizl
 */
public class PlayerAuthenticateException extends KGameDataAccessException {
	/**
	 * 指定用户不存在
	 */
	public static final int CAUSE_PLAYER_NOT_FOUND = 5007;
	/**
	 * 用户通过安全问题取回密码时，问题的真实对应答案与用户输入的答案不相符
	 */
	public static final int CAUSE_UNMATCHED_ANSWER = 5008;
	
	/**
	 * 用户登录或修改密码时，输入的密码与数据库中的密码不符
	 */
	public static final int CAUSE_WRONG_PASSWORD = 5009;

	public PlayerAuthenticateException(String message, Throwable cause,
			int errorCode, String desc) {
		super(message, cause, errorCode, desc);
	}

}
