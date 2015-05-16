package com.koala.verserver;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

import com.koala.game.logging.KGameLogger;
import com.koala.game.tips.KGameTips;

public final class KGameVerControlServer {
	
	public static void main(String[] args) {
		KGameVerControlServer vs = new KGameVerControlServer(Integer.parseInt(args[0]));
		try {
			vs.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static final KGameLogger logger = KGameLogger
			.getLogger(KGameVerControlServer.class);
	private final int port;
	private KGameVerControlServerHandler handler;

	public KGameVerControlServer(int port) {
		this.port = port;
	}

	public void run() throws Exception {
		logger.info("HTTP Resfile Server Start...");
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		KGameTips.load();
		
		handler = new KGameVerControlServerHandler();
		logger.info("HTTP Resfile Server Load Resfiles...");

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new VSPipelineFactory(handler));
		
//		bootstrap.setOption("reuseAddress", true);
//		bootstrap.setOption("child.tcpNoDelay", true);
//		bootstrap.setOption("child.keepAlive", true);

		Map<String,Object> o = bootstrap.getOptions();
		for(String key :o.keySet()){
			System.out.println(key+":"+o.get(key));
		}
		System.out.println(bootstrap.getOption("reuseAddress"));
		
		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress(port));
		logger.info("VS Start OKKKKKKKKKKKKKKKKKKKKKK! http bind{}", port);
	}

	/*********************************************************************
	 * inner class
	 */
	private class VSPipelineFactory implements ChannelPipelineFactory {
		private KGameVerControlServerHandler handler;
//		private ExecutionHandler executionHandler;

		VSPipelineFactory(KGameVerControlServerHandler handler) {
			this.handler = handler;
//			executionHandler = new ExecutionHandler(
//					new OrderedMemoryAwareThreadPoolExecutor(8, 1024 * 1024,
//							1024 * 1024 * 1024 * 4), false, true);
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

			pipeline.addLast("executor", handler.executionHandler);

			pipeline.addLast("handler", handler);
			return pipeline;
		}
	}

}
