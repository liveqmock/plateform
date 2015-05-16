package com.koala.promosupport.sky;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.game.util.StringUtil;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoChannel.PayCallbackMethod;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class SkyChannel implements PromoChannel {

	private int promoID;
	private String description;
	
	private boolean canPay;
	private boolean canLogin;
	
	private String merchantId;
	private String appID;
	private String appKey;
	private String appName;
	private String systemId;
	private String payType;
	private String payMethod;
	private String channelId;
	private String payCallbackUrl;
	private PayCallbackMethod payCallbackMethod;
	
	private String httpUrlUV;
	private String httpMethodUV;
	private IUserVerify action_userverify;
	
	
	public SkyChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	@Override
	public void init(Element xml, boolean reload) throws Exception {
		this.canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		this.canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));
		
		this.appID = xml.getChildTextTrim("AppId");
		this.merchantId = xml.getChildTextTrim("MerchantId");
		this.appName = xml.getChildTextTrim("AppName");
		this.appKey= xml.getChildTextTrim("AppKey");
		
		this.systemId = xml.getChildTextTrim("SystemId");
		this.payType = xml.getChildTextTrim("PayType");
		this.payMethod = xml.getChildTextTrim("PayMethod");
		this.channelId = xml.getChildTextTrim("ChannelId");
		
		this.payCallbackUrl = xml.getChildTextTrim("PayCallbackUrl");
		String pm = xml.getChildTextTrim("PayCallbackMethod");
		this.payCallbackMethod = PayCallbackMethod.valueOf(pm);
//		if ("GET".equalsIgnoreCase(pm)) {
//			this.payCallbackMethod = PayCallbackMethod.GET;
//		} else if ("POST".equalsIgnoreCase(pm)) {
//			this.payCallbackMethod = PayCallbackMethod.POST;
//		} else {
//			this.payCallbackMethod = PayCallbackMethod.GET_POST;
//		}
		
		Element eUserVerify = xml.getChild("UserVerify");
		httpUrlUV = eUserVerify.getAttributeValue("url");
		httpMethodUV = eUserVerify.getAttributeValue("method");
		@SuppressWarnings("unchecked")
		Class<IUserVerify> clazz = (Class<IUserVerify>) Class
				.forName(eUserVerify.getAttributeValue("clazz"));
		Constructor<IUserVerify> constructor = clazz.getConstructor(this
				.getClass());
		action_userverify = constructor.newInstance(this);
	}

	@Override
	public int getPromoID() {
		return promoID;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public String getAppID() {
		return appID;
	}

	public String getAppKey() {
		return appKey;
	}

	@Override
	public boolean canLogin() {
		return canLogin;
	}

	@Override
	public boolean canPay() {
		return (canLogin && canPay);
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public IUserVerify getUserVerify() {
		return action_userverify;
	}

	@Override
	public IPayCallback newPayCallback() {
		return new SkyPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}
	
	private Map<String, String> paramsToClientBeforePay;
	
	/**注：下面前面有!的表示不用
	 * <pre>
	 * 参数名称  必填  说明  类型（长度）  备注 
	 * payMethod  是  付费方式  String  3rd：第三方 
	 * merchantId  是  商户号  String（50）  sky分配 
	 * merchantSign  是  商户签名  String（50） 按要求用商户密钥，对订单生成的签名 注：为减低风险，商户签名应尽量在服务端生成，避免让客户端知道商户密钥。 
	 * appId  是  应用编号  Int  每个应用的唯一标识号 
	 * appName  是  应用名称  String（30）  （应用定义） 
	 * price  是  付费价格  Int  以分为单位 
	 * orderId  是  订单号  String（50）
	 * !skyId  是  Sky的账户ID  Int 使用sky 的账户体系才需要传入skyId 
	 * !token  是    String（30） 使用sky 的账户体系才需要传入token 
	 * systemId  是  系统号  Int system=300020  Android 支付请求（冒泡堂） system=300021 冒泡堂 system=300022 逗斗 system=300023 开放平台 system=300024 支付接入 system=300025 公司自研 
	 * payType  是  支付类型  Int 0=注册 1=道具 2=积分 3=充值，50=网游小额支付（如果不填，默认是道具） 
	 * orderDesc  否  订单描述  String（50） 用于第三方付费时的提示语（将显示在第三方付费首页面），如：“用户名：AAA；金豆 余额 ：1000；1 元 等于100金豆” 
	 * notifyAddress  否  应用服务端通知地址  String（512）  Http协议 
	 * !appVersion  否  应用版本号  String（30） 为统计同一个应用不同版本的收入 
	 * !gameType  否  游戏类型  Int 0：单机 1：联网 2：弱联网 
	 * !orderTitle  否  订单标题  String（50）  （应用定义） 
	 * !productName  否  商品名称  String（50）  （应用定义） 
	 * channelId  否  渠道号  String（30）  （应用定义） 
	 * reserved1  否  保留字段1  String（50） 留给cp 存储自己的变量,通知cp服务端的时候，会把该变量保持不变的传回去。 如此字段中包含“=”、“ ”、“/”等转义字符时，需要进行URLEncode操作 
	 * !reserved2  否  保留字段2  String（50）  同reserved1 
	 * !reserved3  否  保留字段3  String（50）  同reserved1
	 * </pre>
	 */
	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			
			paramsToClientBeforePay.put(KGameProtocol.CONST_PAYMENT_KEY_ORDERID, "");
			paramsToClientBeforePay.put(KGameProtocol.CONST_PAYMENT_KEY_EXT, "");
			paramsToClientBeforePay.put("price", "0");
			
			paramsToClientBeforePay.put("payMethod", this.payMethod);
			paramsToClientBeforePay.put("merchantId",getMerchantId());
			paramsToClientBeforePay.put("merchantPwd", getAppKey());//商户密码
			paramsToClientBeforePay.put("appId", getAppID());
			paramsToClientBeforePay.put("systemId", this.systemId);
			paramsToClientBeforePay.put("payType", this.payType);
			paramsToClientBeforePay.put("notifyUrl", this.payCallbackUrl);
			paramsToClientBeforePay.put("appName", this.appName);
			paramsToClientBeforePay.put("channelId2", this.channelId);
			paramsToClientBeforePay.put("orderDesc", StringUtil.format("1元={}个元宝", (int)(1/PromoSupport.getInstance().getYuanBaoPrice(PriceUnit.YUAN))));
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI, String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}

	@Override
	public String toString() {
		return "SkyChannel [promoID=" + promoID + ", description="
				+ description + ", canPay=" + canPay + ", canLogin=" + canLogin
				+ ", merchantId=" + merchantId + ", appID=" + appID
				+ ", appKey=" + appKey + ", appName=" + appName + ", systemId="
				+ systemId + ", payType=" + payType + ", channelId="
				+ channelId + ", payMethod=" + payMethod + ", payCallbackUrl="
				+ payCallbackUrl + ", payCallbackMethod=" + payCallbackMethod
				+ ", httpUrlUV=" + httpUrlUV + ", httpMethodUV=" + httpMethodUV
				+ "]";
	}
	
}
