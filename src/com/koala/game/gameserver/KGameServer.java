package com.koala.game.gameserver;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.internal.ConcurrentIdentityHashMap;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.koala.game.KGameHotSwitch;
import com.koala.game.KGameMessage;
import com.koala.game.KGameModule;
import com.koala.game.KGameMsgCompressConfig;
import com.koala.game.KGameProtocol;
import com.koala.game.KGameServerType;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.communication.KGameHttpRequestSender;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.exception.KGameServerException;
import com.koala.game.gameserver.paysupport.KGamePaymentListener;
import com.koala.game.gameserver.paysupport.KGamePaymentSupport;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayerManager;
import com.koala.game.timer.KGameTimer;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.StringUtil;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.PromoSupport;

/**
 * 游戏服务器的启动类，同时是参数传递入口
 * 
 * @see KGameServerContext
 * @author AHONG
 */
public final class KGameServer implements KGameServerType {

	/**
	 * <pre>
	 * 用于昆仑日志规范
	 * 要求输出日志文件以pid，rid命名，因此由GS启动时赋值
	 * 注意：此代码必须先于{@link #main(String[] args)}执行！
	 * 
	 * @param product_id
	 * @param region_id
	 * @author CamusHuang
	 * @creation 2013-12-10 下午5:17:28
	 * </pre>
	 */
	static {
		Document doc = XmlUtil.openXml("./res/config/gs/KGameServer.xml");
		Element root = doc.getRootElement();
		String gsId = root.getAttributeValue("GSID");
		String kunlun_pid = root.getAttributeValue("KUNLUN_PRODUCT_ID");
		
		System.setProperty("product_id", kunlun_pid);
        System.setProperty("region_id", gsId);
	}
	
