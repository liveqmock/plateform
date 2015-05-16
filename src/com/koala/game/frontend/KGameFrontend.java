package com.koala.game.frontend;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.KGameHotSwitch;
import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.KGameServerType;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.example.DebugTimer;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayerManager;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.PromoSupport;

/**
 * 前端服务器
 * 
 * @author AHONG
 * 
 */
public class KGameFrontend implements KGameServerType {

	public static void main(String[] args) {
		try {
			instance = new KGameFrontend(args);
			instance.startup();
		} catch (KGameServerException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static final KGameLogger logger = KGameLogger
			.getLogger(KGameFrontend.class);
	private static KGameFrontend instance;
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private KGameFrontendHandler handler;
	private KGameTimer timer;
	private KGameCommunication communication;
	private KGameGSManager gsmgr;
	private KGameVersionControl versionchecker;
	private KGamePlayerManager playerManager;

	KGameFrontend(String[] args) {
	}

	public static KGameFrontend getInstance() {
		return instance;
	}

	@Override
	public int getType() {
		return SERVER_TYPE_FE;
	}

	@Override
	public String getName() {
		return "KGAME-FE";
	}

	KGameFrontendHandler getHandler() {
		return handler;
	}

	public KGameGSManager getGSMgr() {
		return gsmgr;
	}

	KGameVersionControl getVerChecker() {
		return versionchecker;
	}

	public KGameCommunication getCommunication() {
		return communication;
	}

	public KGamePlayerManager getPlayerManager() {
		return playerManager;
	}

	public boolean isShutdown() {
		return shutdown.get();
	}
	
	public KGameTimer getTimer() {
		return timer;
	}

	protected void startup() throws KGameServerException {
		logger.info("startup>>>FE Server Starting...");
		if (shutdown.get()) {
			throw new IllegalStateException("cannot be started when stopping");
		}

		// DB INIT
		try {
			KGameDataAccessFactory.getInstance().initPlatformDB();
		} catch (Exception e) {
			throw new KGameServerException(e.getMessage());
		}

		KGameTips.load();// 加载提示语

		// /////////////////////////////////////////config
		String xml = "res/config/fe/KGameFrontend.xml";
		Document doc = XmlUtil.openXml(xml);
		Element root = doc.getRootElement();
		
		logger.info("startup>>>LOAD XML {}",xml);
		
		// ChannelFactory是创建和管理Channel通道及其相关资源的工厂接口
		ChannelFactory channelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),// Boss线程池
				Executors.newCachedThreadPool()// Worker线程池
		);

		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);

		// 时效任务
		Element eTimer = root.getChild("Timer");
		timer = new KGameTimer(eTimer);
		logger.info("startup>>>CREATE TIMER {}", timer);

		timer.newTimeSignal(KGameHotSwitch.getInstance(), 1, TimeUnit.SECONDS);

		playerManager = new KGamePlayerManager();
		logger.info("startup>>>NEW PLAYERMANAGER {}",playerManager);

		// 提交一个监视FE运行状态的时效任务
		long printIntervalSeconds = 0;
		long writtenAmount2FileSeconds = 0;
		Element eDebug = root.getChild("Monitor");
		if (eDebug != null) {
			Element eNetCount = eDebug.getChild("NetCount");
			if (eNetCount != null) {
				printIntervalSeconds = Long.parseLong(eNetCount
						.getAttributeValue("printIntervalSeconds"));
				writtenAmount2FileSeconds = Long.parseLong(eNetCount
						.getAttributeValue("writtenAmount2FileSeconds"));
			}
		}
		FEStatusMonitor festatusmonitor = new FEStatusMonitor(
				printIntervalSeconds, writtenAmount2FileSeconds);
		if (printIntervalSeconds > 0 || writtenAmount2FileSeconds > 0) {
			timer.newTimeSignal(festatusmonitor, 10, TimeUnit.SECONDS);
		}

