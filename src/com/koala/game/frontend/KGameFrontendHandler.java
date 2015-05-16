package com.koala.game.frontend;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;

import com.koala.game.KGame;
import com.koala.game.KGameHotSwitch;
import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.dataaccess.InvalidUserInfoException;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.frontend.KGameGSManager.GS;
import com.koala.game.frontend.KGameGSManager.GSStatusStruct;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayer;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.game.tips.RespCode;
import com.koala.game.util.DateUtil;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.downjoy.DownjoyChannel;
import com.koala.promosupport.kola.KolaChannel;
import com.koala.promosupport.mi.MiChannel;
import com.koala.promosupport.ng91.NG91Channel;
import com.koala.promosupport.qh360.QH360Channel;
import com.koala.promosupport.uc.UCChannel;

public class KGameFrontendHandler extends SimpleChannelHandler implements
		KGameProtocol/* ,LifeCycleAwareChannelHandler */{

	private static final KGameLogger logger = KGameLogger
			.getLogger(KGameFrontendHandler.class);
	private KGameFrontend frontend;
	public static final ConcurrentHashMap<Channel, KGamePlayerSession> handshakedplayersessions = new ConcurrentHashMap<Channel, KGamePlayerSession>();

	public KGameFrontendHandler(KGameFrontend fe) {
		this.frontend = fe;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		if (!(e.getMessage() instanceof KGameMessage)) {
			// 非法消息，logging & disconnect
			logger.warn("Illegal Message Type! Must be KGameMessage. {}",
					e.getMessage());
			return;
		}
		FEStatusMonitor.commcounter.messageReceived();

		Channel channel = e.getChannel();
		KGameMessage kmsg = (KGameMessage) e.getMessage();
		

		if (kmsg.getMsgType() != KGameMessage.MTYPE_PLATFORM) {
			logger.warn("Illegal Message Type for FE {}", kmsg.getMsgType());
			return;//FIXME 测试暂时注释
		}
		if (kmsg.getClientType() == KGameMessage.CTYPE_GS) {
			frontend.getGSMgr().messageReceived(channel, kmsg);
			return;
		}

		KGamePlayerSession playersession = handshakedplayersessions
				.get(channel);
		int msgID = kmsg.getMsgID();

		switch (msgID) {
		/* 心跳消息 */
		case MID_PING:
			if (playersession == null) {
				channel.close();
				break;
			}

			long pingt = kmsg.readLong();
			KGameMessage pingresp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_PING);
			// 作弊判断
			if (!playersession.checkPingRate(KGameHotSwitch.getInstance()
					.getPingJudgeMin(), KGameHotSwitch.getInstance()
					.getAllowPingPerMin(),pingt)) {
				logger.warn("CHEAT, playersession {}", playersession);
				pingresp.writeLong(-1);
				channel.write(pingresp);
				// 警告并断开并封号X分钟！！！！！！！！！！！！！！！！
				if (playersession.getBoundPlayer() != null) {
					playersession.getBoundPlayer().setBanEndtime(
							System.currentTimeMillis()
									+ (KGameHotSwitch.getInstance()
											.getCheatBanMin() * 60000));
					KGameFrontend.getInstance().getPlayerManager()
							.updatePlayerRemark(playersession.getBoundPlayer());
				}
				KGameMessage sysnotice = KGameCommunication
						.newSystemNoticeMsg(KGameTips.get("CHEAT"));
				channel.write(sysnotice).addListener(
						ChannelFutureListener.CLOSE);
				break;
			}
			pingresp.writeLong(pingt);
			playersession.send(pingresp);
			// logger.warn("MID_PING ({}),{}",pingt , playersession);
			break;

		/* step1-握手消息 */
		case MID_HANDSHAKE:
			String code = kmsg.readUtf8String();
			String clientmodel = kmsg.readUtf8String();
			logger.debug("MID_HANDSHAKE  {} {}", code, clientmodel);
			// TODO 这里要对握手发来的内容做安全验证

			playersession = new KGamePlayerSession(channel);
			playersession.setClientType(kmsg.getClientType());
			playersession.setCurLoginDeviceModel(clientmodel);

			handshakedplayersessions.put(channel, playersession);
			KGameMessage handshakeResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_HANDSHAKE);
			handshakeResp.writeUtf8String("Hi! I'm FE~ ");
			playersession.write(handshakeResp);
			FEStatusMonitor.commcounter.handshaked();
			
			//默认推荐GS---2013-9-18改成放这里了
			frontend.getGSMgr().choiceRecommendatoryGSAndSendtoClient(
					playersession);
			break;

		/* step2-版本检查 */
		case MID_VERCHECK://
			//默认推荐GS
			frontend.getGSMgr().choiceRecommendatoryGSAndSendtoClient(
					playersession);
			//版本验证
			frontend.getVerChecker().checkAndHandle(kmsg, channel);
			break;

		/* step3-账号验证 */
		case MID_PASSPORT_VERIFY:
			String verifyPName = kmsg.readUtf8String();
			String verifyPPw = kmsg.readUtf8String();
			logger.debug("MID_PASSPORT_VERIFY  {},{}", verifyPName, verifyPPw);
			// 还没握手的情况下
			if (playersession == null) {
				KGameMessage verifyResp = KGameCommunication.newMessage(
						kmsg.getMsgType(), kmsg.getClientType(),
						MID_PASSPORT_VERIFY);
				verifyResp.writeUtf8String(verifyPName);
				verifyResp.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				verifyResp.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				// 非法的验证消息和客户端，发送消息后要关闭掉！
				channel.write(verifyResp).addListener(
						ChannelFutureListener.CLOSE);
				break;
			}
			// 已经握过手的情况
			RespCode pl_code = new RespCode();
			pl_code.set(PL_PASSPORT_VERIFY_SUCCEED,
					"PL_PASSPORT_VERIFY_SUCCEED");
			try {
				KGameDataAccessFactory.getInstance()
						.getPlayerManagerDataAccess()
						.verifyPlayerPassport(verifyPName, verifyPPw);
			} catch (PlayerAuthenticateException e1) {
				if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
					pl_code.set(PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST,
							"NAMENOTEXIST");
				} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_WRONG_PASSWORD) {
					pl_code.set(PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG,
							"PASSWORDISWRONG");
				}
			} catch (KGameDBException e1) {
				pl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			// Response
			KGameMessage verifyResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(),
					MID_PASSPORT_VERIFY);
			verifyResp.writeUtf8String(verifyPName);
			if (pl_code.v == PL_PASSPORT_VERIFY_SUCCEED) {
				// 如果验证成功:
				// 1.从数据库加载Player信息
				if (playersession.loadAndBindPlayer(verifyPName)) {
					// 检测是否有封号
					long banEndtime = playersession.getBoundPlayer()
							.getBanEndtime();
					if (banEndtime > System.currentTimeMillis()) {
						pl_code.set(PL_PASSPORT_VERIFY_FAILED_BAN, "BAN");
						verifyResp.writeInt(pl_code.v);
						verifyResp.writeUtf8String(KGameTips
								.get(pl_code.k,
										DateUtil.formatReadability(new Date(
												banEndtime))));
					} else {
						playersession.setAuthenticationPassed(true);// 验证通过
						// 2.直接携带‘服务器列表’的内容
						verifyResp.writeInt(pl_code.v);
						verifyResp.writeUtf8String(KGameTips.get(pl_code.k));
						frontend.getGSMgr().writeGsListOnResponseMsg(
								verifyResp, playersession,PromoSupport.computeParentPromoID(playersession.getBoundPlayer().getPromoID()));
					}
				} else {
					pl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
							"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
					verifyResp.writeInt(pl_code.v);
					verifyResp.writeUtf8String(KGameTips.get(pl_code.k));
				}
			} else {
				verifyResp.writeInt(pl_code.v);
				verifyResp.writeUtf8String(KGameTips.get(pl_code.k));
			}
			playersession.send(verifyResp);
			if (pl_code.v == PL_PASSPORT_VERIFY_SUCCEED) {
				FEStatusMonitor.commcounter.logined();
			}
			break;

		/**
		 * 通过接入第三方推广渠道SDK的账号验证
		 * 
		 * <pre>
		 * 【REQUEST】:
		 *  int promoID;//推广渠道ID（我们自己定义好的不能变的）
		 *  int paramN;//参数KV对数量
		 *  for(paramN){
		 *    String key;//参数KEY，根据不同渠道而定，定义请看协议文件中PROMO_KEY_???_???
		 *    String value;//参数值，根据不同渠道而定
		 *  }
		 * 【RESPONSE】:
		 *  int responsecode =
		 *  {@link #PL_PASSPORT_VERIFY_SUCCEED} / 
		 *  {@link #PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST} /
		 *  {@link #PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG}/
		 *  {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
		 *  String tips;//20130222添加的提示语，有可能是0长度客户端要注意
		 *  if(responsecode==PL_PASSPORT_VERIFY_SUCCEED){
		 *    String kName;//我们自己的kola账号（客户端要保留真正登录GS时用）
		 *    String kPassword;//我们自己的kola密码（客户端要保留真正登录GS时用）
		 *    后面携带服务器列表数据
		 *  }
		 * </pre>
		 */
		case MID_PASSPORT_VERIFY_BY_PROMOCHANNEL:
			KGameMessage pverifyResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(),
					MID_PASSPORT_VERIFY_BY_PROMOCHANNEL);
			// 还没握手的情况下
			if (playersession == null) {
				pverifyResp.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				pverifyResp.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				// 非法的验证消息和客户端，发送消息后要关闭掉！
				channel.write(pverifyResp).addListener(
						ChannelFutureListener.CLOSE);
				break;
			}

			int promoID = kmsg.readInt();
			int paramN = kmsg.readInt();
			logger.debug(
					"MID_PASSPORT_VERIFY_BY_PROMOCHANNEL  promoID={},paramN={}",
					promoID, paramN);
			//跟第三方SDK服务器验证时需要用到的数据
			Map<String, String> params = new HashMap<String, String>(paramN,
					1.0f);
			//只是保存起来为了统计分析之用
			Map<String,String> analysisInfoNeedSaveDB = new HashMap<String, String>(paramN,1.0f);
			for (int i = 0; i < paramN; i++) {
				String pkey = kmsg.readUtf8String();
				String pval = kmsg.readUtf8String();
//				logger.debug("  key={},value={}", pkey, pval);
				if(pkey.startsWith("analysis")){
					analysisInfoNeedSaveDB.put(pkey.replace("analysis_", ""), pval);
				}else{
					params.put(pkey, pval);
				}
			}
			logger.debug("MID_PASSPORT_VERIFY_BY_PROMOCHANNEL  promoID={},params={},analysisInfoNeedSaveDB={}", promoID, params, analysisInfoNeedSaveDB);

			// //////////////////////////////////////////////////////////////
			// 根据不同渠道SDK要求做账号验证流程
			// //////////////////////////////////////////////////////////////
			PromoChannel pchannel = PromoSupport.getInstance().getPromoChannel(
					promoID);
			if (pchannel == null||(!pchannel.canLogin())) {
				// 该渠道支持不存在
				pverifyResp.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND);
				pverifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND",promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND));
				playersession.send(pverifyResp);
				logger.debug("MID_PASSPORT_VERIFY_BY_PROMOCHANNEL  promoID={},pchannel不存在或不允许登录！", promoID);
				break;
			}
			
			IUserVerify userverify = pchannel.getUserVerify();
			if(userverify==null){
				// 该渠道支持不存在
				pverifyResp.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND);
				pverifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND",promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND));
				playersession.send(pverifyResp);
				logger.debug("MID_PASSPORT_VERIFY_BY_PROMOCHANNEL  promoID={},pchannel.getUserVerify()=null！", promoID);
				break;
			}
			
			// 【重点】跟渠道服务器做验证