	public static void main(String[] args) {
		logger.printSwitch();
		// 需要根据启动参数做设置
		instance = new KGameServer(args);
		try {
			instance.startup();
		} catch (KGameServerException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static final String _GS_FIRST_START_TIME_RECORD_PATH = "./.gs_first_start_time";
	private final static KGameLogger logger = KGameLogger.getLogger(KGameServer.class);
	private static KGameServer instance;

	private final ConcurrentIdentityHashMap<String, KGameModule> modules = new ConcurrentIdentityHashMap<String, KGameModule>();
	private KGameServerHandler handler;
	private KGameHttpRequestSender httpSender;
	private KGameCommunication communication;
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private KGamePlayerManager playerManager;
	private KGameTimer timer;
	private KGameGS2FE gs2fe;
	private int gsID;  
	private long _gsFirstStartTime;
	private GSStatusMonitor statusMonitor;
//	private String shutdownNotice;

	KGameServer(String[] args) {
		// TODO 启动参数的定义和解析
	}

	@Override
	public int getType() {
		return SERVER_TYPE_GS;
	}

	@Override
	public String getName() {
		return "KGAME-GS";
	}

	public static KGameServer getInstance() {
		return instance;
	}

	public KGameServerHandler getHandler() {
		return handler;
	}
	
	public KGameHttpRequestSender getHttpSender() {
		return httpSender;
	}

	public KGameCommunication getCommunication() {
		return communication;
	}

	public KGameModule getGameModule(String moduleName) {
		return modules.get(moduleName);
	}

	public Iterator<KGameModule> iteratorModules() {
		return modules.values().iterator();
	}

	public KGamePlayerManager getPlayerManager() {
		return playerManager;
	}

	public KGameTimer getTimer() {
		return timer;
	}

	public int getCurrentOnline() {
		return playerManager.getCachedPlayerSessionSize();
	}

	public GSStatusMonitor getStatusMonitor() {
		return statusMonitor;
	}

	public int getGSID() {
		return gsID;
	}
	
	public KGameGS2FE getGS2FE(){
		return gs2fe;
	}
	
	public long getGSFirstStartTime() {
		return this._gsFirstStartTime;
	}

	protected void startup() throws KGameServerException {
		logger.info("KGameServer starting ...");
		if (shutdown.get()) {
			throw new IllegalStateException("cannot be started when stopping");
		}
		
		try {
			this.initGsFirstStartTime();
		} catch (Exception e) {
			throw new KGameServerException(e);
		}
		
		KGameTips.load();// 加载提示语

		// /////////////////////////////////////////config
		String xml = "res/config/gs/KGameServer.xml";
		Document doc = XmlUtil.openXml(xml);
		Element root = doc.getRootElement();

		int logicDbMode = Integer.parseInt(root.getChildText("logicDbMode"));

		// 初始化数据库连接
		try {
			KGameDataAccessFactory.getInstance().initPlatformDB();
			KGameDataAccessFactory.getInstance().initLogicDB(logicDbMode);
		} catch (Exception e) {
			throw new KGameServerException(e.getMessage(), e);
		}

		// GS服务器的身份标识
		gsID = Integer.parseInt(root.getAttributeValue("GSID"));
		
		KGamePaymentSupport.getInstance();//NEW

		logger.info("Start GS({}) with config xml {} ...", gsID, xml);

		// ChannelFactory是创建和管理Channel通道及其相关资源的工厂接口
		ChannelFactory channelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),// Boss线程池
				Executors.newCachedThreadPool()// Worker线程池
		);

		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

		logger.info("KGameServer create NioServerSocketChannelFactory： {}",
				channelFactory);
		
		// 2014-06-09 读取自动压缩的配置信息 BEGIN
		Element eCompress = root.getChild("compress");
		
		KGameMsgCompressConfig.init(eCompress);  
		
		logger.info("》》》》The length for auto compress is {} b, and the type of auto compress is {}《《《《", KGameMsgCompressConfig.getMsgLengthForAutoCompress(), (KGameMsgCompressConfig
				.getAutoCompressType() == KGameMessage.ENCRYPTION_ZIP ? "zip" : KGameMsgCompressConfig.getAutoCompressType() == KGameMessage.ENCRYPTION_BASE64 ? "base64" : "none"));
		// 2014-06-09 读取自动压缩的配置信息 END
		
		Element eGame = root.getChild("Game");
//		shutdownNotice = eGame.getChildTextTrim("ShutdownNotice");

		// 时效任务执行类，即常说的定时器
		Element eTimer = eGame.getChild("Timer");
		timer = new KGameTimer(eTimer);

		timer.newTimeSignal(KGameHotSwitch.getInstance(),1, TimeUnit.SECONDS);
		
		// 玩家管理器
		playerManager = new KGamePlayerManager();
		
		///////////////////////////////////////////////////////////
		//加载渠道支持中的货币单位，GS逻辑需要用到
		try {
			PromoSupport.getInstance().loadMoneyUnitMapping(
					new File("res/config/promo/PromoChannelConfig.xml"), false);
		} catch (Exception e2) {
			throw new KGameServerException(
					"PromoSupport.getInstance().loadConfig(\"res/config/promo/PromoChannelConfig.xml\");",
					e2);
		}

		logger.info("Load And Register All Modules...");
		Element modulesElement = root.getChild("Modules");
		// 加载所有模块并初始化
		try {
			loadAndInitAllGameModules(modulesElement);
		} catch (InstantiationException e) {
			throw new KGameServerException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new KGameServerException(e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new KGameServerException(e.getMessage());
		}

		// 初始化通信模块
		logger.info("Init And Start Communication...");
		Element eNetwork = root.getChild("Network");
		int delaySesond = Integer.parseInt(eNetwork.getChildTextTrim("delayRemoveSeconds"));  
		handler = new KGameServerHandler(delaySesond);
		httpSender = new KGameHttpRequestSender();
		communication = new KGameCommunication(this, bootstrap, handler);
		communication.init(eNetwork);// start it!

		// 通知全部模块，服务器启动完毕了
		logger.info("Fire 'ServerStartCompleted' Event To All Modules...");
		for (Iterator<KGameModule> ms = iteratorModules(); ms.hasNext();) {
			KGameModule m = ms.next();
			m.serverStartCompleted();
		}
		
		//检测KGamePaymentListener有没有设置正确
		Element ePaySupport = root.getChild("PaySupport");
		String clazzKGamePaymentListener = ePaySupport.getChild("KGamePaymentListener").getAttributeValue("clazz");
		try {
			KGamePaymentListener pl = (KGamePaymentListener) (Class
					.forName(clazzKGamePaymentListener).newInstance());
			KGamePaymentSupport.getInstance().setPaymentListener(pl);
		} catch (Exception e1) {
			throw new KGameServerException(e1);
		}

		// 等所有模块都初始化完毕才下一步
		long thistime = System.currentTimeMillis();
		boolean allmodulesinitfinished = false;
		while (!allmodulesinitfinished) {
			allmodulesinitfinished = true;
			for (Iterator<KGameModule> ms = iteratorModules(); ms.hasNext();) {
				KGameModule m = ms.next();
				if (!m.isInitFinished()) {
					allmodulesinitfinished = false;
					break;//break for
				}
			}
			long now = System.currentTimeMillis();
			if (now - thistime >= 1800000) {// 30min
				throw new KGameServerException(
						"Timeout(30min)! waiting init modules.");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		KGameTips.submitWatcher(timer); // tips重新加载的定时任务 add @ 2015-01-07 11:50
		
		// 真正启动socket网络对外
		communication.start();

		// //////////////////////////////////////////////////////////////
		// 启动GS跟FE的通信功能
		Element eFrontendInfo = eNetwork.getChild("FrontendInfo");
		if (eFrontendInfo != null) {
			gs2fe = new KGameGS2FE(gsID);
			if (gs2fe.initAndRun(eFrontendInfo)) {
				timer.newTimeSignal(gs2fe, 6, TimeUnit.SECONDS);
			}
		}

		// 服务器运行状态检测器
		long printIntervalSeconds = 0;
		long writtenAmount2FileSeconds = 0;
		long onlineflowrecord = 0;
		Element eDebug = root.getChild("Monitor");
		if (eDebug != null) {
			Element eNetCount = eDebug.getChild("NetCount");
			if (eNetCount != null) {
				printIntervalSeconds = Long.parseLong(eNetCount
						.getAttributeValue("printIntervalSeconds"));
				writtenAmount2FileSeconds = Long.parseLong(eNetCount
						.getAttributeValue("writtenAmount2FileSeconds"));
			}
			Element eFlowRecord = eDebug.getChild("FlowRecord");
			if (eFlowRecord != null) {
				onlineflowrecord = Long.parseLong(eFlowRecord
						.getAttributeValue("online"));
			}
		}
		statusMonitor = new GSStatusMonitor(printIntervalSeconds,
				writtenAmount2FileSeconds,onlineflowrecord);
		if (printIntervalSeconds > 0 || writtenAmount2FileSeconds > 0) {
			timer.newTimeSignal(statusMonitor, 10, TimeUnit.SECONDS);
		}

		logger.info("KGameServer started OKKKKKKKKKKKKKKKKKKK!");

		// _ExampleTimerTask.test(timer);

	}

	/**
	 * <h3>Life cycle of threads and graceful shutdown</h3>
	 * <p>
	 * All threads are acquired from the {@link Executor}s which were specified
	 * when a {@link NioServerSocketChannelFactory} was created. Boss threads
	 * are acquired from the {@code bossExecutor}, and worker threads are
	 * acquired from the {@code workerExecutor}. Therefore, you should make sure
	 * the specified {@link Executor}s are able to lend the sufficient number of
	 * threads. It is the best bet to specify
	 * {@linkplain Executors#newCachedThreadPool() a cached thread pool}.
	 * <p>
	 * Both boss and worker threads are acquired lazily, and then released when
	 * there's nothing left to process. All the related resources such as
	 * {@link Selector} are also released when the boss and worker threads are
	 * released. Therefore, to shut down a service gracefully, you should do the
	 * following:
	 * 
	 * <ol>
	 * <li>unbind all channels created by the factory,
	 * <li>close all child channels accepted by the unbound channels, and (these
	 * two steps so far is usually done using {@link ChannelGroup#close()})</li>
	 * <li>call {@link #releaseExternalResources()}.</li>
	 * </ol>
	 * 
	 * Please make sure not to shut down the executor until all channels are
	 * closed. Otherwise, you will end up with a
	 * {@link RejectedExecutionException} and the related resources might not be
	 * released properly.
	 */
	public void shutdown(final Channel cmdChannel) throws KGameServerException {
		if (!shutdown.compareAndSet(false, true)) {
			reportshutdowninfo2client(cmdChannel, "shutdown repeated call.");
			throw new IllegalStateException("shutdown repeated call.");
		}
		System.err
				.println("\r\n======================================================");
		logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!gs shutdown..!!!!!!!!!!!!!!!!!!!!!!!!!!");
		reportshutdowninfo2client(cmdChannel, "GS("
				+ communication.getSocketAddress().toString()
				+ ") accept shutdown command.");
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO 服務器关闭
				logger.warn("gs shutdown...notify all modules");
				reportshutdowninfo2client(cmdChannel,
						"gs shutdown...notify all modules");
				for (Iterator<KGameModule> ms = iteratorModules(); ms.hasNext();) {
					KGameModule m = ms.next();
					try {
						m.serverShutdown();
					} catch (KGameServerException e) {
						e.printStackTrace();
					}
				}

				logger.warn("gs shutdown...stop communication");
				reportshutdowninfo2client(cmdChannel,
						"gs shutdown...stop communication");
				communication.stop();

				logger.warn("gs shutdown...stop cache");
				reportshutdowninfo2client(cmdChannel,
						"gs shutdown...stop cache");
				KGameDataAccessFactory.getInstance().shutdownCache();

				logger.warn("gs shutdown...stop timer");
				reportshutdowninfo2client(cmdChannel,
						"gs shutdown...stop timer");
				timer.stop();// TODO 时效任务停止后的保存

				reportshutdowninfo2client(cmdChannel, "done");

				logger.warn("gs shutdown...releaseExternalResources, exit.");
				communication.releaseExternalResources();
				
				LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
				lc.stop();
				
				logger.warn("shutdown http sender");
				httpSender.shutdown();

				logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!gs shutdowned!!!!!!!!!!!!!!!!!!!!!!!!!!");
				System.err
						.println("======================================================\r\n");

				System.exit(1);
			}
		}).start();
	}

