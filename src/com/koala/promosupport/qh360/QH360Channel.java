package com.koala.promosupport.qh360;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.PromoSupport.PriceUnit;

public class QH360Channel implements PromoChannel {

	public static final int ERROR_CODE_TOKENEXPIRES = 4010202;// access
																// token已过期（OAuth2）

	private int promoID;
	private String description;

	private int appId;// <AppId>200575661</AppId>
	private String appKey;// <AppKey>81a21836788e8bab57deb2043a3e6bc5</AppKey>
	private String appSecret;// <AppSecret>0c5b5d1007d8e4e38ffd7407ccb03e11</AppSecret>
	private String appName;

	private String urlGetAccessToken;
	private String httpMethod;
	private String urlGetUserInfo;

	private IUserVerify action_userverify;
	private PayCallbackMethod payCallbackMethod;
	private String payCallbackUrl;

	public String getUrlGetUserInfo() {
		return urlGetUserInfo;
	}

	public QH360Channel(int promoID, String description) {
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
		appSecret = xml.getChildTextTrim("AppSecret");
		appName = xml.getChildTextTrim("AppName");

		// List<Element> subcs = xml.getChildren("SubPromoChannel");
		// for (Element sub : subcs) {
		// String pi = sub.getAttributeValue("promoid");
		// if (pi != null && pi.length() > 0) {
		// subPromoIDSet.add(Integer.parseInt(pi.trim()));
		// }
		// }

		payCallbackUrl = xml.getChildTextTrim("PayCallbackUrl");
		String pm = xml.getChildTextTrim("PayCallbackMethod");
		if ("GET".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.GET;
		} else if ("POST".equalsIgnoreCase(pm)) {
			payCallbackMethod = PayCallbackMethod.POST;
		} else {
			payCallbackMethod = PayCallbackMethod.GET_POST;
		}

		// if (!reload) {
		// action_userverify = new QH360GetAccessTokenThenUserInfo(this);
		// }

		Element eUserVerify = xml.getChild("UserVerify");
		urlGetAccessToken = eUserVerify.getAttributeValue("urlGetAccessToken");
		urlGetUserInfo = eUserVerify.getAttributeValue("urlGetUserInfo");
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

	public int getAppId() {
		return appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public String getUrlGetAccessToken() {
		return urlGetAccessToken;
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

	public String getAppName() {
		return appName;
	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}

	@Override
	public IPayCallback newPayCallback() {
		return new QH360PayCallback(this);
	}

	@Override
	public String toString() {
		return "QH360Channel [promoID=" + promoID + ", description="
				+ description + ", appId=" + appId + ", appKey=" + appKey
				+ ", appSecret=" + appSecret + ", appName=" + appName
				+ ", urlGetAccessToken=" + urlGetAccessToken + ", httpMethod="
				+ httpMethod + ", urlGetUserInfo=" + urlGetUserInfo
				+ ", payCallbackMethod=" + payCallbackMethod
				+ ", payCallbackUrl=" + payCallbackUrl + ", canLogin="
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
			// -充值S2C的参数MAP：orderId、ext、price、product、productId、exchange_ratio、accessToken、userId、notifyUri、appName、appUserName、appUserId
			paramsToClientBeforePay.put("orderId", "");
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put("price", "0"); // 必需参数，所购买商品金额，以分为单位。金额大于等于100分，360SDK运行定额支付流程；
														// 金额数为0，360SDK运行不定额支付流程。
			paramsToClientBeforePay.put("product",
					KGameProtocol.CONST_PAYMENT_VALUE_PRODUCE);
			paramsToClientBeforePay.put("productId",
					KGameProtocol.CONST_PAYMENT_VALUE_PRODUCEID);
			paramsToClientBeforePay.put("exchange_ratio", String
					.valueOf((int) PromoSupport.getInstance().getYuanBaoPrice(
							PriceUnit.FEN)));// 必需参数，人民币与游戏充值币的默认比例，例如2，代表1元人民币可以兑换2个游戏币，整数。
			// paramsToClientBeforePay.put("accessToken","" );//TODO
			// 难搞了！！！每个用户会话都要不同
			// paramsToClientBeforePay.put("userId", "");//TODO 难搞了！！！每个用户会话都要不同
			paramsToClientBeforePay.put("notifyUri", payCallbackUrl);// TODO
																		// URL要设置了
			paramsToClientBeforePay.put("appName", getAppName());
			paramsToClientBeforePay.put("appUserName", "");
			paramsToClientBeforePay.put("appUserId", "");
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}

	// public static void main(String[] args) {
	// System.out.println(String.valueOf((int)(Float.parseFloat(PromoSupport.getInstance().getYuanBaoPrice4RMBYuan())
	// * 100)));
	// }
}
