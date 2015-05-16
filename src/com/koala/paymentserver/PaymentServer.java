package com.koala.paymentserver;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupportReloadListener;

/**
 * kola支付服务器：
 * 
 * <pre>
 * 1、接受渠道SDK服务器发来的支付结果（HTTP GET/POST）；
 * 2、将结果通过socket方式传给对应的GS，携带足够的信息，让GS可以进行发货（目前属于充值，即增加元宝的操作），并通知客户端。
 * 
 * 所以本支付服务器必须包含功能：
 * a、HTTP服务
 * b、所有渠道的信息，例如渠道号、我方在渠道中的CP信息 -配置
 * c、GS列表，包含IP端口及账号密码，及连接到GS及通信功能（并具备自动重连机制） -配置
 * d、流水记录（DB或log皆可）
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class PaymentServer implements PromoSupportReloadListener {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PaymentServer ps = PaymentServer.getIntance();
		try {
			ps.startup();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static final KGameLogger logger = KGameLogger
			.getLogger(PaymentServer.class);
	private final File xml = new File("res/config/promo/PaymentServer.xml");
	private long lastModifiedOfXml;
	private int modifyCheckSeconds;
	private static PaymentServer instance;
	private PaymentServerHandler handler;
	// /////////////////////////////////////////////
	private ExecutionHandler executionHandler;
	private int corePoolSize;
	private long maxChannelMemorySize;
	private long maxTotalMemorySize;
	private long keepAliveTimeMillis;
	// /////////////////////////////////////////////
	KGameTimer timer;
	// /////////////////////////////////////////////
	int pingIntervalSeconds;
	private int port;
	private ServerBootstrap bootstrap;
	private Channel serverchannel;
	// <PS2GS,Boolean.TRUE>
	private final ConcurrentHashMap<PS2GS, Boolean> gss = new ConcurrentHashMap<PS2GS, Boolean>();
//	private String yuanBaoPrice;
	final AtomicBoolean shutdownflag = new AtomicBoolean();
	private String shudowncode;
	long payOrderCacheDurationSeconds;//缓存时长
	String tempFileRecordCachedPayOrders;

	private PaymentServer() {
	}

	public static PaymentServer getIntance() {
		if (instance == null)
			instance = new PaymentServer();
		return instance;
	}

	public void startup() throws Exception {
		logger.info(">>>>>>{} Startup...", PaymentServer.class.getSimpleName());
		// Configure the server.
		bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// XML
		Document doc = XmlUtil.openXml(xml);
		Element root = doc.getRootElement();

		Element eExecutionHandler = root.getChild("ExecutionHandler");
		corePoolSize = Integer.parseInt(eExecutionHandler
				.getAttributeValue("corePoolSize"));
		maxChannelMemorySize = Long.parseLong(eExecutionHandler
				.getAttributeValue("maxChannelMemorySize"));
		maxTotalMemorySize = Long.parseLong(eExecutionHandler
				.getAttributeValue("maxTotalMemorySize"));
		keepAliveTimeMillis = Long.parseLong(eExecutionHandler
				.getAttributeValue("keepAliveTimeMillis"));

		handler = new PaymentServerHandler();
		// corePoolSize, maxChannelMemorySize, maxTotalMemorySize
		executionHandler = new ExecutionHandler(
				new OrderedMemoryAwareThreadPoolExecutor(corePoolSize,
						maxChannelMemorySize, maxTotalMemorySize,
						keepAliveTimeMillis, TimeUnit.MILLISECONDS), false,
				true);
		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new PayPipelineFactory());

		// Timer
		Element eTimer = root.getChild("Timer");
		timer = new KGameTimer(eTimer);

		pingIntervalSeconds = Integer.parseInt(root
				.getChildTextTrim("PingIntervalSeconds"));

		payOrderCacheDurationSeconds = Long.parseLong(root.getChildTextTrim("PayOrderCachedTimeSeconds"));
		
		shudowncode = root.getChildTextTrim("ShutdownCode");

		// 加载联运渠道支持！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
		try {
			PromoSupport.getInstance().loadConfig(
					new File("res/config/promo/PromoChannelConfig.xml"), false);
			//加入渠道信息重新加载的监听器
			PromoSupport.getInstance().addReloadListener(this);
			//定时检测XML是否修改
			timer.newTimeSignal(PromoSupport.getInstance(), 60,
					TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new Exception(
					"PromoSupport.getInstance().loadConfig(\"res/config/promo/PromoChannelConfig.xml\");",
					e);
		}
		logger.info("PromoSupport Inited.");
		
		tempFileRecordCachedPayOrders = root.getChildTextTrim("TempFileRecordCachedPayOrders");
		
		/////////////////////////////////////////////////////////////////
		//GS列表
		Element eGsList = root.getChild("GsList");
		modifyCheckSeconds = Integer.parseInt(eGsList.getAttributeValue("modifyCheckSeconds"));
		List<Element> eGss = eGsList.getChildren("GS");
		for (Element eGs : eGss) {
			boolean open = "true".equalsIgnoreCase(eGs
					.getAttributeValue("open"));
			if (open) {
				PS2GS gs = new PS2GS(eGs.getAttributeValue("ip"),
						Integer.parseInt(eGs.getAttributeValue("port")));
				logger.info("{}", gs);
				gss.put(gs, Boolean.TRUE);// 加入列表

				timer.newTimeSignal(gs, pingIntervalSeconds, TimeUnit.SECONDS);

				gs.start();

				gs.ensureConnection();
			}
		}
		
		timer.newTimeSignal(new XmlModifyChecker(), modifyCheckSeconds, TimeUnit.SECONDS);
		
		port = Integer.parseInt(root.getChildTextTrim("HttpPort"));
		// Bind and start to accept incoming connections.
		serverchannel = bootstrap.bind(new InetSocketAddress(port));
		logger.info(
				">>>>>>{} Startup.OKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOK! Bind {}",
				PaymentServer.class.getSimpleName(), port);
//		java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
//		System.out.println(ts);
//		System.out.println(System.currentTimeMillis());
//		System.out.println(ts.getTime());
	}
	
	private void reloadGsList(){
		lastModifiedOfXml = xml.lastModified();
		// XML
		Document doc = XmlUtil.openXml(xml);
		Element root = doc.getRootElement();
		
		//GS列表
		Element eGsList = root.getChild("GsList");
		List<Element> eGss = eGsList.getChildren("GS");
		for (Element eGs : eGss) {
			String gsip = eGs.getAttributeValue("ip");
			int gsport = Integer.parseInt(eGs.getAttributeValue("port"));
			boolean open = "true".equalsIgnoreCase(eGs.getAttributeValue("open"));
			PS2GS gs = getPS2GS(gsip, gsport);
			if (gs == null) {
				// 新增的
				if (open) {
					gs = new PS2GS(gsip, gsport);
					logger.warn("new PS2GS {}", gs);
					gss.put(gs, Boolean.TRUE);// 加入列表
					timer.newTimeSignal(gs, pingIntervalSeconds,
							TimeUnit.SECONDS);
					gs.start();
					gs.ensureConnection();
				}
			}else{
				//原有的，检测open状态，如果不为true则关闭之
				if(!open){
					logger.warn("Stop and Remove PS2GS {}",gs);
					Set<PayOrder> unprocessedOrder = gs.stop();
					for (PayOrder payOrder : unprocessedOrder) {
						logger.error("【{} unprocessed order】{}",gs.getGsid(),payOrder);
					}
					gss.remove(gs);
				}
			}
		}
	}

	@Override
	public void notifyPromoSupportReloaded() {
		// 收到渠道信息重新加载后，关闭全部GS连接（不用担心：下次需要的时候其内部会自动重连的了）
		logger.debug("==notifyPromoSupportReloaded==");
		for (PS2GS p2g : gss.keySet()) {
			p2g.disconnect();
		}
	}

	public PS2GS getPS2GS(int gsid) {
		for (PS2GS p2g : gss.keySet()) {
			if (p2g.getGsid() != -1 && p2g.getGsid() == gsid) {
				return p2g;
			}
		}
		return null;
	}
	
	public PS2GS getPS2GS(String ip, int port) {
		for (PS2GS p2g : gss.keySet()) {
			if (ip.equalsIgnoreCase(p2g.getIp()) && port == p2g.getPort()) {
				return p2g;
			}
		}
		return null;
	}
	
	// 关闭服务器的指令 /sd+"md5("yesjustdoit")"
	final String shutdowncmdcheck(String uri) {
		// 16c288adbbbae89d02d61fed2e26bb36
		if (uri.startsWith("/sd")) {
			int cidx = uri.indexOf("code=");
			if (cidx > 0) {
				String cmd = uri.substring("/sd?code=".length());
				logger.warn(
						"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!shutdowncode = {}",
						cmd);
				if (shudowncode.equals(cmd)) {
					System.out.println("shutdown...");
					StringBuilder buf = new StringBuilder();
					buf.append("shutdown code --- passed!\n");
					buf.append("shutdown...");
					return buf.toString();
				}
			}
		}
		return null;
	}
	
	final void shutdown(){
		if (shutdownflag.compareAndSet(false, true)) {
			logger.error("serverchannel close...");
			serverchannel.close().awaitUninterruptibly(30, TimeUnit.SECONDS);
			logger.error("serverchannel closed!");
			
			logger.error("======================================");
			for (PS2GS p2g : gss.keySet()) {
				logger.error("stop PS2GS({}) and LOG unprocessed orders.",p2g.getGsid());
				Set<PayOrder> unprocessedOrder = p2g.stop();
				for (PayOrder payOrder : unprocessedOrder) {
					logger.error("【{} unprocessed order】{}",p2g.getGsid(),payOrder);
				}
				logger.error("----------------------------------");
			}
			logger.error("======================================");
			
			logger.error("releaseExternalResources...");
			timer.stop();
			bootstrap.releaseExternalResources();
			logger.error("releaseExternalResources ed.");
			logger.error("------shutteddown!------");
			
			System.exit(1);
		}
	}

//	public String getYuanBaoPrice() {
//		return yuanBaoPrice;
//	}
	
//	public String getSupportedPromoIdsString(){
//		StringBuilder buf = new StringBuilder();
//		Set<Integer> sets = PromoSupport.getInstance().getSupportPromoIdSet();
//		int size = sets.size();
//		int i=0;
//		for (Integer pid : sets) {
//			i++;
//			buf.append(pid);
//			if (i < size) {
//				buf.append(",");
//			}
//		}
//		return buf.toString();
//	}

	/***************************************************************************
	 * inner class
	 */
	private final class PayPipelineFactory implements ChannelPipelineFactory {

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

	private final class XmlModifyChecker implements KGameTimerTask{

		@Override
		public String getName() {
			return "PaymentServerXmlModifyChecker";
		}

		@Override
		public Object onTimeSignal(KGameTimeSignal timeSignal)
				throws KGameServerException {
			try {
				if (xml.exists() && xml.lastModified() != lastModifiedOfXml) {
					logger.warn("Reload Gs List...{} ",xml.getPath());
					reloadGsList();
				}
			} finally {
				timeSignal.getTimer().newTimeSignal(this, modifyCheckSeconds,
						TimeUnit.SECONDS);
			}
			return null;
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
		}

		@Override
		public void rejected(RejectedExecutionException e) {
		}
		
	}
}