//			userverify.request(playersession, pverifyResp, promoID, params);
			userverify.request(playersession, pverifyResp, promoID, params, analysisInfoNeedSaveDB); // 2014-09-02 添加多了一个参数
			
			// 2014-09-02 注释以下代码，因为这个时候肯定未有boundPlayer，所以这些信息肯定存储不了
//			//////////////////////////////////////////////////////////////
//			// 20131129增加了一些客户端发来的统计类数据，放到Player的attribute属性里面
//			if(playersession.getBoundPlayer()!=null&&(!analysisInfoNeedSaveDB.isEmpty())){
//				for (String key : analysisInfoNeedSaveDB.keySet()) {
//					String value = analysisInfoNeedSaveDB.get(key);
//					playersession.getBoundPlayer().addAnalysisInfoToAttribute(key, value);
//				}
//			}
			// 2014-09-04 END
			
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<kola
//			if (pchannel.getPromoID() == PromoChannel.PROMOID_KOLA) {
//				KolaChannel kola = (KolaChannel) pchannel;
//				String k_pname = params.get(PROMO_KEY_KOLA_PNAME);
//				String k_pw = params.get(PROMO_KEY_KOLA_PW);
//				if ((k_pname == null || k_pname.length() <= 0)
//						| (k_pw == null || k_pw.length() <= 0)) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				kola.doUserVerify(playersession, pverifyResp, promoID, k_pname, k_pw);
//			}
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<当乐
//			else if (pchannel.getPromoID() == PromoChannel.PROMOID_DOWNJOY) {
//				DownjoyChannel downjoy = (DownjoyChannel) pchannel;
//				String d_mid = params.get(PROMO_KEY_DOWNJOY_MID);
//				String d_token = params.get(PROMO_KEY_DOWNJOY_TOKEN);
//				if ((d_mid == null || d_mid.length() <= 0)
//						| (d_token == null || d_token.length() <= 0)) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				// 跟渠道服务器做验证
//				downjoy.doLoginVerify(playersession, pverifyResp, promoID,
//						d_mid, d_token);
//			} 
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<UC
//			else if (pchannel.getPromoID() == PromoChannel.PROMOID_UC) {
//				UCChannel uc = (UCChannel) pchannel;
//				String uc_sid = params.get(PROMO_KEY_UC_SID);
//				if (uc_sid == null || uc_sid.length() <= 0) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				// 跟渠道服务器做验证
//				uc.doSessionVerify(playersession, pverifyResp, promoID, uc_sid);
//			}
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<91
//			else if (pchannel.getPromoID() == PromoChannel.PROMOID_91) {
//				NG91Channel ng91 = (NG91Channel) pchannel;
//				String uin = params.get(PROMO_KEY_91_UIN);
//				String sessionid = params.get(PROMO_KEY_91_SESSIONID);
//				if (uin == null || uin.length() <= 0 || sessionid == null
//						|| sessionid.length() <= 0) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				// 跟渠道服务器做验证
//				ng91.doUserVerify(playersession, pverifyResp, promoID, uin,
//						sessionid);
//
//				// promoMask = uin;//
//				// verifywiththirdpartsdkok = true;
//			}
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<360
//			else if (pchannel.getPromoID() == PromoChannel.PROMOID_360) {
//				QH360Channel qh360 = (QH360Channel) pchannel;
//				String authcode = params.get(PROMO_KEY_360_AUTHCODE);
//				if (authcode == null || authcode.length() <= 0) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				// 跟渠道服务器做验证
//				qh360.doGetAccessTokenAndUserInfo(playersession, pverifyResp,
//						promoID, authcode);
//
//			}
//			// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<MI
//			else if (pchannel.getPromoID() == PromoChannel.PROMOID_MI){
//				MiChannel mi = (MiChannel) pchannel;
//				String mi_uid = params.get(PROMO_KEY_MI_UID);
//				String mi_session = params.get(PROMO_KEY_MI_SESSION);
//				if (mi_uid == null || mi_uid.length() <= 0 || mi_session == null
//						|| mi_session.length() <= 0) {
//					// 格式错误
//					pverifyResp
//							.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
//					pverifyResp.writeUtf8String("");
//					playersession.send(pverifyResp);
//					break;
//				}
//				// 跟渠道服务器做验证
//				mi.doUserVerify(playersession, pverifyResp, promoID, mi_uid, mi_session);
//			}
//			//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<该渠道支持不存在
//			else {
//				// 该渠道支持不存在
//				pverifyResp
//						.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND);
//				pverifyResp.writeUtf8String("");
//				playersession.send(pverifyResp);
//				break;
//			}
			// 为释放当前消息处理线程，后面的流程都在具体的HTTP请求的线程中进行
			// //
			// //////////////////////////////////////////////////////////////////////
			// // 与第三方服务器验证成功
			// // 开始走我们的内部流程：
			// if (verifywiththirdpartsdkok && promoMask != null) {
			// DBPlayer dbPlayer = null;
			// // 1、验证我们对应该渠道的账号是否存在
			// try {
			// dbPlayer = KGameDataAccessFactory
			// .getInstance()
			// .getPlayerManagerDataAccess()
			// .verifyPlayerPassportByPromoMask(promoMask, promoID);
			// } catch (PlayerAuthenticateException e1) {
			// if (e1.getErrorCode() ==
			// PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
			// // 如果不存在这个账号mapping直接注册一个新的KOLA账号
			// try {
			// dbPlayer = KGameFrontend
			// .getInstance()
			// .getPlayerManager()
			// .registerNewPassportByPromoMask(promoID,
			// promoMask, "");// TODO remark要搞
			// } catch (InvalidUserInfoException e11) {
			// } catch (KGameDBException e11) {
			// }
			// }
			// } catch (KGameDBException e1) {
			// }
			// // 2、找到了对应的账号，成功进入下一步
			// if (dbPlayer != null && dbPlayer.getPlayerName() != null) {
			// if (playersession.decodeAndBindPlayer(dbPlayer)) {
			// // 检测是否有封号
			// long banEndtime = playersession.getBoundPlayer()
			// .getBanEndtime();
			// if (banEndtime > 0
			// && banEndtime > System.currentTimeMillis()) {
			// pverifyResp.writeInt(PL_PASSPORT_VERIFY_FAILED_BAN);
			// pverifyResp.writeUtf8String(KGameTips.get("BAN"));
			// playersession.send(pverifyResp);
			// break;
			// } else {
			// playersession.setAuthenticationPassed(true);// 验证通过
			// // 2.直接携带‘服务器列表’的内容
			// pverifyResp.writeInt(PL_PASSPORT_VERIFY_SUCCEED);
			// pverifyResp.writeUtf8String("");
			// pverifyResp.writeUtf8String(dbPlayer
			// .getPlayerName());
			// pverifyResp.writeUtf8String(dbPlayer.getPassword());
			// frontend.getGSMgr().writeGsListOnResponseMsg(
			// pverifyResp, playersession);
			// playersession.send(pverifyResp);
			// break;
			// }
			// }
			// }
			// }
			// // 其它失败情况
			// pverifyResp.writeInt(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY);
			// pverifyResp.writeUtf8String(KGameTips
			// .get("PL_UNKNOWNEXCEPTION_OR_SERVERBUSY"));
			// playersession.send(pverifyResp);
			break;

		/* 注册新账号 */
		case MID_REGISTER_NEWPASSPORT:
			KGameMessage registerresponsemsg = KGameCommunication.newMessage(
					KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(),
					MID_REGISTER_NEWPASSPORT);
			if (playersession == null) {
				registerresponsemsg.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				registerresponsemsg.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				channel.write(registerresponsemsg);
				break;
			}
			String registername = kmsg.readUtf8String();
			String registerpassword = kmsg.readUtf8String();
			String registerremark = kmsg.readUtf8String();//20130806这里内容定义promoID（无自身登录SDK的渠道）
			logger.debug("MID_REGISTER_NEWPASSPORT  {},{},{}", registername,
					registerpassword, registerremark);
			DBPlayer newplayer = null;
			RespCode registerfailedcode = new RespCode();
			registerfailedcode.set(PL_REGISTER_FAILED_DUPLICATIONOFNAME,
					"PL_REGISTER_FAILED_DUPLICATIONOFNAME");
			try {
				newplayer = KGameFrontend
						.getInstance()
						.getPlayerManager()
						.registerNewPassport(registername, registerpassword,
								Integer.parseInt(registerremark), registername,
								"");
			} catch (InvalidUserInfoException e1) {
				switch (e1.getErrorCode()) {
				case InvalidUserInfoException.PLAYER_MOBILE_INVALID_LENGTH:
					registerfailedcode.set(PL_REGISTER_FAILED_MOBILE_INVALID,
							"PL_REGISTER_FAILED_MOBILE_INVALID");
					break;
				case InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH:
					registerfailedcode.set(PL_REGISTER_FAILED_PASSWORD_INVALID,
							"PL_REGISTER_FAILED_PASSWORD_INVALID");
					break;
				case InvalidUserInfoException.PLAYER_NAME_INVALID:
					registerfailedcode.set(
							PL_REGISTER_FAILED_DUPLICATIONOFNAME,
							"PL_REGISTER_FAILED_DUPLICATIONOFNAME");
					break;
				case InvalidUserInfoException.PLAYER_NAME_INVALID_LENGTH:
					registerfailedcode.set(
							PL_REGISTER_FAILED_UN_INVALID_LENGTH,
							"PL_REGISTER_FAILED_UN_INVALID_LENGTH");
					break;
				}
			} catch (KGameDBException e1) {
				registerfailedcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			if (newplayer != null && newplayer.getPlayerId() != -1) {
				// succeed
				registerresponsemsg.writeInt(PL_REGISTER_SUCCEED);
				registerresponsemsg.writeLong(newplayer.getPlayerId());
				registerresponsemsg.writeUtf8String(KGameTips
						.get("PL_REGISTER_SUCCEED"));
				logger.debug("MID_REGISTER_NEWPASSPORT SUCCEED! newpid={}",
						newplayer.getPlayerId());
			} else {
				// failed
				registerresponsemsg.writeInt(registerfailedcode.v);
				registerresponsemsg.writeUtf8String(KGameTips
						.get(registerfailedcode.k));
				logger.debug("MID_REGISTER_NEWPASSPORT FAILED! failedcode={}",
						registerfailedcode);
			}
			playersession.send(registerresponsemsg);
			break;

		/* 注册新账号-自动 */
		case MID_REGISTER_NEWPASSPORT_AUTO:
			break;

		case MID_REGISTER_NEWPASSPORT_BY_PROMOCHANNEL:
			break;

		/* 修改密码 */
		case MID_CHANGE_PASSWORD:
			KGameMessage changepw_responsemsg = KGameCommunication.newMessage(
					KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(),
					MID_CHANGE_PASSWORD);
			if (playersession == null) {
				changepw_responsemsg.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				changepw_responsemsg.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				channel.write(changepw_responsemsg);
				break;
			}
			String changepw_pname = kmsg.readUtf8String();
			String changepw_oldpw = kmsg.readUtf8String();
			String changepw_newpw = kmsg.readUtf8String();
			logger.debug("MID_CHANGE_PASSWORD  {},{},{}", changepw_pname,
					changepw_oldpw, changepw_newpw);
			RespCode changepwcode = new RespCode();
			changepwcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
					"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			try {
				if (KGameDataAccessFactory
						.getInstance()
						.getPlayerManagerDataAccess()
						.changePassword(changepw_pname, changepw_oldpw,
								changepw_newpw)) {
					changepwcode.set(PL_CHANGE_PASSWORD_SUCCEED,
							"PL_CHANGE_PASSWORD_SUCCEED");
				}
			} catch (InvalidUserInfoException e1) {
				if (e1.getErrorCode() == InvalidUserInfoException.PLAYER_PASSWORD_INVALID_LENGTH) {
					changepwcode.set(
							PL_CHANGE_PASSWORD_FAILED_NEWPASSWORDINVALID,
							"PL_CHANGE_PASSWORD_FAILED_NEWPASSWORDINVALID");
				}
			} catch (PlayerAuthenticateException e1) {
				if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
					changepwcode.set(PL_CHANGE_PASSWORD_FAILED_NAMENOTEXIST,
							"PL_CHANGE_PASSWORD_FAILED_NAMENOTEXIST");
				} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_WRONG_PASSWORD) {
					changepwcode.set(PL_CHANGE_PASSWORD_FAILED_PASSWORDISWRONG,
							"PL_CHANGE_PASSWORD_FAILED_PASSWORDISWRONG");
				}
			} catch (KGameDBException e1) {
				changepwcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			changepw_responsemsg.writeInt(changepwcode.v);
			changepw_responsemsg.writeUtf8String(KGameTips.get(changepwcode.k));
			playersession.send(changepw_responsemsg);
			break;

		/* 忘记密码 */
		case MID_FORGET_PASSWORD:
			KGameMessage forgetpw_responsemsg = KGameCommunication.newMessage(
					KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(),
					MID_FORGET_PASSWORD);
			if (playersession == null) {
				forgetpw_responsemsg.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				forgetpw_responsemsg.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				channel.write(forgetpw_responsemsg);
				break;
			}
			String forgetpw_userName = kmsg.readUtf8String();
			int forgetpw_questionId = kmsg.readInt();
			String forgetpw_answer = kmsg.readUtf8String();
			logger.debug("MID_FORGET_PASSWORD  {},{},{}", forgetpw_userName,
					forgetpw_questionId, forgetpw_answer);
			RespCode forgetpwcode = new RespCode();
			forgetpwcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
					"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			String passwordgot = null;
			try {
				passwordgot = KGameDataAccessFactory
						.getInstance()
						.getPlayerManagerDataAccess()
						.reclaimPasswordOf(forgetpw_userName,
								forgetpw_questionId, forgetpw_answer);
			} catch (PlayerAuthenticateException e1) {
				if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
					forgetpwcode.set(PL_FORGET_PASSWORD_FAILED_NAMENOTEXIST,
							"NAMENOTEXIST");
				} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_UNMATCHED_ANSWER) {
					forgetpwcode.set(PL_FORGET_PASSWORD_FAILED_UNMATCHED,
							"PL_FORGET_PASSWORD_FAILED_UNMATCHED");
				}
			} catch (KGameDBException e1) {
				forgetpwcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			if (passwordgot != null && passwordgot.length() > 0) {
				forgetpwcode.set(PL_FORGET_PASSWORD_SUCCEED,
						"PL_FORGET_PASSWORD_SUCCEED");
				forgetpw_responsemsg.writeInt(forgetpwcode.v);
				forgetpw_responsemsg.writeUtf8String(passwordgot);
				forgetpw_responsemsg.writeUtf8String(KGameTips.get(
						forgetpwcode.k, passwordgot));
			} else {
				forgetpw_responsemsg.writeInt(forgetpwcode.v);
				forgetpw_responsemsg.writeUtf8String(KGameTips
						.get(forgetpwcode.k));
			}
			playersession.send(forgetpw_responsemsg);
			break;

		/* step4-获取服务器列表 ./排队进入GS时，用户选择了取消 */
		case MID_CANCEL_LOGINQUEUE:
		case MID_GET_GSLIST:
			KGameMessage gslistResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), msgID);
			if (playersession == null
					|| (!playersession.isAuthenticationPassed())) {
				gslistResp.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				channel.write(gslistResp).addListener(
						ChannelFutureListener.CLOSE);// 非法的验证消息和客户端，要关闭掉！
				break;
			}
			if (msgID == MID_CANCEL_LOGINQUEUE) {
				// 取消排队
				frontend.getGSMgr().cancelLoginqueue(playersession);
			}
			gslistResp.writeInt(PL_GET_GSLIST_SUCCEED);
			frontend.getGSMgr().writeGsListOnResponseMsg(gslistResp,
					playersession,PromoSupport.computeParentPromoID(playersession.getBoundPlayer().getPromoID()));
			KGameNoticeManager.writeNoticesToMsgForClient(gslistResp);
			playersession.send(gslistResp);
			break;

		/* 选择服务器并下一步 */
		case MID_SELECT_GS:
			KGameMessage selectgsResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_SELECT_GS);
			if (playersession == null) {// 非法会话
				selectgsResp.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
				selectgsResp.writeUtf8String(KGameTips
						.get("PL_ILLEGAL_SESSION_OR_MSG"));
				channel.write(selectgsResp).addListener(
						ChannelFutureListener.CLOSE);
				break;
			}
			int selectedgsid = kmsg.readInt();
			GS gs = frontend.getGSMgr().getGS(selectedgsid);
			if (gs == null) {// 请求的GS不存在
				selectgsResp.writeInt(PL_SELECT_GS_FAILED_REJECTED);
				selectgsResp.writeUtf8String(KGameTips
						.get("PL_SELECT_GS_FAILED_REJECTED"));
				playersession.send(selectgsResp);
				break;
			}
			playersession.setLastSelectGsId(selectedgsid);
