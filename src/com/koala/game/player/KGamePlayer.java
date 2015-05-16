/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.koala.game.KGame;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.gameserver.KGameServer;
import com.koala.game.logging.KGameLogger;
import com.koala.game.util.DateUtil;
import com.koala.thirdpart.json.JSONArray;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * 代表一个‘玩家’的数据
 * 
 * @author AHONG
 */
public class KGamePlayer {

	private static final KGameLogger _LOGGER = KGameLogger
			.getLogger(KGamePlayer.class);
	/**
	 * 玩家自定义数据的JSON KEY：表示账号是否首次充值
	 */
	private final static String JSON_KEY_PLAYER_ATTRIBUTE_FIRST_CHARGE = "K0";

	/**
	 * 玩家自定义数据的JSON KEY：表示账号的创建角色信息
	 */
	private final static String JSON_KEY_PLAYER_ATTRIBUTE_ROLE_INFO = "K1";

	/**
	 * 玩家自定义数据的JSON KEY：表示账号的首次创建角色游戏区ID
	 */
	private final static String JSON_KEY_PLAYER_FIRST_CREATE_ROLE_SERVER_ID = "K2";

	/**
	 * 玩家自定义数据的JSON KEY：表示账号的首次创建角色是否大于3级
	 */
	private final static String JSON_KEY_PLAYER_FIRST_CREATE_ROLE_LV_LARGER_THAN_3 = "K3";

	/**
	 * 玩家自定义数据的JSON KEY：表示账号的首次创建角色时的邀请码
	 */
	private final static String JSON_KEY_PLAYER_FIRST_CREATE_ROLE_INVITE_CODE = "K4";

	/**
	 * 玩家自定义数据的JSON KEY：表示账号的首次创建角色时的邀请码
	 */
	private final static String JSON_KEY_PLAYER_FIRST_CREATE_ROLE_BE_INVITE = "K5";

	/* -----------账号相关------------ */
	private long playerID;
	private String playerName;
	private String password;

	private int promoID;
	private int parentPromoID;// 推广子渠道ID
	private int securityQuestionIdx;
	private int securityAnswerIdx;
	// private String remark;
	private int type;// 玩家类型
	// private String attribute;//自定义一些附加信息，例如角色简明信息等

	private long createTimeMillis;
	private long lastestLoginTimeMillis;
	private long lastestLogoutTimeMillis;
	private long totalLoginCount;

	// 游戏数据相关>>>>>>>>>>>>>>>>>>>>

	// +++++放在'attribute'中++++++++++
	// 注意！！！放在attribute中的内容只在GS中更新！！！
	private static final String JSON_KEY_ATTR_CREATE_ROLE_GS_IDS = "crgsids";
	private static final String JSON_KEY_ATTR_INFOS = "infos";
	private static final String JSON_KEY_ATTR_LASTLOGINEDGSID = "lgsid";
	private static final String JSON_KEY_ATTR_LASTLOGINEDDEVICE = "ldevice";
	private final static String JSON_KEY_ATTR_ANALYSIS_20131129 = "analysis_";
//	private final ConcurrentHashMap<KGameSimpleRoleInfoCID, KGameSimpleRoleInfo> simpleroleinfos = new ConcurrentHashMap<KGameSimpleRoleInfoCID, KGameSimpleRoleInfo>(
//			10);
	private final ConcurrentHashMap<Integer, List<KGameSimpleRoleInfo>> simpleroleinfos = new ConcurrentHashMap<Integer, List<KGameSimpleRoleInfo>>(10);
	private final ConcurrentHashMap<Integer, List<KGameSimpleRoleInfo>> simpleroleinfosRO = new ConcurrentHashMap<Integer, List<KGameSimpleRoleInfo>>(10);
	
	private int lastLoginedGSID;
	private String lastLoginDeviceModel;
	private final List<Integer> _createRoleGsIds = new ArrayList<Integer>();
	private final List<Integer> _createRoleGsIdsRO = Collections.unmodifiableList(_createRoleGsIds);

