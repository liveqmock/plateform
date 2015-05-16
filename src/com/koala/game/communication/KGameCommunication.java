/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.communication;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;
import org.jdom.Element;

import com.koala.game.KGameMessage;
import com.koala.game.KGameMessagePayload;
import com.koala.game.KGameProtocol;
import com.koala.game.KGameServerType;
import com.koala.game.exception.KGameServerException;
import com.koala.game.frontend.KGameFrontendHandler;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.gameserver.KGameServerHandler;
import com.koala.game.logging.KGameLogger;
import com.koala.game.tips.KGameTips;

/**
 * KGAME平台通信模块
 * 
 * @author AHONG
 */
public final class KGameCommunication implements KGameProtocol {

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameCommunication.class);

	private final AtomicBoolean shutdown = new AtomicBoolean();
	private Channel serverChannel;// 服务器绑定某端口的通信通道
	// private final ChannelGroup allChannels;// 所有通信通道
	private ServerBootstrap bootstrap;
	private ChannelHandler handler;
	private KGameServerType serverType;
	private String wanIP;
	private InetSocketAddress serverAddress;
	private int allowedConnect, allowedOnline;
	private boolean inited;
	//public int allowPingPerMin;

	private final static KGameMessage PING_MSG = new KGameMessageImpl(
			KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_ANDROID,
			KGameProtocol.MID_PING);

	public KGameCommunication(KGameServerType serverType,
			ServerBootstrap bootstrap, ChannelHandler handler) {
		this.serverType = serverType;
		this.bootstrap = bootstrap;
		this.handler = handler;
		// allChannels = new DefaultChannelGroup(serverType.getName());  
		// logger.info("Created ChannelGroup: {}", allChannels.getName());
	}

	public void init(Element eNetwork) throws KGameServerException {

		if (shutdown.get()) {
			throw new IllegalStateException("cannot be started once stopped");
		}

		String lanIP = eNetwork.getChildTextTrim("LanIP");
		String _wanIP = eNetwork.getChildTextTrim("WanIP");
		wanIP = _wanIP == null ? lanIP : _wanIP;
		int port = Integer.parseInt(eNetwork.getChildTextTrim("SocketPort"));
		serverAddress = new InetSocketAddress(lanIP, port);

		allowedConnect = Integer.parseInt(eNetwork
				.getChildTextTrim("AllowedConnect"));
		allowedOnline = Integer.parseInt(eNetwork
				.getChildTextTrim("AllowedOnline"));

		// /////////////////////////////////////////////////////////////////
		Element eIdle = eNetwork.getChild("Idle");
		Element eWheelTimer = eIdle.getChild("WheelTimer");
		int tickDurationSeconds = Integer.parseInt(eWheelTimer
				.getAttributeValue("tickDurationSeconds"));
		int ticksPerWheel = Integer.parseInt(eWheelTimer
				.getAttributeValue("ticksPerWheel"));
		Timer timer = new HashedWheelTimer(tickDurationSeconds,
				TimeUnit.SECONDS, ticksPerWheel);// 检测Timeout
		int readerIdleTimeSeconds = Integer.parseInt(eIdle
				.getAttributeValue("readerIdleTimeSeconds"));
		int writerIdleTimeSeconds = Integer.parseInt(eIdle
				.getAttributeValue("writerIdleTimeSeconds"));
		int allIdleTimeSeconds = Integer.parseInt(eIdle
				.getAttributeValue("allIdleTimeSeconds"));
		IdleStateHandler idleStateHandler = new IdleStateHandler(timer,
				readerIdleTimeSeconds, writerIdleTimeSeconds,
				allIdleTimeSeconds);
		//allowPingPerMin =Integer.parseInt(eIdle.getChildTextTrim("AllowPingPerMin"));

		// /////////////////////////////////////////////////////////////////
		Element eExecutionHandler = eNetwork.getChild("ExecutionHandler");
		int corePoolSize = Integer.parseInt(eExecutionHandler
				.getAttributeValue("corePoolSize"));
		long maxChannelMemorySize = Long.parseLong(eExecutionHandler
				.getAttributeValue("maxChannelMemorySize"));
		long maxTotalMemorySize = Long.parseLong(eExecutionHandler
				.getAttributeValue("maxTotalMemorySize"));
		long keepAliveTimeMillis = Long.parseLong(eExecutionHandler
				.getAttributeValue("keepAliveTimeMillis"));
		// 一个专门用来处理服务器收到的消息（Upstream）的线程池，目的就是提高worker线程的效率
		ExecutionHandler executionHandler = new ExecutionHandler(
				new OrderedMemoryAwareThreadPoolExecutor(corePoolSize,
						maxChannelMemorySize, maxTotalMemorySize,
						keepAliveTimeMillis, TimeUnit.MILLISECONDS));

		// //////////////////////////////////////////////////////////////////
		bootstrap.setPipelineFactory(new KGameServerChannelPipelineFactory(
				this, handler, executionHandler, idleStateHandler));
		// 一些socket参数设置
		Element eOptions = eNetwork.getChild("Options");
		List eOptionList = eOptions.getChildren("Option");
		for (Iterator iterator = eOptionList.iterator(); iterator.hasNext();) {
			Element eOption = (Element) iterator.next();
			bootstrap.setOption(eOption.getAttributeValue("key"),
					eOption.getAttributeValue("value"));
		}

//		// //////////////////////////////////////////////////////////////////
//		// 绑定端口，顺便记录服务器本身的Channel
//		serverAddress = new InetSocketAddress(lanIP, port);
//		try {
//			serverChannel = bootstrap.bind(serverAddress);
//		} catch (ChannelException e) {
//			throw new KGameServerException("socket bind failed! " + lanIP + ":"
//					+ port);
//		}
//		// allChannels.add(serverChannel);
//
//		logger.info("{} bind {} ", serverType.getName(), serverAddress);
//
//		logger.info("{}'s server channel: {}", serverType.getName(),
//				serverChannel);
//		logger.info(
//				"{}'s Communication '{}' Start OK! AllowedConnect {} ,AllowedOnline {}",
//				serverType.getName(), this, allowedConnect, allowedOnline);
		inited = true;
	}
	
	public void start() throws KGameServerException{
		if (shutdown.get()) {
			throw new IllegalStateException("cannot be started once stopped");
		}
		if (!inited) {
			throw new IllegalStateException("must inited before start.");
		}
		// //////////////////////////////////////////////////////////////////
		// 绑定端口，顺便记录服务器本身的Channel
		try {
			serverChannel = bootstrap.bind(serverAddress);
		} catch (ChannelException e) {
			throw new KGameServerException("socket bind failed! " + serverAddress.toString());
		}
		// allChannels.add(serverChannel);

		logger.info("{} bind {} ", serverType.getName(), serverAddress);

		logger.info("{}'s server channel: {}", serverType.getName(),
				serverChannel);
		logger.info(
				"{}'s Communication '{}' Start OK! AllowedConnect {} ,AllowedOnline {}",
				serverType.getName(), this, allowedConnect, allowedOnline);
	}

	public void stop() {
		if (!shutdown.compareAndSet(false, true)) {
			throw new IllegalStateException("communication stop repeated call.");
		}

		// 关闭服务器通道
		logger.warn("serverChannel close ...");
		ChannelFuture scfuture = serverChannel.close().awaitUninterruptibly();
		logger.warn("serverChannel close success?{} ", scfuture.isSuccess());

		// 通知并关闭所有客户端连接
		if (serverType.getType() == KGameServerType.SERVER_TYPE_FE) {
			String shutdownnotice = KGameTips.get("ShutdownNotice");
			KGameMessage shutdownnoticemsg = null;
			if (shutdownnotice != null && shutdownnotice.length() > 0) {
				shutdownnoticemsg = newSystemNoticeMsg(shutdownnotice);
			}
			// KGameFrontendHandler feh = (KGameFrontendHandler) handler;
			logger.warn("close all handshaked sessions... {}",
					KGameFrontendHandler.handshakedplayersessions.size());
			// 关闭已经连接并握手成功的会话
			for (Channel c : KGameFrontendHandler.handshakedplayersessions
					.keySet()) {
				if (shutdownnoticemsg != null) {
					c.write(shutdownnoticemsg.duplicate()).addListener(
							ChannelFutureListener.CLOSE);
				} else {
					c.close();
				}
			}
		} else if (serverType.getType() == KGameServerType.SERVER_TYPE_GS) {
			String shutdownnotice = KGameTips.get("ShutdownNotice");
			KGameMessage shutdownnoticemsg = null;
			if (shutdownnotice != null && shutdownnotice.length() > 0) {
				shutdownnoticemsg = newSystemNoticeMsg(shutdownnotice);
			}
			// ///////////////////////////////////////////////////
			// 关闭已经连接并握手成功的会话
			// KGameServerHandler sh = (KGameServerHandler) handler;
			logger.warn("close all handshaked sessions... {}",
					KGameServerHandler.handshakedplayersessions.size());
			for (Channel c : KGameServerHandler.handshakedplayersessions
					.keySet()) {
				// 发送通知消息并关闭
				if (shutdownnoticemsg != null) {
					c.write(shutdownnoticemsg.duplicate()).addListener(
							ChannelFutureListener.CLOSE);
				} else {
					c.close();
				}
			}
			// ///////////////////////////////////////////////////
			// 关闭所有已经登录了的玩家会话
			logger.warn("close all logined sessions... {}", KGameServer
					.getInstance().getPlayerManager()
					.getCachedPlayerSessionSize());
//			KGameServer.getInstance().getPlayerManager()
//					.closeAllSessions(shutdownnoticemsg);
			KGameServer.getInstance().getPlayerManager().closeAllSessions(); // 里面的停机消息带有系统广播的内容
		}

		// logger.warn("bootstrap releaseExternalResources ...");
		// bootstrap.releaseExternalResources();
	}

	public void releaseExternalResources() {
		bootstrap.releaseExternalResources();
	}

	public boolean isStopped() {
		return shutdown.get();
	}

	public int getAllowedConnect() {
		return allowedConnect;
	}

	public int getAllowedOnline() {
		return allowedOnline;
	}

	public KGameServerType serverType() {
		return serverType;
	}

	// public ChannelGroup getAllChannels() {
	// return allChannels;
	// }

	public InetSocketAddress getSocketAddress() {
		return serverAddress;
	}
	
	public String getWanIP(){
		return wanIP;
	}

	/**
	 * 从服务器内部写入一条下发消息（平台层使用，例如关服时构造些内部消息内部处理）
	 * 
	 * @param senderChannel
	 *            发送者通道
	 * @param msg
	 *            消息体
	 * @return
	 */
	public ChannelFuture sendDownstream(Channel senderChannel, KGameMessage msg) {
		return Channels.write(senderChannel, msg);
	}

	// public static String getServerName(int serverType) {
	// return (serverType == SERVER_TYPE_FE ? "KGAME-FE"
	// : serverType == SERVER_TYPE_GS ? "KGAME-GS"
	// : serverType == SERVER_TYPE_GMS ? "KGAME-GMS"
	// : serverType == SERVER_TYPE_LOGS ? "KGAME-LOGS"
	// : "KGAME");
	// }

	/**
	 * 新建一个待发送的消息
	 * 
	 * @param msgType
	 *            消息类型 {@link KGameMessage#MTYPE_PLATFORM} /
	 *            {@link KGameMessage#MTYPE_GAMELOGIC}
	 * @param clientType
	 *            客户端类型 {@link KGameMessage#CTYPE_ANDROID} /
	 *            {@link KGameMessage#CTYPE_IPAD} /
	 *            {@link KGameMessage#CTYPE_IPHONE} /
	 *            {@link KGameMessage#CTYPE_PC}
	 * @param msgID
	 *            消息ID （程序自定义）
	 * @return 一个可操作的消息实例
	 */
	public static KGameMessage newMessage(byte msgType, byte clientType,
			int msgID) {
		KGameMessage msg = new KGameMessageImpl(msgType, clientType, msgID);
		return msg;
	}

	public static KGameMessage getPingMessage() {
		return PING_MSG.duplicate();
	}

	/**
	 * 构建一个系统公告通知消息
	 * 
	 * @param notice
	 *            公告内容
	 * @return
	 */
	public static KGameMessage newSystemNoticeMsg(String notice) {
		KGameMessage msg = new KGameMessageImpl(KGameMessage.MTYPE_PLATFORM,
				KGameMessage.CTYPE_ANDROID, MID_SYSTEM_NOTICE);
		msg.writeUtf8String(notice);
		return msg;
	}

	/**
	 * 构建一个异常消息
	 * 
	 * @param exceCode
	 *            异常码
	 * @param exceInfo
	 *            异常信息
	 * @return
	 */
	public static KGameMessage newExceptionMsg(int exceCode, String exceInfo) {
		KGameMessage msg = new KGameMessageImpl(KGameMessage.MTYPE_PLATFORM,
				KGameMessage.CTYPE_ANDROID, MID_EXCEPTION);
		msg.writeInt(exceCode);
		msg.writeUtf8String(exceInfo);
		return msg;
	}

	/**
	 * 向某个客户端发送异常通知消息
	 * 
	 * @param clientChannel
	 *            客户端通道
	 * @param exceCode
	 *            异常码
	 * @param exceInfo
	 *            异常信息
	 * @param doClose
	 *            是否在发送后关闭此客户端通道
	 */
	public static void sendException(Channel clientChannel, int exceCode,
			String exceInfo, boolean doClose) {
		KGameMessage msg = newExceptionMsg(exceCode, exceInfo);
		if (clientChannel != null && clientChannel.isConnected()) {
			ChannelFuture future = clientChannel.write(msg);
			if (doClose) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	public static void sendDisconnect(Channel clientChannel, byte clientType,
			int disconnectCause, String disconnectTips, boolean doClose) {
		KGameMessage msg = newMessage(KGameMessage.MTYPE_PLATFORM, clientType,
				MID_DISCONNECT);
		msg.writeInt(disconnectCause);
		msg.writeUtf8String(disconnectTips);
		if (clientChannel != null && clientChannel.isConnected()) {
			ChannelFuture future = clientChannel.write(msg);
			if (doClose) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}
	
	public static void sendInfoClientOffline(Channel clientChannel, byte clientType, String tips) {
		KGameMessage msg = newMessage(KGameMessage.MTYPE_PLATFORM, clientType, MID_INFO_CLIENT_OFFLINE);
		msg.writeUtf8String(tips);
		if (clientChannel != null && clientChannel.isConnected()) {
			clientChannel.write(msg).addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	/**
	 * 把一个按约定格式写入的byte[]转换成可读取的对象。具体格式跟{@link KGameMessage}一致
	 * 
	 * @param payloadBytes
	 *            待包装的字节数组
	 * @param offset
	 *            数据起点
	 * @param length
	 *            数据长度
	 * @return
	 */
	public static KGameMessagePayload wrappedMessagePayload(byte[] payloadBytes,
			int offset, int length) {
		return new KGameMessagePayloadImpl(payloadBytes, offset, length);
	}

	/**
	 * 基本平台内部用，逻辑尽量别用！！！
	 * 
	 * @param msg
	 * @return
	 */
	public static byte[] message2bytes(KGameMessage msg) {
		return ((KGameMessageImpl) msg).backingChannelBuffer.array();
	}
}
