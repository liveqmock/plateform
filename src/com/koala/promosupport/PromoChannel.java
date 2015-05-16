package com.koala.promosupport;

import java.util.Map;

import org.jdom.Element;

import com.koala.paymentserver.IPayCallback;

public interface PromoChannel {

//	public static final int PROMOID_KOLA = 1000;
//	public static final int PROMOID_DOWNJOY = 1001;
//	public static final int PROMOID_UC = 1002;
//	public static final int PROMOID_91 = 1003;
//	public static final int PROMOID_360 = 1004;
//	public static final int PROMOID_MI = 1005;
//	public static final int PROMOID_LENOVO = 1006;
//	public static final int PROMOID_DUOKOO = 1007;

	/** 充值回调是“SDK服务器”通过什么HTTP方法发送数据给“我们的支付服务器” */
	public enum PayCallbackMethod {
		GET, POST, GET_POST,GET_STRING
	}

	void init(Element xml, boolean reload) throws Exception;

	int getPromoID();

	String getDescription();
	
	/**可以登录（安全防范，怕有个别渠道临时有问题，我们可以即时开关其对应功能）*/
	boolean canLogin();
	
	/**可以支付（安全防范，怕有个别渠道临时有问题，我们可以即时开关其对应功能），依赖{@link #canLogin()}*/
	boolean canPay();
	
//	/**是否要求客户端打开固定充值金额的界面*/
//	boolean openDefaultRechargeUI();

	// /**
	// * 子渠道的ID集合
	// *
	// * @return
	// */
	// Set<Integer> subPromoIDSet();

	/**
	 * 获取父渠道的ID（如果本身就是父渠道则返回{@link #getPromoID()}相同的值）
	 * <p>
	 * <i> 【子渠道ID规则】子渠道的ID = 父渠道ID*10000+N，（N取值区间0~9999）
	 * ，例如当乐子渠道就是10010000~10019999共支持10000个子渠道 </i>
	 * </p>
	 * @return
	 * @see PromoSupport#computeParentPromoID(int)
	 */
	int getParentPromoID();
	
	IUserVerify getUserVerify();

	IPayCallback newPayCallback();

	/**
	 * 充值回调是“SDK服务器”通过什么HTTP方法发送数据给“我们的支付服务器”
	 * 
	 * @return {@link PayCallbackMethod#GET} / {@link PayCallbackMethod#POST} /
	 *         {@link PayCallbackMethod#GET_POST}
	 */
	PayCallbackMethod getPayCallbackMethod();
	
	/**
	 * 当客户端要进行充值时会向GS获取一些参数（各渠道不同）发去给SDK服务器。所以在PS跟GS握手的时候就把这些参数先发给GS缓存起来（注：
	 * GS是没有渠道相关的内容的）
	 * <pre>
	 * 以下是对各渠道的param说明：
	 * ============================================
	 * 当乐 (1001)
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：元 --0.1
	 * String productName = info.get("product");  // 商品名称 --元宝
	 * String extInfo = info.get("ext");  // CP自定义信息，多为CP订单号 -- 字符串pid1001gid1rid1234
	 * ------------------------------------------------------------------------------------
	 * UC(1002)
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：元 --0.1
	 * String callbackInfo = info.get("ext"); //CP自定义信息 --字符串pid1001gid1rid1234
	 * ------------------------------------------------------------------------------------
	 * 91(1003)
	 * String orderId = info.get("orderId");  //CP订单号，必须保证唯一
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：在91服务器配置里改兑换率
	 * String payDescription = info.get("ext");//即支付描述（客户端API参数中的payDescription字段） 购买时客户端应用通过API传入，原样返回给应用服务器 开发者可以利用该字段，定义自己的扩展数据。例如区分游戏服务器 
	 * -------------------------------------------------------------------------------------
	 * 360(1004)
	 *               -充值S2C的参数MAP：orderId、ext、price、product、productId、exchange_ratio、accessToken、userId、notifyUri、appName、appUserName、appUserId
	 * --------------------------------------------------------------------------------------
	 * Lenovo(1005)  -登录时c2s的参数MAP：lpsust
	 *               -充值S2C的参数MAP：orderId、ext、appKey、notifyUri、productCode、price(单位分)									
	 * --------------------------------------------------------------------------------------										
	 * Xiaomi(1006)  -登录时c2s的参数MAP：uid,sessionId
	 *               -充值S2C的参数MAP：orderId、ext、price								
	 * --------------------------------------------------------------------------------------
	 * Duokoo(1007)  -登录时c2s的参数MAP：uid,sessionId
	 *               -充值S2C的参数MAP：orderId、ext、price、product、exchange_ratio
	 * ==============================================
	 * </pre>
	 */
	Map<String, String> getParamsToClientBeforePay();
	
	public final static String PARAM_KEY_OPENOURPRICEUI = "openpriceui";
}