	// ++++放在‘remark’中++++++++++++++++
	// 注意！！！放在remark中的内容只在FE中更新！！！
	private static final String JSON_KEY_REMARK_PROMOREMARK = "pch";
	private static final String JSON_KEY_REMARK_BAN = "ban";
	private static final String JSON_KEY_REMARK_GAG = "gag";
	private String promoRemark;
	// GM操作
	private long banEndtime;// 封号结束时间
	private long gagEndtime;// 禁言结束时间

	// 临时变量::::::::::::::::::::::::::::::::
	private KGameAccount account;

	// 推广渠道账号标识（唯一），与playerName为一对一关系
	private String promoMask;
	// 是否首次充值
	private boolean isFirstCharge;
	// 首次创建角色的游戏区ID
	private int firstCreateRoleServerId;
	// 是否有大于3级的角色，是：1，否：0
	private byte isLvLargerThan3 = 0;
	// 账号首次创建角色时输入的邀请码
	private String inviteCode = null;

	private boolean isFirstCreateRoleBeInvite = false;

	/**
	 * 玩家数据构造函数
	 * 
	 * @param playerID
	 * @param playerName
	 * @param password
	 * @param promoID
	 * @param securityQuestionIdx
	 * @param securityAnswerIdx
	 * @param remark
	 * @param lastestLoginTimeMillis
	 * @param lastestLogoutTimeMillis
	 * @param totalLoginCount
	 */
	public KGamePlayer(long playerID, String playerName, String password,
			int type, int promoID, int parentPromoID, int securityQuestionIdx,
			int securityAnswerIdx, String remark, long lastestLoginTimeMillis,
			long lastestLogoutTimeMillis, long createTimeMillis, long totalLoginCount, String attribute) {
		this.playerID = playerID;
		this.playerName = playerName;
		this.password = password;
		this.promoID = promoID;
		this.parentPromoID = parentPromoID;
		this.securityQuestionIdx = securityQuestionIdx;
		this.securityAnswerIdx = securityAnswerIdx;
		// this.remark = remark;
		decodeRemark(remark);
		this.lastestLoginTimeMillis = lastestLoginTimeMillis;
		this.lastestLogoutTimeMillis = lastestLogoutTimeMillis;
		this.totalLoginCount = totalLoginCount;
		this.createTimeMillis = createTimeMillis;
		decodeAttribute(attribute);
	}

	public KGamePlayer(long playerID, String playerName, String channelMask,
			String password, int type, int promoID, int parentPromoID,
			String promoMask, int securityQuestionIdx, int securityAnswerIdx,
			String remark, long lastestLoginTimeMillis,
			long lastestLogoutTimeMillis, long createTimeMillis, long totalLoginCount, String attribute) {
		this.playerID = playerID;
		this.playerName = playerName;
		this.promoMask = promoMask;
		this.password = password;
		this.promoID = promoID;
		this.parentPromoID = parentPromoID;
		this.securityQuestionIdx = securityQuestionIdx;
		this.securityAnswerIdx = securityAnswerIdx;
		// this.remark = remark;
		decodeRemark(remark);
		this.lastestLoginTimeMillis = lastestLoginTimeMillis;
		this.lastestLogoutTimeMillis = lastestLogoutTimeMillis;
		this.totalLoginCount = totalLoginCount;
		this.createTimeMillis = createTimeMillis;
		decodeAttribute(attribute);
	}

	public KGamePlayer(DBPlayer data) {
		this.playerID = data.getPlayerId();
		this.playerName = data.getPlayerName();
		this.promoMask = data.getPromoMask();
		this.password = data.getPassword();
		this.type = data.getType();
		this.promoID = data.getPromoId();
		this.parentPromoID = data.getParentPromoId();
		this.securityQuestionIdx = data.getSecurityQuestionIdx();
		this.securityAnswerIdx = data.getSecurityAnswerIdx();
		// this.remark = data.getRemark();
		decodeRemark(data.getRemark());
		this.lastestLoginTimeMillis = data.getLastestLoginTimeMillis();
		this.lastestLogoutTimeMillis = data.getLastestLogoutTimeMillis();
		this.totalLoginCount = data.getTotalLoginCount();
		this.createTimeMillis = data.getCreateTimeMillis();
		decodeAttribute(data.getAttribute());
	}

