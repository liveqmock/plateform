/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.gameserver;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

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
import com.koala.game.KGameModule;
import com.koala.game.KGamePlayerSessionListener;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameChannelAttachment;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameMessageEvent;
import com.koala.game.dataaccess.InvalidUserInfoException;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.exception.KGameServerException;
import com.koala.game.gameserver.paysupport.KGamePaymentSupport;
import com.koala.game.gameserver.paysupport.PayOrderIdGenerator;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayer;
import com.koala.game.player.KGamePlayerManager;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.tips.KGameTips;
import com.koala.game.tips.RespCode;
import com.koala.game.util.DateUtil;
import com.koala.game.util.StringUtil;
import com.koala.paymentserver.PayExtParam;
import com.koala.promosupport.PromoSupport;

/**
 * GS的通信层处理器。<br>
 * ·通道（连接）的建立、断开等事件，从而关系到通信通道{@link org.jboss.netty.channel.Channel}的缓存；<br>
 * ·收到底层消息（以{@link org.jboss.netty.buffer.ChannelBuffer}为载体）内容后，
 * 我们需要转换成特定的消息格式并根据消息类型作出处理（或平台层处理或传递到游戏逻辑层处理）；<br>
 * ·异常处理
 * 
 * @author AHONG
 */
public final class KGameServerHandler extends SimpleChannelHandler implements KGameProtocol {

	private static final KGameLogger logger = KGameLogger.getLogger(KGameServerHandler.class);
	// 临时的，握手后放入此缓存，登录成功后移除，并加入登录后的缓存
	public static final ConcurrentHashMap<Channel, KGamePlayerSession> handshakedplayersessions = new ConcurrentHashMap<Channel, KGamePlayerSession>();
	// 延迟移除的队列
	private final Map<Long, Integer> _delayQueue = new ConcurrentHashMap<Long, Integer>(); // key=playerId, value=断线原因
	private final int delayRemoveMillisSeconds; // 延迟移除的延迟时间
	private boolean delayRemoveEnable; // 是否允许延迟移除，当delayRemoveMillisSeconds>0的时候为true
	