//			if (gs.status == CONST_GS_STATUS_MAINTENANCE) {// '-1'表示停服维护状态
//				selectgsResp.writeInt(PL_SELECT_GS_FAILED_UNOPENED);
//				selectgsResp.writeUtf8String(KGameTips
//						.get("PL_SELECT_GS_FAILED_UNOPENED"));
//			} else if (gs.status == CONST_GS_STATUS_STARTBUTNOTOPEN // '0'表示已经启动但未对外开放，对白名单开放;;
//					|| gs.status == CONST_GS_STATUS_OPEN) {// '1'表示对外开放
//				// 检测是否有封号
//				long banEndtime = playersession.getBoundPlayer()
//						.getBanEndtime();
//				if (banEndtime > 0 && banEndtime > System.currentTimeMillis()) {
//					selectgsResp.writeInt(PL_SELECT_GS_FAILED_UNOPENED);
//					selectgsResp.writeUtf8String(KGameTips.get("BAN",
//							DateUtil.formatReadability(new Date(banEndtime))));
//					playersession.send(selectgsResp);
//					break;
//				}
//				
//				// 检查账号是否在白名单范围
//				if (frontend.getGSMgr().containInWhitelist(selectedgsid,
//						playersession.getBoundPlayer().getID(),
//						playersession.getBoundPlayer().getPlayerName())) {
//					selectgsResp.writeInt(PL_SELECT_GS_SUCCEED);
//					selectgsResp.writeUtf8String("");
//					selectgsResp.writeUtf8String(gs.gsIP);
//					selectgsResp.writeInt(gs.gsSocketPort);
//					playersession.send(selectgsResp);
//					break;
//				}
//				
//				if (gs.status == CONST_GS_STATUS_STARTBUTNOTOPEN) {
//					// '0'表示已经启动但未对外开放
//					selectgsResp.writeInt(PL_SELECT_GS_FAILED_OPENSOON);
//					selectgsResp.writeUtf8String(KGameTips
//							.get("PL_SELECT_GS_FAILED_OPENSOON"));
//					selectgsResp.writeUtf8String(KGameTips
//							.get("PL_SELECT_GS_FAILED_OPENSOON"));// OPENSOON
//				} else {
//					// 要看是否要排队
//					int remain = gs.allowedOnline
//							- (gs.currentOnline + gs.sizeOfLoginQueue());
//					// TODO 选择服务器，还要看某个玩家是否有优先进入的特权；排队机制
//					if (remain > 0) {// can join
//						selectgsResp.writeInt(PL_SELECT_GS_SUCCEED);
//						selectgsResp.writeUtf8String("");
//						selectgsResp.writeUtf8String(gs.gsIP);
//						selectgsResp.writeInt(gs.gsSocketPort);
//					} else {
//						gs.add2LoginQueue(playersession);// 第一次加入排队
//						selectgsResp.writeInt(PL_SELECT_GS_FAILED_QUEUE);
//						selectgsResp.writeUtf8String(KGameTips
//								.get("PL_SELECT_GS_FAILED_OPENSOON"));
//						selectgsResp.writeInt(gs.sizeOfLoginQueue());
//						selectgsResp.writeInt(gs.getAvgpolltimeseconds());
//					}
//				}
//			}
			// 2014-06-21 修改 BEGIN
			if (gs.status == GSStatusStruct.STATUS_MAINTAIN) {// '-1'表示停服维护状态
				selectgsResp.writeInt(PL_SELECT_GS_FAILED_UNOPENED);
				selectgsResp.writeUtf8String(KGameTips
						.get("PL_SELECT_GS_FAILED_UNOPENED"));
			} else {// '1'表示对外开放
				// 检测是否有封号
				long banEndtime = playersession.getBoundPlayer()
						.getBanEndtime();
				if (banEndtime > 0 && banEndtime > System.currentTimeMillis()) {
					selectgsResp.writeInt(PL_SELECT_GS_FAILED_UNOPENED);
					selectgsResp.writeUtf8String(KGameTips.get("BAN",
							DateUtil.formatReadability(new Date(banEndtime))));
					playersession.send(selectgsResp);
					break;
				}
				
				// 检查账号是否在白名单范围
				if (frontend.getGSMgr().containInWhitelist(selectedgsid,
						playersession.getBoundPlayer().getID(),
						playersession.getBoundPlayer().getPlayerName())) {
					selectgsResp.writeInt(PL_SELECT_GS_SUCCEED);
					selectgsResp.writeUtf8String("");
					selectgsResp.writeUtf8String(gs.gsIP);
					selectgsResp.writeInt(gs.gsSocketPort);
					playersession.send(selectgsResp);
					break;
				}
				
				if (gs.status == GSStatusStruct.STATUS_WHITE_LIST_ONLY) {
					// '0'表示已经启动但未对外开放
					selectgsResp.writeInt(PL_SELECT_GS_FAILED_OPENSOON);
					selectgsResp.writeUtf8String(KGameTips.get("PL_SELECT_GS_FAILED_OPENSOON"));
					selectgsResp.writeUtf8String(gs.gsIP);// OPENSOON
					selectgsResp.writeInt(gs.gsSocketPort);
				} else {
					// 要看是否要排队
					int remain = gs.allowedOnline - (gs.currentOnline + gs.sizeOfLoginQueue());
					// TODO 选择服务器，还要看某个玩家是否有优先进入的特权；排队机制
					if (remain > 0) {// can join
						selectgsResp.writeInt(PL_SELECT_GS_SUCCEED);
						selectgsResp.writeUtf8String("");
						selectgsResp.writeUtf8String(gs.gsIP);
						selectgsResp.writeInt(gs.gsSocketPort);
					} else {
						gs.add2LoginQueue(playersession);// 第一次加入排队
						selectgsResp.writeInt(PL_SELECT_GS_FAILED_QUEUE);
						selectgsResp.writeUtf8String(KGameTips.get("PL_SELECT_GS_FAILED_QUEUE"));
						selectgsResp.writeInt(gs.sizeOfLoginQueue());
						selectgsResp.writeInt(gs.getAvgpolltimeseconds());
//						// 2014-10-16 暂时屏蔽排队
//						selectgsResp.writeInt(PL_SELECT_GS_FAILED_OPENSOON);
//						selectgsResp.writeUtf8String(KGameTips.get("PL_SELECT_GS_FULL"));
//						selectgsResp.writeUtf8String(gs.gsIP);// OPENSOON
//						selectgsResp.writeInt(gs.gsSocketPort);
					}
				}
			}
			//2014 06-21 修改 END
			playersession.send(selectgsResp);
			break;

		/* 登出 */
		case MID_LOGOUT:
			channel.close();// 自然就会触发channelClose回调
			break;

		case MID_SHUTDOWN:
			String shutdownKey;
			try {
				shutdownKey = kmsg.readUtf8String();
			} catch (Exception ex) {
				shutdownKey = null;
			}
			if (shutdownKey != null && shutdownKey.equals(KEY_SHUTDOWN_FE)) {
				KGameFrontend.getInstance().shutdown(channel);
			} else {
				logger.error("停机密匙不符合！", shutdownKey);
			}
			break;

		case MID_RECONNECT:
			KGameMessage reconnResp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_RECONNECT);
			// =======握手相关======
			String hcode = kmsg.readUtf8String();
			String hclientmodel = kmsg.readUtf8String();
			logger.debug("MID_RECONNECT===HANDSHAKE  {} {}", hcode,
					hclientmodel);
			// TODO 这里要对握手发来的内容做安全验证
			// 新建KGamePlayerSession并加入缓存
			playersession = new KGamePlayerSession(channel);
			playersession.setClientType(kmsg.getClientType());
			playersession.setCurLoginDeviceModel(hclientmodel);
			handshakedplayersessions.put(channel, playersession);

			// ======账号验证======
			String vverifyPName = kmsg.readUtf8String();
			String vverifyPPw = kmsg.readUtf8String();
			logger.debug("MID_RECONNECT===VERIFY  {},{}", vverifyPName,
					vverifyPPw);
			// 已经握过手的情况
			RespCode vpl_code = new RespCode();
			vpl_code.set(PL_RECONNECT_SUCCEED, "PL_RECONNECT_SUCCEED");
			try {
				KGameDataAccessFactory.getInstance()
						.getPlayerManagerDataAccess()
						.verifyPlayerPassport(vverifyPName, vverifyPPw);
			} catch (PlayerAuthenticateException e1) {
				if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
					vpl_code.set(PL_RECONNECT_FAILED_VERIFY, "NAMENOTEXIST");
				} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_WRONG_PASSWORD) {
					vpl_code.set(PL_RECONNECT_FAILED_VERIFY, "PASSWORDISWRONG");
				}
			} catch (KGameDBException e1) {
				vpl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			if (vpl_code.v == PL_RECONNECT_SUCCEED) {
				// 如果验证成功:
				// 1.从数据库加载Player信息
				if (playersession.loadAndBindPlayer(vverifyPName)) {

					// 检测是否有封号
					long banEndtime = playersession.getBoundPlayer()
							.getBanEndtime();
					if (banEndtime > System.currentTimeMillis()) {
						reconnResp.writeInt(PL_RECONNECT_FAILED_VERIFY);
						playersession.send(reconnResp);
						break;
					}

					playersession.setAuthenticationPassed(true);// 验证通过
					// ======找到最后登录的GS返回给客户端去连接======
					int lastgsid = playersession.getBoundPlayer()
							.getLastLoginedGSID();
					GS lastgs = KGameFrontend.getInstance().getGSMgr()
							.getGS(lastgsid);
					if (lastgs != null
							&& (lastgs.status == CONST_GS_STATUS_OPEN || lastgs.status == CONST_GS_STATUS_STARTBUTNOTOPEN)) {
						// !!成功重连
						reconnResp.writeInt(vpl_code.v);
						reconnResp.writeUtf8String(lastgs.gsIP);
						reconnResp.writeInt(lastgs.gsSocketPort);
						playersession.send(reconnResp);
						break;
					} else {
						vpl_code.set(PL_RECONNECT_FAILED_GSSHUTDOWN,
								"PL_RECONNECT_FAILED_GSSHUTDOWN");
						reconnResp.writeInt(vpl_code.v);
						playersession.send(reconnResp);
						break;
					}
				}
				vpl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
			}
			reconnResp.writeInt(vpl_code.v);
			playersession.send(reconnResp);
			break;

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>4GM
		case MID_GAG:
			KGameMessage gagresp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_GAG);
			long gplayerid = kmsg.readLong();
			long gendtime = kmsg.readLong();
			long ct = System.currentTimeMillis();
			if (gendtime != 0 && gendtime <= ct) {
				gagresp.writeInt(PL_GAG_FAILED_TIMEFORMATERROR);
				gagresp.writeLong(gplayerid);
				gagresp.writeLong(gendtime);
				playersession.send(gagresp);
				break;
			}
			KGamePlayer p = getplayeronlineordb(gplayerid);
			// 被禁言的玩家在线
			if (p != null) {
				p.setGagEndtime(gendtime);
				gagresp.writeInt(PL_GAG_SUCCEED);
				gagresp.writeLong(gplayerid);
				gagresp.writeLong(gendtime);
				playersession.send(gagresp);
				// 即时保存到DB
				KGameFrontend.getInstance().getPlayerManager()
						.updatePlayerRemark(p);
				break;
			}
			gagresp.writeInt(PL_GAG_FAILED_PLAYERNOTEXIST);
			gagresp.writeLong(gplayerid);
			gagresp.writeLong(gendtime);
			playersession.send(gagresp);
			break;
		case MID_BAN:
			KGameMessage banresp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), MID_BAN);
			long bplayerid = kmsg.readLong();
			long bendtime = kmsg.readLong();
			long ct1 = System.currentTimeMillis();
			if (bendtime != 0 && bendtime <= ct1) {
				banresp.writeInt(PL_BAN_FAILED_TIMEFORMATERROR);
				banresp.writeLong(bplayerid);
				banresp.writeLong(bendtime);
				playersession.send(banresp);
				break;
			}
			KGamePlayer p1 = getplayeronlineordb(bplayerid);
			// 被禁言的玩家在线
			if (p1 != null) {
				p1.setBanEndtime(bendtime);
				banresp.writeInt(PL_BAN_SUCCEED);
				banresp.writeLong(bplayerid);
				banresp.writeLong(bendtime);
				playersession.send(banresp);
				// 即时保存到DB
				KGameFrontend.getInstance().getPlayerManager()
						.updatePlayerRemark(p1);
				break;
			}
			banresp.writeInt(PL_BAN_FAILED_PLAYERNOTEXIST);
			banresp.writeLong(bplayerid);
			banresp.writeLong(bendtime);
			playersession.send(banresp);
			break;
		case MID_QUERY_BAN:
		case MID_QUERY_GAG:
			KGameMessage qresp = KGameCommunication.newMessage(
					kmsg.getMsgType(), kmsg.getClientType(), kmsg.getMsgID());
			long qpid = kmsg.readLong();
			long t = -1;
			KGamePlayer p2 = getplayeronlineordb(qpid);
			// 被禁言的玩家在线
			if (p2 != null) {
				long ct2 = System.currentTimeMillis();
				if (kmsg.getMsgID() == MID_QUERY_BAN) {
					t = p2.getBanEndtime() <= ct2 ? 0 : p2.getBanEndtime();
				} else {
					t = p2.getGagEndtime() <= ct2 ? 0 : p2.getGagEndtime();
				}
			}
			qresp.writeLong(qpid);
			qresp.writeLong(t);// 没查到
			playersession.send(qresp);
			break;
		case MID_GET_LOGIN_NOTICE_LIST:
			KGameMessage loginNoticeMsg = KGame.newMessage(kmsg.getMsgType(), kmsg.getClientType(), MID_GET_LOGIN_NOTICE_LIST);
			KGameNoticeManager.writeNoticeToMsgForGM(loginNoticeMsg);
			playersession.send(loginNoticeMsg);
			break;
		case MID_UPDATE_LOGIN_NOTICE:
			boolean modifyResult = KGameNoticeManager.updateNoticeList(kmsg);
			KGameMessage resultMsg = KGame.newMessage(kmsg.getMsgType(), kmsg.getClientType(), MID_UPDATE_LOGIN_NOTICE);
			resultMsg.writeBoolean(modifyResult);
			playersession.send(resultMsg);
			break;
		case MID_CHG_SERVER_TO_MAINTENANCE_STATUS:
			KGameFrontend.getInstance().getGSMgr().changeToMaintenanceStatus();
			break;
		case MID_CHG_SERVER_TO_OPEN_STATUS:
			KGameFrontend.getInstance().getGSMgr().changeToOpenStatus();
			break;
		case MID_UPDATE_STATUS_OF_SERVER:
			int gsId = kmsg.readInt();
			int status = kmsg.readInt();
			KGameFrontend.getInstance().getGSMgr().changeStatusOfServer(gsId, status);
			break;
		case MID_GET_STATUS_OF_ALL_SERVER: 
			{
				KGameMessage msg = KGame.newMessage(kmsg.getMsgType(), kmsg.getClientType(), MID_GET_STATUS_OF_ALL_SERVER);
				KGameFrontend.getInstance().getGSMgr().packAllServerStatusToMsg(msg);
				channel.write(msg);
			}
			break;

