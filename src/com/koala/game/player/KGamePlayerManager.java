package com.koala.game.player;

import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelFutureListener;

import com.koala.game.KGame;
import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameChannelAttachment;
import com.koala.game.dataaccess.InvalidUserInfoException;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.logging.KGameLogger;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.PromoSupport;

/**
 * 玩家管理器，包括缓存、获取等功能<br>
 * <br>
 * 注册/登陆验证/注销/活跃玩家...
 * 
 * @author AHONG
 * 
 */
public final class KGamePlayerManager {

	public final static int PROMOID_DEFAULT = 0;
	public final static int PARENT_PROMOID_DEFAULT = 0;
	public final static int SECURITYQUESTION_INDEX_NONE = -1;// securityQuestionIdx
	public final static String SECURITYANSWER_NONE = "";
	public final static String MOBILENUM_NONE = "12345678901";
	public final static String ATTRIBUTE_NONE = "";

	private static KGameLogger logger = KGameLogger.getLogger(KGamePlayerManager.class);
	private final ConcurrentHashMap<Long, KGamePlayerSession> playersessionsKeyID = new ConcurrentHashMap<Long, KGamePlayerSession>(4000, 1.0f);
	private final ConcurrentHashMap<String, Long> playerNameToPlayerId = new ConcurrentHashMap<String, Long>(4000, 1.0f); // 2014-08-14 新加 key=playerName, value=playerId

	// private KGamePlayerManagerDataAccess dataaccess;

	public KGamePlayerManager() {
		// dataaccess = new XmlDbAccess();
	}

	public KGamePlayerSession getPlayerSession(Long playerID) {
		return playersessionsKeyID.get(playerID);
	}
	
	public KGamePlayerSession getPlayerSession(String playerName) {
		Long playerId = this.playerNameToPlayerId.get(playerName);
		if (playerId != null) {
			return playersessionsKeyID.get(playerId);
		} else {
			return null;
		}
	}

	public KGamePlayerSession putPlayerSession(KGamePlayerSession playersession) {
		if (playersession != null && playersession.getBoundPlayer() != null) {
			// TODO 要检测是否有不同实例被覆盖！！！
			playerNameToPlayerId.put(playersession.getBoundPlayer().getPlayerName(), playersession.getBoundPlayer().getID()); // 2014-08-14 12:46新加
			return playersessionsKeyID.put(playersession.getBoundPlayer().getID(), playersession);
		}
		return null;
	}

	public KGamePlayerSession putPlayerSessionIfAbsent(
			KGamePlayerSession playersession) {
		if (playersession != null && playersession.getBoundPlayer() != null) {
			// TODO 要检测是否有不同实例被覆盖！！！
			
			KGamePlayerSession putSession = playersessionsKeyID.putIfAbsent(playersession.getBoundPlayer().getID(), playersession);
			if (putSession != null) {
				playerNameToPlayerId.putIfAbsent(playersession.getBoundPlayer().getPlayerName(), playersession.getBoundPlayer().getID()); // 2014-08-14 12:46新加
			}
		}
		return null;
	}

	/**
	 * 从缓存中移除玩家会话<br>
	 * <br>
	 * This is equivalent to
	 * 
	 * <pre>
	 * if (map.containsKey(key) &amp;&amp; map.get(key).equals(value)) {
	 * 	map.remove(key);
	 * 	return true;
	 * } else
	 * 	return false;
	 * </pre>
	 * 
	 * @param playerID
	 *            玩家ID
	 * @param playersession
	 *            希望移除的PS实例
	 * @return 如果缓存中的PS跟参数的PS不是同一个对象的话返回false
	 */
	public boolean removePlayerSession(long playerID,
			KGamePlayerSession playersession) {
//		return playersessionsKeyID.remove(playerID, playersession);
		boolean result = playersessionsKeyID.remove(playerID, playersession);
		if (result) {
			playerNameToPlayerId.remove(playersession.getBoundPlayer().getPlayerName());
		}
		return result;
	}