	KGameServerHandler(int pDelayRemoveSeconds) {
		delayRemoveMillisSeconds = (int)TimeUnit.MILLISECONDS.convert(pDelayRemoveSeconds, TimeUnit.SECONDS);
		if (pDelayRemoveSeconds > 0) {
			KGame.newTimeSignal(new KDelayRemoveTask(), 30, TimeUnit.SECONDS);
			delayRemoveEnable = true;
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.messageReceived();
		if (!(e.getMessage() instanceof KGameMessage)) {
			// 非法消息，logging & disconnect
			logger.warn("Illegal Message Type!!" + e.getMessage());
			return;
		}
		
		Channel channel = e.getChannel();
		KGameMessage kmsg = (KGameMessage) e.getMessage();
		KGamePlayerSession playersession = null;

		KGameChannelAttachment channelAttachment = (KGameChannelAttachment) channel
				.getAttachment();// 每个登录后的Channel才有绑定的附件
		if (channelAttachment != null) {
			playersession = KGameServer.getInstance().getPlayerManager()
					.getPlayerSession(channelAttachment.getPlayerID());
		}

		// !!!注意!!!此时playersession有可能为null

		// logger.info("messageReceived:{} ", msgEvent);

		// 根据消息类型做处理
		if (kmsg.getMsgType() == KGameMessage.MTYPE_PLATFORM) {
			// 平台消息消息直接处理//////////////////////////////////////////////////////////

			// ///////////////////////////////////////////////////
			// 如果是支付服务器发来的消息，直接交由KGamePaymentSupport处理
			if (kmsg.getClientType() == KGameMessage.CTYPE_PAYMENT) {
				KGamePaymentSupport.getInstance()
						.messageReceived(channel, kmsg);
				return;
			}
			// ///////////////////////////////////////////////////

			int msgID = kmsg.getMsgID();
			switch (msgID) {

			/* 客户端心跳消息 */
			case MID_PING:
				if (playersession == null) {
					if ((playersession = handshakedplayersessions.get(channel)) == null) {
						channel.close();
						break;
					}
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
						KGameServer
								.getInstance()
								.getPlayerManager()
								.updatePlayerRemark(
										playersession.getBoundPlayer());
					}
					channelAttachment
							.setDisconnectedCause(CAUSE_PLAYEROUT_KICK);
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

			/* 握手消息，比较关键，每个连接必须先进行握手才是合法 */
			case MID_HANDSHAKE:
				String code = kmsg.readUtf8String();
				String clientmodel = kmsg.readUtf8String();
				logger.debug("MID_HANDSHAKE  {} {} {}", code, clientmodel,
						KGameServer.getInstance().getCurrentOnline());

				// TODO 握手消息要携带内容并且要考虑加密，通过验证的才是合法的客户端

				playersession = new KGamePlayerSession(channel);
				playersession.setClientType(kmsg.getClientType());
				playersession.setCurLoginDeviceModel(clientmodel);

				// 握手验证成功后加入临时缓存
				handshakedplayersessions.put(channel, playersession);

				KGameMessage handshakeResp = KGameCommunication.newMessage(
						kmsg.getMsgType(), kmsg.getClientType(), MID_HANDSHAKE);
				handshakeResp.writeUtf8String("Hi! I'm GS~ ");
				playersession.write(handshakeResp);
				GSStatusMonitor.commcounter.handshaked();
				break;

			/* 通过用户名登录GS */
			case MID_LOGIN_BY_NAME:
				KGameMessage loginresponsemsg = KGameCommunication.newMessage(
						KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(),
						MID_LOGIN_BY_NAME);
				RespCode plcode = new RespCode();
				// 登录的会话是从临时缓存拿的
				if ((playersession = handshakedplayersessions.get(channel)) == null) {
					plcode.set(PL_ILLEGAL_SESSION_OR_MSG, "PL_ILLEGAL_SESSION_OR_MSG");
					loginresponsemsg.writeInt(plcode.v);
					loginresponsemsg.writeUtf8String(KGameTips.get(plcode.k));
					channel.write(loginresponsemsg);
					break;
				}
				String loginname = kmsg.readUtf8String();
				String loginpassword = kmsg.readUtf8String();
				logger.debug("MID_LOGIN_BYPASSPORTNAME {},{}", loginname, loginpassword);
				DBPlayer dbplayer = null;
				plcode.set(PL_LOGIN_FAILED_NAMENOTEXIST, "NAMENOTEXIST");
				try {
					dbplayer = KGameServer.getInstance().getPlayerManager().login(loginname, loginpassword);
				} catch (InvalidUserInfoException e1) {
					logger.error("账号验证异常1！", e1);
				} catch (PlayerAuthenticateException e1) {
					if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
						plcode.set(PL_LOGIN_FAILED_NAMENOTEXIST, "NAMENOTEXIST");
					} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_WRONG_PASSWORD) {
						plcode.set(PL_LOGIN_FAILED_PASSWORDISWRONG, "PASSWORDISWRONG");
					}
					logger.error("账号验证异常2！", e1);
				} catch (KGameDBException e1) {
					plcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY, "PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
				}
				// 登录成功后要对playersession一些数据做设置
				if (dbplayer != null && playersession.decodeAndBindPlayer(dbplayer)) {

					playersession.setAuthenticationPassed(true);
					// 相关缓存操作
					KGamePlayerSession psPrevious = KGameServer.getInstance().getPlayerManager().putPlayerSession(playersession);
					handshakedplayersessions.remove(channel);

					// *如果相同账号本身就已经登录了，那需要踢出
					if (psPrevious != null && psPrevious.getChannel() != null) {
						Integer value = _delayQueue.remove(playersession.getBoundPlayer().getID());
						if (value == null) {
							// 如果不是延迟踢出的，则表示是顶掉别人的
							((KGameChannelAttachment) psPrevious.getChannel().getAttachment()).setDisconnectedCause(CAUSE_PLAYEROUT_OVERLAP);
							((KGameChannelAttachment) psPrevious.getChannel().getAttachment()).setOverlapedPlayersession(psPrevious);
//							KGameCommunication.sendDisconnect(psPrevious.getChannel(), psPrevious.getClientType(), CAUSE_PLAYEROUT_OVERLAP, KGameTips.get("CAUSE_PLAYEROUT_OVERLAP"), true);
							KGameCommunication.sendInfoClientOffline(psPrevious.getChannel(), psPrevious.getClientType(), KGameTips.get("CAUSE_PLAYEROUT_OVERLAP"));
							((KGameChannelAttachment) playersession.getChannel().getAttachment()).setOverlap(true); // 把自己设置为overlap，证明是顶掉别人的，这样，在重连的时候，就不会被别人顶掉，即被顶掉的客户端不会重连成功
						} else {
							// 如果是延迟踢出的，则要把上次的session关闭，通知逻辑做一些清除工作
							psPrevious.getChannel().setAttachment(null); // 移除这个之后，设置一下attachment
							psPrevious.getChannel().close(); // 其实channel已经关闭了
							firePlayerSessionEvent(psPrevious, FIRE_EVENT_LOGOUTED, CAUSE_PLAYEROUT_USEROP, true); // 把事件传递下去
						}
					}

					// 事件广播
					onPlayerLogined(playersession);

					KGamePlayer p = playersession.getBoundPlayer();

					// 记录最后一次登录的GSID
					if (p != null) {
						p.setLastLoginedGSID(KGameServer.getInstance().getGSID());
					}

					// 检测上次登录的设备是否一致
					String lastloginmodel = p == null ? null : p.setLastLoginDeviceModel(playersession.getCurLoginDeviceModel());

					plcode.set(PL_LOGIN_SUCCEED, "PL_LOGIN_SUCCEED");
					loginresponsemsg.writeInt(plcode.v);
					loginresponsemsg.writeLong(dbplayer.getPlayerId());
					loginresponsemsg.writeUtf8String((lastloginmodel != null && lastloginmodel.trim().length() > 0) ? KGameTips.get("login_safe_warning",
							DateUtil.format(new Date(playersession.getBoundPlayer().getLastestLoginTimeMillis()), KGameTips.DATE_FORMAT_TIPS), lastloginmodel) : KGameTips.get(plcode.k));

					GSStatusMonitor.commcounter.logined();
				} else {
					logger.error("账户：{}的dbplayer为null！！！", loginname);
					loginresponsemsg.writeInt(plcode.v);
					loginresponsemsg.writeUtf8String(KGameTips.get(plcode.k));
				}
				playersession.send(loginresponsemsg);
				break;

			/* 断线重连 */
			case MID_RECONNECT:
				KGameMessage reconnResp = KGameCommunication.newMessage(KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(), MID_RECONNECT);
				
				String rcode = kmsg.readUtf8String();
				String rclientmodel = kmsg.readUtf8String();
				String rloginname = kmsg.readUtf8String();
				String rloginpassword = kmsg.readUtf8String();
				
				logger.info("客户端请求断线重连！！账号：{}", rloginname);
				
				KGamePlayerSession existsSession = KGame.getPlayerManager().getPlayerSession(rloginname.toLowerCase());
				boolean allow = true;
				if(existsSession != null) {
					if (existsSession.getChannel().getAttachment() != null && ((KGameChannelAttachment) existsSession.getChannel().getAttachment()).isOverLap()) {
						logger.info("断线重连！！账号：{}是被顶替的，所以不允许重连", rloginname);
						allow = false;
					}
				} else {
					allow = false;
				}
				
				if (allow) {
					// 曾经登录过才允许重连

//					logger.debug("MID_RECONNECT MID_HANDSHAKE  {} {} {}", rcode, rclientmodel, KGameServer.getInstance().getCurrentOnline());
					logger.info("MID_RECONNECT MID_HANDSHAKE  {} {} {}", rcode, rclientmodel, KGameServer.getInstance().getCurrentOnline());

					// TODO 握手消息要携带内容并且要考虑加密，通过验证的才是合法的客户端

//					playersession = new KGamePlayerSession(channel);
//					playersession.setClientType(kmsg.getClientType());
//					playersession.setCurLoginDeviceModel(rclientmodel);
					KGameChannelAttachment attachment = (KGameChannelAttachment) existsSession.getChannel().getAttachment();
					attachment.setDisconnectedCause(CAUSE_PLAYEROUT_RECONNECT);
					existsSession.getChannel().setAttachment(null); // 把附件设置为null，这样在channelClosed的时候，就不用做什么事情了
					existsSession.getChannel().close(); // 关闭原来的channel（其实这里应该不用再关闭了，不过这里为了以防上一个channel未关闭）
					
					playersession = existsSession; // 用回之前的playersession
					playersession.bindChannel(channel);
					playersession.setDisconnectTime(0); // 重置一下disconnectTime
					_delayQueue.remove(playersession.getBoundPlayer().getID());
					
					
					// 握手验证成功后加入临时缓存，防止未做完断线重连，但是ping消息来了
					handshakedplayersessions.put(channel, playersession);

					// ==================通过用户名登录GS=================
					RespCode rplcode = new RespCode();
					logger.debug("MID_RECONNECT MID_LOGIN_BYPASSPORTNAME {},{}", rloginname, rloginpassword);
					DBPlayer rdbplayer = null;
					rplcode.set(PL_RECONNECT_FAILED_VERIFY, "PL_RECONNECT_FAILED_VERIFY");
					try {
						rdbplayer = KGameServer.getInstance().getPlayerManager().login(rloginname, rloginpassword);
					} catch (InvalidUserInfoException e1) {
						// login不应该出现这个异常
					} catch (PlayerAuthenticateException e1) {
						rplcode.set(PL_RECONNECT_FAILED_VERIFY, "PL_RECONNECT_FAILED_VERIFY");
					} catch (KGameDBException e1) {
						rplcode.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY, "PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
					}
					// 登录成功后要对playersession一些数据做设置
					if (rdbplayer != null && playersession.decodeAndBindPlayer(rdbplayer)) {

						// 检测是否有封号
						long banEndtime = playersession.getBoundPlayer().getBanEndtime();
						if (banEndtime > 0 && banEndtime > System.currentTimeMillis()) {
							reconnResp.writeInt(PL_RECONNECT_FAILED_VERIFY);
							// reconnResp.writeUtf8String(KGameTips.get("BAN",
							// DateUtil.formatReadability(new
							// Date(banEndtime))));
							playersession.send(reconnResp);
							break;
						}

						playersession.setAuthenticationPassed(true);
//						// 相关缓存操作  // 2014-08-15 注释掉这行代码，因为session会重用，所以没有必要在put一次
//						KGamePlayerSession psPrevious = KGameServer.getInstance().getPlayerManager().putPlayerSession(playersession);
						// 移除前面handshake的缓存
						handshakedplayersessions.remove(channel);

//						// *如果相同账号本身就已经登录了，那需要踢出  // 2014-08-15 注释掉以下代码，已经不需要在这里做操作，前面已经作了channel相关的操作
//						if (psPrevious != null && psPrevious.getChannel() != null) {
//							/*
//							 * 2014-07-29 修改，如果这里不把channel关闭，后面的会出现这个channel idle的情况
//							 * 原因是netty会缓存这个channel，并且channel的ChannelPipeLine里面有绑定一个
//							 * IdelHandler（详情参见KGameServerChannelPipelineFactory），而客户端后续来的消息
//							 * 已经不是本来的channel（因为客户端重连的时候，netty会检测到channel connect，这里
//							 * 应该就是创建了一个新的channel，所以旧的channel就相当于被丢弃了）。
//							 * 但是因为在平台的逻辑，是通过channel的attachment，也就是一个playerId去找回对应的
//							 * playerSession，所以当idle的时候，因为新的channel也是绑定这个id，会把现有的channel
//							 * 所关联的playerSession也关闭掉
//							 */
//							// ((KGameChannelAttachment)psPrevious.getChannel().getAttachment()).setDisconnectedCause(CAUSE_PLAYEROUT_OVERLAP);
//							// ((KGameChannelAttachment)psPrevious.getChannel().getAttachment()).setOverlapedPlayersession(psPrevious);
//							KGameChannelAttachment attachment = (KGameChannelAttachment) psPrevious.getChannel().getAttachment();
//							attachment.setDisconnectedCause(CAUSE_PLAYEROUT_RECONNECT);
//							psPrevious.getChannel().setAttachment(null); // 把附件设置为null，这样在channelClosed的时候，就不用做什么事情了
//							psPrevious.getChannel().close();
//						}
						// 2014-08-15 END

						// 事件广播
						firePlayerSessionEvent(playersession, FIRE_EVENT_RECONNECTED, -1, true);

						KGamePlayer p = playersession.getBoundPlayer();

						// 记录最后一次登录的GSID
						if (p != null) {
							p.setLastLoginedGSID(KGameServer.getInstance().getGSID());
						}

						rplcode.set(PL_RECONNECT_SUCCEED, "PL_RECONNECT_SUCCEED");
						reconnResp.writeInt(rplcode.v);
						reconnResp.writeLong(rdbplayer.getPlayerId());
						// reconnResp.writeUtf8String(KGameTips.get(rplcode.k));
					} else {
						reconnResp.writeInt(rplcode.v);
						// reconnResp.writeUtf8String(KGameTips.get(rplcode.k));
					}
					playersession.send(reconnResp);
				} else {
					logger.info("断线重连，账号[{}]并没有登录记录，重连失败！", rloginname);
					reconnResp.writeInt(PL_RECONNECT_FAILED_VERIFY);
					channel.write(reconnResp); // 直接调用channel的write方法，因为没有创建PlayerSession
				}
				break;

			/* 登出GS */
			case MID_LOGOUT:
				// set the out cause
				if (channelAttachment != null) {
					channelAttachment
							.setDisconnectedCause(CAUSE_PLAYEROUT_USEROP);
				}
				// close this channel
				channel.close();
				break;

			/* 系统公告 */
			case MID_SYSTEM_NOTICE:
				String notice = kmsg.readUtf8String();
				KGameMessage sysnoticemsg = KGameCommunication
						.newSystemNoticeMsg(notice);
				KGameServer.getInstance().getPlayerManager()
						.broadcast(sysnoticemsg);
				break;

			/* 服务器关闭for test */
			case MID_SHUTDOWN:// XXX test shutdown
				String passwd = kmsg.readUtf8String();
				if (passwd != null && passwd.equals(KEY_SHUTDOWN_GS)) {
					KGameServer.getInstance().shutdown(channel);
				} else {
					logger.error("停机消息，密码错误！");
				}
				break;

			/* 获取GS服务器状态，可以是任意客户端 */
			case MID_SERVERSTATUS:
				KGameMessage sstatusresponse = KGameCommunication.newMessage(
						KGameMessage.MTYPE_PLATFORM, kmsg.getClientType(),
						MID_SERVERSTATUS);
				if (kmsg.readInt() == 520520) {
					sstatusresponse.writeUtf8String(KGameServer.getInstance()
							.getStatusMonitor().toString());
					channel.write(sstatusresponse);
				} else {// 白撞直接关闭！
					channel.close();
				}
				break;

			/* 测试消息 */
			case MID_DEBUG:// XXX DEBUG消息解析发送
				String debugbc = kmsg.readUtf8String();
				KGameMessage debugresp = KGameCommunication.newMessage(
						kmsg.getMsgType(), kmsg.getClientType(), MID_DEBUG);
				debugresp.writeUtf8String(debugbc);
				// broadcast2AllLogined(debugresp);
				channel.write(debugresp);
				break;

			// //////////////////////////////////////////////////////////////////
			case MID_PAY_BEFORE:
				int promoid = kmsg.readInt();
				long roleid = kmsg.readLong();
				String rolename = kmsg.readUtf8String();

				KGameMessage paybefore = KGameCommunication
						.newMessage(kmsg.getMsgType(), kmsg.getClientType(),
								MID_PAY_BEFORE);
				//第一个写入promoid
				paybefore.writeInt(promoid);

				// 未登录
				if (playersession == null) {
					paybefore.writeInt(PL_ILLEGAL_SESSION_OR_MSG);
					paybefore.writeUtf8String(KGameTips
							.get("PL_ILLEGAL_SESSION_OR_MSG"));
					channel.write(paybefore);
					break;
				}

				// 检查promoid是否已经支持?
				if (!KGamePaymentSupport.getInstance().isSupportedPromoChannel(
						promoid)) {
					// 未支持的渠道ID
					paybefore.writeInt(PL_PAY_BEFORE_FAILED_PROMONOSUPPORT);
					paybefore.writeUtf8String(KGameTips
							.get("PL_PAY_BEFORE_FAILED_PROMONOSUPPORT",promoid,roleid));
					channel.write(paybefore);
					break;
				}

				int responsecode = PL_PAY_BEFORE_SUCCEED;
				String paybeforetips = "";

				paybefore.writeInt(responsecode);
				paybefore.writeUtf8String(paybeforetips);
				
				//所需参数（不同promoid可能有其它不同的参数），如果是orderId或ext参数必须每次都重新生成
				Map<String, String> params2c = KGamePaymentSupport
						.getInstance().getParamsToClientBeforePay(promoid);
				//是否打开我们自定义的价格对话框
				String _openpriceui = params2c.get("openpriceui");
				boolean openpriceui = (_openpriceui == null || _openpriceui
						.length() < 1) ? false : Boolean
						.parseBoolean(_openpriceui);
				paybefore.writeBoolean(openpriceui);
				// 发到客户端
				paybefore.writeInt(params2c.size());
				for (String k : params2c.keySet()) {
					paybefore.writeUtf8String(k);
					// 'orderId'参数每次是新生成的
					if (CONST_PAYMENT_KEY_ORDERID.equals(k)) {
						//【有个手盟渠道没有ext设置，只能用自定义订单号来顶上，特别处理】
						if(PromoSupport.computeParentPromoID(promoid)==1202){
							paybefore.writeUtf8String(new PayExtParam(promoid,
									KGameServer.getInstance().getGSID(), roleid/*,
									playersession.getBoundPlayer().getPromoMask()*/)
									.toString());
						}else{
							paybefore.writeUtf8String(PayOrderIdGenerator.gen(
									promoid, roleid));
						}
					}
					// 'ext'参数也是每次都要新生成的
					else if (CONST_PAYMENT_KEY_EXT.equals(k)) {
						paybefore.writeUtf8String(new PayExtParam(promoid,
								KGameServer.getInstance().getGSID(), roleid/*,
								playersession.getBoundPlayer().getPromoMask()*/)
								.toString());
					}
					// 'appUserName'参数也是每次都要新生成的
					else if("appUserName".equals(k)){
						paybefore.writeUtf8String(rolename);
					}
					// 'appUserId'参数也是每次都要新生成的
					else if("appUserId".equals(k)){
						paybefore.writeUtf8String(String.valueOf(roleid));
					}
					//有特别渠道（如baoruan）的notifyUrl中要直接带ext的
					else if(PROMO_KEY_NOTIFYURL.equalsIgnoreCase(k)){
						String vv= params2c.get(k);
						if((!StringUtil.hasNullOr0LengthString(vv))&&vv.endsWith("ext=")){
							vv = (new StringBuilder().append(vv).append((new PayExtParam(promoid,
									KGameServer.getInstance().getGSID(), roleid/*,
									playersession.getBoundPlayer().getPromoMask()*/)
									.toString()))).toString();
						}
						paybefore.writeUtf8String(vv);
					}
					//昆仑的serverId我们之间就是用gsid的
					else if("serverId".equalsIgnoreCase(k)){
						String vv= params2c.get(k);
						if(vv==null||vv.length()==0){
							paybefore.writeUtf8String(String.valueOf(KGameServer.getInstance().getGSID()));
						}else{
							paybefore.writeUtf8String(vv);
						}
					}
					//其它正常参数
					else {
						String vv= params2c.get(k);
						paybefore.writeUtf8String(vv);
					}
				}

				playersession.send(paybefore);

				break;
			// //////////////////////////////////////////////////////////////////

			/* 其它没处理到的消息最大的可能是其它平台模块（如GS系统）的消息 */
			default:
				// TODO GM等模块的消息的分发
				break;
			}
		} else {
			// 游戏逻辑消息分发//////////////////////////////////////////////////////////

			// 先判断playersession是否为null，为null属于不合法的消息
			if (playersession == null) {
				logger.warn("Illegal session msg before deliver, close it {}.",
						channel);
				channel.close();
				return;//FIXED@20130723
			}

			// 开始做分发
			KGameMessageEvent msgEvent = new KGameMessageEvent(playersession,
					kmsg);
			// 直接做分发处理即可，因为内部已经设置了ExecutionHandler，代表本方法已经是多线程执行
			for (Iterator<KGameModule> ms = KGameServer.getInstance()
					.iteratorModules(); ms.hasNext();) {
				KGameModule m = ms.next();
				if (m.messageReceived(msgEvent)) {
					break;
				}
			}
			msgEvent = null;
		}

