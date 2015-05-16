package com.koala.game.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.XmlUtil;
import com.koala.promosupport.PromoSupport;

/**
 * 客户端版本控制器：版本记录、检查、自动更新、统计、查询等功能。
 * 
 * <pre>
 * </pre>
 * 
 * @author AHONG
 * 
 */
final class KGameVersionControl implements KGameTimerTask {

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameVersionControl.class);
	private final static File xmlFile = new File("res/config/fe/VersionControl.xml");
	 
	private long lastModifiedOfXml;
	public final static long modifyCheckSeconds = 60;
	 
	private final static int PROMOID_DEFAULT = -1;
//	private String appVersion;
//	private ApkUpdateInfo apkPatch;
	private final ConcurrentHashMap<Integer, ApkUpdateInfo> apkupdateinfos = new ConcurrentHashMap<Integer, ApkUpdateInfo>();
	private final ConcurrentHashMap<Integer, RespackUpdateInfo> respackupdateinfos = new ConcurrentHashMap<Integer, RespackUpdateInfo>();
	private boolean updateway_outside;//20130714增加的通知客户端由系统系在，而不是在应用内下载，主要是防止客户端出现问题而采取的紧急处理办法，平时一定是false，紧急情况才会设为true
	private String compatible_md5_version;//20130724增加md5的发送，为兼容旧版本需要定义大于此版本的才发md5，否则客户端会读取出错
	
	KGameVersionControl() {
		loadConfig(false);
	}

	private void loadConfig(boolean reload) {
		lastModifiedOfXml = xmlFile.lastModified();

		Document doc = XmlUtil.openXml(xmlFile);
		Element root = doc.getRootElement();
		
		compatible_md5_version = root.getChildTextTrim("compatible_md5_version");

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>AppVersionControl
		Element eAppVersionControl = root.getChild("AppVersionControl");
		updateway_outside = Boolean.parseBoolean(eAppVersionControl.getAttributeValue("updateway_outside"));
		logger.info("updateway_outside:{}", updateway_outside);
		
//		appVersion =eAppVersionControl.getChildTextTrim("AppVersion");
//		logger.info("AppVersion:{}", appVersion);

		Element eApkPatch = eAppVersionControl.getChild("ApkPatch");
//		apkPatch = new ApkUpdateInfo();
//		apkPatch.promoid = PROMOID_DEFAULT;
//		apkPatch.filesize = Long.parseLong(eApkPatch
//				.getAttributeValue("filesize"));
//		apkPatch.url = eApkPatch.getTextTrim();
//		logger.info("ApkPatch:{}", apkPatch);

		List<Element> eApkUpdateByPromoChannels = eAppVersionControl
				.getChildren("ApkUpdateByPromoChannel");
		for (Element eApkUpdateByPromoChannel : eApkUpdateByPromoChannels) {
			ApkUpdateInfo aui = new ApkUpdateInfo();
			String pid = eApkUpdateByPromoChannel.getAttributeValue("promoid");
			aui.promoid = "default".equalsIgnoreCase(pid) ? PROMOID_DEFAULT
					: Integer.parseInt(pid);
			aui.filesize = Long.parseLong(eApkUpdateByPromoChannel
					.getAttributeValue("filesize"));
			aui.appVer = eApkUpdateByPromoChannel.getAttributeValue("appver");
			aui.md5 = eApkUpdateByPromoChannel.getAttributeValue("md5");//20130724每个文件增加md5校验码，发给客户端自己对比
			aui.url = eApkUpdateByPromoChannel.getTextTrim();

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
			String md50 = eFullPack.getAttributeValue("md5");//20130724每个文件增加md5校验码，发给客户端自己对比
			String url0 = eFullPack.getTextTrim();
			rui.lastestFullRespack = new Respack(name0, Integer.parseInt(ver0),
					filesize0, url0,md50);
			rui.lastestRespackVersion = rui.lastestFullRespack.ver;

			// 各版本增量包
			List<Element> ePatches = eRespack.getChildren("Patch");
			for (Element ePatch : ePatches) {
				String name = ePatch.getAttributeValue("name").trim();
				String ver = ePatch.getAttributeValue("ver").trim();
				long filesize = Long.parseLong(ePatch.getAttributeValue(
						"filesize").trim());
				String md5 = ePatch.getAttributeValue("md5");//20130724每个文件增加md5校验码，发给客户端自己对比
				String url = ePatch.getTextTrim();
				Respack patch = new Respack(name, Integer.parseInt(ver), filesize, url,md5);
				rui.respacks.add(patch);
				logger.info("Patch:{}", patch);
			}

			String spid = eRespack.getAttributeValue("promoid");// 这里的promoid有可能是多个的
			String[] spids = spid.split(",");
			if (spids.length == 1) {
				rui.promoid = "default".equalsIgnoreCase(spid) ? PROMOID_DEFAULT
						: Integer.parseInt(spid);
			} else {
				rui.promoid = PROMOID_DEFAULT;
			}
			for (String pid : spids) {
				if (pid != null && pid.trim().length() > 0) {
					pid = pid.trim();
					int ppid = "default".equalsIgnoreCase(pid) ? PROMOID_DEFAULT
							: Integer.parseInt(pid);
					respackupdateinfos.put(ppid, rui);
					logger.info("Respacks:{}", rui);
				}
			}
		}
	}

	/**
	 * TODO 【临时的】客户端资源版本检查 返回true
	 * 
	 * @param msg
	 * @param channel
	 * @return
	 * @deprecated 用{@link #checkAndHandle(KGameMessage, Channel)}
	 */
	public boolean checkAndHandle0(KGameMessage msg, Channel channel) {
		String clientversion = msg.readUtf8String();
		logger.debug("MID_VERCHECK  {}", clientversion);

		KGameMessage resp = KGameCommunication.newMessage(
				KGameMessage.MTYPE_PLATFORM, msg.getClientType(),
				KGameProtocol.MID_VERCHECK);
		int result = KGameProtocol.PL_VERCHECK_NEWEST;
		resp.writeInt(result);// test 最新了不用更新
		channel.write(resp);
		return (result == KGameProtocol.PL_VERCHECK_NEWEST);
	}

	/**
	 * 版本检测（FE）- 【20130611更新了协议】【20130724更新了协议添加md5】
	 * 
	 * <pre>
	 * 请求PL:
	 *   int promoid;  //渠道ID
	 *   String appVer;//应用版本号（客户端和服务器协商自定义）
	 *   int resVer;//资源版本号（必须为整型值）
	 * 响应PL:
	 *   int responsecode = 
	 *     {@link #PL_VERCHECK_NEWEST}/ 
	 *     {@link #PL_VERCHECK_UPDATE_APK}/
	 *     {@link #PL_VERCHECK_UPDATE_APK_SYS}/
	 *     {@link #PL_VERCHECK_UPDATE_RESPACKS}/
	 *     {@link #PL_VERSIONFORMAT_ERROR}/
	 *     {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   String tips;//版本更新结果提示
	 *   if({@link #PL_VERCHECK_UPDATE_APK}){
	 *     String lastestAppVer; //当前最新版本号
	 *     long apkfilesize;     //apk文件大小
	 *     String apkdownloadurl;//下载地址
	 *     String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
	 *   }
	 *   else if({@link #PL_VERCHECK_UPDATE_RESPACKS}){
	 *     int updatefileN;//需要更新的文件数量
	 *     for(updatefileN){
	 *       String filename;//文件名
	 *       int patchver;//版本号,为整型值
	 *       long filesize;//文件大小
	 *       String downloadurl;//HTTP下载地址
	 *       String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
	 *     }
	 *   }
	 * </pre>
	 */
	public boolean checkAndHandle(KGameMessage msg, Channel channel) {
		int promoid = msg.readInt();// 20130611增加,渠道ID
		String appVer = msg.readUtf8String();
		int resVer = msg.readInt();
		logger.debug("MID_VERCHECK  promoid={},appVer={},resVer={}", promoid,
				appVer, resVer);
		
		promoid = PromoSupport.computeParentPromoID(promoid);

		KGameMessage resp = KGameCommunication.newMessage(
				KGameMessage.MTYPE_PLATFORM, msg.getClientType(),
				KGameProtocol.MID_VERCHECK);

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>先检查APK包是否需要更新
		ApkUpdateInfo apkud = apkupdateinfos.get(promoid);
		if (apkud == null) {
			apkud = apkupdateinfos.get(PROMOID_DEFAULT);
		}
		
		if (apkud == null) {
			resp.writeInt(KGameProtocol.PL_VERSIONFORMAT_ERROR);
			resp.writeUtf8String(KGameTips.get("PL_VERSIONFORMAT_ERROR"));
			channel.write(resp);
			return false;
		}
		
		if (apkud.appVer.compareTo(appVer) > 0) {
			//APK版本小于最新版本需要更新APK！！！
			resp.writeInt(updateway_outside?KGameProtocol.PL_VERCHECK_UPDATE_APK_SYS:KGameProtocol.PL_VERCHECK_UPDATE_APK);
			resp.writeUtf8String(KGameTips.get("PL_VERCHECK_UPDATE_APK"));
			resp.writeUtf8String(apkud.appVer);
			resp.writeLong(apkud.filesize);
			resp.writeUtf8String(apkud.url);
			if (appVer.compareTo(compatible_md5_version) > 0) {
				resp.writeUtf8String(apkud.md5);// 20130724每个文件增加md5校验码，发给客户端自己对比
			}
			channel.write(resp);
			return false;
		}

		// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>再检查资源包是否需要更新
		RespackUpdateInfo resud = respackupdateinfos.get(promoid);
		if (resud == null) {
			resud = respackupdateinfos.get(PROMOID_DEFAULT);
		}
		// 根据版本判断是否需要更新
		if (resud.lastestRespackVersion > resVer) {
			resp.writeInt(KGameProtocol.PL_VERCHECK_UPDATE_RESPACKS);
			resp.writeUtf8String(KGameTips.get("PL_VERCHECK_UPDATE_RESPACKS"));
			long totalsize = 0;
			List<Respack> list = new ArrayList<Respack>(resud.respacks.size());
			for (Respack respack : resud.respacks) {
				if (respack.ver > resVer) {
					list.add(respack);
					totalsize += respack.filesize;
				}
			}
			// 看下载N个增量包和最新完整包哪个更划算
			if (totalsize >= resud.lastestFullRespack.filesize) {
				// 下载最新完整包更划算
				resp.writeInt(1);
				writerespackintomessage(resud.lastestFullRespack, resp,appVer);
			} else {
				// 下载N个增量包更划算
				resp.writeInt(list.size());
				for (Respack respack : list) {
					writerespackintomessage(respack, resp,appVer);
				}
			}
			list.clear();
			list = null;
			channel.write(resp);
			return false;
		}
		// 无需更新
		resp.writeInt(KGameProtocol.PL_VERCHECK_NEWEST);
		resp.writeUtf8String(KGameTips.get("PL_VERCHECK_NEWEST"));
		channel.write(resp);
		return true;
	}

	private void writerespackintomessage(Respack respack, KGameMessage msg,String appVer) {
		msg.writeUtf8String(respack.name);
		msg.writeInt(respack.ver);
		msg.writeLong(respack.filesize);
		msg.writeUtf8String(respack.url);
		if (appVer.compareTo(compatible_md5_version) > 0) {
			msg.writeUtf8String(respack.md5);// 20130724每个文件增加md5校验码，发给客户端自己对比
		}
		logger.debug("writerespackintomessage: {}", respack);
	}