	public static void reportshutdowninfo2client(Channel cmdChannel, String info) {
		if (cmdChannel == null || (!cmdChannel.isConnected())) {
			return;
		}
		KGameMessage msg = KGameCommunication.newMessage(
				KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_PC,
				KGameProtocol.MID_SHUTDOWN);
		msg.writeUtf8String(info);
		cmdChannel.write(msg);
	}

	private void loadAndInitAllGameModules(Element modulesElement)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, KGameServerException {
		@SuppressWarnings("unchecked")
		List<Element> modulesList = modulesElement.getChildren();
		for (Object obj : modulesList) {
			Element mElement = (Element) obj;
			String name = mElement.getAttributeValue("name");
			String clazz = mElement.getAttributeValue("clazz");
			boolean isPlayerSessionListener = Boolean.parseBoolean(mElement
					.getAttributeValue("isPlayerSessionListener"));
			logger.info("load module '{}' from xml,className = {}", name, clazz);
			if (clazz != null) {
				KGameModule module = (KGameModule) Class.forName(clazz)
						.newInstance();
				module.init(name, isPlayerSessionListener,
						mElement.getChild("ModuleSelfDefiningConfig"));
				modules.put(name, module);
			}
		}
	}
	
	private void initGsFirstStartTime() throws Exception {
		File file = new File(_GS_FIRST_START_TIME_RECORD_PATH);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (file.exists()) {
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			byte[] array = new byte[1024];
			int length = dis.read(array);
			String dateStr = new String(Arrays.copyOfRange(array, 0, length), "UTF-8");
			_gsFirstStartTime = sdf.parse(dateStr).getTime();
			if(_gsFirstStartTime > System.currentTimeMillis()) {
				_gsFirstStartTime = System.currentTimeMillis();
			}
			dis.close();
		} else {
			_gsFirstStartTime = System.currentTimeMillis();
			Date date = new Date(_gsFirstStartTime);
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.write(sdf.format(date).getBytes("UTF-8"));
			raf.close();
			String osName = System.getProperty("os.name");
			if (osName != null && osName.startsWith("Windows")) {
				Runtime.getRuntime().exec(StringUtil.format("attrib \"{}\" +H", file.getAbsoluteFile()));
			}
		}
	}

//	public String getShutdownNotice() {
//		return shutdownNotice;
//	}

}