		// super.messageReceived(ctx, e);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.channelOpen();
		// Channel channel = ctx.getChannel();
		// boolean added = KGameServer.getInstance().getCommunication()
		// .getAllChannels().add(channel);
		// // logging
		// logger.info("channelOpen " + added + e.getChannel());

		super.channelOpen(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		GSStatusMonitor.commcounter.channelClosed();

		Channel channel = ctx.getChannel();
		if (channel == null) {
			return;
		}
		// boolean removed = KGameServer.getInstance().getCommunication()
		// .getAllChannels().remove(channel);

		// // logging
		// logger.info("channelClosed " + removed + e.getChannel());

		KGamePlayerSession playersession = null;
		int cause = CAUSE_PLAYEROUT_UNKNOWN;
		KGameChannelAttachment channelAttachment = (KGameChannelAttachment) channel.getAttachment();// 每个登录后的Channel才有绑定的附件
		if (channelAttachment != null) {
			// 【注#1】：如果是被“踢掉”的时候通过ID获取的PS是“后来者”的PS对象，所以如果移除掉就明显的不正确
			cause = channelAttachment.getDisconnectedCause();
			if (cause == CAUSE_PLAYEROUT_OVERLAP) {
				playersession = channelAttachment.getOverlapedPlayersession();
			} else {
				playersession = KGameServer.getInstance().getPlayerManager().getPlayerSession(channelAttachment.getPlayerID());
			}
		}
		if (KGameServer.getInstance().getCommunication().isStopped()) {
			cause = CAUSE_PLAYEROUT_SERVERSHUTDOWN;
		}
		switch (cause) {
		// 2014-08-15 16:57 如果是因为以下三种情况关闭，则延迟移除session，以防客户端在短时间内重连
		case CAUSE_PLAYEROUT_UNKNOWN:
		case CAUSE_PLAYEROUT_IDLE:
		case CAUSE_PLAYEROUT_DISCONNECT:
		case CAUSE_PLAYEROUT_EXCEPTION:
			if (delayRemoveEnable && playersession != null && playersession.getBoundPlayer() != null) {
				playersession.setDisconnectTime(System.currentTimeMillis());
				long playerId = playersession.getBoundPlayer().getID();
				_delayQueue.put(playerId, cause);
				logger.info("CHANNEL CLOSED({}). {}，延迟移除！", outcause2string(cause), playersession);
				if(channelAttachment != null && channelAttachment.isOverLap()) {
					channelAttachment.setOverlap(false); // 设置成不是overlap，否则自己在重连的时候会不成功
				}
				// 保存一次attribute，主要就是为了保存device，如果不是，下次重连的时候，还是老的device，会不断弹出warning
				KGame.getPlayerManager().updatePlayerAttribute(playersession.getBoundPlayer()); 
				KGameServer.getInstance().getGS2FE().sendDelayRemove(playerId);
				return;
			}
			break;
		}
		// 其他情况自己关闭
		this.closePlayerSession(playersession, channel, cause, true);
	}
	
