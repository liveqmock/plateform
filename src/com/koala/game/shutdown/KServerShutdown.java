package com.koala.game.shutdown;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameMessageDecoder;
import com.koala.game.communication.KGameMessageEncoder;

public class KServerShutdown extends SimpleChannelUpstreamHandler implements
		KGameProtocol {

	private Channel channel;

	public static void main(String[] args) {
		System.err.println("Get command to shutdown server(" + args[0] + ":"
				+ args[1] + ")");
		KServerShutdown sd = new KServerShutdown();
		try {
			sd.connect2(args[0], Integer.parseInt(args[1]));
			sd.sendshutdowncommand();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.println("Passed on Shutdown Command to Target Server.\r\n");
	}

	private void connect2(String targetip, int targetport) throws InterruptedException {
		ClientBootstrap bootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newSingleThreadExecutor(),
						Executors.newSingleThreadExecutor()));
		bootstrap.setPipelineFactory(new ShutdownPipelineFactory());
		channel = bootstrap
				.connect(new InetSocketAddress(targetip, targetport)).await()
				.getChannel();
	}

	private void sendshutdowncommand() {
		if (channel == null || (!channel.isConnected())) {
			System.err.println("ERROR: channel is null or not connected.");
			return;
		}
		KGameMessage shutdownmsg = KGameCommunication.newMessage(
				KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_PC,
				KGameProtocol.MID_SHUTDOWN);
		shutdownmsg.writeUtf8String(KEY_SHUTDOWN_GS);
		channel.write(shutdownmsg);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		KGameMessage msg = (KGameMessage) e.getMessage();
		if (msg.getMsgType() == KGameMessage.MTYPE_PLATFORM
				&& msg.getMsgID() == KGameProtocol.MID_SHUTDOWN) {
			String info = msg.readUtf8String();
			System.err.println(info);
			if ("done".equalsIgnoreCase(info)) {
				System.exit(1);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		System.err.println("exceptionCaught.");
		e.getCause().printStackTrace();
		System.exit(1);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		System.err.println("channelClosed.");
		System.exit(1);
	}

	private class ShutdownPipelineFactory implements ChannelPipelineFactory {
		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			// 自定义编码解码器
			pipeline.addLast("kgame_decoder", new KGameMessageDecoder());
			pipeline.addLast("kgame_encoder", new KGameMessageEncoder());
			pipeline.addLast("handler", KServerShutdown.this);
			return pipeline;
		}
	}

}