	public int getCachedPlayerSessionSize() {
		return playersessionsKeyID.size();
	}

//	public/* Map<Long, ChannelFuture> */void closeAllSessions(KGameMessage broadcastmsg) {
	public void closeAllSessions() { // 取消系统广播消息，停机消息直接带字符串
		// Map<Long, ChannelFuture> futures = new LinkedHashMap<Long,
		// ChannelFuture>(
		// playersessionsKeyID.size());
		// 2014-08-22 添加 停服通知消息
		String shutdownnotice = KGameTips.get("ShutdownNotice");
		KGameMessage msg = KGame.newMessage(KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_ANDROID, KGameProtocol.MID_SEND_SHUTDOWN_TO_CLIENT);
		msg.writeUtf8String(shutdownnotice != null ? shutdownnotice : "");
		for (Long id : playersessionsKeyID.keySet()) {
			KGamePlayerSession ps = playersessionsKeyID.get(id);
			if (ps != null) {
				KGameChannelAttachment attachment = (KGameChannelAttachment) ps
						.getChannel().getAttachment();
				if (attachment == null) {
					attachment = new KGameChannelAttachment(ps.getBoundPlayer()
							.getID());
					ps.getChannel().setAttachment(attachment);
				}
				attachment
						.setDisconnectedCause(KGameProtocol.CAUSE_PLAYEROUT_SERVERSHUTDOWN);
				// 发送消息并关闭
//				if (broadcastmsg != null) {
//					ps.getChannel().write(broadcastmsg.duplicate())
//							.addListener(ChannelFutureListener.CLOSE);
//				} else {
//					ps.close().awaitUninterruptibly();
//				}
				ps.getChannel().write(msg.duplicate()).addListener(ChannelFutureListener.CLOSE); // 2014-08-22 添加 停服通知消息

				// futures.put(id, ps.close().awaitUninterruptibly());
			}
		}
		// return futures;
	}

	public int broadcast(KGameMessage msg) {
		int c = 0;
		for (KGamePlayerSession ps : playersessionsKeyID.values()) {
			ps.send(msg.duplicate());
			c++;
		}
		return c;
	}

	/* ---------------------注册新账号----------------------- */
	/**
	 * 
	 * @param playerName
	 * @param password
	 * @param remark
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws KGameDBException
	 * @throws Exception
	 */
	public DBPlayer registerNewPassport(String playerName, String password,
			int promoID, String promoMask, String remark)
			throws InvalidUserInfoException, KGameDBException, Exception {
		return registerNewPassport(playerName, password, MOBILENUM_NONE,
				promoID, PromoSupport.computeParentPromoID(promoID), promoMask,
				SECURITYQUESTION_INDEX_NONE, SECURITYANSWER_NONE,
				ATTRIBUTE_NONE, remark);
	}

	/**
	 * 
	 * @param promoID
	 * @param parentPromoId
	 * @param promoMask
	 * @param remark
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws KGameDBException
	 * @throws Exception
	 */
	public DBPlayer registerNewPassportByPromoMask(int promoID,int parentPromoId,
			String promoMask, String remark) throws InvalidUserInfoException,
			KGameDBException, Exception {
		return registerNewPassport(null, null, MOBILENUM_NONE, promoID,parentPromoId,
				promoMask, SECURITYQUESTION_INDEX_NONE, SECURITYANSWER_NONE,
				ATTRIBUTE_NONE, remark);
	}

	/**
	 * 
	 * @param playerName
	 * @param password
	 * @param mobileNum
	 * @param promoID
	 * @param promoMask
	 * @param securityQuestionIdx
	 * @param securityAnswer
	 * @param attribute
	 * @param remark
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws KGameDBException
	 * @throws Exception
	 */
	public DBPlayer registerNewPassport(String playerName, String password,
			String mobileNum, int promoID,int parentPromoId, String promoMask,
			int securityQuestionIdx, String securityAnswer, String attribute,
			String remark) throws InvalidUserInfoException, KGameDBException,
			Exception {
		DBPlayer player = null;
		if (playerName == null || playerName.length() <= 0) {
			player = KGameDataAccessFactory
					.getInstance()
					.getPlayerManagerDataAccess()
					.registerNewPassportByPromoMask(promoMask, mobileNum,
							promoID,parentPromoId, securityQuestionIdx, securityAnswer,
							attribute, remark);
		} else {
			player = KGameDataAccessFactory
					.getInstance()
					.getPlayerManagerDataAccess()
					.registerNewPassport(playerName, password, mobileNum,
							promoID,parentPromoId, securityQuestionIdx, securityAnswer,
							attribute, remark);
		}
		return player;
	}