	private void closePlayerSession(KGamePlayerSession playersession, Channel channel, int cause, boolean saveAttribute) {
		// a、如果是已登陆玩家，需要通知游戏逻辑层、根据其断开时的状态通知其他玩家、并作必要的数据持久化处理，从相关缓存中移除对象
		// b、如果是非登陆玩家，只需要根据其当时状态作出相应的处理，从相关缓存中移除对象
		// if (playersession != null) {
		if (playersession != null && playersession.getChannel() == channel) { // 2014-07-30  修改 playerSession中的channel相同，才往下做操作
			// 通知所有监听器
			this.firePlayerSessionEvent(playersession, FIRE_EVENT_LOGOUTED, cause, true);
			boolean bRemoved = false;
			KGamePlayer player = playersession.getBoundPlayer();
			if (player != null) {
				if (cause != CAUSE_PLAYEROUT_OVERLAP) {// 这个判断就是为了避免上面【注#1】的情况
					bRemoved = KGameServer.getInstance().getPlayerManager().removePlayerSession(player.getID(), playersession);
				}
				// save it
				if (saveAttribute) {
					KGameServer.getInstance().getPlayerManager().updatePlayerAttribute(player);
				}

				// 通知数据库登出，记录流水等
				try {
					KGameDataAccessFactory.getInstance().getPlayerManagerDataAccess().logout(player.getID());
				} catch (PlayerAuthenticateException ex) {
					ex.printStackTrace();
				} catch (KGameDBException ex) {
					ex.printStackTrace();
				}
			}
			logger.info("LOGOUTED1({}). removed {} ,{}", outcause2string(cause), bRemoved, playersession);
		}
		// 看握手的临时缓存有没有需要移除的对象
		KGamePlayerSession psRemoved = handshakedplayersessions.remove(channel);
		if (psRemoved != null) {
			logger.info("LOGOUTED2({}). removed {} ,{}", outcause2string(cause), psRemoved, playersession);
		}

		// super.channelClosed(ctx, e);
	}

