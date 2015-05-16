package com.koala.game.gameserver;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jdom.Element;

import com.koala.game.FEGSProtocol;
import com.koala.game.KGameMessage;
import com.koala.game.KGameMessagePayload;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameMessageDecoder;
import com.koala.game.communication.KGameMessageEncoder;
import com.koala.game.exception.KGameServerException;
import com.koala.game.gameserver.crossserversupport.KGameCrossServerSupport;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;

/**
 * GS跟FE的关联。
 * 
 * 在GS正常启动后，会连接到所配置的FE，并定时（用{@link KGameTimerTask}）将GS状态通知到FE
 * 
 * @author AHONG
 * 
 */
public class KGameGS2FE extends SimpleChannelHandler implements
		KGameTimerTask, KGameProtocol,FEGSProtocol {

	private static final KGameLogger logger = KGameLogger
			.getLogger(KGameGS2FE.class);
	private final AtomicBoolean connecting = new AtomicBoolean();// 防止不同线程连接
	private final static int CONN_NULL = 0x0;
	private final static int CONN_NEW = 0x1;
	private final static int CONN_FINE = 0x2;
	private Channel channel;
	private String feServerIP;
	private int feSocketPort;
	private final int gsID;
	private long statusUpdateDurationSeconds;

	public KGameGS2FE(int gsID) {
		this.gsID = gsID;
	}

	// <FrontendInfo>
	// <FeLanIP>localhost</FeLanIP>
	// <FeSocketPort>8887</FeSocketPort>
	// <GsSimpleInfo name="快乐过神仙" gsNO="1" />
	// </FrontendInfo>
	boolean initAndRun(Element eFrontendInfo) {
		if (eFrontendInfo == null) {
			logger.warn("<FrontendInfo> null.");
			return false;
		}

		String feIP = eFrontendInfo.getChildTextTrim("FeLanIP");
		String fePort = eFrontendInfo.getChildTextTrim("FeSocketPort");
		if (feIP == null || feIP.length() == 0 || fePort == null
				|| fePort.length() == 0) {
			logger.warn("<FrontendInfo> feIP==null||feIP.length()==0||fePort==null||fePort.length()==0.");
			return false;
		}

		statusUpdateDurationSeconds = Long.parseLong(eFrontendInfo
				.getChildTextTrim("StatusUpdateDurationSeconds"));
		feServerIP = feIP;
		feSocketPort = Integer.parseInt(fePort);

		// 先主动跟FE握手
		if ((ensureConnection() & CONN_NEW) != 0) {
			handshake2fe();
		}

		return true;
	}

	private synchronized int ensureConnection() {
		int re = CONN_NULL;
		if (connecting.compareAndSet(false, true)) {
			try {
				// 如果没有连接就建立
				if (channel == null || (!channel.isConnected())) {
					ClientBootstrap bootstrap = new ClientBootstrap(
							new NioClientSocketChannelFactory(
									Executors.newSingleThreadExecutor(),
									Executors.newSingleThreadExecutor()));
					bootstrap.setPipelineFactory(new GS2FEPipelineFactory());
					ChannelFuture connectFuture = bootstrap
							.connect(new InetSocketAddress(feServerIP,
									feSocketPort));
					channel = connectFuture.awaitUninterruptibly().getChannel();
					logger.info("GS2FE {}", this);
					re |= CONN_NEW;
				}
			} finally {
				connecting.set(false);
			}
		}
		return (channel != null && channel.isConnected()) ? (re | CONN_FINE)
				: CONN_NULL;
	}

	private void handshake2fe() {
		KGameMessage handshake = newMessage(MID_HANDSHAKE);
		handshake.writeInt(gsID);// 区号
		handshake.writeInt(KGameServer.getInstance().getCurrentOnline());// 当前客户端连接数
		handshake.writeLong(Runtime.getRuntime().totalMemory() / 1024 / 1024);
		handshake.writeLong(Runtime.getRuntime().freeMemory() / 1024 / 1024);
		handshake.writeUtf8String(KGameServer.getInstance().getCommunication()
				/*.getSocketAddress().getHostName()*/.getWanIP());
		handshake.writeInt(KGameServer.getInstance().getCommunication()
				.getSocketAddress().getPort());
		channel.write(handshake);//
	}

	private void updatestatus2fe() {
		KGameMessage gsstatus = newMessage(MID_GS2FE_UPDATESTATUS);
		gsstatus.writeInt(gsID);
		gsstatus.writeInt(KGameServer.getInstance().getCurrentOnline());
		gsstatus.writeLong(Runtime.getRuntime().freeMemory() / 1024 / 1024);
		channel.write(gsstatus);
	}

	void tell2feishutdown() {
		KGameMessage gsshutdown = newMessage(MID_GS2FE_ISHUTDOWN);
		gsshutdown.writeInt(gsID);
		channel.write(gsshutdown);
	}
	
	void sendDelayRemove(long playerId) {
		KGameMessage msg = newMessage(MID_GS2FE_DELAY_REMOVE);
		msg.writeInt(gsID);
		msg.writeLong(playerId);
		channel.write(msg);
	}
	
	void sendReallyRemove(long playerId) {
		KGameMessage msg = newMessage(MID_GS2FE_REALLY_REMOVE);
		msg.writeInt(gsID);
		msg.writeLong(playerId);
		channel.write(msg);
	}

	@Override
	public String getName() {
		return KGameGS2FE.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {

		int connectresult = ensureConnection();
		// System.out.println("connectresult "+connectresult);
		if ((connectresult & CONN_NEW) != 0) {
			handshake2fe();
		} else if ((connectresult & CONN_FINE) != 0) {
			updatestatus2fe();
		} else {
			// -1 没连接
		}

		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
		timeSignal.getTimer().newTimeSignal(this, statusUpdateDurationSeconds,
				TimeUnit.SECONDS);
	}

	@Override
	public void rejected(RejectedExecutionException e) {

	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		//super.messageReceived(ctx, e);
		if (!(e.getMessage() instanceof KGameMessage)) {
			// 非法消息，logging & disconnect
			logger.warn("Illegal Message Type!!" + e.getMessage());
			return;
		}

		//Channel channel = e.getChannel();//FE-GS
		KGameMessage kmsg = (KGameMessage) e.getMessage();
		
		logger.debug("receive fe msg {}",kmsg.getMsgID());
		
		if(kmsg.getMsgType()==KGameMessage.MTYPE_PLATFORM&&kmsg.getClientType()==KGameMessage.CTYPE_GS){
			if(kmsg.getMsgID()==KGameProtocol.MID_CROSS_SERVER_MSG){
				int sGsID = kmsg.readInt();
				int rGsID = kmsg.readInt();
				int bLen = kmsg.readInt();
				byte[] bytes = new byte[bLen];
				kmsg.readBytes(bytes);
				KGameMessagePayload payload = KGameCommunication.wrappedMessagePayload(bytes, 0, bLen);
				//TODO 直接当前线程处理的
				KGameCrossServerSupport.notifyHandlers(sGsID, payload);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		super.exceptionCaught(ctx, e);
	}

	@Override
	public String toString() {
		return channel == null ? "null" : channel.toString();
	}

	private static KGameMessage newMessage(int msgID) {
		return KGameCommunication.newMessage(KGameMessage.MTYPE_PLATFORM,
				KGameMessage.CTYPE_GS, msgID);
	}

	private final class GS2FEPipelineFactory implements ChannelPipelineFactory {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			// 自定义编码解码器
			pipeline.addLast("kgame_decoder", new KGameMessageDecoder());
			pipeline.addLast("kgame_encoder", new KGameMessageEncoder());
			pipeline.addLast("handler", KGameGS2FE.this);
			return pipeline;
		}
	}

	public void sendCrossMessage(KGameMessage msgCross){
		channel.write(msgCross);
	}
}
