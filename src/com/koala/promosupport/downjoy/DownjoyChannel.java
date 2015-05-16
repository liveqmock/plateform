package com.koala.promosupport.downjoy;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

public class DownjoyChannel implements PromoChannel {

	private int promoID;
	private String description;

	private String urlLoginVerify;
	private String httpMethod;

	private String cp;// <CP></CP> <!--接入当乐开放平台的游戏/应用公司 -->
	private String app;// <APP></APP> <!-- 接入当乐开放平台的游戏/应用 -->
	private int merchantID;// <MERCHANT_ID></MERCHANT_ID> <!-- 接入时由当乐分配的厂商ID。
							// -->
	private int appID;// <APP_ID></APP_ID> <!--接入时由当乐分配的游戏/应用ID。 -->
	private String serverSeqNum;// <SERVER_SEQ_NUM></SERVER_SEQ_NUM>
								// <!--接入时由当乐分配的服务器序列号，用以标识和使用不同的计费通知地址。
	// -->
	private String appKey;// <APP_KEY></APP_KEY> <!-- 接入时由当乐分配的游戏/应用密钥。 -->
	private String paymentKey;// <PAYMENT_KEY></PAYMENT_KEY> <!--
								// 接入时由当乐分配的游戏/应用支付密钥，用以验证计费通知合法性或发送退款请求。

	// -->
	private IUserVerify action_userverify;
	private PayCallbackMethod payCallbackMethod;

	public DownjoyChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	private boolean canLogin;

	@Override
	public boolean canLogin() {
		return canLogin;
	}

	private boolean canPay;

	@Override
	public boolean canPay() {
		return (canLogin && canPay);
	}

	public void init(Element xml, boolean reload) throws Exception {
		canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));

		cp = xml.getChildTextTrim("CP");
		app = xml.getChildTextTrim("APP");
		merchantID = Integer.parseInt(xml.getChildTextTrim("MERCHANT_ID"));
		appID = Integer.parseInt(xml.getChildTextTrim("APP_ID"));
		serverSeqNum = xml.getChildTextTrim("SERVER_SEQ_NUM");
		appKey = xml.getChildTextTrim("APP_KEY");
		paymentKey = xml.getChildTextTrim("PAYMENT_KEY");

		// List<Element> subcs = xml.getChildren("SubPromoChannel");
		// for (Element sub : subcs) {
		// String pi = sub.getAttributeValue("promoid");
		// if (pi != null && pi.length() > 0) {
		// subPromoIDSet.add(Integer.parseInt(pi.trim()));
		// }
		// }

		String pm = xml.getChildTextTrim("PayCallbackMethod");
		if ("GET".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.GET;
		} else if ("POST".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.POST;
		} else {
			payCallbackMethod = PayCallbackMethod.GET_POST;
		}

		// if (!reload) {
		// action_userverify = new DownjoyUserVerify(this);
		// }
		Element eUserVerify = xml.getChild("UserVerify");
		urlLoginVerify = eUserVerify.getAttributeValue("url");
		httpMethod = eUserVerify.getAttributeValue("method");

		@SuppressWarnings("unchecked")
		Class<IUserVerify> clazz = (Class<IUserVerify>) Class
				.forName(eUserVerify.getAttributeValue("clazz"));
		Constructor<IUserVerify> constructor = clazz.getConstructor(this
				.getClass());
		action_userverify = constructor.newInstance(this);
	}

	@Override
	public IUserVerify getUserVerify() {
		return action_userverify;
	}

	@Override
	public int getPromoID() {
		return promoID;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public int getPid() {
		return promoID;
	}

	public String getUrlLoginVerify() {
		return urlLoginVerify;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getCp() {
		return cp;
	}

	public String getApp() {
		return app;
	}

	public int getMerchantID() {
		return merchantID;
	}

	public int getAppID() {
		return appID;
	}

	public String getServerSeqNum() {
		return serverSeqNum;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	// private final Set<Integer> subPromoIDSet = new HashSet<Integer>();
	//
	// @Override
	// public Set<Integer> subPromoIDSet() {
	// return subPromoIDSet;
	// }

	@Override
	public IPayCallback newPayCallback() {
		return new DownjoyPayCallback(this);
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public String toString() {
		return "DownjoyChannel [promoID=" + promoID + ", description="
				+ description + ", urlLoginVerify=" + urlLoginVerify
				+ ", httpMethod=" + httpMethod + ", cp=" + cp + ", app=" + app
				+ ", merchantID=" + merchantID + ", appID=" + appID
				+ ", serverSeqNum=" + serverSeqNum + ", appKey=" + appKey
				+ ", paymentKey=" + paymentKey + ", payCallbackMethod="
				+ payCallbackMethod + ", canLogin=" + canLogin + ", canPay="
				+ canPay + ", getParentPromoID=" + getParentPromoID() + "]";
	}

	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// 当乐 (1001)
			// float money = Float.parseFloat(info.get("price")); //
			// 商品价格，单位：元--0.1
			// String productName = info.get("product"); // 商品名称 --元宝
			// String extInfo = info.get("ext"); // CP自定义信息，多为CP订单号 --
			// 字符串pid1001gid1rid1234
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put("price", "0");
			paramsToClientBeforePay.put("product",
					KGameProtocol.CONST_PAYMENT_VALUE_PRODUCE);
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true));
		}
		return paramsToClientBeforePay;
	}
}