	private String outcause2string(int cause) {
		switch (cause) {
		case CAUSE_PLAYEROUT_UNKNOWN:
			return "UNKNOWN";
		case CAUSE_PLAYEROUT_USEROP:
			return "USEROP";
		case CAUSE_PLAYEROUT_DISCONNECT:
			return "DISCONNECT";
		case CAUSE_PLAYEROUT_IDLE:
			return "IDLE";
		case CAUSE_PLAYEROUT_KICK:
			return "KICK";
		case CAUSE_PLAYEROUT_SERVERSHUTDOWN:
			return "SERVERSHUTDOWN";
		case CAUSE_PLAYEROUT_EXCEPTION:
			return "EXCEPTION";
		case CAUSE_PLAYEROUT_OVERLAP:
			return "OVERLAP";
		case CAUSE_PLAYEROUT_RECONNECT:
			return "RECONNECT";
		}
		return String.valueOf(cause);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.channelConnected();

		// logging
		logger.debug("channelConnected " + e.getChannel());

		// KGameChannelAttachment katt = (KGameChannelAttachment) e.getChannel()
		// .getAttachment();
		// KGamePlayerSession kps = null;
		// if (katt != null) {
		// kps = KGameServer.getInstance().getPlayerManager()
		// .getPlayerSession(katt.getPlayerID());
		// }
		// if (kps == null) {// 未加入缓存
		// kps = new KGamePlayerSession(e.getChannel());
		// // TODO channelConnected()但未登陆的PlayerSession是否加入缓存？？
		// }

		super.channelConnected(ctx, e);
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		GSStatusMonitor.commcounter.channelDisconnected();

		// set the out cause
		Channel channel = e.getChannel();
		KGameChannelAttachment channelAttachment = (KGameChannelAttachment) channel
				.getAttachment();// 每个登录后的Channel才有绑定的附件
		if (channelAttachment != null) {
			if (channelAttachment.getDisconnectedCause() == CAUSE_PLAYEROUT_UNKNOWN) {
				channelAttachment
						.setDisconnectedCause(CAUSE_PLAYEROUT_DISCONNECT);
			}
		}

		// super.channelDisconnected(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.exceptionCaught();

		Channel channel = e.getChannel();

		// logging
		logger.warn("Unexpected exception from downstream. {},{}", channel,
				e.getCause());

		// TODO exceptionCaught时对channel的处理
		// set the out cause
		KGameChannelAttachment channelAttachment = (KGameChannelAttachment) channel
				.getAttachment();// 每个登录后的Channel才有绑定的附件
		if (channelAttachment != null) {
			if (channelAttachment.getDisconnectedCause() == CAUSE_PLAYEROUT_UNKNOWN) {
				channelAttachment
						.setDisconnectedCause(CAUSE_PLAYEROUT_EXCEPTION);
			}
		}
		// close this channel??
		 channel.close();

		KGameServerException kexc = new KGameServerException(e.getCause());
		for (Iterator<KGameModule> imodules = KGameServer.getInstance()
				.iteratorModules(); imodules.hasNext();) {
			KGameModule m = imodules.next();
			m.exceptionCaught(kexc);
		}

		// super.exceptionCaught(ctx, e);
		e.getCause().printStackTrace();
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.writeComplete(e.getWrittenAmount());
		super.writeComplete(ctx, e);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		GSStatusMonitor.commcounter.writeRequested();
		super.writeRequested(ctx, e);
	}