////////////////////////////////////////////////////////////////////////////////////////
			case 102:
				System.out.println("getEncryption=========="+kmsg.getEncryption());
				byte b = kmsg.readByte();
				short st = kmsg.readShort();
				int i = kmsg.readInt();
				long l = kmsg.readLong();
				String s = kmsg.readUtf8String();
				System.out.println(b);
				System.out.println(st);
				System.out.println(i);
				System.out.println(l);
				System.out.println(s);
				KGameMessage rmsg = KGameCommunication.newMessage(kmsg.getMsgType()	,kmsg.getClientType(), kmsg.getMsgID());
				rmsg.setEncryption(kmsg.getEncryption());
				rmsg.writeByte(b);
				rmsg.writeShort(st);
				rmsg.writeInt(i);
				rmsg.writeLong(l);
				rmsg.writeUtf8String(s);
				channel.write(rmsg);//do send
				break;
			case 101:
				System.out.println("getEncryption=========="+kmsg.getEncryption());
				float f = kmsg.readFloat();
				double d = kmsg.readDouble();
				boolean bb = kmsg.readBoolean();
				String ss = kmsg.readUtf8String();
				System.out.println(f);
				System.out.println(d);
				System.out.println(bb);
				System.out.println(ss);
				KGameMessage rmsg2 = KGameCommunication.newMessage(kmsg.getMsgType()	,kmsg.getClientType(), kmsg.getMsgID());
				rmsg2.setEncryption(kmsg.getEncryption());
				rmsg2.writeFloat(f);
				rmsg2.writeDouble(d);
				rmsg2.writeBoolean(bb);
				rmsg2.writeUtf8String(ss);
				channel.write(rmsg2);//do send
				break;
