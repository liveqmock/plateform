package com.koala.game.communication;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import com.koala.game.KGame;
import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.KGameServerType;
import com.koala.game.frontend.KGameGSManager;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.logging.KGameLogger;

final class KGameHeartbeat extends IdleStateAwareChannelHandler {

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameServer.class);
	private KGameCommunication communication;

	static void ping(Channel client) {

	}

	KGameHeartbeat(KGameCommunication communication) {
		this.communication = communication;
	}

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e)
			throws Exception {
		super.channelIdle(ctx, e);

		Channel channel = e.getChannel();

		// logger.debug("channelIdle " + e.getState() + channel);

		if (e.getState() == IdleState.READER_IDLE) {

			switch (communication.serverType().getType()) {

			// 前端服务器FE
			case KGameServerType.SERVER_TYPE_FE:
				//GS gs = null; 
				if ((/*gs = */KGameGSManager.isGSChannel(channel)) != null) {
					// 在FE服务器中如果GS连接超时，那么原因可能是GS和FE的连接有异常、GS关闭维护、GS进程有异常……
					logger.warn("GS's Channel({}) Idle. ", channel);
					// TODO GS channel idle
					//KGameFrontend.getInstance().getGSMgr().gsidle(gs);
					channel.close();
				} else {
					// 关闭掉这个客户端
					channel.close();
					// logging
					logger.warn("{} close channel {}", e.getState(), channel);
				}
				break;

			// 游戏服务器GS
			case KGameServerType.SERVER_TYPE_GS:
				KGameChannelAttachment channelAttachment = (KGameChannelAttachment) channel
						.getAttachment();// 每个Channel都会绑定一个附件
				if (channelAttachment == null) {
					channelAttachment = new KGameChannelAttachment(-1);
					channel.setAttachment(channelAttachment);
				}
				channelAttachment
						.setDisconnectedCause(KGameProtocol.CAUSE_PLAYEROUT_IDLE);
				// 关闭掉这个客户端
//				channel.close();
				// 2014-08-22 修改，发送一条断连消息到客户端
				KGameMessage msg = KGame.newMessage(KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_ANDROID, KGameProtocol.MID_SEND_IDLE_TO_CLIENT);
				channel.write(msg).addListener(ChannelFutureListener.CLOSE);

				// logging
				logger.warn("{} close channel={},playerID={}", e.getState(), channel,channelAttachment.getPlayerID());
				break;
			}
		} else if (e.getState() == IdleState.WRITER_IDLE) {
			// TODO WRITER_IDLE,send PingMessage？
		}

	}

}