	private static final byte FIRE_EVENT_LOGINED = 0;
	private static final byte FIRE_EVENT_LOGOUTED = 1;
	private static final byte FIRE_EVENT_DISCONNECTED = 2;
	private static final byte FIRE_EVENT_RECONNECTED = 3;

	private void firePlayerSessionEvent(KGamePlayerSession playerSession,
			byte event, int cause, boolean butme) {
		for (Iterator<KGameModule> ms = KGameServer.getInstance()
				.iteratorModules(); ms.hasNext();) {
			KGameModule module = ms.next();
			// if (butme && (module == this)) {
			// continue;
			// }
			if (module instanceof KGamePlayerSessionListener) {
				KGamePlayerSessionListener listener = (KGamePlayerSessionListener) module;
				switch (event) {
				case FIRE_EVENT_LOGINED:
					listener.playerLogined(playerSession);
					break;
				case FIRE_EVENT_DISCONNECTED:
					listener.playerSessionDisconnected(playerSession, cause);
					break;
				case FIRE_EVENT_LOGOUTED:
					listener.playerLogouted(playerSession, cause);
					break;
				case FIRE_EVENT_RECONNECTED:
					listener.playerSessionReconnected(playerSession);
					break;
				default:
					break;
				}
			}
		}
	}

	private void onPlayerLogined(KGamePlayerSession playersession) {
		logger.info("LOGINED.{}", playersession);

		// 通知所有监听器
		this.firePlayerSessionEvent(playersession, FIRE_EVENT_LOGINED, -1, true);
	}

