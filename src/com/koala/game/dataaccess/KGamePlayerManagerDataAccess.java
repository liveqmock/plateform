package com.koala.game.dataaccess;

import com.koala.game.dataaccess.dbobj.DBPlayer;

public interface KGamePlayerManagerDataAccess {

	/**
	 * 注册一个新的账号
	 * 
	 * @param playerName
	 *            用户名
	 * @param password
	 *            密码
	 * @param promoID
	 *            推广ID
	 * @param securityQuestionIdx
	 *            安全提问问题号
	 * @param securityAnswerIdx
	 *            安全回答号
	 * @param remark
	 *            其它备注信息
	 * @return 新分配的账号ID，如果返回-1则表示注册失败
	 * @throws KGameDBException
	 * 
	 */
	@Deprecated
	long registerNewPassport(String playerName, String password, int promoID,
			int securityQuestionIdx, int securityAnswerIdx, String remark)
			throws KGameDBException;

	/**
	 * 注册新账号
	 * 
	 * @param playerName
	 *            用户名
	 * @param password
	 *            密码
	 * @param mobileNum
	 *            手机号码
	 * @param promoID
	 *            推广Id
	 * @param securityQuestionIdx
	 *            安全提问问题号
	 * @param securityAnswer
	 *            安全回答号
	 * @param attribute
	 *            账号自定义属性
	 * @param remark
	 *            其它备注信息
	 * @return 新分配的账号ID，如果返回-1则表示注册失败
	 * @throws InvalidUserInfoException
	 *             ，如果用户名重复或者长度不符则抛出此异常
	 * @throws KGameDBException
	 */
	DBPlayer registerNewPassport(String playerName, String password,
			String mobileNum, int promoID,int parentPromoId, int securityQuestionIdx,
			String securityAnswer, String attribute, String remark)
			throws InvalidUserInfoException, KGameDBException;

	/**
	 * 根据一个渠道账号标识注册新账号
	 * 
	 * @param promoMask
	 *            渠道账号标识
	 * @param password
	 *            密码
	 * @param mobileNum
	 *            手机号码
	 * @param promoID
	 *            推广Id
	 * @param parentPromoId  
	 *            推广渠道的父渠道ID           
	 * @param securityQuestionIdx
	 *            安全提问问题号
	 * @param securityAnswer
	 *            安全回答号
	 * @param attribute
	 *            账号自定义属性
	 * @param remark
	 *            其它备注信息
	 * @return 新分配的账号ID，如果返回-1则表示注册失败
	 * @throws InvalidUserInfoException
	 *             ，如果用户名重复或者长度不符则抛出此异常
	 * @throws KGameDBException
	 */
	DBPlayer registerNewPassportByPromoMask(String promoMask, String mobileNum,
			int promoID,int parentPromoId, int securityQuestionIdx, String securityAnswer,
			String attribute, String remark) throws InvalidUserInfoException,
			KGameDBException;

	@Deprecated
	boolean login(String pname, String password)
			throws PlayerAuthenticateException, KGameDBException;

	@Deprecated
	boolean login(long pid, String password)
			throws PlayerAuthenticateException, KGameDBException;

	/**
	 * 通过渠道账号标识和密码登录
	 * 
	 * @param promoMask
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	DBPlayer loginByPromoMask(String promoMask, int promoId)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException;

	/**
	 * 通过帐号名和密码登录
	 * 
	 * @param pname
	 * @param password
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	DBPlayer loginByName(String pname, String password)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException;

	/**
	 * 通过账号ID和密码登录
	 * 
	 * @param pid
	 * @param password
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	DBPlayer loginById(long pid, String password)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException;

	/**
	 * 用户账号登出服务器（记录登录流水）
	 * 
	 * @param playerId
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void logout(long playerId) throws PlayerAuthenticateException,
			KGameDBException;

	/**
	 * <p>
	 * 检查给定的 appliedUserName 是否已在数据系统中被注册使用。如果发现给定的 appliedUserName
	 * 已被其它用户使用，则抛出：<br>
	 * new InvalidUserInfoException("...",
	 * InvalidUserInfoException.CREATE_PLAYER_UN_INVALID);<br>
	 * 如果appliedUserName长度不符合规范，则抛出：<br>
	 * new InvalidUserInfoException("...",
	 * InvalidUserInfoException.CREATE_PLAYER_UN_INVALID_LENGTH);<br>
	 * </p>
	 * <br>
	 * 
	 * @param appliedUserName
	 *            String
	 * @throws InvalidUserInfoException
	 * @throws KGameDBException
	 */
	void checkDuplicationOfUserName(String appliedUserName)
			throws InvalidUserInfoException, KGameDBException;

	/**
	 * <p>
	 * 在数据系统中回索指定用户的密码。如果指定用户不存在，则抛出<br>
	 * <br>
	 * PasswordReclaimException.CAUSE_PLAYER_NOT_FOUND<br>
	 * <br>
	 * 如果答案和问题不吻合，则抛出：<br>
	 * PasswordReclaimException.CAUSE_UNMATCHED_ANSWER<br>
	 * </p>
	 * <br>
	 * 
	 * @param userName
	 *            String 用户名
	 * @param questionId
	 *            int 指定的问题ID
	 * @param answer
	 *            String 用户输入对应问题的答案
	 * @return String, 被回索的用户密码。
	 * @throws com.PlayerAuthenticateException.gamexp.platform.server.playermanagement.PasswordReclaimException
	 * @throws KGameDBException
	 */
	String reclaimPasswordOf(String userName, int questionId, String answer)
			throws PlayerAuthenticateException, KGameDBException;