	// /**
	// * 根据推广渠道的一个账号标识注册一个新的账号
	// *
	// * @param promoMask
	// * 渠道用户名标识
	// * @param promoID
	// * 推广ID
	// * @param securityQuestionIdx
	// * 安全提问问题号
	// * @param securityAnswerIdx
	// * 安全回答号
	// * @param remark
	// * 其它备注信息
	// * @throws KGameDBException
	// * ,Exception
	// * @throws InvalidUserInfoException
	// * @throws Exception
	// */
	// public DBPlayer registerNewPassportByPromoMask(int promoID,String
	// promoMask,
	// String mobileNum, int securityQuestionIdx,
	// String securityAnswer, String attribute, String remark)
	// throws InvalidUserInfoException, KGameDBException, Exception {
	//
	// DBPlayer player = KGameDataAccessFactory
	// .getInstance()
	// .getPlayerManagerDataAccess()
	// .registerNewPassportByPromoMask(promoMask, mobileNum, promoID,
	// securityQuestionIdx, securityAnswer, attribute, remark);
	//
	// return player;
	// }

	/**
	 * 通过账号名和密码登录
	 * 
	 * @param playerName
	 * @param password
	 * @return
	 * @throws InvalidUserInfoException
	 *             //FIXME 啊麟多余的异常InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 */
	public DBPlayer login(String playerName, String password)
			throws InvalidUserInfoException, PlayerAuthenticateException,
			KGameDBException {
		return KGameDataAccessFactory.getInstance()
				.getPlayerManagerDataAccess().loginByName(playerName, password);
	}

	/**
	 * <pre>
	 * 通过推广渠道的账号标识登录
	 * 如果渠道账号标识对应的玩家账号不存在，该方法会自动创建一个匹配的平台账号
	 * @param promoMask 渠道账号标识码
	 * @param mobileNum 渠道附带手机号码
	 * @param promoID   推广渠道ID
	 * @param remark 
	 * @return
	 * @throws InvalidUserInfoException
	 * @throws PlayerAuthenticateException
	 * @throws KGameDBException
	 * </pre>
	 */
	public DBPlayer loginByPromoMask(String promoMask, String mobileNum,
			int promoID,int parentPromoId, String remark) throws InvalidUserInfoException,
			PlayerAuthenticateException, KGameDBException, Exception {
		DBPlayer player = null;
		try {
			player = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess()
					.loginByPromoMask(promoMask, promoID);
		} catch (PlayerAuthenticateException e) {
			if (e.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
				player = registerNewPassportByPromoMask(promoID,parentPromoId, promoMask,
						remark);
				if (player != null) {
					player = loginByPromoMask(promoMask, mobileNum, promoID,parentPromoId,
							remark);
				}
			} else {
				throw e;
			}
		}

		return player;
	}

	public KGamePlayer loadPlayerData(String playerName) {
		DBPlayer data = null;
		try {
			if ((data = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess().loadDBPlayer(playerName)) != null) {
				KGamePlayer player = new KGamePlayer(data);
				return player;
			}
		} catch (KGameDataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public KGamePlayer loadPlayerData(long playerId) {
		DBPlayer data = null;
		try {
			if ((data = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess().loadDBPlayer(playerId)) != null) {
				KGamePlayer player = new KGamePlayer(data);
				return player;
			}
		} catch (KGameDataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void updatePlayerAttribute(KGamePlayer player) {
		if(player != null) {
		try {
			KGameDataAccessFactory
					.getInstance()
					.getPlayerManagerDataAccess()
					.updatePlayerAttributeById(player.getID(),player.encodeAttribute());
		} catch (PlayerAuthenticateException e) {
			e.printStackTrace();
		} catch (KGameDBException e) {
			e.printStackTrace();
		}
		logger.debug("updatePlayerAttributeAndRemark {}", player);
		} else {
			logger.error("updatePlayerAttributeAndRemark, player is null!");
		}
	}
	
	public void updatePlayerRemark(KGamePlayer player){
		if(player==null){
			return;
		}
			
		try {
			KGameDataAccessFactory
					.getInstance()
					.getPlayerManagerDataAccess()
					.updatePlayerRemarkById(player.getID(), player.encodeRemark());
		} catch (PlayerAuthenticateException e) {
			e.printStackTrace();
		} catch (KGameDBException e) {
			e.printStackTrace();
		}
		logger.debug("updatePlayerRemark {}", player);
	}
}
