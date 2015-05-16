package com.koala.verserver;

import static org.jboss.netty.handler.codec.http.HttpHeaders.getHost;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.multipart.Attribute;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.CharsetUtil;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.KGameExcelFile;
import com.koala.game.util.KGameExcelTable.KGameExcelRow;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.PromoSupport;

public class KGameVerControlServerHandler extends SimpleChannelHandler
		implements KGameTimerTask, KGameVerControlProtocol {

	private final static KGameLogger logger = KGameLogger.getLogger(KGameVerControlServerHandler.class);
	private final static File xmlFile = new File("res/config/fe/VersionControl.xml");
	private final static String _notificationFilePath = "./res/config/iosNotification.xls";

	private long lastModifiedOfXml;
	public final static long modifyCheckSeconds = 60;// s

	ExecutionHandler executionHandler;
	KGameTimer timer;

	private final static int PROMOID_DEFAULT = -1;
	// private String appVersion;
	// private ApkUpdateInfo apkPatch;
	private final ConcurrentHashMap<Integer, ApkUpdateInfo> apkupdateinfos = new ConcurrentHashMap<Integer, ApkUpdateInfo>();
	private final ConcurrentHashMap<Integer, RespackUpdateInfo> respackupdateinfos = new ConcurrentHashMap<Integer, RespackUpdateInfo>();
	private final ConcurrentHashMap<Integer, SopackUpdateInfo> sopackupdateinfos = new ConcurrentHashMap<Integer, SopackUpdateInfo>();
	private boolean updateway_outside;// 20130714增加的通知客户端由系统系在，而不是在应用内下载，主要是防止客户端出现问题而采取的紧急处理办法，平时一定是false，紧急情况才会设为true
	private String compatible_md5_version;// 20130724增加md5的发送，为兼容旧版本需要定义大于此版本的才发md5，否则客户端会读取出错
	private final List<KGameVerControlServerHandler.FE> feList = new ArrayList<KGameVerControlServerHandler.FE>();
	private final List<Notification> notificationList = new ArrayList<KGameVerControlServerHandler.Notification>();
	private String miniresdir;//小包资源
	public final static String REQUEST_PATH_VERSIONCHECK = "vc";
	public final static String REQUEST_PATH_PUSH = "push";
	private final Map<String, String> serverConfigMap = new HashMap<String, String>();
	private KGamePush pushFunction;

	KGameVerControlServerHandler() throws Exception {
		loadConfig(false);
		loadNotification();
		pushFunction = new KGamePush();
	}

	private void loadConfig(boolean reload) {
		lastModifiedOfXml = xmlFile.lastModified();

		Document doc = XmlUtil.openXml(xmlFile);
		Element root = doc.getRootElement();

		// ////////////////////////////////////////////////////
		// 系统设置
		if (!reload) {
			Element eServerConfig = root.getChild("ServerConfig");
			Element eExecutionHandler = eServerConfig
					.getChild("ExecutionHandler");
			int corePoolSize = Integer.parseInt(eExecutionHandler
					.getAttributeValue("corePoolSize"));
			long maxChannelMemorySize = Long.parseLong(eExecutionHandler
					.getAttributeValue("maxChannelMemorySize"));
			long maxTotalMemorySize = Long.parseLong(eExecutionHandler
					.getAttributeValue("maxTotalMemorySize"));
			long keepAliveTimeMillis = Long.parseLong(eExecutionHandler
					.getAttributeValue("keepAliveTimeMillis"));
			// 一个专门用来处理服务器收到的消息（Upstream）的线程池，目的就是提高worker线程的效率
			executionHandler = new ExecutionHandler(
					new OrderedMemoryAwareThreadPoolExecutor(corePoolSize,
							maxChannelMemorySize, maxTotalMemorySize,
							keepAliveTimeMillis, TimeUnit.MILLISECONDS));
			// 时效任务
			Element eTimer = eServerConfig.getChild("Timer");
			timer = new KGameTimer(eTimer);
			logger.info("startup>>>CREATE TIMER {}", timer);
			timer.newTimeSignal(this, 2, TimeUnit.MINUTES);
		}
		// ////////////////////////////////////////////////////

		compatible_md5_version = root
				.getChildTextTrim("compatible_md5_version");

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>AppVersionControl
		Element eAppVersionControl = root.getChild("AppVersionControl");
		updateway_outside = Boolean.parseBoolean(eAppVersionControl
				.getAttributeValue("updateway_outside"));
		logger.info("updateway_outside:{}", updateway_outside);

		// appVersion =eAppVersionControl.getChildTextTrim("AppVersion");
		// logger.info("AppVersion:{}", appVersion);

		Element eApkPatch = eAppVersionControl.getChild("ApkPatch");
		// apkPatch = new ApkUpdateInfo();
		// apkPatch.promoid = PROMOID_DEFAULT;
		// apkPatch.filesize = Long.parseLong(eApkPatch
		// .getAttributeValue("filesize"));
		// apkPatch.url = eApkPatch.getTextTrim();
		// logger.info("ApkPatch:{}", apkPatch);

		List<Element> eApkUpdateByPromoChannels = eAppVersionControl
				.getChildren("ApkUpdateByPromoChannel");
		for (Element eApkUpdateByPromoChannel : eApkUpdateByPromoChannels) {
			ApkUpdateInfo aui = new ApkUpdateInfo();
			String pid = eApkUpdateByPromoChannel.getAttributeValue("promoid");
			aui.promoid = "default".equalsIgnoreCase(pid) ? PROMOID_DEFAULT
					: Integer.parseInt(pid);
			aui.filesize = Long.parseLong(eApkUpdateByPromoChannel
					.getAttributeValue("filesize"));
//			aui.appVer = eApkUpdateByPromoChannel.getAttributeValue("appver");
			aui.displayAppVer = eApkUpdateByPromoChannel.getAttributeValue("appver");
			aui.calAppVer = Float.parseFloat(aui.displayAppVer);
			aui.md5 = eApkUpdateByPromoChannel.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
			aui.url = eApkUpdateByPromoChannel.getTextTrim();
			
			String userconfirmlevel = eApkUpdateByPromoChannel
					.getAttributeValue("userconfirmlevel");
			if (userconfirmlevel != null) {
				aui.userconfirmlevel = Integer.parseInt(userconfirmlevel);
			}

			apkupdateinfos.put(aui.promoid, aui);
			logger.info("ApkUpdateByPromoChannel:{}", aui);
		}

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>ResVersionControl
		Element eResVersionControl = root.getChild("ResVersionControl");
		List<Element> eRespacks = eResVersionControl.getChildren("Respacks");
		for (Element eRespack : eRespacks) {
			RespackUpdateInfo rui = new RespackUpdateInfo();

			// 最新版本完整包
			Element eFullPack = eRespack.getChild("LastestFullRespack");
			String name0 = eFullPack.getAttributeValue("name").trim();
			String ver0 = eFullPack.getAttributeValue("ver").trim();
			long filesize0 = Long.parseLong(eFullPack.getAttributeValue(
					"filesize").trim());
			String md50 = eFullPack.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
			String url0 = eFullPack.getTextTrim();
			rui.lastestFullRespack = new Respack(name0, Integer.parseInt(ver0),
					filesize0, url0, md50);
			rui.lastestRespackVersion = rui.lastestFullRespack.ver;
			
			//add@20131114//////////////////////////////////////////////////////////////////////
			// mini包40级后的大资源包
			Element eMini40Res = eRespack.getChild("Mini40Res");
			if (eMini40Res != null) {
				String nameMini40Res = eMini40Res.getAttributeValue("name")
						.trim();
				String verMini40Res = eMini40Res.getAttributeValue("ver").trim();
				long filesizeMini40Res = Long.parseLong(eMini40Res
						.getAttributeValue("filesize").trim());
				String md5Mini40Res = eMini40Res.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
				String urlMini40Res = eMini40Res.getTextTrim();
				rui.mini40Res = new Respack(nameMini40Res,
						Integer.parseInt(verMini40Res), filesizeMini40Res,
						urlMini40Res, md5Mini40Res);
				System.out.println("》》》》》》rui.mini40Res,isBigPkg=" + rui.mini40Res.bigpkg + "，url=" + rui.mini40Res.url + "size=" + rui.mini40Res.filesize + "《《《《《《");
			}
			/////////////////////////////////////////////////////////////////////////////////

			// 各版本增量包
			List<Element> ePatches = eRespack.getChildren("Patch");
			for (Element ePatch : ePatches) {
				String name = ePatch.getAttributeValue("name").trim();
				String ver = ePatch.getAttributeValue("ver").trim();
				long filesize = Long.parseLong(ePatch.getAttributeValue(
						"filesize").trim());
				String md5 = ePatch.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
				String url = ePatch.getTextTrim();
				Respack patch = new Respack(name, Integer.parseInt(ver),
						filesize, url, md5);
				rui.patchs.add(patch);
				logger.info("Patch:{}", patch);
			}
			
			String userconfirmlevel = eRespack
					.getAttributeValue("userconfirmlevel");
			if (userconfirmlevel != null) {
				rui.userconfirmlevel = Integer.parseInt(userconfirmlevel);
			}

			String spid = eRespack.getAttributeValue("promoid");// 这里的promoid有可能是多个的
			String[] spids = spid.split(",");
			// if (spids.length == 1) {
			// rui.promoid = "default".equalsIgnoreCase(spid) ? PROMOID_DEFAULT
			// : Integer.parseInt(spid);
			// } else {
			// rui.promoid = PROMOID_DEFAULT;
			// }
			for (String pid : spids) {
				if (pid != null && pid.trim().length() > 0) {
					pid = pid.trim();
					int ppid = "default".equalsIgnoreCase(pid) ? PROMOID_DEFAULT
							: Integer.parseInt(pid);
					respackupdateinfos.put(ppid, rui);
					logger.info("put (promoid:{}) Respacks:{}", ppid, rui);
				}
			}
		}

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>SoVersionControl
		Element eSoVersionControl = root.getChild("SoVersionControl");
		List<Element> eSopacks = eSoVersionControl.getChildren("Sopacks");
		for (Element eSopack : eSopacks) {
			SopackUpdateInfo rui = new SopackUpdateInfo();

			// 最新版本完整包
			Element eFullPack = eSopack.getChild("LastestFullSopack");
			String name0 = eFullPack.getAttributeValue("name").trim();
			String ver0 = eFullPack.getAttributeValue("ver").trim();
			long filesize0 = Long.parseLong(eFullPack.getAttributeValue(
					"filesize").trim());
			String md50 = eFullPack.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
			String url0 = eFullPack.getTextTrim();
			rui.lastestFullSopack = new Sopack(name0, Integer.parseInt(ver0),
					filesize0, url0, md50);
			rui.lastestSopackVersion = rui.lastestFullSopack.ver;

			// 各版本增量包
			List<Element> ePatches = eSopack.getChildren("Patch");
			for (Element ePatch : ePatches) {
				String name = ePatch.getAttributeValue("name").trim();
				String ver = ePatch.getAttributeValue("ver").trim();
				long filesize = Long.parseLong(ePatch.getAttributeValue(
						"filesize").trim());
				String md5 = ePatch.getAttributeValue("md5");// 20130724每个文件增加md5校验码，发给客户端自己对比
				String url = ePatch.getTextTrim();
				Sopack patch = new Sopack(name, Integer.parseInt(ver),
						filesize, url, md5);
				rui.patchs.add(patch);
				logger.info("Patch:{}", patch);
			}
			
			String userconfirmlevel = eSopack
					.getAttributeValue("userconfirmlevel");
			if (userconfirmlevel != null) {
				rui.userconfirmlevel = Integer.parseInt(userconfirmlevel);
			}

			String spid = eSopack.getAttributeValue("promoid");// 这里的promoid有可能是多个的
			String[] spids = spid.split(",");
			// if (spids.length == 1) {
			// rui.promoid = "default".equalsIgnoreCase(spid) ? PROMOID_DEFAULT
			// : Integer.parseInt(spid);
			// } else {
			// rui.promoid = PROMOID_DEFAULT;
			// }
			for (String pid : spids) {
				if (pid != null && pid.trim().length() > 0) {
					pid = pid.trim();
					int ppid = "default".equalsIgnoreCase(pid) ? PROMOID_DEFAULT
							: Integer.parseInt(pid);
					sopackupdateinfos.put(ppid, rui);
					logger.info("put (promoid:{}) Sopacks:{}", ppid, rui);
				}
			}
		}

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>FE
		Element eFEList = root.getChild("FEList");
		List<Element> fes = eFEList.getChildren("FE");
		List<FE> tempList = new ArrayList<FE>();
		for (Element f : fes) {
			FE fe = new FE();
			fe.ip = f.getAttributeValue("ip");
			fe.port = Integer.parseInt(f.getAttributeValue("port"));
			fe.promoids = Arrays.asList(f.getChildTextTrim("promoid").split(","));
			String apkver = f.getChildTextTrim("apkver");
			int idx = apkver.indexOf(",");
			String apkver0 = apkver.substring(0, idx);
			String apkver1 = apkver.substring(Math.min(idx+1, apkver.length()));
//			fe.apkVerLower = (apkver0==null||apkver0.length()==0)?String.valueOf(0):apkver0;
//			fe.apkVerUpper = (apkver1==null||apkver1.length()==0)?String.valueOf(Integer.MAX_VALUE):apkver1;
			fe.apkVerLower = (apkver0 == null || apkver0.length() == 0) ? 0 : Float.parseFloat(apkver0);
			fe.apkVerUpper = (apkver1 == null || apkver1.length() == 0) ? Float.MAX_VALUE : Float.parseFloat(apkver1);
			if (!tempList.contains(fe)) {
				logger.info(fe.toString());
				tempList.add(fe);
			}
		}
		feList.clear();
		feList.addAll(tempList);
		
		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Global config
		@SuppressWarnings("unchecked")
		List<Element> globalConfigElementList = root.getChild("serverConfig").getChildren();
		if (globalConfigElementList.size() > 0) {
			Element tempConfig;
			for (int i = 0; i < globalConfigElementList.size(); i++) {
				tempConfig = globalConfigElementList.get(i);
				serverConfigMap.put(tempConfig.getAttributeValue("name"), tempConfig.getAttributeValue("value"));
			}
		}
		
		//小包缺失资源的目录，包含所有单独资源文件，客户端发现缺失的时候会从此目录申请下载个别文件（随FE信息一起返回）
		miniresdir = root.getChildTextTrim("MiniResDir");
	}
	
	private void loadNotification() throws Exception {
		KGameExcelFile file = new KGameExcelFile(_notificationFilePath);
		KGameExcelRow[] allRows = file.getTable("notification", 2).getAllDataRows();
		KGameExcelRow row;
		Notification temp;
		Calendar cNow = Calendar.getInstance();
		Calendar cNotificationStart = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		int nowYear = cNow.get(Calendar.YEAR);
		for(int i = 0; i < allRows.length; i++) {
			row = allRows[i];
			temp = new Notification();
			String startTime = row.getData("startTime");
			cNotificationStart.setTime(sdf.parse(startTime));
			temp.content = row.getData("content");
			int year = cNotificationStart.get(Calendar.YEAR);
			temp.year = String.valueOf(year);
			int month = cNotificationStart.get(Calendar.MONTH);
			temp.month = String.valueOf(month + 1);
			int day = cNotificationStart.get(Calendar.DAY_OF_MONTH);
			temp.day = String.valueOf(day);
			int hour = cNotificationStart.get(Calendar.HOUR_OF_DAY);
			temp.hour = String.valueOf(hour);
			int minute = cNotificationStart.get(Calendar.MINUTE);
			temp.minute = String.valueOf(minute);
			int repeatType = row.getInt("repeatType");
			temp.repeatType = repeatType;
			temp.strRepeatType = String.valueOf(repeatType);
			temp.startTime = cNotificationStart.getTimeInMillis();
			if (year < nowYear) {
				continue;
			}
			if (month < Calendar.JANUARY || month > Calendar.DECEMBER) {
				throw new RuntimeException("月份异常！" + temp.month + "，推送的内容：" + temp.content);
			}
			if(day < 1 || day > 31) {
				throw new RuntimeException("日期异常！" + temp.day + "，推送的内容：" + temp.content);
			}
			if(hour < 0 || hour > 23) {
				throw new RuntimeException("时间异常！" + temp.hour + "，推送的内容：" + temp.content);
			}
			if(minute < 0 || minute > 59) {
				throw new RuntimeException("分钟异常！" + temp.minute + "，推送的内容：" + temp.content);
			}
			switch(month) {
			case Calendar.APRIL:
			case Calendar.JUNE:
			case Calendar.SEPTEMBER:
			case Calendar.NOVEMBER:
				if(day > 30) {
					throw new RuntimeException("月份" + temp.month + "最多只能有30天！推送的内容：" + temp.content);
				}
				break;
			case Calendar.FEBRUARY:
				if(day > 28) {
					if(year % 4 != 0 || year % 400 == 0) {
						throw new RuntimeException("年份" + temp.year + "的2月，只能有28天！");
					}
				}
				break;
			}
			switch (repeatType) {
			case Notification.REPEAT_TYPE_NONE:
			case Notification.REPEAT_TYPE_MINUTE:
			case Notification.REPEAT_TYPE_HOUR:
			case Notification.REPEAT_TYPE_DAY:
			case Notification.REPEAT_TYPE_WEEK:
				break;
			default:
				throw new RuntimeException("重复类型异常！推送的内容：" + temp.content);
			}
			notificationList.add(temp);
		}
	}
	
	private void appendNotificationInfo(Element root, int promoId) {
		// 以下是推送内容 start at 2014-09-29 16:36
		Element eNotification = new Element("Notification");
		root.addContent(eNotification);
		if (notificationList.size() > 0) {
			Notification notification;
			long nowTimeMillis = System.currentTimeMillis();
			Element notice;
			for (int i = 0; i < notificationList.size(); i++) {
				notification = notificationList.get(i);
				if (notification.startTime < nowTimeMillis && notification.repeatType == Notification.REPEAT_TYPE_NONE) {
					continue;
				}
				notice = new Element(XML_TAG_NOTIFICATION_NOTICE);
				notice.setAttribute(XML_TAG_NOTIFICATION_MSG, notification.content);
				notice.setAttribute(XML_TAG_NOTIFICATION_YEAR, notification.year);
				notice.setAttribute(XML_TAG_NOTIFICATION_MONTH, notification.month);
				notice.setAttribute(XML_TAG_NOTIFICATION_DAY, notification.day);
				notice.setAttribute(XML_TAG_NOTIFICATION_HOUR, notification.hour);
				notice.setAttribute(XML_TAG_NOTIFICATION_MINUTE, notification.minute);
				notice.setAttribute(XML_TAG_NOTIFICATION_REPEATTYPE, notification.strRepeatType);
				eNotification.addContent(notice);
			}
		}
		// 推送内容END
	}
	
	private void appendServerConfig(Element root, int promoId) {
		// 以下是全局配置 start at 2014-11-03 15:58
		Element eServerConfig = new Element(XML_TAG_SERVER_CONFIG);
		root.addContent(eServerConfig);
		if (serverConfigMap.size() > 0) {
			Element eConfig;
			Map.Entry<String, String> entry;
			for (Iterator<Map.Entry<String, String>> itr = serverConfigMap.entrySet().iterator(); itr.hasNext();) {
				entry = itr.next();
				eConfig = new Element(entry.getKey());
				eConfig.setText(entry.getValue());
				eServerConfig.addContent(eConfig);
			}
		}
		// 全局配置END
	}

	// TODO FE的选择需要加策略
//	private FE selectFE(int parentPromoId, int actualPromoId, String apkver) {
	private FE selectFE(int parentPromoId, int actualPromoId, float apkver) {
		String strParentPromoId = String.valueOf(parentPromoId);
		String strPromoId = String.valueOf(actualPromoId);
		for (FE fe : feList) {
			// 先找子渠道
			if (fe.promoids.contains(strPromoId)) {
//				if (apkver.compareTo(fe.apkVerLower) >=0
//						&& apkver.compareTo(fe.apkVerUpper) <= 0) {
//					return fe;
//				}
				if (apkver <= fe.apkVerUpper && apkver >= fe.apkVerLower) {
					return fe;
				}
			}
		}
		for (FE fe : feList) {
			// 再找父渠道
			if (fe.promoids.contains(strParentPromoId)) {
//				if (apkver.compareTo(fe.apkVerLower) >=0
//						&& apkver.compareTo(fe.apkVerUpper) <= 0) {
//					return fe;
//				}
				if (apkver <= fe.apkVerUpper && apkver >= fe.apkVerLower) {
					return fe;
				}
			}
		}
		for (FE fe : feList) {
			// 再找默认
			if (fe.promoids.contains("default")) {
//				if (apkver.compareTo(fe.apkVerLower) >= 0
//						&& apkver.compareTo(fe.apkVerUpper) <= 0) {
//					return fe;
//				}
				if (apkver <= fe.apkVerUpper && apkver >= fe.apkVerLower) {
					return fe;
				}
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return KGameVerControlServerHandler.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		try {
			if (xmlFile.exists() && xmlFile.lastModified() != lastModifiedOfXml) {
				logger.info("--------------reload xml {}", xmlFile);
				try {
					loadConfig(true);
				} catch (Exception e) {
					logger.warn("reload xmlfile exception. {}", e);
				}
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
		logger.warn("RejectedExecutionException {}", e);
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	private final class ApkUpdateInfo {
		int promoid;
		long filesize;
		String url;
		String displayAppVer;
		float calAppVer;
		String md5;
		int userconfirmlevel;

		@Override
		public String toString() {
			return "ApkUpdateInfo [promoid=" + promoid + ", filesize="
					+ filesize + ", url=" + url + ", calAppVer=" + calAppVer + ", displayAppVer=" + displayAppVer
					+ ", md5=" + md5 + "]";
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	private final class RespackUpdateInfo {
		// int promoid;
		int lastestRespackVersion;
		Respack lastestFullRespack;
		Respack mini40Res;
		final List<KGameVerControlServerHandler.Respack> patchs = new ArrayList<KGameVerControlServerHandler.Respack>();
		int userconfirmlevel;

		@Override
		public String toString() {
			return "RespackUpdateInfo [lastestRespackVersion="
					+ lastestRespackVersion + ", lastestFullRespack="
					+ lastestFullRespack + ", patchs=" + patchs.size() + "]";
		}
	}

	private final class Respack {
		String name;
		int ver;
		long filesize;
		String url;
		String md5;
		String bigpkg;//是否mini包中的大资源

		Respack(String name, int ver, long filesize, String url, String md5) {
			this.name = name;
			this.ver = ver;
			this.filesize = filesize;
			this.url = url;
			this.md5 = md5;
		}

		@Override
		public String toString() {
			return "Respack [name=" + name + ", ver=" + ver + ", filesize="
					+ filesize + ", url=" + url + ", md5=" + md5+",bigpkg="+bigpkg + "]";
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	private final class SopackUpdateInfo {
		// int promoid;
		int lastestSopackVersion;
		Sopack lastestFullSopack;
		final List<KGameVerControlServerHandler.Sopack> patchs = new ArrayList<KGameVerControlServerHandler.Sopack>();
		int userconfirmlevel;

		@Override
		public String toString() {
			return "SopackUpdateInfo [lastestSopackVersion="
					+ lastestSopackVersion + ", lastestFullSopack="
					+ lastestFullSopack + ", patchs=" + patchs.size() + "]";
		}
	}

	private final class Sopack {
		String name;
		int ver;
		long filesize;
		String url;
		String md5;

		Sopack(String name, int ver, long filesize, String url, String md5) {
			this.name = name;
			this.ver = ver;
			this.filesize = filesize;
			this.url = url;
			this.md5 = md5;
		}

		@Override
		public String toString() {
			return "Sopack [name=" + name + ", ver=" + ver + ", filesize="
					+ filesize + ", url=" + url + ", md5=" + md5 + "]";
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	private final class FE {
		String ip;
		int port;
		List<String> promoids;
//		String apkVerLower;
//		String apkVerUpper;
		float apkVerLower;
		float apkVerUpper;
		@Override
		public String toString() {
			return "FE [ip=" + ip + ", port=" + port + ", promoids=" + promoids
					+ ", apkVerLower=" + apkVerLower + ", apkVerUpper="
					+ apkVerUpper + "]";
		}
	}
	
	private class Notification {
		
		private static final int REPEAT_TYPE_NONE = 0;
		private static final int REPEAT_TYPE_MINUTE = 1;
		private static final int REPEAT_TYPE_HOUR = 2;
		private static final int REPEAT_TYPE_DAY = 3;
		private static final int REPEAT_TYPE_WEEK = 4;
		
		String content;
		String year;
		String month;
		String day;
		String hour;
		String minute;
		int repeatType;
		String strRepeatType;
		long startTime;
	}

	/***********************************************************************************
	 ***********************************************************************************/
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {

		HttpRequest request = (HttpRequest) e.getMessage();
		System.out.println("--------------------------------" + request);
		// DEBUG
		// _printHttpInfo(request);

		// RESPONSE
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		// ChannelBuffer content = ChannelBuffers.dynamicBuffer();
		
		
		/**增加PUSH的功能模块@20131121*/
		if(pushFunction!=null){
			if(pushFunction.messageReceived(ctx, request, response)){
				return;
			}
		}
		
		//请求格式
		//http://10.10.0.188:8080/vc?p=0&pid=1002&appver=1.0&resver=999&sover=999&bigpkg=0&sign=jh3jlsd234jl42l3j
		
		Document doc = new Document();
		Element root = new Element(XML_TAG_ROOT);
		doc.setRootElement(root);
		try {
			Map<String, String> params = readContentAsParams(request);
			// if ((!params.containsKey(PKEY_PROMOID))
			// || (!params.containsKey(PKEY_APPVER))
			// || (!params.containsKey(PKEY_RESVER))
			// || (!params.containsKey(PKEY_SIGN))) {
			// sendError(ctx, request, response, doc, PL_VERCHECK_PARAM_ERROR,
			// KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
			// return;
			// }

			String platform = params.get(PKEY_PLATFORM);
			if(platform==null||platform.length()<=0){
				logger.debug("platform==null||platform.length()<=0");
				sendError(ctx, request, response, doc, PL_VERCHECK_PARAM_ERROR,
						KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
				return;
			}
//			int promoid = Integer.parseInt(params.get(PKEY_PROMOID));
//			int actualPromoId = promoid;
//			promoid = PromoSupport.computeParentPromoID(promoid);
			int actualPromoId = Integer.parseInt(params.get(PKEY_PROMOID));
			int parentPromoId = PromoSupport.computeParentPromoID(actualPromoId);
//			String appVer = params.get(PKEY_APPVER);
			float appVer = 0;
			try {
				appVer = Float.parseFloat(params.get(PKEY_APPVER));
			} catch (Exception ex) {
				logger.error("错误的版本号：{}", params.get(PKEY_APPVER));
			}
			int soVer = Integer.parseInt(params.get(PKEY_SOVER));
			int resVer = Integer.parseInt(params.get(PKEY_RESVER));
			String sign = params.get(PKEY_SIGN);
			String bigpkg = params.get("bigpkg");//是否要下载mini包的40级大资源包
			boolean isbigpkg = "1".equals(bigpkg);
			logger.debug("MID_VERCHECK  promoid={},appVer={},resVer={},bigpkg={}",
					parentPromoId, appVer, resVer,bigpkg);
			StringBuilder buf = new StringBuilder();
			buf.append(PKEY_PLATFORM).append("=").append(platform).append("&")
					.append(PKEY_PROMOID).append("=").append(parentPromoId)
					.append("&").append(PKEY_APPVER).append("=").append(appVer)
					.append("&").append(PKEY_SOVER).append("=").append(soVer)
					.append("&").append(PKEY_RESVER).append("=").append(resVer);
			if (bigpkg != null) {
				buf.append("&").append("bigpkg=").append(bigpkg);
			}
			buf.append("&key=").append(SIGN_KEY);
//			if (!MD5.MD5Encode(buf.toString()).equals(sign)) {
//				sendError(ctx, request, response, doc, PL_VERCHECK_SIGN_ERROR,
//						KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
//				return;
//			}

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>先检查APK包是否需要更新（有更新的话return）
			ApkUpdateInfo apkud = apkupdateinfos.get(actualPromoId);
			if (apkud == null) {
				//如果找不到，就从其父渠道找找看有没有，如果再没有就用默认的！
				if ((apkud = apkupdateinfos.get(parentPromoId)) == null) {
					apkud = apkupdateinfos.get(PROMOID_DEFAULT);
				}
			}
			if (apkud == null) {
				sendError(ctx, request, response, doc, PL_VERCHECK_NOTFOUND,
						KGameTips.get("PL_VERCHECK_NOTFOUND"));
				return;
			}
//			if (apkud.calAppVer.compareTo(appVer) > 0) {
			if (apkud.calAppVer > appVer) {
				// APK版本小于最新版本需要更新APK！！！
				root.addContent(new Element(XML_TAG_RESPONSECODE).addContent(String
						.valueOf(updateway_outside ? PL_VERCHECK_UPDATE_APK_SYS
								: PL_VERCHECK_UPDATE_APK)));
				root.addContent(new Element(XML_TAG_TIPS).addContent(KGameTips
						.get("PL_VERCHECK_UPDATE_APK")));
				Element eapk = new Element(XML_TAG_APK);
				
				// userconfirmlevel
				eapk.addContent(new Element("userconfirmlevel")
						.addContent(String.valueOf(apkud.userconfirmlevel)));
				
//				eapk.addContent(new Element(XML_TAG_APPVER)
//						.addContent(apkud.calAppVer));
				eapk.addContent(new Element(XML_TAG_APPVER)
				.addContent(apkud.displayAppVer));
				eapk.addContent(new Element(XML_TAG_FILESIZE).addContent(String
						.valueOf(apkud.filesize)));
				eapk.addContent(new Element(XML_TAG_DOWNURL)
						.addContent(apkud.url));
				eapk.addContent(new Element(XML_TAG_MD5).addContent(apkud.md5));// 20130724每个文件增加md5校验码，发给客户端自己对比
				root.addContent(eapk);
				appendFeAndMiniresdirInfo(root, parentPromoId, actualPromoId, appVer);
				sendHttpResponse(ctx, request, response, doc);
				return;
			}
			
			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>第二检测so包是否需要更新（有更新也继续检测res）
			boolean bsoupdate = false;
			SopackUpdateInfo soud = sopackupdateinfos.get(parentPromoId);
			if(soud==null){
				soud = sopackupdateinfos.get(PROMOID_DEFAULT);
			}
			if (soud == null) {
				sendError(ctx, request, response, doc, PL_VERCHECK_NOTFOUND,
						KGameTips.get("PL_VERCHECK_NOTFOUND"));
				return;
			}
			//判断so是否需要更新
			if(soud.lastestSopackVersion > soVer){
				root.addContent(new Element(XML_TAG_RESPONSECODE)
						.addContent(String.valueOf(PL_VERCHECK_UPDATE_RESPACKS)));
				root.addContent(new Element(XML_TAG_TIPS).addContent(KGameTips
						.get("PL_VERCHECK_UPDATE_RESPACKS")));
				// userconfirmlevel
				root.addContent(new Element("userconfirmlevel")
						.addContent(String.valueOf(soud.userconfirmlevel)));
				
				long totalsize=0;
				List<Sopack> list = new ArrayList<KGameVerControlServerHandler.Sopack>(soud.patchs.size());
				for (Sopack sopack : soud.patchs) {
					if(sopack.ver > soVer){
						list.add(sopack);
						totalsize += sopack.filesize;
					}
				}
				if(totalsize>=soud.lastestFullSopack.filesize){
					// 下载最新完整包更划算
					writesopackintomessage(soud.lastestFullSopack, root);
				}else{
					// 下载N个增量包更划算
					for (Sopack sopack : list) {
						writesopackintomessage(sopack, root);
					}
				}
				list.clear();
				list = null;
				bsoupdate = true;
			}
			

			// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>最后检查资源包是否需要更新
			boolean bresupdate = false;
			RespackUpdateInfo resud = respackupdateinfos.get(parentPromoId);
			if (resud == null) {
				resud = respackupdateinfos.get(PROMOID_DEFAULT);
			}
			// 根据版本判断是否需要更新
			if (resud.lastestRespackVersion > resVer) {
				if (!bsoupdate) {//资源跟SO是一起更新的
					root.addContent(new Element(XML_TAG_RESPONSECODE)
							.addContent(String
									.valueOf(PL_VERCHECK_UPDATE_RESPACKS)));
					root.addContent(new Element(XML_TAG_TIPS)
							.addContent(KGameTips
									.get("PL_VERCHECK_UPDATE_RESPACKS")));
					// userconfirmlevel
					root.addContent(new Element("userconfirmlevel")
							.addContent(String.valueOf(resud.userconfirmlevel)));
				}
				long totalsize = 0;
				List<Respack> list = new ArrayList<Respack>(resud.patchs.size());
				for (Respack respack : resud.patchs) {
					if (respack.ver > resVer) {
						list.add(respack);
						totalsize += respack.filesize;
					}
				}
				// 看下载N个增量包和最新完整包哪个更划算
				if (totalsize >= resud.lastestFullRespack.filesize) {
					// 下载最新完整包更划算
					// content.writeInt(1);
					writerespackintomessage(resud.lastestFullRespack, root);
				} else {
					// 下载N个增量包更划算
					// content.writeInt(list.size());
					for (Respack respack : list) {
						writerespackintomessage(respack, root);
					}
				}
				list.clear();
				list = null;
				bresupdate = true;
			}
			
			//综合so和res是否有更新再一起发到客户端（add@20131114：mini包40级后的大资源包）
			if (bsoupdate || bresupdate || isbigpkg) {
				if (isbigpkg) {// mini包40级后的大资源包需要更新
					if ((!bsoupdate) && (!bresupdate)) {
						root.addContent(new Element(XML_TAG_RESPONSECODE).addContent(String.valueOf(PL_VERCHECK_UPDATE_RESPACKS)));
						root.addContent(new Element(XML_TAG_TIPS).addContent(KGameTips.get("PL_VERCHECK_UPDATE_RESPACKS")));
						// userconfirmlevel
						root.addContent(new Element("userconfirmlevel").addContent(String.valueOf(resud.userconfirmlevel)));
					}
					writerespackintomessage(resud.mini40Res, root, isbigpkg);
				}
				appendFeAndMiniresdirInfo(root, parentPromoId, actualPromoId, appVer);
				// sendHttpResponse(ctx, request, response, doc);
				// return;
			} else {
				// 无需更新
				root.addContent(new Element(XML_TAG_RESPONSECODE).addContent(String.valueOf(PL_VERCHECK_NEWEST)));
				root.addContent(new Element(XML_TAG_TIPS).addContent(KGameTips.get("PL_VERCHECK_NEWEST")));

				appendFeAndMiniresdirInfo(root, parentPromoId, actualPromoId, appVer);
				// sendHttpResponse(ctx, request, response, doc);
			}
			// 以下是推送内容 start at 2014-09-29 16:36
			appendNotificationInfo(root, actualPromoId);
			appendServerConfig(root, actualPromoId);
			// 推送内容END
			sendHttpResponse(ctx, request, response, doc);
		} catch (Throwable e0) {
			e0.printStackTrace();
			sendError(ctx, request, response, doc, PL_VERCHECK_PARAM_ERROR,
					KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
		}
	}

//	/**
//	 * 版本检测（FE）- 【20130611更新了协议】【20130724更新了协议添加md5】
//	 * 
//	 * <pre>
//	 * 客户端请求采用HTTP GET方法
//	 * 携带参数范例：pid=1&appver=1.2&resver=99&sign=jh3jlsd234jl42l3j
//	 * 【注】sign的生成采用MD5加密，MD5("pid=1&appver=1.2&resver=99&key=kola")
//	 * 
//	 * --------------------------------
//	 * 响应PL:
//	 *   int responsecode = 
//	 *     {@link #PL_VERCHECK_NEWEST}/ 
//	 *     {@link #PL_VERCHECK_UPDATE_APK}/
//	 *     {@link #PL_VERCHECK_UPDATE_APK_SYS}/
//	 *     {@link #PL_VERCHECK_UPDATE_RESPACKS}/
//	 *     {@link #PL_VERSIONFORMAT_ERROR}/
//	 *     {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
//	 *   String tips;//版本更新结果提示
//	 *   if({@link #PL_VERCHECK_UPDATE_APK}){
//	 *     String lastestAppVer; //当前最新版本号
//	 *     long apkfilesize;     //apk文件大小
//	 *     String apkdownloadurl;//下载地址
//	 *     String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
//	 *   }
//	 *   else if({@link #PL_VERCHECK_UPDATE_RESPACKS}){
//	 *     int updatefileN;//需要更新的文件数量
//	 *     for(updatefileN){
//	 *       String filename;//文件名
//	 *       int patchver;//版本号,为整型值
//	 *       long filesize;//文件大小
//	 *       String downloadurl;//HTTP下载地址
//	 *       String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
//	 *     }
//	 *   }
//	 *   else if({@link #PL_VERCHECK_NEWEST}){
//	 *     String feip;
//	 *     int feport;
//	 *   }
//	 * </pre>
//	 * 
//	 * @deprecated 已改成用xml格式返回
//	 */
//	// @Override
//	public void messageReceived0000(ChannelHandlerContext ctx, MessageEvent e)
//			throws Exception {
//		HttpRequest request = (HttpRequest) e.getMessage();
//		System.out.println("--------------------------------" + request);
//		// DEBUG
//		_printHttpInfo(request);
//
//		// RESPONSE
//		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
//		ChannelBuffer content = ChannelBuffers.dynamicBuffer();
//
//		Map<String, String> params = readContentAsParams(request);
//		if ((!params.containsKey(PKEY_PROMOID))
//				|| (!params.containsKey(PKEY_APPVER))
//				|| (!params.containsKey(PKEY_RESVER))
//				|| (!params.containsKey(PKEY_SIGN))) {
//			content.writeInt(PL_VERCHECK_PARAM_ERROR);
//			writeUtf8String(content, KGameTips.get("PL_VERCHECK_PARAM_ERROR"));
//			sendHttpResponse(ctx, request, response, content);
//			return;
//		}
//
//		int promoid = Integer.parseInt(params.get(PKEY_PROMOID));
//		promoid = PromoSupport.computeParentPromoID(promoid);
//		
//		String appVer = params.get(PKEY_APPVER);
//		int resVer = Integer.parseInt(params.get(PKEY_RESVER));
//		String sign = params.get(PKEY_SIGN);
//		logger.debug("MID_VERCHECK  promoid={},appVer={},resVer={}", promoid,
//				appVer, resVer);
//
//		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>先检查APK包是否需要更新
//		ApkUpdateInfo apkud = apkupdateinfos.get(promoid);
//		if (apkud == null) {
//			apkud = apkupdateinfos.get(PROMOID_DEFAULT);
//		}
//
//		if (apkud == null) {
//			content.writeInt(PL_VERCHECK_NOTFOUND);
//			writeUtf8String(content, KGameTips.get("PL_VERCHECK_NOTFOUND"));
//			sendHttpResponse(ctx, request, response, content);
//			return;
//		}
//
//		if (apkud.appVer.compareTo(appVer) > 0) {
//			// APK版本小于最新版本需要更新APK！！！
//			content.writeInt(updateway_outside ? PL_VERCHECK_UPDATE_APK_SYS
//					: PL_VERCHECK_UPDATE_APK);
//			writeUtf8String(content, KGameTips.get("PL_VERCHECK_UPDATE_APK"));
//			writeUtf8String(content, apkud.appVer);
//			content.writeLong(apkud.filesize);
//			writeUtf8String(content, apkud.url);
//			if (appVer.compareTo(compatible_md5_version) > 0) {
//				writeUtf8String(content, apkud.md5);// 20130724每个文件增加md5校验码，发给客户端自己对比
//			}
//			sendHttpResponse(ctx, request, response, content);
//			return;
//		}
//
//		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>再检查资源包是否需要更新
//		RespackUpdateInfo resud = respackupdateinfos.get(promoid);
//		if (resud == null) {
//			resud = respackupdateinfos.get(PROMOID_DEFAULT);
//		}
//		// 根据版本判断是否需要更新
//		if (resud.lastestRespackVersion > resVer) {
//			content.writeInt(PL_VERCHECK_UPDATE_RESPACKS);
//			writeUtf8String(content,
//					KGameTips.get("PL_VERCHECK_UPDATE_RESPACKS"));
//			long totalsize = 0;
//			List<Respack> list = new ArrayList<Respack>(resud.patchs.size());
//			for (Respack respack : resud.patchs) {
//				if (respack.ver > resVer) {
//					list.add(respack);
//					totalsize += respack.filesize;
//				}
//			}
//			// 看下载N个增量包和最新完整包哪个更划算
//			if (totalsize >= resud.lastestFullRespack.filesize) {
//				// 下载最新完整包更划算
//				content.writeInt(1);
//				writerespackintomessage(resud.lastestFullRespack, content,
//						appVer);
//			} else {
//				// 下载N个增量包更划算
//				content.writeInt(list.size());
//				for (Respack respack : list) {
//					writerespackintomessage(respack, content, appVer);
//				}
//			}
//			list.clear();
//			list = null;
//			sendHttpResponse(ctx, request, response, content);
//			return;
//		}
//		// 无需更新
//		content.writeInt(PL_VERCHECK_NEWEST);
//		writeUtf8String(content, KGameTips.get("PL_VERCHECK_NEWEST"));
//		FE fe = selectFE(promoid, appVer);
//		writeUtf8String(content, fe.ip);
//		content.writeInt(fe.port);
//		sendHttpResponse(ctx, request, response, content);
//	}

	private void appendFeAndMiniresdirInfo(Element root, int parentPromoId, int promoId, float apkVer) {
		// /////////////////////////////////////////
		Element eminiresdir = new Element("miniresdir");
		eminiresdir.addContent(miniresdir);
		root.addContent(eminiresdir);
		// /////////////////////////////////////////
		FE fe = selectFE(parentPromoId, promoId, apkVer);
		if (fe != null) {
			Element efe = new Element(XML_TAG_FE);
			efe.addContent(new Element(XML_TAG_FE_IP).addContent(fe.ip));
			efe.addContent(new Element(XML_TAG_FE_PORT).addContent(String
					.valueOf(fe.port)));
			root.addContent(efe);
		} else {
			logger.error("找不到符合条件的FE！！！");
		}
	}
	
	private void writesopackintomessage(Sopack sopack, Element root) {
		Element patch = new Element(XML_TAG_SOPATCH);
		patch.addContent(new Element(XML_TAG_FILENAME).addContent(sopack.name));
		patch.addContent(new Element(XML_TAG_SOVER).addContent(String
				.valueOf(sopack.ver)));
		patch.addContent(new Element(XML_TAG_FILESIZE).addContent(String
				.valueOf(sopack.filesize)));
		patch.addContent(new Element(XML_TAG_DOWNURL).addContent(sopack.url));
		patch.addContent(new Element(XML_TAG_MD5).addContent(sopack.md5));// 20130724每个文件增加md5校验码，发给客户端自己对比
		root.addContent(patch);
		// logger.debug("writerespackintomessage: {}", respack);
	}

	private void writerespackintomessage(Respack respack, Element root,boolean isBigPkg) {
		Element patch = new Element(XML_TAG_RESPATCH);
		patch.addContent(new Element(XML_TAG_FILENAME).addContent(respack.name));
		patch.addContent(new Element(XML_TAG_RESVER).addContent(String
				.valueOf(respack.ver)));
		patch.addContent(new Element(XML_TAG_FILESIZE).addContent(String
				.valueOf(respack.filesize)));
		patch.addContent(new Element(XML_TAG_DOWNURL).addContent(respack.url));
		patch.addContent(new Element(XML_TAG_MD5).addContent(respack.md5));// 20130724每个文件增加md5校验码，发给客户端自己对比
		patch.addContent(new Element("isBigPkg").addContent(String.valueOf(isBigPkg)));
		root.addContent(patch);
		// logger.debug("writerespackintomessage: {}", respack);
	}
	
	private void writerespackintomessage(Respack respack, Element root){
		writerespackintomessage(respack, root, false);
	}

	@Deprecated
	private void writerespackintomessage(Respack respack,
			ChannelBuffer content, String appVer) {
		writeUtf8String(content, respack.name);
		content.writeInt(respack.ver);
		content.writeLong(respack.filesize);
		writeUtf8String(content, respack.url);
		if (appVer.compareTo(compatible_md5_version) > 0) {
			writeUtf8String(content, respack.md5);// 20130724每个文件增加md5校验码，发给客户端自己对比
		}
		logger.debug("writerespackintomessage: {}", respack);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		e.getCause().printStackTrace();
		// super.exceptionCaught(ctx, e);
	}

	static Map<String, String> readContentAsParams(HttpRequest request)
			throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		// HttpRequest request = (HttpRequest) e.getMessage();

		// GET>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		if (HttpMethod.GET.equals(request.getMethod())) {
			logger.info("【GET】{}", request.getUri());
			QueryStringDecoder decoderQuery = new QueryStringDecoder(
					request.getUri(), true);
			Map<String, List<String>> uriAttributes = decoderQuery
					.getParameters();
			for (String key : uriAttributes.keySet()) {
				for (String valuen : uriAttributes.get(key)) {
					params.put(key, valuen);
					logger.debug("[Param] {} = {}", key, valuen);
				}
			}
		}
		// POST>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		else if (HttpMethod.POST.equals(request.getMethod())
				|| HttpMethod.PUT.equals(request.getMethod())) {
			if (request.isChunked()) {
				// chunk方式
				logger.error("Not Support Chunk Version.");
			} else {
				// 非chunk方式
				HttpPostRequestDecoder postDecoder = new HttpPostRequestDecoder(
						request);
				List<InterfaceHttpData> datas = postDecoder.getBodyHttpDatas();
				if (datas == null || datas.size() <= 0) {
					// String postContent = request.getContent().toString(
					// Charset.forName("UTF-8"));
					// logger.info("【POST】{}", postContent);
				} else {
					for (InterfaceHttpData data : datas) {
						if (data.getHttpDataType() == HttpDataType.Attribute) {
							Attribute attribute = (Attribute) data;
							logger.debug("[Attribute] {} = {}",
									attribute.getName(), attribute.getValue());
							params.put(attribute.getName(),
									attribute.getValue());
						}
					}
				}
			}
		}
		return params;
	}

	private static String readContentAsString(HttpRequest request) {
		// GET>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		if (HttpMethod.GET.equals(request.getMethod())) {
			URI uri = null;
			try {
				uri = new URI(request.getUri());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			return uri == null ? "" : uri.getQuery();
		}
		// POST>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		else if (HttpMethod.POST.equals(request.getMethod())
				|| HttpMethod.PUT.equals(request.getMethod())) {
			String postContent = request.getContent().toString(
					Charset.forName("UTF-8"));
			logger.info("【POST】{}", postContent);
			return postContent;
		}
		return "";
	}

	private static void _printHttpInfo(HttpRequest request) {
		StringBuilder buf = new StringBuilder();
		buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
		buf.append("HOSTNAME: " + getHost(request, "unknown") + "\r\n");
		buf.append("REQUEST_URI: " + request.getUri() + "\r\n\r\n");
		for (Map.Entry<String, String> h : request.getHeaders()) {
			buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
		}
		buf.append("\r\n");
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				request.getUri());
		Map<String, List<String>> params = queryStringDecoder.getParameters();
		if (!params.isEmpty()) {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for (String val : vals) {
					buf.append("PARAM: " + key + " = " + val + "\r\n");
				}
			}
			buf.append("\r\n");
		}
		System.out.println(buf.toString());
	}

	private static void sendError(ChannelHandlerContext ctx, HttpRequest request,
			HttpResponse response, Document doc, int errorcode, String errormsg)
			throws Exception {
		Element responsecode = new Element(XML_TAG_RESPONSECODE)
				.addContent(String.valueOf(errorcode));
		Element tips = new Element(XML_TAG_TIPS).addContent(errormsg);
		doc.getRootElement().addContent(responsecode);
		doc.getRootElement().addContent(tips);
		// appendFeInfo(doc.getRootElement());
		sendHttpResponse(ctx, request, response, doc);
	}

	static void sendHttpResponse(ChannelHandlerContext ctx,
			HttpRequest request, HttpResponse response, Document doc)
			throws Exception {
		// Generate an error page if response status code is not OK (200).
		// if (response.getStatus().getCode() != 200) {
		// StringBuilder buf = new StringBuilder();
		// // {"rcode":10,"rmsg":"无效的请求数据"}
		// buf.append("{\"").append(JKEY_RESULT_CODE).append("\":")
		// .append(JVALUE_RESULT_CODE_ERROR);
		// buf.append(",");
		// buf.append("\"").append(errormsg)
		// .append(response.getStatus().toString()).append("\"}");
		// setContentTypeHeader(response);
		// response.setContent(ChannelBuffers.copiedBuffer(response
		// .getStatus().toString(), CharsetUtil.UTF_8));
		// setContentLength(response, response.getContent().readableBytes());
		// }

		String xmlstring = XmlUtil.doc2String(doc);

		System.out.println(xmlstring);

		setContentTypeHeader(response, "text/xml");
		response.setContent(ChannelBuffers.copiedBuffer(xmlstring,
				CharsetUtil.UTF_8));
		setContentLength(response, response.getContent().readableBytes());

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(response);
		if (!isKeepAlive(request) || response.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Deprecated
	private static void sendHttpResponse(ChannelHandlerContext ctx,
			HttpRequest request, HttpResponse response, ChannelBuffer content) {
		// Generate an error page if response status code is not OK (200).
		// if (response.getStatus().getCode() != 200) {
		// StringBuilder buf = new StringBuilder();
		// // {"rcode":10,"rmsg":"无效的请求数据"}
		// buf.append("{\"").append(JKEY_RESULT_CODE).append("\":")
		// .append(JVALUE_RESULT_CODE_ERROR);
		// buf.append(",");
		// buf.append("\"").append(errormsg)
		// .append(response.getStatus().toString()).append("\"}");
		// setContentTypeHeader(response);
		// response.setContent(ChannelBuffers.copiedBuffer(response
		// .getStatus().toString(), CharsetUtil.UTF_8));
		// setContentLength(response, response.getContent().readableBytes());
		// }

		setContentTypeHeader(response, "application/octet-stream");
		response.setContent(content);
		setContentLength(response, content.readableBytes());

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(response);
		if (!isKeepAlive(request) || response.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	/**
	 * <pre>
	 * 二进制流: "application/octet-stream"
	 * xml: "text/xml"
	 * </pre>
	 */
	private static void setContentTypeHeader(HttpResponse response,
			String contenttype) {
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, contenttype);
	}

	private static String sanitizeUri(String uri) {
		// Decode the path.
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		// Convert file separators.
		uri = uri.replace('/', File.separatorChar);

		// Simplistic dumb security check.
		// You will have to do something serious in the production environment.
		if (uri.contains(File.separator + ".")
				|| uri.contains("." + File.separator) || uri.startsWith(".")
				|| uri.endsWith(".")) {
			return null;
		}

		// Convert to absolute path.
		return System.getProperty("user.dir") + File.separator + uri;
	}

	static void writeUtf8String(ChannelBuffer content, String utf8string) {
		if (utf8string != null) {
			byte[] bs = utf8string.getBytes(CharsetUtil.UTF_8);
			if (bs != null) {
				content.writeInt(bs.length);
				content.writeBytes(bs);
				bs = null;
				return;
			}
		}
		content.writeInt(0);
	}
}
