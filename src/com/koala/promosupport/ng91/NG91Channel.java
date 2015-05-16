package com.koala.promosupport.ng91;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class NG91Channel implements PromoChannel {

	private int promoID;
	private String description;

	public static final int ACT_USERVERIFY = 4;
	private int appId;
	private String appKey;
	private String sdkServer;
	private String httpMethod;

	private IUserVerify action_userverify;
	private PayCallbackMethod payCallbackMethod;

	public NG91Channel(int promoID, String description) {
		super();
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

		appId = Integer.parseInt(xml.getChildTextTrim("AppId"));
		appKey = xml.getChildTextTrim("AppKey");

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

		// if(!reload){
		// action_userverify = new NG91UserVerify(this);
		// }
		Element eUserVerify = xml.getChild("UserVerify");
		sdkServer = eUserVerify.getAttributeValue("url");
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

	public int getPromoID() {
		return promoID;
	}

	public String getDescription() {
		return description;
	}

	public int getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getSdkServer() {
		return sdkServer;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	// private final Set<Integer> subPromoIDSet = new HashSet<Integer>();
	//
	// @Override
	// public Set<Integer> subPromoIDSet() {
	// return subPromoIDSet;
	// }

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public IPayCallback newPayCallback() {
		return new NG91PayCallback(this);
	}

	/**
	 * 对字符串进行MD5并返回结果
	 * 
	 * @param sourceStr
	 * @return
	 */
	String md5(String sourceStr) {
		String signStr = "";
		try {
			byte[] bytes = sourceStr.getBytes("utf-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(bytes);
			byte[] md5Byte = md5.digest();
			if (md5Byte != null) {
				signStr = HexBin.encode(md5Byte);
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return signStr;
	}

	@Override
	public String toString() {
		return "NG91Channel [promoID=" + promoID + ", description="
				+ description + ", appId=" + appId + ", appKey=" + appKey
				+ ", sdkServer=" + sdkServer + ", httpMethod=" + httpMethod
				+ ", payCallbackMethod=" + payCallbackMethod + ", canLogin="
				+ canLogin + ", canPay=" + canPay + ", getParentPromoID="
				+ getParentPromoID() + "]";
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// String orderId = info.get("orderId"); //CP订单号，必须保证唯一
			// float money = Float.parseFloat(info.get("price")); //
			// 商品价格，单位：在91服务器配置里改兑换率
			// String payDescription =
			// info.get("ext");//即支付描述（客户端API参数中的payDescription字段）
			// 购买时客户端应用通过API传入，原样返回给应用服务器 开发者可以利用该字段，定义自己的扩展数据。例如区分游戏服务器
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put(
					"price",
					String.valueOf(PromoSupport.getInstance().getYuanBaoPrice(
							PriceUnit.YUAN)));
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}
}