	private void decodeRemark(String remark) {
		if (remark != null && remark.length() > 0) {
			JSONObject j = null;
			try {
				if ((j = new JSONObject(remark)) != null) {
					promoRemark = j.optString(JSON_KEY_REMARK_PROMOREMARK);
					banEndtime = j.optLong(JSON_KEY_REMARK_BAN);
					gagEndtime = j.optLong(JSON_KEY_REMARK_GAG);
				}
			} catch (JSONException e) {
				// TODO 临时兼容旧数据，待删除
				String[] sep = remark.split(";");
				if (sep != null) {
					if (sep.length > 0)
						lastLoginedGSID = Integer.parseInt(sep[0]);
					if (sep.length > 1)
						lastLoginDeviceModel = sep[1];
					if (sep.length > 2)
						promoRemark = sep[2];
					if (sep.length > 3)
						banEndtime = Long.parseLong(sep[3]);
					if (sep.length > 4)
						gagEndtime = Long.parseLong(sep[4]);
				}
			}
		}
	}

	String encodeRemark() {
		// return lastLoginedGSID + ";" + lastLoginDeviceModel;
		try {
			JSONObject j = new JSONObject();
			j.put(JSON_KEY_REMARK_PROMOREMARK, promoRemark);
			j.put(JSON_KEY_REMARK_BAN, banEndtime);
			j.put(JSON_KEY_REMARK_GAG, gagEndtime);
			return j.toString();
		} catch (JSONException ex) {
			return promoRemark + ";" + banEndtime + ";" + gagEndtime;
		}
	}

	// TODO 自定义一些Player附加信息，例如角色简明信息等
	private void decodeAttribute(String attribute) {
		if (attribute != null && attribute.length() > 0) {
			boolean isJsonAttribute = false;
			JSONObject json = null;
			try {
				json = new JSONObject(attribute);
				isJsonAttribute = true;
			} catch (JSONException ex) {
				_LOGGER.warn("####ERROR:Player decodeAttribute出现异常！此字符串不是JSON格式，"
						+ "将采用普通解释方法解释attribute。attribute==" + attribute);
			}

			if (isJsonAttribute && json != null) {

				try {

					if (json.has(JSON_KEY_PLAYER_ATTRIBUTE_FIRST_CHARGE)) {
						byte firstChargeValue = json.optByte(
								JSON_KEY_PLAYER_ATTRIBUTE_FIRST_CHARGE,
								(byte) 0);
						if (firstChargeValue == 1) {
							isFirstCharge = true;
						} else {
							isFirstCharge = false;
						}

					}
					if (json.has(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_SERVER_ID)) {
						this.firstCreateRoleServerId = json.optInt(
								JSON_KEY_PLAYER_ATTRIBUTE_FIRST_CHARGE, 0);
					}
					this.isLvLargerThan3 = json.optByte(
							JSON_KEY_PLAYER_FIRST_CREATE_ROLE_LV_LARGER_THAN_3,
							(byte) 0);

					if (json.has(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_INVITE_CODE)) {
						this.inviteCode = json
								.getString(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_INVITE_CODE);
					}

					if (json.has(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_BE_INVITE)) {
						this.isFirstCreateRoleBeInvite = (json
								.optInt(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_BE_INVITE) == 1);
					}

					String roleInfoAttribute = json.optString(
							JSON_KEY_PLAYER_ATTRIBUTE_ROLE_INFO, "");
					decodeRoleInfoJsonAttribute(roleInfoAttribute);
					
					if (json.has(JSON_KEY_ATTR_CREATE_ROLE_GS_IDS)) {
						JSONArray array = json.getJSONArray(JSON_KEY_ATTR_CREATE_ROLE_GS_IDS);
						for (int i = 0; i < array.length(); i++) {
							this._createRoleGsIds.add(array.getInt(i));
						}
					}
					
					if (json.has(JSON_KEY_ATTR_ANALYSIS_20131129)) {
						decodeAnalysisInfoToAttribute(json.optString(JSON_KEY_ATTR_ANALYSIS_20131129));
					}

				} catch (Exception ex) {
					_LOGGER.error(
							"####ERROR:Player decodeAttribute出现异常！此时json的字符串是："
									+ attribute, ex);
				}
			} else {
				decodeRoleInfoJsonAttribute(attribute);

			}

		}
	}
	
