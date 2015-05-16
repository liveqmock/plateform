package com.koala.game.resserver;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.koala.game.logging.KGameLogger;

/**
 * 客户端更新及资源下载服务器
 * 
 * @author AHONG
 * 
 */
public final class KGameHttpResfileServer {

	private final int port;
	private KGameHttpResfileServerHandler handler;
	public static final KGameLogger logger = KGameLogger
			.getLogger(KGameHttpResfileServer.class);

	public KGameHttpResfileServer(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		logger.info("HTTP Resfile Server Start...");
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		handler = new KGameHttpResfileServerHandler();
		logger.info("HTTP Resfile Server Load Resfiles...");
		handler.loadAllResource("");// 加载所有资源

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ResfileHttpServerPipelineFactory(
				handler));

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress(port));
		logger.info(
				"HTTP Resfile Server Start OKKKKKKKKKKKKKKKKKKKKKK! bind{}",
				port);
	}

	/*
	 * inner class
	 */
	private class ResfileHttpServerPipelineFactory implements
			ChannelPipelineFactory {
		private KGameHttpResfileServerHandler handler;
		private ExecutionHandler executionHandler;

		ResfileHttpServerPipelineFactory(KGameHttpResfileServerHandler handler) {
			this.handler = handler;
			// corePoolSize, maxChannelMemorySize, maxTotalMemorySize
			executionHandler = new ExecutionHandler(
					new OrderedMemoryAwareThreadPoolExecutor(8, 1024 * 1024,
							1024 * 1024 * 1024 * 4), false, true);
		}

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			// Create a default pipeline implementation.
			ChannelPipeline pipeline = pipeline();

			// Uncomment the following line if you want HTTPS
			// SSLEngine engine =
			// SecureChatSslContextFactory.getServerContext().createSSLEngine();
			// engine.setUseClientMode(false);
			// pipeline.addLast("ssl", new SslHandler(engine));

			pipeline.addLast("decoder", new HttpRequestDecoder());
			pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
			pipeline.addLast("encoder", new HttpResponseEncoder());
			pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

			pipeline.addLast("executor", executionHandler);
			
			pipeline.addLast("handler", handler);
			return pipeline;
		}
	}

	/*
	 * Test main
	 */
	public static void main(String[] args) {
		// KGameHttpResfileServerHandler mgr = new
		// KGameHttpResfileServerHandler();
		// try {
		// mgr.loadAllResource("");
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		KGameHttpResfileServer serv = new KGameHttpResfileServer(8080);
		try {
			serv.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
