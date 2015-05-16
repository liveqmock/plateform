package com.koala.promosupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.dataaccess.InvalidUserInfoException;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.dataaccess.dbobj.DBPlayer;
import com.koala.game.exception.KGameServerException;
import com.koala.game.frontend.FEStatusMonitor;
import com.koala.game.frontend.KGameFrontend;
import com.koala.game.logging.KGameLogger;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.player.KGamePlayerUtil;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.DateUtil;
import com.koala.game.util.XmlUtil;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * 推广渠道（含SDK）支持。
 * 
 * <pre>
 * 1. 涉及配置文件 PromoChannelConfig.xml
 * 
 * 20131105接入的渠道与客户端的约定
 * --------------------------------------------------
 * 渠道		        c->s				   s->c
 * 5G		    sign/token			    orderId/ext
 * AGames		sessionId/accountId		orderId/notifyUrl/productName
 * AnZhi		sid				        orderNo(整型)/productDesc/ext
 * BaoRuan		token				    orderId/notifyUrl
 * BXGame		userId/ticket			orderId/notifyUrl/ext
 * Sohu		    uid/sessionId			orderId/productName/ext
 * UMeng		uid/sessionId			orderId/ratio/coinName
 * MZW			userId/token			orderId/ext/productName/productDesc/productId
 * ---------------------------------------------------
 * Kunlun		userId/klsso			orderId/ext
 * ---------------------------------------------------
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class PromoSupport implements KGameProtocol, KGameTimerTask {

	/**
	 * 为了适配子渠道id的规则。
	 * <p>
	 * <i> 【子渠道ID规则】子渠道的ID = 父渠道ID*10000+N，（N取值区间0~9999）
	 * ，例如当乐子渠道就是10010000~10019999共支持10000个子渠道。[PS：父渠道ID一定是4位数内最小1000最大9999]
	 * </i>
	 * </p>
	 * 
	 * @param promoid
	 *            渠道ID
	 * @return 父渠道（SDK的渠道）ID，如果本身就是父渠道则返回本身
	 */
	public static int computeParentPromoID(int promoid) {
		int i = promoid / 10000;
		return i == 0 ? promoid : i;
	}

	private final static KGameLogger logger = KGameLogger
			.getLogger(PromoSupport.class);
	// <渠道ID(SDK级的),PromoChannel对象>
	private final static Map<Integer, PromoChannel> promochannels = new HashMap<Integer, PromoChannel>(
			10);
	private static PromoSupport instance;
	private DoHttpRequest http;
	private float yuanBaoPrice4RMBFen;
	private boolean debugPayNoSign;
	private final Set<PromoSupportReloadListener> reloadListener = new HashSet<PromoSupportReloadListener>();
	final ConcurrentHashMap<String, PromoSupport.MoneyUnit> moneyUnit = new ConcurrentHashMap<String, PromoSupport.MoneyUnit>();

	private PromoSupport() {
		//http = new DoHttpRequest();
	}

	public static PromoSupport getInstance() {
		if (instance == null) {
			instance = new PromoSupport();
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	public void loadMoneyUnitMapping(File xmlfile, boolean reload) throws Exception{
		if (!reload) {
			if (xmlfile == null || (!xmlfile.exists())) {
				throw new FileNotFoundException("PromoSupport,xml not found. "
						+ xmlfile);
			}
			xmlFile = xmlfile;
		}
		Document doc = XmlUtil.openXml(xmlfile);
		Element root = doc.getRootElement();
		
		Element eMoneyUnitMapping = root.getChild("MoneyUnitMapping");
		if(eMoneyUnitMapping!=null){
			List<Element> moneys = eMoneyUnitMapping.getChildren("Money");
			for (Element m : moneys) {
				int dbType = Integer.parseInt(m.getAttributeValue("dbType"));
				String name = m.getAttributeValue("name");
				String code = m.getAttributeValue("code");
				String unit = m.getAttributeValue("unit");
				if(code!=null&&unit!=null){
					MoneyUnit mu = new MoneyUnit(dbType, name, code, unit);
					moneyUnit.put(code, mu);
				}
			}
		}
	}

	public void loadConfig(File xmlfile, boolean reload) throws Exception {
		if (!reload) {
			if (xmlfile == null || (!xmlfile.exists())) {
				throw new FileNotFoundException("PromoSupport,xml not found. "
						+ xmlfile);
			}
			xmlFile = xmlfile;
		}
		lastModifiedOfXml = xmlFile.lastModified();

		logger.info(">>>>>>>>>>LOAD XML {} ...", xmlFile);

		Document doc = XmlUtil.openXml(xmlfile);
		Element root = doc.getRootElement();

		modifyCheckSeconds = Integer.parseInt(root.getAttributeValue("modifyCheckSeconds"));
		
		yuanBaoPrice4RMBFen = Float.parseFloat(root
				.getChildTextTrim("YuanBaoPrice4RMBFen"));

		debugPayNoSign = Boolean.parseBoolean(root
				.getChildTextTrim("DebugPayNoSign"));
		
		Element eMoneyUnitMapping = root.getChild("MoneyUnitMapping");
		if(eMoneyUnitMapping!=null){
			List<Element> moneys = eMoneyUnitMapping.getChildren("Money");
			for (Element m : moneys) {
				int dbType = Integer.parseInt(m.getAttributeValue("dbType"));
				String name = m.getAttributeValue("name");
				String code = m.getAttributeValue("code");
				String unit = m.getAttributeValue("unit");
				if(code!=null&&unit!=null){
					MoneyUnit mu = new MoneyUnit(dbType, name, code, unit);
					moneyUnit.put(code, mu);
				}
			}
		}

		List<Element> ePromoChannels = root.getChildren("PromoChannel");
		for (Element ePromoChannel : ePromoChannels) {
			int pid = Integer.parseInt(ePromoChannel
					.getAttributeValue("promoid"));
			String desc = ePromoChannel.getAttributeValue("description");
			String clazzName = ePromoChannel.getAttributeValue("clazz");
			PromoChannel promochannel = promochannels.get(pid);
			//logger.debug("get promochannel {}",promochannel);
			if (promochannel == null) {
				@SuppressWarnings("unchecked")
				Class<PromoChannel> clazz = (Class<PromoChannel>) Class
						.forName(clazzName);
				Constructor<PromoChannel> constructor = clazz.getConstructor(
						int.class, String.class);
				promochannel = constructor.newInstance(pid, desc);
			}
			if (promochannel != null) {
				promochannel.init(ePromoChannel, reload);
				promochannels.put(pid, promochannel);
				logger.debug("put promochannel {}",pid);
			}

			// 重新加载的通知
			if (reload) {
				reloadNotify();
			}

			logger.info(">>>>>>>>>>PromoChannel【{}】(reload:{}) {}", pid,
					reload, promochannel);
		}
//		for (int p : getSupportPromoIdSet()) {
//			System.out.println(p);
//		}
	}

	/**
	 * 返回的对象都是以父渠道存在，即含SDK的渠道
	 * 
	 * @param promoID
	 *            有可能是子渠道ID，本方法内部会自行转换成父渠道ID去获取渠道对象
	 * @return
	 */
	public PromoChannel getPromoChannel(int promoID) {
		//20131223改成先检查子渠道ID有没有，如果有返回，没有就取其父渠道的对象
		PromoChannel pc = promochannels.get(promoID);
		if(pc==null){
			pc = promochannels.get(computeParentPromoID(promoID));
		}
		return pc;
	}

	public DoHttpRequest getHttp() {
		if(http==null){
			http = new DoHttpRequest();
		}
		return http;
	}

	public Set<Integer> getSupportPromoIdSet() {
		return promochannels.keySet();
	}

	public boolean isDebugPayNoSign() {
		return debugPayNoSign;
	}

	public boolean addReloadListener(PromoSupportReloadListener listener) {
		return (listener != null) ? reloadListener.add(listener) : false;
	}

	private void reloadNotify() {
		for (PromoSupportReloadListener listener : reloadListener) {
			listener.notifyPromoSupportReloaded();
		}
	}

	/**
	 * 价格单位
	 */
	public enum PriceUnit {
		YUAN {
			@Override
			public float toFen(float p) {
				return p * 100;
			}

			@Override
			public float toYuan(float p) {
				return p;
			}

			@Override
			public float convert(float p, PriceUnit srcUnit) {
				return srcUnit.toYuan(p);
			}
		},
		FEN {
			@Override
			public float toFen(float p) {
				return p;
			}

			@Override
			public float toYuan(float p) {
				return p / 100;
			}

			@Override
			public float convert(float p, PriceUnit srcUnit) {
				return srcUnit.toFen(p);
			}
		};
		public float convert(float p, PriceUnit srcUnit) {
			throw new AbstractMethodError();
		}

		public float toYuan(float p) {
			throw new AbstractMethodError();
		}

		public float toFen(float p) {
			throw new AbstractMethodError();
		}
	}

	/**
	 * 元宝价格
	 * 
	 * <pre>
	 * <b>
	 * ！！！！！！！！！！！！！！！！！
	 * 
	 * 这个非常重要，而且一经确定不再修改。
	 * 
	 * <u>例如：目前是1元兑换10个元宝，即元宝价格为10分或0.10元</u>
	 * 
	 * 客户端会将此值转换成float类型的
	 * </b>
	 * </pre>
	 * 
	 * @param priceUnit
	 *            价格单位
	 * @return 元宝价格
	 */
	public float getYuanBaoPrice(PriceUnit priceUnit) {
		return priceUnit.convert(yuanBaoPrice4RMBFen, PriceUnit.FEN);
	}

	// /**
	// * 元宝价格，单位：RMB元
	// *
	// * <pre>
	// * <b>
	// * ！！！！！！！！！！！！！！！！！
	// *
	// * 这个非常重要，而且一经确定不再修改。
	// *
	// * <u>如目前是1元兑换10个元宝，即元宝价格为0.10元</u>
	// *
	// * 客户端会将此值转换成float类型的
	// * </b>
	// * </pre>
	// *
	// * @return 元宝价格，单位：RMB元
	// */
	// public float getYuanBaoPrice4RMBYuan(){
	// return yuanBaoPrice4RMBFen/100;
	// }

	/**
	 * 各渠道自行做完跟SDK服务器的验证后。后面的工作（统一验证和返回客户端）交由本方法处理
	 * 
	 * @param playersession
	 * @param promoID
	 * @param parentPromoID
	 * @param promoMask
	 * @param promoRemark
	 * @param responsemsg
	 * @param extresponseparamsifsucceed
	 */
	public void afterUserVerified(KGamePlayerSession playersession,
			int promoID, int parentPromoID, String promoMask,
			String promoRemark, KGameMessage responsemsg,
			Map<String, String> extresponseparamsifsucceed, Map<String, String> analysisInfoNeedSaveToDB) {
		DBPlayer dbPlayer = null;
		// 1、验证我们对应该渠道的账号是否存在
		try {
			// logger.debug("获取player前的信息：promoID={}，parentPromoID={}，promoRemark={}，promoMask={}",
			// promoID, parentPromoID, promoRemark, promoMask);
			// dbPlayer = KGameDataAccessFactory.getInstance()
			// .getPlayerManagerDataAccess()
			// .verifyPlayerPassportByPromoMask(promoMask, promoID);
			dbPlayer = KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess()
					.verifyPlayerPassportByPromoMask(promoMask, parentPromoID); // 2013-10-13
																				// 改用parent_promo_id
		} catch (PlayerAuthenticateException e1) {
			if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
				// 如果不存在这个账号mapping直接注册一个新的KOLA账号
				try {
					dbPlayer = KGameFrontend
							.getInstance()
							.getPlayerManager()
							.registerNewPassportByPromoMask(promoID,
									parentPromoID, promoMask, "");
				} catch (InvalidUserInfoException e11) {
					logger.error(
							"registerNewPassportByPromoMask异常！promoID={}，promoMask={}",
							promoID, promoMask, e11);
				} catch (KGameDBException e11) {
					logger.error(
							"registerNewPassportByPromoMask异常！promoID={}，promoMask={}",
							promoID, promoMask, e11);
				} catch (Exception e) {
					logger.error(
							"registerNewPassportByPromoMask异常！promoID={}，promoMask={}",
							promoID, promoMask, e);
				}
			} else {
				logger.error("afterUserVerified异常！promoID={}，promoMask={}",
						promoID, promoMask, e1);
			}
		} catch (KGameDBException e1) {
			logger.error("afterUserVerified异常！promoID={}，promoMask={}",
					promoID, promoMask, e1);
		}
		logger.debug(
				"[afterUserVerified]promoID={},promoMask={},promoRemark={},dbPlayer={}",
				promoID, promoMask, promoRemark, dbPlayer);
		// 2、找到了对应的账号，成功进入下一步
		if (dbPlayer != null && dbPlayer.getPlayerName() != null) {

			if (playersession.decodeAndBindPlayer(dbPlayer)) {
				playersession.getBoundPlayer().setPromoRemark(promoRemark);
				// 2014-09-02 添加 保存analysisInfo
				boolean updateAnalysisInfo = false;
				if (analysisInfoNeedSaveToDB.size() > 0) {
					Map.Entry<String, String> entry;
					boolean temp;
					for (Iterator<Map.Entry<String, String>> itr = analysisInfoNeedSaveToDB.entrySet().iterator(); itr.hasNext();) {
						entry = itr.next();
						temp = playersession.getBoundPlayer().addAnalysisInfoToAttribute(entry.getKey(), entry.getValue());
						if (temp && !updateAnalysisInfo) {
							updateAnalysisInfo = true;
						}
					}
				}
				// 2014-09-02 END
				// 检测是否有封号
				long banEndtime = playersession.getBoundPlayer()
						.getBanEndtime();
				if (banEndtime > 0 && banEndtime > System.currentTimeMillis()) {
					responsemsg.writeInt(PL_PASSPORT_VERIFY_FAILED_BAN);
					responsemsg.writeUtf8String(KGameTips.get("BAN",
							DateUtil.formatReadability(new Date(banEndtime))));
					playersession.send(responsemsg);
					return;
				} else {
					playersession.setAuthenticationPassed(true);// 验证通过

					responsemsg.writeInt(PL_PASSPORT_VERIFY_SUCCEED);
					responsemsg.writeUtf8String("");
					responsemsg.writeUtf8String(dbPlayer.getPlayerName());
					responsemsg.writeUtf8String(dbPlayer.getPassword());
					// ----------------------------------------------------------
					// 20130710新增附加返回参数KV形式 int paramN; for(paramN){
					// String key;//具体看常量PROMO_KEY_???
					// String value;
					// }
					responsemsg.writeInt(extresponseparamsifsucceed.size());
					for (String k : extresponseparamsifsucceed.keySet()) {
						responsemsg.writeUtf8String(k);
						responsemsg.writeUtf8String(extresponseparamsifsucceed
								.get(k));
					}
					// -----------------------------------------------------------
					// 2.直接携带‘服务器列表’的内容
//					KGameFrontend.getInstance().getGSMgr().writeGsListOnResponseMsg(responsemsg, playersession, parentPromoID); // 2014-11-06 去掉，客户端已经不读这里了
					playersession.send(responsemsg);
					// 统计
					FEStatusMonitor.commcounter.logined();
					if(updateAnalysisInfo) {
						// 先保存一次attribute，因为在sessionClose的时候保存，可能会来不及被GS读取
						KGamePlayerUtil.updatePlayerAttribute(playersession.getBoundPlayer());
					}
					return;
				}
			}
		}
		// 其它失败情况
		responsemsg.writeInt(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY);
		responsemsg.writeUtf8String(KGameTips
				.get("PL_UNKNOWNEXCEPTION_OR_SERVERBUSY"));
		playersession.send(responsemsg);
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>自动检测XML是否有修改
	private File xmlFile;
	private long lastModifiedOfXml;
	private int modifyCheckSeconds = 60;

	@Override
	public String getName() {
		return "PromoSupport";
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		try {
			if (xmlFile.exists() && xmlFile.lastModified() != lastModifiedOfXml) {
				logger.info("--------------reload xml {}", xmlFile);
				try {
					loadConfig(xmlFile, true);
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
		logger.warn("RejectedExecutionException@{},{}", getName(), e);
	}

	public static void main(String[] args) {
		// System.out.println(MD5.MD5Encode("0c5b5d1007d8e4e38ffd7407ccb03e11#81a21836788e8bab57deb2043a3e6bc5"));
		JSONObject j = new JSONObject();
		try {
			j.put("a", 123);
			j.put("b", "abc");
			JSONObject j1 = new JSONObject();
			j1.put("c", 111);
			j1.put("d", "aaa");
			j.put("data", j1);
			System.out.println(j.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	
	public final class MoneyUnit{
		private int dbType;//用于充值流水
		private String name;
		private String code;
		private String unit;
		public MoneyUnit(int dbType, String name, String code, String unit) {
			this.dbType = dbType;
			this.name = name;
			this.code = code;
			this.unit = unit;
		}
		public int getDbType() {
			return dbType;
		}
		public String getName() {
			return name;
		}
		public String getCode() {
			return code;
		}
		public String getUnit() {
			return unit;
		}
	}
	
	public MoneyUnit getMoneyUnit(String code){
		return moneyUnit.get(code);
	}
}