	private void addSimpleRoleInfoToMap(KGameSimpleRoleInfo info) {
		List<KGameSimpleRoleInfo> tempList = simpleroleinfos.get(info.getCid().gsID);
		if(tempList == null) {
			tempList = new ArrayList<KGameSimpleRoleInfo>();
			simpleroleinfos.put(info.getCid().gsID, tempList);
			simpleroleinfosRO.put(info.getCid().gsID, Collections.unmodifiableList(tempList));
		}
		tempList.add(info);
	}

	private void decodeRoleInfoJsonAttribute(String attribute) {

		JSONObject j = null;
		try {
			if ((j = new JSONObject(attribute)) != null) {
				try {
					JSONArray jinfos = j.optJSONArray(JSON_KEY_ATTR_INFOS);
					for (int i = jinfos.length(); --i >= 0;) {
						JSONObject j1 = (JSONObject) jinfos.get(i);
						KGameSimpleRoleInfo sri = new KGameSimpleRoleInfo(j1);
//						simpleroleinfos.put(sri.getCid(), sri);
						this.addSimpleRoleInfoToMap(sri);
					}
					lastLoginedGSID = j.optInt(JSON_KEY_ATTR_LASTLOGINEDGSID);
					lastLoginDeviceModel = j
							.optString(JSON_KEY_ATTR_LASTLOGINEDDEVICE);

					decodeAnalysisInfoToAttribute(j
							.optString(JSON_KEY_ATTR_ANALYSIS_20131129));

				} catch (JSONException e0) {
					_LOGGER.warn("decodeRoleInfoJsonAttribute Exception. {}",
							attribute);
				}
			}
		} catch (JSONException e) {
			// TODO 临时兼容旧数据，待删除
			String[] dataSegment = attribute.split("#");
			// 1段：角色信息段
			if (dataSegment != null && dataSegment.length > 0
					&& dataSegment[0] != null) {
				String[] infos = dataSegment[0].split(";");
				for (String info : infos) {
					if (info.length() > 0) {
						// gsID + "," + roleID + "," + roleLV + "," +
						// roleJobType;
						String[] att = info.split(",");
						if (att.length != 4) {
							continue;
						}
						KGameSimpleRoleInfo roleinfo = new KGameSimpleRoleInfo(
								Integer.parseInt(att[0]),
								Long.parseLong(att[1]));
						roleinfo.roleLV = Integer.parseInt(att[2]);
						roleinfo.roleJobType = Integer.parseInt(att[3]);
//						simpleroleinfos.put(roleinfo.getCid(), roleinfo);
						this.addSimpleRoleInfoToMap(roleinfo);
					}
				}
			}
			// 2段
			if (dataSegment != null && dataSegment.length > 1
					&& dataSegment[1] != null) {
				String[] gmops = dataSegment[1].split(",");
				if (gmops != null && gmops.length > 1) {
					lastLoginedGSID = Integer.parseInt(gmops[0]);
					lastLoginDeviceModel = gmops[1];
				}
			}
		}
	}

	// TODO 自定义一些Player附加信息，例如角色简明信息等
	String encodeAttribute() {
		String attribute = "";
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_KEY_PLAYER_ATTRIBUTE_FIRST_CHARGE, isFirstCharge ? 1
					: 0);
			json.put(JSON_KEY_PLAYER_ATTRIBUTE_ROLE_INFO,
					encodeRoleInfoAttribute());
			if (firstCreateRoleServerId != 0) {
				json.put(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_SERVER_ID,
						firstCreateRoleServerId);
			}
			json.put(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_LV_LARGER_THAN_3,
					isLvLargerThan3);

			if (inviteCode != null) {
				json.put(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_INVITE_CODE,
						inviteCode);
			}