		// GS管理器
		gsmgr = new KGameGSManager();
		gsmgr.loadGsListXml(false);
		timer.newTimeSignal(gsmgr, 10, TimeUnit.SECONDS);
		logger.info("startup>>>NEW KGameGSManager {}",gsmgr);

		// 初始化通信模块
		logger.info("startup>>>Communication Init...");
		Element eNetwork = root.getChild("Network");
		handler = new KGameFrontendHandler(this);
		communication = new KGameCommunication(this, bootstrap, handler);
		communication.init(eNetwork);// start it!
		logger.info("startup>>>Communication Inited.");

		// 加载联运渠道支持！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
		try {
			PromoSupport.getInstance().loadConfig(
					new File("res/config/promo/PromoChannelConfig.xml"), false);
			timer.newTimeSignal(PromoSupport.getInstance(), 60,
					TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new KGameServerException(
					"PromoSupport.getInstance().loadConfig(\"res/config/promo/PromoChannelConfig.xml\");",
					e);
		}
		logger.info("startup>>>PromoSupport Inited.");

		// 客户端资源版本管理器
//		versionchecker = new KGameVersionControl(); // 2014-11-24 去掉这部分，版本控制已经交由专门的版本控制服务器
//		timer.newTimeSignal(versionchecker, KGameVersionControl.modifyCheckSeconds, TimeUnit.SECONDS); // 2014-11-24 去掉这部分
		
		try {
			KGameNoticeManager.load();
		} catch (Exception e) {
			throw new KGameServerException(e.getMessage());
		}
		
		KGameTips.submitWatcher(timer); // tips重新加载的定时任务 add @ 2015-01-07 11:50

		// 开始网络对外连接
		communication.start();
		logger.info("startup>>>Communication Started.{}",communication);
		
		logger.info("startup>>>OKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOKOK");

		//DEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUG
//		KGameTimerTask _task = new DebugTimer();
//		for (int i = 0; i < 3600; i++) {
//			timer.newTimeSignal(_task, i + 1, TimeUnit.SECONDS);
//			if (i < 90)
//				timer.newTimeSignal(_task, i + 1, TimeUnit.MINUTES);
//			if (i < 30)
//				timer.newTimeSignal(_task, i + 1, TimeUnit.HOURS);
//		}
		
//		timer.newTimeSignal(_task, 1, TimeUnit.SECONDS);
//		timer.newTimeSignal(_task, 1, TimeUnit.MINUTES);
//		timer.newTimeSignal(_task, 1, TimeUnit.HOURS);
//		
//		timer.newTimeSignal(_task, 1, TimeUnit.DAYS);
//		timer.printwheel();
		//DEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUGDEBUG
	}

	public void shutdown(final Channel cmdChannel) throws KGameServerException {
		if (!shutdown.compareAndSet(false, true)) {
			reportshutdowninfo2client(cmdChannel, "shutdown repeated call.");
			throw new IllegalStateException("shutdown repeated call.");
		}
		System.err
				.println("\r\n======================================================");
		logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!fe shutdown..!!!!!!!!!!!!!!!!!!!!!!!!!!");
		reportshutdowninfo2client(cmdChannel, "FE("
				+ communication.getSocketAddress().toString()
				+ ") accept shutdown command.");
		// 开启一条新的线程执行停服的操作
		new Thread(new Runnable() {
			@Override
			public void run() {

				logger.warn("fe shutdown...stop communication");
				reportshutdowninfo2client(cmdChannel,
						"fe shutdown...stop communication");
				communication.stop();

				logger.warn("fe shutdown...stop timer");
				reportshutdowninfo2client(cmdChannel,
						"fe shutdown...stop timer");
				timer.stop();// TODO 时效任务停止后的保存

				reportshutdowninfo2client(cmdChannel, "done");

				logger.warn("fe shutdown...releaseExternalResources, exit.");
				communication.releaseExternalResources();

				logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!fe shutdowned!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
}