//	public boolean ensureVer3format(String verstring) {
//		String[] vs = verstring.split(".");
//		if (vs == null || vs.length != 3) {
//			return false;
//		}
//		try {
//			Integer.parseInt(vs[0]);
//			Integer.parseInt(vs[1]);
//			Integer.parseInt(vs[2]);
//		} catch (Throwable t) {
//			return false;
//		}
//		return true;
//	}

	// private final class PromoChannelUpdateInfo{
	// }

	@Override
	public String getName() {
		return KGameVersionControl.class.getSimpleName();
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
		logger.warn("RejectedExecutionException {}",e);
	}

	private final class ApkUpdateInfo {
		int promoid;
		long filesize;
		String url;
		String appVer;
		String md5;
		@Override
		public String toString() {
			return "ApkUpdateInfo [promoid=" + promoid + ", filesize="
					+ filesize + ", url=" + url + ", appVer=" + appVer
					+ ", md5=" + md5 + "]";
		}
	}

	private final class RespackUpdateInfo {
		int promoid;
		int lastestRespackVersion;
		Respack lastestFullRespack;
		final List<Respack> respacks = new ArrayList<Respack>();

		@Override
		public String toString() {
			return "RespackUpdateInfo [promoid=" + promoid
					+ ", lastestRespackVersion=" + lastestRespackVersion
					+ ", lastestFullRespack=" + lastestFullRespack
					+ ", respacks=" + respacks.size() + "]";
		}
	}

	private final class Respack {
		String name;
		int ver;
		long filesize;
		String url;
		String md5;

		Respack(String name, int ver, long filesize, String url,
				String md5) {
			this.name = name;
			this.ver = ver;
			this.filesize = filesize;
			this.url = url;
			this.md5 = md5;
		}

		@Override
		public String toString() {
			return "Respack [name=" + name + ", ver=" + ver + ", filesize="
					+ filesize + ", url=" + url + ", md5=" + md5 + "]";
		}
	}

}