			json.put(JSON_KEY_PLAYER_FIRST_CREATE_ROLE_BE_INVITE,
					(isFirstCreateRoleBeInvite ? 1 : 0));

			json.putOpt(JSON_KEY_ATTR_ANALYSIS_20131129,
					encodeAnalysisInfoToAttribute());
			
			json.put(JSON_KEY_ATTR_CREATE_ROLE_GS_IDS, _createRoleGsIds);

			attribute = json.toString();
		} catch (JSONException ex) {
			_LOGGER.error("####ERROR:Player encodeAttribute出现异常！", ex);
		}

		return attribute;
	}

	private String encodeRoleInfoAttribute() throws JSONException {
		// StringBuilder sb = new StringBuilder();
		//
		// // 第一段：
		// for (KGameSimpleRoleInfo info : simpleroleinfos.values()) {
		// sb.append(info.toString()).append(";");
		// }
		//
		// sb.append("#");
		//
		// // 第二段：
		// sb.append(banEndtime).append(",").append(gagEndtime);
		//
		// return sb.toString();

		JSONObject j = new JSONObject();
		JSONArray jinfos = new JSONArray();
//		for (KGameSimpleRoleInfo info : simpleroleinfos.values()) {
//			jinfos.put(info.toJSON());
//		}
		List<KGameSimpleRoleInfo> list;
		for (Iterator<List<KGameSimpleRoleInfo>> itr = simpleroleinfos.values().iterator(); itr.hasNext();) {
			list = itr.next();
			for (KGameSimpleRoleInfo info : list) {
				jinfos.put(info.toJSON());
			}
		}
		j.put(JSON_KEY_ATTR_INFOS, jinfos);
		j.put(JSON_KEY_ATTR_LASTLOGINEDGSID, lastLoginedGSID);
		j.put(JSON_KEY_ATTR_LASTLOGINEDDEVICE, lastLoginDeviceModel);
		return j.toString();
	}

	private final Map<String, String> analysisInfoToAttribute = new ConcurrentHashMap<String, String>();

	/**
	 * 20131129为了统计客户端信息而增加的信息记录（增加到attribute字段，json格式） analysis_{ "k1":"v1" }
	 * 
	 * @param key
	 * @param value
	 * @return 该key的内容是否被更新了
	 */
	public boolean addAnalysisInfoToAttribute(String key, String value) {
		if (key != null && value != null) {
			String pre = analysisInfoToAttribute.put(key, value);
			if(pre == null) {
				return true;
			} else {
				return !pre.equals(value);
			}
		}
		return false;
	}
	
	/**
	 * 
	 * 获取player里面的analysisInfo的某个key的信息
	 * 
	 * @param key
	 * @return
	 */
	public String getAnalysisInfo(String key) {
		return analysisInfoToAttribute.get(key);
	}

	private String encodeAnalysisInfoToAttribute() throws JSONException {
		JSONObject j = new JSONObject();
		for (String k : analysisInfoToAttribute.keySet()) {
			String v = analysisInfoToAttribute.get(k);
			if (k != null && k.length() > 0 && v != null && v.length() > 0) {
				j.putOpt(k, v);
			}
		}
		return j.toString();
	}

	private void decodeAnalysisInfoToAttribute(String json)
			throws JSONException {
		if (json != null && json.length() > 0) {
			JSONObject j = new JSONObject(json);
			for (Iterator it = j.keys(); it.hasNext();) {
				String k = (String) it.next();
				String v = j.optString(k);
				if (k != null && k.length() > 0 && v != null && v.length() > 0) {
					analysisInfoToAttribute.put(k, v);
				}
			}
		}
	}

	public long getID() {
		return playerID;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String getPromoMask() {
		return promoMask;
	}

	public String getPassword() {
		return password;
	}

	public int getType() {
		return type;
	}

	public int getPromoID() {
		return promoID;
	}

	public int getParentPromoID() {
		return parentPromoID;
	}

	public int getSecurityQuestionIdx() {
		return securityQuestionIdx;
	}

	public int getSecurityAnswerIdx() {
		return securityAnswerIdx;
	}

	// public String getRemark() {
	// return remark;
	// }
	
	public long getCreateTimeMillis() {
		return createTimeMillis;
	}

	public long getLastestLoginTimeMillis() {
		return lastestLoginTimeMillis;
	}

	public long getLastestLogoutTimeMillis() {
		return lastestLogoutTimeMillis;
	}

	public long getTotalLoginCount() {
		return totalLoginCount;
	}

	public List<KGameSimpleRoleInfo> getRoleSimpleInfo4GsListShow(int gsID) {
//		List<KGameSimpleRoleInfo> list = new ArrayList<KGameSimpleRoleInfo>();
//		for (KGameSimpleRoleInfoCID kGameSimpleRoleInfoCID : simpleroleinfos
//				.keySet()) {
//			if (kGameSimpleRoleInfoCID.gsID == gsID) {
//				list.add(simpleroleinfos.get(kGameSimpleRoleInfoCID));
//			}
//		}
//		return list;
		List<KGameSimpleRoleInfo> list = simpleroleinfosRO.get(gsID);
		if(list == null) {
			return Collections.emptyList();
		}
		return list;
	}

	public KGameAccount getAccount() {
		return account;
	}

	public int getLastLoginedGSID() {
		return lastLoginedGSID;
	}

	public void setLastLoginedGSID(int lastLoginedGSID) {
		this.lastLoginedGSID = lastLoginedGSID;
	}

	public String getLastLoginDeviceModel() {
		return lastLoginDeviceModel;
	}

	public String setLastLoginDeviceModel(String lastLoginDeviceModel) {
		if (!lastLoginDeviceModel.equals(this.lastLoginDeviceModel)) {
			String tmp = this.lastLoginDeviceModel;
			this.lastLoginDeviceModel = lastLoginDeviceModel;
			return tmp;
		}
		return null;
	}
	
	public List<Integer> getCreateRoleGsIds() {
		return _createRoleGsIdsRO;
	}

	/**
	 * 【游戏逻辑调用】新建角色和角色信息更新（roleLV）时调用
	 * 
	 * @param gsID
	 *            游戏服务器ID
	 * @param roleID
	 *            角色ID
	 * @param roleLV
	 *            角色等级
	 * @param roleJob
	 *            角色职业类型
	 */
	public void updateRoleSimpleInfo(int gsID, long roleID, int roleLV,
			int roleJob) {
		if (firstCreateRoleServerId == 0) {
			firstCreateRoleServerId = KGame.getGSID();
			KGameServer.getInstance().getPlayerManager()
					.updatePlayerAttribute(this);
		}

		if (isLvLargerThan3 == 0 && roleLV >= 3) {
			isLvLargerThan3 = 1;
			KGameServer.getInstance().getPlayerManager()
					.updatePlayerAttribute(this);
		}

//		KGameSimpleRoleInfoCID cid = new KGameSimpleRoleInfoCID(gsID, roleID);
//		KGameSimpleRoleInfo rinfo = simpleroleinfos.get(cid);
		List<KGameSimpleRoleInfo> list = simpleroleinfos.get(gsID);
		KGameSimpleRoleInfo rinfo = null;
		if(list != null) {
			for(int i = 0; i < list.size(); i++) {
				rinfo = list.get(i);
				if(rinfo.getCid().roleID == roleID) {
					break;
				} else {
					rinfo = null;
				}
			}
		}
		if (rinfo == null) {
			rinfo = new KGameSimpleRoleInfo(gsID, roleID);
			rinfo.roleLV = roleLV;
			rinfo.roleJobType = roleJob;
//			simpleroleinfos.put(rinfo.getCid(), rinfo);
			this.addSimpleRoleInfoToMap(rinfo);
		} else {
			rinfo.roleLV = roleLV;
			rinfo.roleJobType = roleJob;
		}
		if (!_createRoleGsIds.contains(gsID)) {
			_createRoleGsIds.add(gsID);
		}
	}

	/**
	 * 更新账号首次创建角色时，该角色是有邀请码用户
	 * 
	 * @param inviteCode
	 */
	public void updateFirstCreateRoleBeInvite(String inviteCode) {
		this.inviteCode = inviteCode;
		this.isFirstCreateRoleBeInvite = true;
		KGameServer.getInstance().getPlayerManager()
				.updatePlayerAttribute(this);
	}

	/**
	 * 更新账号首次创建角色时，该角色不是邀请码用户
	 * 
	 * @param inviteCode
	 */
	public void updateFirstCreateRoleNotBeInvite() {
		this.isFirstCreateRoleBeInvite = true;
		KGameServer.getInstance().getPlayerManager()
				.updatePlayerAttribute(this);
	}

	/**
	 * 【游戏逻辑调用】删除角色时调用
	 * 
	 * @param gsID
	 *            游戏服务器ID
	 * @param roleID
	 *            角色ID
	 */
	public boolean deleteRoleSimpleInfo(int gsID, long roleID) {
		KGameSimpleRoleInfoCID cid = new KGameSimpleRoleInfoCID(gsID, roleID);
		return simpleroleinfos.remove(cid) != null;
	}

	/**
	 * 获取账号创建角色的数量
	 * 
	 * @return
	 */
	public int getCreateRoleSize() {
		return simpleroleinfos.size();
	}

	/**
	 * 解封时间，如果为0表示没被封号，否则需要对比当前时间看是否已解封
	 * 
	 * @return
	 */
	public long getBanEndtime() {
		return banEndtime;
	}

	/**
	 * 设定解封时间，设0表示即时解封，可设某个时间点（可通过{@link DateUtil}中的方法转换）解封
	 * 
	 * @param banEndtime
	 */
	public void setBanEndtime(long banEndtime) {
		this.banEndtime = banEndtime;
	}

	/**
	 * 禁言解封时间，如果为0表示没被禁言，否则需要对比当前时间看是否已解禁
	 * 
	 * @return
	 */
	public long getGagEndtime() {
		return gagEndtime;
	}

	/**
	 * 是否首次充值
	 * 
	 * @return
	 */
	public boolean isFirstCharge() {
		return isFirstCharge;
	}

	public void setFirstCharge(boolean isFirstCharge) {
		this.isFirstCharge = isFirstCharge;
		KGameServer.getInstance().getPlayerManager()
				.updatePlayerAttribute(this);
	}

	/**
	 * 设定禁言解封时间，设0表示即时解封禁言，可设某个时间点（可通过{@link DateUtil}中的方法转换）解封禁言
	 * 
	 * @param gagEndtime
	 */
	public void setGagEndtime(long gagEndtime) {
		this.gagEndtime = gagEndtime;
	}

	public String getPromoRemark() {
		return promoRemark;
	}

	public void setPromoRemark(String promoRemark) {
		this.promoRemark = promoRemark;
	}

	@Override
	public String toString() {
		return "KGamePlayer [playerID=" + playerID + ", playerName="
				+ playerName + ", password=" + password + ", promoID="
				+ promoID + ", lastLoginedGSID=" + lastLoginedGSID
				+ ", promoRemark=" + promoRemark + ", promoMask=" + promoMask
				+ "]";
	}

	public String getInviteCode() {
		return inviteCode;
	}

	public boolean isFirstCreateRoleBeInvite() {
		return isFirstCreateRoleBeInvite;
	}
	
	public boolean isHadCreatedRoleBefore(int gsId) {
		return this._createRoleGsIds.contains(gsId);
	}
	
	

	// private boolean online;
	// /**<i>超级临时变量，平台内部自己用，请勿乱用</i>*/
	// public boolean isOnline() {
	// return online;
	// }
	// /**<i>超级临时变量，平台内部自己用，请勿乱用</i>*/
	// public void setOnline(boolean online) {
	// this.online = online;
	// }
	//
	// /**单独保存一次remark和attribute--不要滥用！！*/
	// public void commitAttribute() {
	// try {
	// KGameDataAccessFactory
	// .getInstance()
	// .getPlayerManagerDataAccess()
	// .updatePlayerAttributeById(playerID, encodeRemark(),
	// encodeAttribute());
	// } catch (PlayerAuthenticateException e) {
	// e.printStackTrace();
	// } catch (KGameDBException e) {
	// e.printStackTrace();
	// }
	// }
}