	int getHandshakedPsSize() {
		return handshakedplayersessions.size();
	}

	public void broadcast2AllConnected(KGameMessage msg) {
		for (KGamePlayerSession ps : handshakedplayersessions.values()) {
			ps.send(msg.duplicate());
		}
		KGameServer.getInstance().getPlayerManager().broadcast(msg);
	}

	public void broadcast2AllLogined(KGameMessage msg) {
		KGameServer.getInstance().getPlayerManager().broadcast(msg);
	}
	
	private class KDelayRemoveTask implements KGameTimerTask {

		@Override
		public String getName() {
			return "KDelayRemoveTask";
		}

		@Override
		public Object onTimeSignal(KGameTimeSignal timeSignal) throws KGameServerException {
			if (KGameServerHandler.this._delayQueue.size() > 0) {
				// 如果队列>0的时候才进入这个循环
				long currentTime = System.currentTimeMillis();
				int allowMax = KGameServerHandler.this.delayRemoveMillisSeconds;
				Map<Long, Integer> tempQueue = KGameServerHandler.this._delayQueue;
				Map.Entry<Long, Integer> entry;
				KGamePlayerSession session;
				for (Iterator<Map.Entry<Long, Integer>> itr = tempQueue.entrySet().iterator(); itr.hasNext();) {
					entry = itr.next();
					session = KGame.getPlayerManager().getPlayerSession(entry.getKey());
					try {
						if (session != null) {
							if (currentTime - session.getDisconnectTime() >= allowMax) {
								KGameServerHandler.this.closePlayerSession(session, session.getChannel(), entry.getValue(), false);
								KGameServer.getInstance().getGS2FE().sendReallyRemove(entry.getKey());
								itr.remove();
							}
						} else {
							itr.remove();
						}
					} catch (Exception e) {
						logger.error("检查超时队列出错！playerId={}", entry.getKey(), e);
					}
				}
			}
			timeSignal.getTimer().newTimeSignal(this, 30, TimeUnit.SECONDS);
			return "SUCCESS";
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
			
		}

		@Override
		public void rejected(RejectedExecutionException e) {
			
		}
		
	}
}