///////////////////////////////////////////////////////////////////////////////////
				
		default:
			break;
		}
	}

	private KGamePlayer getplayeronlineordb(long playerID) {
		KGamePlayer p = null;
		// ONLINE
		for (KGamePlayerSession ps : handshakedplayersessions.values()) {
			if (ps.getBoundPlayer() != null
					&& ps.getBoundPlayer().getID() == playerID) {
				return ps.getBoundPlayer();
			}
		}
		// DB
		try {
			DBPlayer dbp = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess().loadDBPlayer(playerID);
			if (dbp != null) {
				p = new KGamePlayer(dbp);
			}
		} catch (KGameDBException e) {
		}
		return p;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.exceptionCaught();
		logger.warn("exceptionCaught {},{}", e, e.getChannel().getAttachment());
		// super.exceptionCaught(ctx, e);
		e.getCause().printStackTrace();
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.channelOpen();
		// logger.debug("channelOpen {}",e.getChannel());
		super.channelOpen(ctx, e);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.channelConnected();
		logger.info("channelConnected {}", e.getChannel());
		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		FEStatusMonitor.commcounter.channelDisconnected();
		// logger.info("channelDisconnected {}", e.getChannel());
		super.channelDisconnected(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.channelClosed();

		Channel channel = e.getChannel();
		KGamePlayerSession playersession;
		if ((playersession = handshakedplayersessions.remove(channel)) != null) {
			logger.info("channelClosed.PlayerSession {}", playersession);
			// 保存Remark数据
			frontend.getPlayerManager().updatePlayerRemark(
					playersession.getBoundPlayer());
			frontend.getPlayerManager().updatePlayerAttribute(playersession.getBoundPlayer()); // 2014-09-02 保存Attribute
			// 告诉这个服排队的玩家
			GS gs = frontend.getGSMgr().getGS(playersession.getLastSelectGsId());
			if(gs != null) {
				gs.cancelLoginqueue(playersession);
			}
//			frontend.getGSMgr().notifyqueueaftersomeonelogout(
//					playersession.getLastSelectGsId());
		} else {
			// 如果是GS的channel
			GS gs = null;
			if ((gs = KGameGSManager.isGSChannel(channel)) != null) {
				KGameFrontend.getInstance().getGSMgr().gsClosed(gs);
				logger.info("channelClosed.GS {}", gs);
			} else {
				logger.info("channelClosed.null {}", channel);
			}

		}
		// super.channelClosed(ctx, e);
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.writeComplete(e.getWrittenAmount());
		super.writeComplete(ctx, e);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		FEStatusMonitor.commcounter.writeRequested();
		super.writeRequested(ctx, e);
	}

	static int getHandshakedPsSize() {
		return handshakedplayersessions.size();
	}

}
