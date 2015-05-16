package com.koala.promosupport.uc;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

public class UCChannel implements PromoChannel {

	private final boolean test = false;

	private int promoID;
	private String description;

	private String cpId; // <CPID>25540</CPID>
	private String gameId; // <GameID>505489</GameID>
	private String serverId; // <ServerID>1923</ServerID>
	private String channelId; // <ChannelID>2</ChannelID>
	private String apiKey; // <ApiKey>509a963f23f66af85f2382b9278383d3</ApiKey>
	private String sdkServerURL; // <SdkServerURL>http://sdk.g.uc.cn/ss</SdkServerURL>

	private String httpMethod;

	private String service_sidinfo;// <service_sidinfo>ucid.user.sidInfo</service_sidinfo>
	private String service_bind;// <service_bind>ucid.bind.create</service_bind>

	private IUserVerify action_userverify;
	private PayCallbackMethod payCallbackMethod;

	public UCChannel(int promoID, String description) {
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

		Element e = test ? (xml.getChild("Test")) : xml;
		cpId = (e.getChildTextTrim("CPID"));
		gameId = (e.getChildTextTrim("GameID"));
		serverId = (e.getChildTextTrim("ServerID"));
		channelId = (e.getChildTextTrim("ChannelID"));
		apiKey = e.getChildTextTrim("ApiKey");

		service_sidinfo = xml.getChildTextTrim("service_sidinfo");
		service_bind = xml.getChildTextTrim("service_bind");

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
		// action_userverify = new UCUserVerify(this);
		// }
		Element eUserVerify = xml.getChild("UserVerify");
		sdkServerURL = eUserVerify.getAttributeValue("url");
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
		return new UCPayCallback(this);
	}

	public String getCpId() {
		return cpId;
	}

	public String getGameId() {
		return gameId;
	}

	public String getServerId() {
		return serverId;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getSdkServerURL() {
		return sdkServerURL;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getService_sidinfo() {
		return service_sidinfo;
	}

	public String getService_bind() {
		return service_bind;
	}

	@Override
	public String toString() {
		return "UCChannel [test=" + test + ", promoID=" + promoID
				+ ", description=" + description + ", cpId=" + cpId
				+ ", gameId=" + gameId + ", serverId=" + serverId
				+ ", channelId=" + channelId + ", apiKey=" + apiKey
				+ ", sdkServerURL=" + sdkServerURL + ", httpMethod="
				+ httpMethod + ", service_sidinfo=" + service_sidinfo
				+ ", service_bind=" + service_bind + ", payCallbackMethod="
				+ payCallbackMethod + ", canLogin=" + canLogin + ", canPay="
				+ canPay + ", getParentPromoID=" + getParentPromoID() + "]";
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
			// float money = Float.parseFloat(info.get("price")); // 商品价格，单位：元
			// --0.1
			// String callbackInfo = info.get("ext"); //CP自定义信息
			// --字符串pid1001gid1rid1234
			paramsToClientBeforePay.put("ext", "");
			paramsToClientBeforePay.put("price", "0");// 金额数为0，运行不定额支付流程。
			paramsToClientBeforePay.put("serverId", getServerId());
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI, String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}
}
