package com.koala.promosupport.kola;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;

import com.koala.game.KGameProtocol;
import com.koala.paymentserver.IPayCallback;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoChannel;
import com.koala.promosupport.PromoSupport;

/**
 * KOLA自有渠道，即无SDK的渠道
 * 
 * @author AHONG
 * 
 */
public class KolaChannel implements PromoChannel,KGameProtocol {

	private int promoID;
	private String description;

//	private final Set<Integer> subPromoIDSet = new HashSet<Integer>();
	
	private PayCallbackMethod payCallbackMethod;
	
	private IUserVerify action_userverify;

	public KolaChannel(int promoID, String description) {
		this.promoID = promoID;
		this.description = description;
	}

	private boolean canLogin;
	@Override
	public boolean canLogin(){return canLogin;}
	private boolean canPay;
	@Override
	public boolean canPay(){return (canLogin&&canPay);}
	
	public void init(Element xml, boolean reload) throws Exception {
		canLogin = Boolean.parseBoolean(xml.getAttributeValue("canlogin"));
		canPay = Boolean.parseBoolean(xml.getAttributeValue("canpay"));

//		List<Element> subcs = xml.getChildren("SubPromoChannel");
//		for (Element sub : subcs) {
//			String pi = sub.getAttributeValue("promoid");
//			if (pi != null && pi.length() > 0) {
//				subPromoIDSet.add(Integer.parseInt(pi.trim()));
//			}
//		}
		
//		String pm = xml.getChildTextTrim("PayCallbackMethod");
//		if ("GET".equalsIgnoreCase(pm)) {
//			payCallbackMethod = PayCallbackMethod.GET;
//		} else if ("POST".equalsIgnoreCase(pm)) {
//			payCallbackMethod = PayCallbackMethod.POST;
//		} else {
//			payCallbackMethod = PayCallbackMethod.GET_POST;
//		}
		
		@SuppressWarnings("unchecked")
		Class<IUserVerify> clazz = (Class<IUserVerify> )Class.forName(xml.getChild("UserVerify").getAttributeValue("clazz"));
		action_userverify = clazz.newInstance();
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

//	@Override
//	public Set<Integer> subPromoIDSet() {
//		return subPromoIDSet;
//	}

	@Override
	public int getParentPromoID() {
		return PromoSupport.computeParentPromoID(promoID);
	}
	
	@Override
	public IPayCallback newPayCallback() {
		return new KolaPayCallback(this);
	}

	@Override
	public PayCallbackMethod getPayCallbackMethod() {
		return payCallbackMethod;
	}

	@Override
	public String toString() {
		return "KolaChannel [promoID=" + promoID + ", description="
				+ description + ", payCallbackMethod=" + payCallbackMethod
				+ "]";
	}


	private Map<String, String> paramsToClientBeforePay;

	@Override
	public Map<String, String> getParamsToClientBeforePay() {
		if (paramsToClientBeforePay == null) {
			paramsToClientBeforePay = new HashMap<String, String>();
			// TODO 加入内容
			paramsToClientBeforePay.put(PARAM_KEY_OPENOURPRICEUI,
					String.valueOf(true)); // 测试全部开放UI 2013-09-25
		}
		return paramsToClientBeforePay;
	}
}