	/**
	 * 通过帐号名加载Player账号数据
	 * 
	 * @param playerName
	 * @return
	 * @throws KGameDBException
	 */
	DBPlayer loadDBPlayer(String playerName) throws KGameDBException;
	
	/**
	 * 通过帐号ID加载Player账号数据
	 * 
	 * @param playerId
	 * @return
	 * @throws KGameDBException
	 */
	DBPlayer loadDBPlayer(long playerId) throws KGameDBException;

	/**
	 * 修改账号密码
	 * 
	 * @param playerName
	 *            ，帐号名
	 * @param oldPassword
	 *            ，提交验证的账号旧密码
	 * @param newPassword
	 *            ，提交的新密码
	 * @return ，修改成功返回true
	 * @throws InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	boolean changePassword(String playerName, String oldPassword,
			String newPassword) throws InvalidUserInfoException,
			PlayerAuthenticateException, KGameDBException;

	/**
	 * 修改账号密保安全的问题与答案
	 * 
	 * @param playerId
	 * @param securityQuestionIdx
	 * @param securityAnswer
	 * @return
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	boolean changeSecurityQuestionAnswer(long playerId,
			int securityQuestionIdx, String securityAnswer)
			throws PlayerAuthenticateException, KGameDBException;

	/**
	 * <p>
	 * 检测帐号名与密码的正确性，是否匹配（FE验证账号使用）,调用该方法会有以下情况：<br>
	 * 1.当账号不存在时，会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 2.当密码与账号名不匹配时，会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_WRONG_PASSWORD}<br>
	 * </p>
	 * 
	 * @param playerName
	 *            ,帐号名
	 * @param password
	 *            ,账号密码
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void verifyPlayerPassport(String playerName, String password)
			throws PlayerAuthenticateException, KGameDBException;

	/**
	 * <pre>
	 * 根据渠道标识码检测帐号是否存在，（FE验证账号使用）,调用该方法会有以下情况：<br>
	 * 1.当账号不存在时，会抛出PlayerAuthenticateException异常，类型为{@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 2.当账号存在，则返回{@link DBPlayer}的实例对象
	 * 注意：此方法为渠道SDK包接入时专用
	 * </pre>
	 * 
	 * @param promoMask
	 *            ,推广渠道
	 * @return DBPlayer
	 * @throws KGameDBException
	 */
	DBPlayer verifyPlayerPassportByPromoMask(String promoMask, int parentPromoId)
			throws PlayerAuthenticateException, KGameDBException;

	/**
	 * 根据账号ID更新账号自定义属性，若账号不存在，则抛出会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 
	 * @param playerId
	 * @param attribute
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void updatePlayerAttributeById(long playerId,
			String attribute) throws PlayerAuthenticateException,
			KGameDBException;

	/**
	 * 根据账号名更新账号自定义属性，若账号不存在，则抛出会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 
	 * @param playerName
	 * @param attribute
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void updatePlayerAttributeByName(String playerName,
			String attribute) throws PlayerAuthenticateException,
			KGameDBException;
	
	/**
	 * 根据账号ID更新账号自定义remark，若账号不存在，则抛出会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 
	 * @param playerId
	 * @param remark
	 * @param attribute
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void updatePlayerRemarkById(long playerId, String remark) throws PlayerAuthenticateException,
			KGameDBException;
	
	/**
	 * 根据账号ID更新账号自定义remark，若账号不存在，则抛出会抛出PlayerAuthenticateException异常，类型为
	 * {@link PlayerAuthenticateException#CAUSE_PLAYER_NOT_FOUND}<br>
	 * 
	 * @param playerId
	 * @param remark
	 * @param attribute
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	void updatePlayerRemarkByName(String playerName, String remark) throws PlayerAuthenticateException,
			KGameDBException;

	/**
	 * 添加一笔充值流水记录
	 * 
	 * @param player_id
	 * @param role_id
	 * @param role_name
	 * @param role_level        充值角色等级
	 * @param is_first_charge   是否首次充值
	 * @param rmb               充值人民币金额（单位：分）
	 * @param charge_point      充值对应的游戏点数
	 * @param card_num          充值卡卡号（没有可以不填）
	 * @param card_password     充值卡密码（没有可以不填）
	 * @param charge_time       充值时间
	 * @param charge_type       充值类型（如：神州行卡、联通卡，盛大卡等，没有填0）
	 * @param promo_id          注册账号推广渠道的ID
	 * @param
	 * @param channel_id        充值通道ID（如当乐、91、360等充值通道）    
	 * @param desc              描述信息（没有可以不填）
	 */
	void addChargeReocrd(long player_id, long role_id, String role_name,
			int role_level, byte is_first_charge, float rmb, int charge_point,
			String card_num, String card_password, long charge_time,
			int charge_type, int promo_id,int parent_promo_id, int channel_id,
			String desc);

	/**
	 * 添加一笔服务器在线人数记录
	 * 
	 * @param area_id    大区ID
	 * @param server_id  游戏区ID
	 * @param online_count  在线人数
	 * @param record_time   记录时间点
	 * @param desc          描述信息（没有可以不填）
	 */
	void addServerOnlineRecord(int area_id, int server_id, int online_count,
			long record_time, int connectCount);

	/**
	 * 添加一笔赠送游戏点数记录
	 * 
	 * @param player_id
	 * @param role_id
	 * @param role_name
	 * @param present_point   赠送点数
	 * @param type            赠送点数的类型
	 * @param desc            描述信息（没有可以不填）
	 * @param present_time    赠送发生的时间点
	 * @param promo_id        注册账号推广渠道的ID
	 */
	void addPresentPointRecord(long player_id, long role_id, String role_name,
			int present_point, int type, String desc, long present_time,
			int promo_id,int parent_promo_id);
}
