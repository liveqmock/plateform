package com.koala.game.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.netty.channel.Channel;
import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.FEGSProtocol;
import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.communication.KGameCommunication;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayer;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.player.KGameSimpleRoleInfo;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.XmlUtil;

public class KGameGSManager implements KGameTimerTask, KGameProtocol,
		FEGSProtocol {

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameGSManager.class);
	private final static File xmlGsList = new File("res/config/fe/GsList.xml");
	private static long lastModifiedOfXml;
	private int modifyCheckSeconds = 30;
	private final static ReadWriteLock lockGsList = new ReentrantReadWriteLock();
	// <gsID,GSInfo>
	private final static ConcurrentHashMap<Integer, KGameGSManager.GS> gss = new ConcurrentHashMap<Integer, KGameGSManager.GS>();
	private final static List<KGameGSManager.GSZone> zones = new ArrayList<KGameGSManager.GSZone>();
	private final static ConcurrentHashMap<Integer, KGameGSManager.WhiteList> whitelists = new ConcurrentHashMap<Integer, KGameGSManager.WhiteList>();
	private final static Set<StatusHeader> statusHeaderSet = new HashSet<KGameGSManager.StatusHeader>();
	private final static List<GSStatusStruct> gsStatusList = new ArrayList<KGameGSManager.GSStatusStruct>();
	private final static int ZONE_ID_RECOMMENDATORY = 0;// recommendatory zone

	@SuppressWarnings("unchecked")
	public void loadGsListXml(boolean reload) throws KGameServerException {
		if (lockGsList.writeLock().tryLock()) {
			try {
				lastModifiedOfXml = xmlGsList.lastModified();
				Document doc = XmlUtil.openXml(xmlGsList);
				Element root = doc.getRootElement();

				modifyCheckSeconds = Integer.parseInt(root
						.getChildTextTrim("ModifyCheckSeconds"));

				// //////////////////////////////////////////////////////////
				Element eWhiteLists = root.getChild("WhiteLists");
				if (reload) {// 重加载需要先清空原来的数据
					whitelists.clear();
				}
				List<Element> wll = eWhiteLists.getChildren("WhiteList");
				for (Element eWL : wll) {
					int wlid = Integer.parseInt(eWL.getAttributeValue("id"));
					WhiteList wl = new WhiteList(wlid);
					List<Element> ePIDs = eWL.getChildren("PID");
					for (Element ePID : ePIDs) {
						Long pid = Long.parseLong(ePID.getTextTrim());
						wl.pIDs.add(pid);
					}
					List<Element> ePNames = eWL.getChildren("PName");
					for (Element ePName : ePNames) {
						wl.pNames.add(ePName.getTextTrim());
					}
					whitelists.put(wlid, wl);
				}

				// //////////////////////////////////////////////////////////
				Element eGSS = root.getChild("GSS");
				List<Element> eGSSList = eGSS.getChildren("GS");
				// Set<Integer> existGsIDs = (reload ? (new HashSet<Integer>())
				// : null);//检测用
				for (Element eGS : eGSSList) {
					int gsID = Integer.parseInt(eGS.getAttributeValue("id"));
					GS gsexist = gss.get(gsID);
					GS gs = (gsexist == null ? (new GS(gsID)) : gsexist);
					gs.label = eGS.getAttributeValue("label");
					gs.remark = eGS.getAttributeValue("remark");
					gs.allowedOnline = Integer.parseInt(eGS
							.getAttributeValue("allowedOnline"));
					gs.status = Byte.parseByte(eGS.getAttributeValue("status"));
					gs.srcStatus = gs.status;
					gs.history = Byte.parseByte(eGS
							.getAttributeValue("history"));

					// /////////////////////////////////////////////////////////////////////////////
					// 可见条件检测ADD@20131125
					gs.visiblePromoChannel = eGS
							.getChildTextTrim("VisiblePromoChannel");
					gs.invisiblePromoChannel = eGS
							.getChildTextTrim("InvisiblePromoChannel");
					if (gs.visiblePromoChannel != null
							&& gs.invisiblePromoChannel != null) {
						throw new KGameServerException(
								"VisiblePromoChannel和InvisiblePromoChannel两个互斥条件不能同时存在！");
					}
					String sVisiblePlayer = eGS
							.getChildTextTrim("VisiblePlayer");
					String sInvisiblePlayer = eGS
							.getChildTextTrim("InvisiblePlayer");
					if (sVisiblePlayer != null && sInvisiblePlayer != null) {
						throw new KGameServerException(
								"VisiblePlayer和InvisiblePlayer两个互斥条件不能同时存在！");
					}
					if (sVisiblePlayer != null) {
						int idx = sVisiblePlayer.indexOf(",");
						String low = sVisiblePlayer.substring(0, idx);
						String up = sVisiblePlayer.substring(Math.min(idx + 1,
								sVisiblePlayer.length()));
						gs.visiblePlayerIDInterval = new long[] {
								(low==null||low.length()<=0)?0:Long.parseLong(low), 
										(up==null||up.length()<=0)?Long.MAX_VALUE:Long.parseLong(up) };
					}
					if (sInvisiblePlayer != null) {
						int idx = sInvisiblePlayer.indexOf(",");
						String low = sInvisiblePlayer.substring(0, idx);
						String up = sInvisiblePlayer.substring(Math.min(
								idx + 1, sInvisiblePlayer.length()));
						gs.invisiblePlayerIDInterval = new long[] {
								(low==null||low.length()<=0)?0:Long.parseLong(low), 
										(up==null||up.length()<=0)?Long.MAX_VALUE:Long.parseLong(up) };
					}
					// /////////////////////////////////////////////////////////////////////////////

					List<Element> eWhiteListLinks = eGS
							.getChildren("WhiteListLink");
					if (eWhiteListLinks != null && eWhiteListLinks.size() > 0) {
						if (gs.whitelist == null) {
							gs.whitelist = new WhiteList(0);// 这里的id没意义
						} else {// reload时需要先清空
							gs.whitelist.pIDs.clear();
							gs.whitelist.pNames.clear();
						}
						for (Element eWhiteListLink : eWhiteListLinks) {
							int wlid = Integer.parseInt(eWhiteListLink
									.getTextTrim());
							WhiteList twl = whitelists.get(wlid);
							if (twl != null) {
								gs.whitelist.pIDs.addAll(twl.pIDs);
								gs.whitelist.pNames.addAll(twl.pNames);
							}
						}
					}
					if (gsexist == null) {
						gss.put(gsID, gs);// 放到缓存
						logger.debug("LOAD GS {}",gs);
					}
					// existGsIDs.add(gsID);
				}
				// if (existGsIDs != null && existGsIDs.size() > 0) {
				// for (Integer gsid : gss.keySet()) {
				// if (!existGsIDs.contains(gsid)) {
				// GS gsRemoved = gss.remove(gsid);
				// //release
				// }
				// }
				// }

				// ///////////////////////////////////////////////////////////
				Element eZones = root.getChild("Zones");
				List<Element> eZonesList = eZones.getChildren("Zone");
				if (reload) {// reload时需要先清空
					zones.clear();
				}
				for (Element eZone : eZonesList) {
					GSZone zone = new GSZone();
					zone.id = Integer.parseInt(eZone.getAttributeValue("id"));
					zone.label = eZone.getAttributeValue("label");
					List<Element> eGSLinks = eZone.getChildren("GSLink");
					for (Element eGSLink : eGSLinks) {
						GSOnZone gsonzone = new GSOnZone();
						gsonzone.id = Integer.parseInt(eGSLink
								.getAttributeValue("id"));
						gsonzone.showlabel = eGSLink
								.getAttributeValue("showlabel");
						zone.gsonzone.add(gsonzone);

						GS gsexist = gss.get(gsonzone.id);
						if (gsexist == null) {
							throw new KGameServerException(
									"GsList.xml ERROR：在大区" + zone.label
											+ "指向一个不存在的GS " + gsonzone.id);
						}
						// 将GS和Zone关联起来，其实不是很严谨，因为有可能同一个GS放不同的Zone中
						gsexist.zone = zone;
					}
					zones.add(zone);
				}

				// ////////////////////////////////////////////////////////////
				Element eStatusHeader = root.getChild("StatusHeader");
				List<Element> eStatusHeaders = eStatusHeader
						.getChildren("Header");
				for (Element h : eStatusHeaders) {
					StatusHeader sh = new StatusHeader();
					sh.fontcolor = h.getAttributeValue("fontcolor");
					sh.headerString = h.getTextTrim();
					String sstatus = h.getAttributeValue("status");
					if (sstatus != null) {
						sh.status = Byte.parseByte(sstatus);
						statusHeaderSet.add(sh);
						continue;
					}
					String shistory = h.getAttributeValue("history");
					if (shistory != null) {
						sh.history = Byte.parseByte(shistory);
						statusHeaderSet.add(sh);
						continue;
					}
					String sonline = h.getAttributeValue("online");
					int sep;
					if (sonline != null && ((sep = sonline.indexOf("~")) != -1)) {
						sh.onlineLower = Integer.parseInt(sonline.substring(0,
								sep));
						sh.onlineUpper = Integer.parseInt(sonline
								.substring(sep + 1));
						statusHeaderSet.add(sh);
						continue;
					}
				}
				
				List<Element> eGsStatus = root.getChild("statusConfig").getChildren();
				Element temp;
				GSStatusStruct gsStatus;
				String[] onlines;
				for (int i = 0; i < eGsStatus.size(); i++) {
					temp = eGsStatus.get(i);
					gsStatus = new GSStatusStruct();
					onlines = temp.getAttributeValue("online").split(",");
					gsStatus.flag = Byte.parseByte(temp.getAttributeValue("flag"));
					gsStatus.onlineLower = Integer.parseInt(onlines[0]);
					gsStatus.onlineUpper = Integer.parseInt(onlines[1]);
					gsStatusList.add(gsStatus);
				}

				logger.info(
						"Load '{}' SUCCEED. gsCount={},zoneCount={},whitelistCount={},statusHeaderSet={}",
						xmlGsList, gss.size(), zones.size(), whitelists.size(),
						statusHeaderSet.size());
			} finally {
				lockGsList.writeLock().unlock();
			}
		}
	}

	private void updateGSStatusFromXml(GS g) {
		Document doc = XmlUtil.openXml(xmlGsList);
		Element root = doc.getRootElement();
		Element eGSS = root.getChild("GSS");
		List<Element> eGSSList = eGSS.getChildren("GS");
		for (Element eGS : eGSSList) {
			int gsID = Integer.parseInt(eGS.getAttributeValue("id"));
			if (gsID == g.gsID) {
				g.status = Byte.parseByte(eGS.getAttributeValue("status"));
				break;
			}
		}
	}

	public static GS isGSChannel(Channel channel) {
		if (lockGsList.readLock().tryLock()) {
			try {
				for (GS gs : gss.values()) {
					if (gs.channel == channel) {
						return gs;
					}
				}
			} finally {
				lockGsList.readLock().unlock();
			}
		}
		return null;
	}

	public boolean containInWhitelist(int gsID, long playerID, String playerName) {
		if (lockGsList.readLock().tryLock()) {
			try {
				GS gs = gss.get(gsID);
				if (gs != null && gs.whitelist != null) {
					return gs.whitelist.pIDs.contains(playerID)
							|| gs.whitelist.pNames.contains(playerName);
				}
			} finally {
				lockGsList.readLock().unlock();
			}
		}
		return false;
	}

	void messageReceived(Channel channel, KGameMessage kmsg) throws Exception {
		switch (kmsg.getMsgID()) {
		case MID_HANDSHAKE:
			handshake(kmsg, channel);
			break;
		// 更新GS状态
		case MID_GS2FE_UPDATESTATUS:
			updategsstatus(kmsg, channel);
			break;
		// GS关闭
		case MID_GS2FE_ISHUTDOWN:
			gsshutdown(kmsg, channel);
			break;
			
		case MID_CROSS_SERVER_MSG:
			int sGsID = kmsg.readInt();
			int rGsID = kmsg.readInt();
			int bLen = kmsg.readInt();
			byte[] bytes = new byte[bLen];
			kmsg.readBytes(bytes);
			logger.debug("fe received cross msg. {}->{} len:{}", sGsID,rGsID,bLen);
			// 找到接收者GS
			GS gs = gss.get(rGsID);
			if (gs != null) {
				KGameMessage msgCross = KGameCommunication.newMessage(
						KGameMessage.MTYPE_PLATFORM, KGameMessage.CTYPE_GS,
						KGameProtocol.MID_CROSS_SERVER_MSG);
				msgCross.writeInt(sGsID);
				msgCross.writeInt(rGsID);
				msgCross.writeInt(bLen);
				msgCross.writeBytes(bytes);
				gs.channel.write(msgCross);
			}
			break;
		case MID_GS2FE_DELAY_REMOVE:
			int gsId = kmsg.readInt();
			long dPlayerId = kmsg.readLong();
			logger.debug("收到GS延迟移除的消息！！gsid={}, playerId={}", gsId, dPlayerId);
			break;
		case MID_GS2FE_REALLY_REMOVE:
			int rGsId = kmsg.readInt();
			long rPlayerId = kmsg.readLong();
			logger.debug("收到GS真正移除的消息！！gsid={}, playerId={}", rGsId, rPlayerId);
			break;
		}
	}

	private void handshake(KGameMessage handshakemsg, Channel channel) {
		int gsID = handshakemsg.readInt();
		if (lockGsList.writeLock().tryLock()) {
			try {
				GS gs = gss.get(gsID);
				if (gs == null) {
					logger.warn(
							"!!! No this GS ({}) defined in GsList.xml !!!",
							gsID);
					return;
				}
				gs.channel = channel;
				gs.currentOnline = handshakemsg.readInt();
				gs.allocatedTotalMemory = handshakemsg.readLong();
				gs.freeMemory = handshakemsg.readLong();
				gs.gsIP = handshakemsg.readUtf8String();
				gs.gsSocketPort = handshakemsg.readInt();

				// gs.status = CONST_GS_STATUS_OPEN;
				// 加载一次对应的xml位置以更新状态跟xml一致，如果xml修改时间不一致-全部加载，如果一致单单加载本GS的status
				if (xmlGsList.exists()) {
					if (xmlGsList.lastModified() != lastModifiedOfXml) {
						// 更新过的xml
						logger.info("{} had modified! reload...",
								xmlGsList.getName());
						try {
							loadGsListXml(true);
						} catch (KGameServerException e) {
							e.printStackTrace();
						}// Reload
					} else {
						updateGSStatusFromXml(gs);
					}
				}
				logger.debug("gs handshake with fe. {}", gs);
			} finally {
				lockGsList.writeLock().unlock();
			}
		}
	}

	private void updategsstatus(KGameMessage updatestatusmsg, Channel channel) {
		int gsID = updatestatusmsg.readInt();
		if (lockGsList.writeLock().tryLock()) {
			try {
				GS gs = gss.get(gsID);
				if (gs != null) {
					gs.currentOnline = updatestatusmsg.readInt();
					gs.freeMemory = updatestatusmsg.readLong();
					int remainconnectspace = gs.allowedOnline
							- gs.currentOnline;
					gs.updateLoginQueueInfo2Players(remainconnectspace);
					gs.updateStatus();
				} else {
					// 不存在的GS，证明没通过握手，所以属于非法更新消息
					channel.close();
				}
				//logger.debug("gs update status. {}", gs);
			} finally {
				lockGsList.writeLock().unlock();
			}
		}
	}

	private void gsshutdown(KGameMessage kmsg, Channel channel) {
		int gsID = kmsg.readInt();
		if (lockGsList.writeLock().tryLock()) {
			try {
				GS gs = gss.get(gsID);
				if (gs != null) {
					gs.status = KGameProtocol.CONST_GS_STATUS_MAINTENANCE;
				}
				logger.warn("gs shutdown. {}", gs);
			} finally {
				lockGsList.writeLock().unlock();
			}
		}
	}
	
	private void fillGSListToMsg(KGamePlayerSession playersession, int promoid, KGameMessage responsemsg, List<GSOnZone> list) {
		int writerIndex = responsemsg.writerIndex();
		byte sizecount = 0;
		responsemsg.writeByte(list.size());
		int historyIndex = 0;
		for (GSOnZone gsonzone : list) {
			GS gs = gss.get(gsonzone.id);
			// /////////////////////////////////////////////
			// 如果不可见
			// logger.debug("gs.isVisiblePromoChannel(promoid:{}) {}",promoid,gs.isVisiblePromoChannel(promoid));
			if (!gs.isVisiblePromoChannel(promoid)) {
				continue;
			}
			KGamePlayer player = playersession.getBoundPlayer();
			if (player != null) {
				// logger.debug("gs.isVisblePlayer(player.getID():{}) {}",player.getID(),gs.isVisblePlayer(player.getID()));
				if (!gs.isVisblePlayer(player.getID())) {
					continue;
				}
			}
			// //////////////////////////////////////////////
			responsemsg.writeInt(gs.gsID);
			StatusHeader sh = getHeader(gs);
			responsemsg.writeUtf8String(sh == null ? "" : sh.headerString);
			responsemsg.writeUtf8String(sh == null ? "0xff000000" : sh.fontcolor);
			responsemsg.writeUtf8String((gsonzone.showlabel == null || gsonzone.showlabel.length() <= 0) ? gs.label : gsonzone.showlabel);
			responsemsg.writeUtf8String(gs.remark);
			responsemsg.writeByte(gs.status);
			historyIndex = responsemsg.writerIndex();
//			responsemsg.writeByte(gs.history);
			responsemsg.writeByte(0);
			responsemsg.writeInt(gs.allowedOnline);
			responsemsg.writeInt(gs.currentOnline);
			responsemsg.writeUtf8String(gs.gsIP);
			responsemsg.writeInt(gs.gsSocketPort);
			// add playersession roles info on gslist

			if (player == null) {
				logger.warn("playersession({})'s player is null.", playersession);
				responsemsg.writeByte(0);
			} else {
				List<KGameSimpleRoleInfo> roles = player.getRoleSimpleInfo4GsListShow(gs.gsID);
				responsemsg.writeByte(roles.size());
				if (roles.size() > 0) {
					for (KGameSimpleRoleInfo role : roles) {
						responsemsg.writeInt(role.roleJobType);
						responsemsg.writeInt(role.roleLV);
					}
					responsemsg.setByte(historyIndex, 1);
				}
			}
			sizecount++;
		}//~for
		responsemsg.setByte(writerIndex, sizecount);
	}

	public void gsClosed(GS gs) {
		if (gs == null)
			return;
		if (lockGsList.writeLock().tryLock()) {
			try {
				gs.status = KGameProtocol.CONST_GS_STATUS_MAINTENANCE;
				logger.warn("GS({}) IDLE.", gs);
			} finally {
				lockGsList.writeLock().unlock();
			}
		}
	}

	public void writeGsListOnResponseMsg(KGameMessage responsemsg,
			KGamePlayerSession playersession, int promoid) {
		if (lockGsList.readLock().tryLock()) {
			try {
				int writerIndex = responsemsg.writerIndex();
				int writeCount = 0;
				List<GSOnZone> gsList;
				responsemsg.writeByte(zones.size());
				for (GSZone zone : zones) {
//					responsemsg.writeInt(zone.id);
//					responsemsg.writeUtf8String(zone.label);
					if (zone.id < 0) {
						if(playersession.getBoundPlayer() == null) {
							gsList = Collections.emptyList();
						} else {
							List<Integer> gsIds = playersession.getBoundPlayer().getCreateRoleGsIds();
							gsList = new ArrayList<KGameGSManager.GSOnZone>(gsIds.size());
							GS tempGS;
							GSOnZone tempGSOnZone;
							for(int i = 0; i < gsIds.size(); i++) {
								tempGS = gss.get(gsIds.get(i));
								if(tempGS != null) {
									tempGSOnZone = new GSOnZone();
									tempGSOnZone.id = tempGS.gsID;
									tempGSOnZone.showlabel = tempGS.label;
									gsList.add(tempGSOnZone);
								}
							}
						}
//						fillGSListToMsg(playersession, promoid, responsemsg, gsList);
					} else {
//						fillGSListToMsg(playersession, promoid, responsemsg, zone.gsonzone);
						gsList = zone.gsonzone;
					}
					if(gsList.isEmpty()) {
						continue;
					}
					responsemsg.writeInt(zone.id);
					responsemsg.writeUtf8String(zone.label);
					fillGSListToMsg(playersession, promoid, responsemsg, gsList);
					writeCount++;
				}
				responsemsg.setByte(writerIndex, writeCount);
			} finally {
				lockGsList.readLock().unlock();
			}
		}
	}

	/**
	 * 选择推荐服务器
	 * 
	 * <pre>
	 *  3、推荐服务器规则：
	 * 	推荐服务器规则包含两块，首次登陆默认选择 与 服务器推荐列表
	 * 	服务器推荐列表
	 * 	服务器推荐列表指服务器选择界面中“推荐”标签下属的服务器列表
	 * 	服务器推荐列表默认情况下，推荐当前最近新增的六组服务器
	 * 	推荐列表需要可以人为修改调整推荐的服务器，推荐列表优先以配置内容为准
	 * 	首次登陆默认选择
	 * 	首次登录默认选择指玩家第一进行游戏时，游戏默认所选择服务器；该处不影响其他已经记录过上次登录服务器的玩家
	 * 	首次登陆默认则从服务器推荐列表中选择，优先选择最新增加的服务器，若服务器人数已满时，则选择下一组服务器；服务器人数都处于满状态时，则依然优先最新服务器
	 * 	首次登录默认选择需要可进行人为在线修改配置并且应用；同时默认以配置内容为准
	 * </pre>
	 * FIXME 选择默认推荐服务器的时候由于未有promoid和playerid，这里做不到那种服务器是否可见的判断。这里有可能导致进了一个不可见的区，那以后就一直找不到这个区了
	 * @param playersession
	 */
	void choiceRecommendatoryGSAndSendtoClient(KGamePlayerSession playersession) {

//		GS gsRc = null;// 待推荐的GS
//		String gsshowlabel = null;
		
		//XXX 20131126由于上面FIXME的原因，这里暂时都是返回null表示告诉客户端找不到默认推荐服务器
//
//		// 选择【推荐】大区--然后选一个有空位的GS
//		for (GSZone gz : zones) {
//			if (gz != null && gz.id == ZONE_ID_RECOMMENDATORY) {
//				for (GSOnZone goz : gz.gsonzone) {
//					GS g = gss.get(goz.id);
//					if (g != null && g.space() > 0
//							&& g.status == CONST_GS_STATUS_OPEN) {
//						gsRc = g;
//						gsshowlabel = (goz.showlabel != null && goz.showlabel
//								.length() > 0) ? goz.showlabel : gsRc.label;
//						break;
//					}
//				}
//				break;
//			}
//		}
//		// 如果【推荐】大区不存在或推荐大区的GS都满人，则选择下一个大区
//		if (gsRc == null) {
//			for (GSZone gz : zones) {
//				if (gz != null) {
//					for (GSOnZone goz : gz.gsonzone) {
//						GS g = gss.get(goz.id);
//						if (g != null && g.space() > 0
//								&& g.status == CONST_GS_STATUS_OPEN) {
//							gsRc = g;
//							gsshowlabel = (goz.showlabel != null && goz.showlabel
//									.length() > 0) ? goz.showlabel : gsRc.label;
//							break;
//						}
//					}
//					break;
//				}
//			}
//		}
//		// 最后保障：如果全部区都满员，直接给一个最新区（GSID越大越新）
//		if (gsRc == null) {
//			int maxid = Integer.MIN_VALUE;
//			for (Integer gsid : gss.keySet()) {
//				if (gsid > maxid) {
//					GS gslastest = gss.get(gsid);
//					if (gslastest != null
//							&& gslastest.status == CONST_GS_STATUS_OPEN) {
//						maxid = gsid;
//					}
//				}
//			}
//			if ((gsRc = gss.get(maxid)) != null) {
//				for (GSOnZone goz : gsRc.zone.gsonzone) {
//					if (goz.id == gsRc.gsID) {
//						gsshowlabel = (goz.showlabel != null && goz.showlabel
//								.length() > 0) ? goz.showlabel : gsRc.label;
//						break;
//					}
//				}
//			}
//		}
		
//		// 如果此处gsRc还是为null则客户端会直接弹出服务器列表给玩家选择
//
//		System.out.println("===========recommendatory gs: " + gsRc);
//		// /////////////////////////////////////////
//
//		// 组织消息并发送
//		KGameMessage responsemsg = KGameCommunication.newMessage(
//				KGameMessage.MTYPE_PLATFORM, playersession.getClientType(),
//				MID_DEFAULT_GAMESERVER);
//		responsemsg.writeBoolean(gsRc != null);
//		if (gsRc != null) {
//			responsemsg.writeUtf8String(gsRc.zone.label);
//			responsemsg.writeInt(gsRc.gsID);
//			StatusHeader sh = getHeader(gsRc);
//			responsemsg.writeUtf8String(sh == null ? "" : sh.headerString);
//			responsemsg.writeUtf8String(sh == null ? "0xff000000"
//					: sh.fontcolor);
//			responsemsg.writeUtf8String(gsshowlabel);
//			responsemsg.writeUtf8String(gsRc.remark);
//			responsemsg.writeByte(gsRc.status);
//			responsemsg.writeByte(gsRc.history);
//			responsemsg.writeInt(gsRc.allowedOnline);
//			responsemsg.writeInt(gsRc.currentOnline);
//			responsemsg.writeUtf8String(gsRc.gsIP);
//			responsemsg.writeInt(gsRc.gsSocketPort);
//			// // add playersession roles info on gslist
//			// List<KGameRoleSimpleInfo4GsListShow> roles = playersession
//			// .getBoundPlayer()
//			// .getRoleSimpleInfo4GsListShow();
//			// responsemsg.writeByte(roles.size());
//			// if (roles.size() > 0) {
//			// for (KGameRoleSimpleInfo4GsListShow role : roles) {
//			// responsemsg.writeInt(role.roleJobType);
//			// responsemsg.writeInt(role.roleLV);
//			// }
//			// }
//		}
//		playersession.send(responsemsg);
	}

	GS getGS(int gsID) {
		return gss.get(gsID);
	}

	void notifyqueueaftersomeonelogout(int gsID) {
		GS gs = gss.get(gsID);
		if (gs != null) {
			gs.updatequeue();
		}
	}

	void cancelLoginqueue(KGamePlayerSession playerSession) {
		if (lockGsList.readLock().tryLock()) {
			try {
				for (GS gs : gss.values()) {
					if (gs.cancelLoginqueue(playerSession)) {
						break;
					}
				}
			} finally {
				lockGsList.readLock().unlock();
			}
		}
	}
	
	void changeToMaintenanceStatus() {
		GS gs;
		for (Iterator<GS> itr = gss.values().iterator(); itr.hasNext();) {
			gs = itr.next();
			gs.status = CONST_GS_STATUS_STARTBUTNOTOPEN;
		}
	}
	
	void changeToOpenStatus() {
		GS gs;
		for (Iterator<GS> itr = gss.values().iterator(); itr.hasNext();) {
			gs = itr.next();
			if (gs.status != GSStatusStruct.STATUS_MAINTAIN) {
				if (gs.srcStatus == GSStatusStruct.STATUS_NEW_SERVER) {
					gs.status = gs.srcStatus;
				} else {
					gs.status = GSStatusStruct.STATUS_OPEN;
				}
			}
		}
	}
	
	void changeStatusOfServer(int gsId, int status) {
		GS gs = gss.get(gsId);
		if (gs != null) {
			switch (status) {
			case GSStatusStruct.STATUS_MAINTAIN:
			case GSStatusStruct.STATUS_WHITE_LIST_ONLY:
				gs.status = (byte) status;
				break;
			default:
			case GSStatusStruct.STATUS_OPEN:
				if (gs.srcStatus == GSStatusStruct.STATUS_NEW_SERVER) {
					gs.status = gs.srcStatus;
				} else {
					gs.status = GSStatusStruct.STATUS_OPEN;
				}
				break;

			}
		}
	}
	
	void packAllServerStatusToMsg(KGameMessage msg) {
		msg.writeInt(gss.size());
		Map.Entry<Integer, GS> entry;
		for (Iterator<Map.Entry<Integer, GS>> itr = gss.entrySet().iterator(); itr.hasNext();) {
			entry = itr.next();
			msg.writeInt(entry.getValue().zone.id);
			msg.writeInt(entry.getKey());
			msg.writeByte(entry.getValue().status);
		}
	}

	@Override
	public String getName() {
		return "GsList.xml-modify-check";
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		// logger.debug(xmlGsList.lastModified() + ":::" + lastModifiedOfXml);
		if (xmlGsList.exists()
				&& (xmlGsList.lastModified() != lastModifiedOfXml)) {
			// 更新过的xml
			logger.info("{} had modified! reload...", xmlGsList.getName());
			loadGsListXml(true);// Reload
		}
		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
		timeSignal.getTimer().newTimeSignal(this, modifyCheckSeconds,
				TimeUnit.SECONDS);
	}

	@Override
	public void rejected(RejectedExecutionException e) {

	}

	/*************************************************************
	 * Inner class
	 */
	private final class GSZone {
		int id;
		String label;
		List<GSOnZone> gsonzone = new ArrayList<GSOnZone>(6);
	}

	/*************************************************************
	 * Inner class
	 */
	private final class GSOnZone {
		int id;
		String showlabel;
	}

	/*************************************************************
	 * Inner class
	 */
	public final class GS {

		GSZone zone;
		// /////////////////////
		final int gsID;
		String label;
		int allowedOnline;
		String remark;
		byte status;// status(byte): '-1'表示停服维护状态;'0'表示已经启动但未对外开放;'1'表示对外开放。
					// PS:只要客户端协商好可增加自定义状态值
		byte srcStatus; // 原始状态（即在表中的状态）
		byte history;// history(byte): ‘0’表示新区；‘1’普通老区；‘2’表示合区 。PS：可不断增加标记
		WhiteList whitelist;
		// /////////////////////
		int currentOnline;
		Channel channel;
		long allocatedTotalMemory;
		long freeMemory;
		String gsIP = "0.0.0.0";
		int gsSocketPort;
		// /////////////////////
		private final LinkedList<KGamePlayerSession> loginqueue = new LinkedList<KGamePlayerSession>();
		private final Map<KGamePlayerSession, Long> _loginTime = new HashMap<KGamePlayerSession, Long>();
		private final ReadWriteLock lockQueue = new ReentrantReadWriteLock();
		private long lastpolltimems;
		private int avgpolltimeseconds = 300;
		private long totalpollusedtimems;
		private long totalpollN;

		// //////////////////////
		private String visiblePromoChannel;
		private String invisiblePromoChannel;
		private long[] visiblePlayerIDInterval;
		private long[] invisiblePlayerIDInterval;

		public GS(int gsID) {
			this.gsID = gsID;
		}

		@Override
		public String toString() {
			return "GS [gsID=" + gsID + ", label=" + label + ", allowedOnline="
					+ allowedOnline + ", remark=" + remark + ", status="
					+ status + ", history=" + history + ", currentOnline="
					+ currentOnline + ", allocatedTotalMemory="
					+ allocatedTotalMemory + ", freeMemory=" + freeMemory
					+ ", gsIP=" + gsIP + ", gsSocketPort=" + gsSocketPort
					+ ", visiblePromoChannel=" + visiblePromoChannel
					+ ", invisiblePromoChannel=" + invisiblePromoChannel
					+ ", visiblePlayerIDInterval="
					+ Arrays.toString(visiblePlayerIDInterval)
					+ ", invisiblePlayerIDInterval="
					+ Arrays.toString(invisiblePlayerIDInterval) + "]";
		}

		boolean isVisiblePromoChannel(int promoid) {
			String pid = String.valueOf(promoid);
			// 可见中包含即可见
			if (visiblePromoChannel != null) {
				return (visiblePromoChannel.contains(pid) || visiblePromoChannel
						.contains("all"));
			}
			// 不可见中包含即不可见
			if (invisiblePromoChannel != null) {
				return !(invisiblePromoChannel.contains(pid) || invisiblePromoChannel
						.contains("all"));
			}
			// 两个都没设表示默认可见
			return true;
		}

		boolean isVisblePlayer(long playerId) {
			// 可见中包含即可见
			if (visiblePlayerIDInterval != null) {
				return (playerId >= visiblePlayerIDInterval[0] && playerId <= visiblePlayerIDInterval[1]);
			}
			// 不可见中包含即不可见
			if (invisiblePlayerIDInterval != null) {
				return (!(playerId >= invisiblePlayerIDInterval[0] && playerId <= invisiblePlayerIDInterval[1]));
			}
			// 两个都没设表示默认可见
			return true;
		}

		int space() {
			return allowedOnline - currentOnline;
		}

		int sizeOfLoginQueue() {
			if (lockQueue.readLock().tryLock()) {
				try {
					return loginqueue.size();
				} finally {
					lockQueue.readLock().unlock();
				}
			}
			return 0;
		}

		int indexAtLoginQueue(KGamePlayerSession playerSession) {
			if (lockQueue.readLock().tryLock()) {
				try {
					return loginqueue.indexOf(playerSession);
				} finally {
					lockQueue.readLock().unlock();
				}
			}
			return -1;
		}

		void add2LoginQueue(KGamePlayerSession playerSession) {
			if (lockQueue.writeLock().tryLock()) {
				try {
					loginqueue.offer(playerSession);
					_loginTime.put(playerSession, System.currentTimeMillis());
					if (lastpolltimems == 0) {
						lastpolltimems = System.currentTimeMillis();
					}
				} finally {
					lockQueue.writeLock().unlock();
				}
			}
		}

		/** 插队 */
		void jump2LoginQueue(KGamePlayerSession playerSession) {
			if (lockQueue.writeLock().tryLock()) {
				try {
					loginqueue.addFirst(playerSession);
					_loginTime.put(playerSession, System.currentTimeMillis());
					if (lastpolltimems == 0) {
						lastpolltimems = System.currentTimeMillis();
					}
				} finally {
					lockQueue.writeLock().unlock();
				}
			}
		}

		private void writeplayerqueueposition(KGamePlayerSession ps, int i) {
			if (ps != null) {
				KGameMessage selectgsResp = KGameCommunication.newMessage(
						KGameMessage.MTYPE_PLATFORM, ps.getClientType(),
						KGameProtocol.MID_SELECT_GS);
				selectgsResp.writeInt(KGameProtocol.PL_SELECT_GS_FAILED_QUEUE);
				selectgsResp.writeUtf8String("排队友好提示内容");
				selectgsResp.writeInt(i + 1);
				selectgsResp.writeInt(avgpolltimeseconds);
				ps.send(selectgsResp);
			}
		}

		boolean cancelLoginqueue(KGamePlayerSession playerSession) {
			if (lockQueue.writeLock().tryLock()) {
				try {
					int index = loginqueue.indexOf(playerSession);
					boolean b = loginqueue.remove(playerSession);
					_loginTime.remove(playerSession);
					if (b && (index != -1) && (index < (loginqueue.size()))) {
						// 更新后面玩家的排队位置
						for (int i = index; i < loginqueue.size(); i++) {
							KGamePlayerSession ps = loginqueue.get(i);
							writeplayerqueueposition(ps, i);
						}
					}
					return b;
				} finally {
					lockQueue.writeLock().unlock();
				}
			}
			return false;
		}

		void updatequeue() {
			if (lockQueue.writeLock().tryLock()) {
				try {
					// 更新后面玩家的排队位置
					for (int i = 0; i < loginqueue.size(); i++) {
						KGamePlayerSession ps = loginqueue.get(i);
						writeplayerqueueposition(ps, i);
					}
				} finally {
					lockQueue.writeLock().unlock();
				}
			}
		}

		// 注意updateLoginQueueInfo2Players线程安全
		void updateLoginQueueInfo2Players(int remainconnectspace) {
			logger.debug("tell clients remainconnectspace: {}", remainconnectspace);
			if (remainconnectspace > 0 && loginqueue.size() > 0) {
				if (lockQueue.writeLock().tryLock()) {
					try {
						// 告诉前面几个可以连接了
						for (int i = 0; i < remainconnectspace; i++) {
							KGamePlayerSession ps = loginqueue.poll();
//							countavgtime();
							if (ps != null) {
								Long time = this._loginTime.remove(ps);
								if (time != null) {
									countavgtime(time);
								}
								KGameMessage selectgsResp = KGameCommunication.newMessage(KGameMessage.MTYPE_PLATFORM, ps.getClientType(), KGameProtocol.MID_SELECT_GS);
								selectgsResp.writeInt(KGameProtocol.PL_SELECT_GS_SUCCEED);
								selectgsResp.writeUtf8String("");
								selectgsResp.writeUtf8String(this.gsIP);
								selectgsResp.writeInt(this.gsSocketPort);
								ps.send(selectgsResp);
							} else {
								break;
							}
						}
						// 更新后面玩家的排队位置
						if (loginqueue.size() > 0) {
							for (int i = 0; i < loginqueue.size(); i++) {
								KGamePlayerSession ps = loginqueue.get(i);
								writeplayerqueueposition(ps, i);
							}
						}
					} finally {
						lockQueue.writeLock().unlock();
					}
				}
			}
		}
		
		void updateStatus() {
			switch (this.status) {
			case GSStatusStruct.STATUS_MAINTAIN:
			case GSStatusStruct.STATUS_WHITE_LIST_ONLY:
			case GSStatusStruct.STATUS_NEW_SERVER:
			case GSStatusStruct.STATUS_RECOMMEND:
				break;
			default:
				GSStatusStruct gsStatus;
				for(int i = 0; i < gsStatusList.size(); i++) {
					gsStatus = gsStatusList.get(i);
					if(gsStatus.onlineUpper > 0) {
						if(gsStatus.onlineLower <= currentOnline && currentOnline <= gsStatus.onlineUpper) {
							this.status = gsStatus.flag;
							break;
						}
					}
				}
			}
		}

		// @Override
		// public String toString() {
		// StringBuilder sb = new StringBuilder("GS(");
		// sb.append(gsID).append(",").append(label).append(",")
		// .append(remark).append(",").append(currentOnline)
		// .append("/").append(allowedOnline).append(",")
		// .append(status).append(",").append(history).append(",")
		// .append(freeMemory).append("/")
		// .append(allocatedTotalMemory).append(";");
		// // sb.append(whitelist).append(";");
		// sb.append(channel).append(";");
		// sb.append(gsIP).append(":").append(gsSocketPort).append(";");
		// sb.append("queuesize=" + sizeOfLoginQueue());
		// sb.append(")");
		// return sb.toString();
		// }

		private void countavgtime(long beginTime) {
//			long ct = System.currentTimeMillis();
//			totalpollN++;
//			totalpollusedtimems += (ct - lastpolltimems);
//			avgpolltimeseconds = (int) Math.max(1,
//					(totalpollusedtimems / totalpollN) / 1000);
			long ct = System.currentTimeMillis();
			totalpollN++;
			totalpollusedtimems += ct - beginTime;
			avgpolltimeseconds = (int) Math.max(1, (totalpollusedtimems / totalpollN) / 1000);
		}

		public int getAvgpolltimeseconds() {
			return avgpolltimeseconds;
		}

	}

	/*************************************************************
	 * Inner class
	 */
	public final class WhiteList {
		final int id;
		final Set<Long> pIDs = new HashSet<Long>();
		final Set<String> pNames = new HashSet<String>();

		WhiteList(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("WhiteList(");
			sb.append(id).append(" IDs[");
			for (Iterator<Long> iterator = pIDs.iterator(); iterator.hasNext();) {
				Long id = iterator.next();
				sb.append(id).append(",");
			}
			sb.append("] Names[");
			for (Iterator<String> iterator = pNames.iterator(); iterator
					.hasNext();) {
				String name = iterator.next();
				sb.append(name).append(",");
			}
			sb.append("]").append(")");
			return sb.toString();
		}
	}

	/********************************************
	 * Inner class
	 */
	public final class StatusHeader {
		// final static String HEADER_NAME_GOOD = "";
		// final static String HEADER_NAME_BUSY = "";
		// final static String HEADER_NAME_FULL = "";
		// final static String HEADER_NAME_PAUSE = "";
		// final static String HEADER_NAME_NEW = "";
		int onlineLower;
		int onlineUpper;
		byte status = -99;
		byte history = -99;
		String fontcolor;
		String headerString;
	}
	
	public final class GSStatusStruct {
		/** 服务器状态：维护 */
		public static final int STATUS_MAINTAIN = -1;
		/** 服务器状态：白名单 */
		public static final int STATUS_WHITE_LIST_ONLY = 0;
		/** 服务器状态：新服 */
		public static final int STATUS_NEW_SERVER = 1;
		/** 服务器状态：推荐 */
		public static final int STATUS_RECOMMEND = 2;
		/** 服务器状态：开放 */
		public static final int STATUS_OPEN = 3;
		/** 服务器状态：良好 */
		public static final int STATUS_GOOD = 4;
		/** 服务器状态：繁忙 */
		public static final int STATUS_BUSY = 5;
		/** 服务器状态：火爆 */
		public static final int STATUS_CRAZY = 6;
		/** 服务器状态：满 */
		public static final int STATUS_FULL = 7;
		
		byte flag;
		int onlineLower;
		int onlineUpper;
	}

	public static StatusHeader getHeader(GS gs) {
		if (gs != null) {
			// 是否维护状态
			for (StatusHeader h : statusHeaderSet) {
				if (h.status == gs.status) {
					if (h.status == -1) {
						return h;
					} else {
						break;
					}
				}
			}
			// 是否新区
			for (StatusHeader h : statusHeaderSet) {
				if (h.history == gs.history) {
					return h;
				}
			}
			// 根据在线判断
			for (StatusHeader h : statusHeaderSet) {
				if (gs.currentOnline >= h.onlineLower
						&& gs.currentOnline <= h.onlineUpper) {
					return h;
				}
			}
		}
		return null;
	}
}
