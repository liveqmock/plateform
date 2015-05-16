package com.koala.paymentserver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.koala.game.KGameProtocol;
import com.koala.game.util.DateUtil;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * 支付/充值订单
 * 
 * @author AHONG
 * 
 */
public class PayOrder {

	/** 时间格式：yyyy-MM-dd HH:mm:ss */
	public static final DateFormat FORMAT_PAYTIME = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	private PayExtParam ext;// 自定义属性，在客户端发起时发给SDK服务器然后原样传给我们的支付服务器PS
	private String orderId;// 订单ID，由渠道方SDK生成
	private String money; // 充值数额，单位：RMB分
	private String promoMask;// 各渠道的用户ID/用户名
	private String payWay;// 支付通道

	private String paytime;// 支付时间，格式yyyy-MM-dd HH:mm:ss
	private String otherinfo;// 每个渠道的额外信息，但LOG/流水中还是记录下来以防需要查询
	
	private String goodsName;
	private float goodsPrice;
	private int goodsCount;
	private String payCurrencyCode;
	
//	private int coins;

	/** 订单状态 */
	public enum OrderStatus {
		/** 订单等待处理中（可能还没轮到发送去GS） */
		WAITING2PROCESS,
		/** 处理中（等待发送到GS及返回） */
		PROCESSING,
		/** 处理完成：成功 */
		DONE_SUCCESS,
		/** 处理完成：失败 */
		DONE_FAILED;
	}

	private OrderStatus orderStatus = OrderStatus.WAITING2PROCESS;
	private PayOrderID payOrderID;// 复合ID，通过promoID+渠道的orderId复合避免“不同渠道的orderId有可能一样”的隐患

	/**
	 * 生成一个订单
	 * 
	 * @param ext
	 *            自定义属性，透传参数：GS(编码)->Client->SDKServer->PS(解码)
	 * @param orderId
	 *            订单ID，部分是由渠道方SDK生成，部分是由GS生成发给客户端传给SDK
	 * @param money
	 *            充值数额，单位：RMB分
	 * @param promoMask
	 *            各渠道的用户ID/用户名（注：部分渠道返回为空）
	 * @param payWay
	 *            支付通道
	 * @param paytime
	 *            支付时间，格式{@link #FORMAT_PAYTIME}
	 * @param otherinfo
	 *            每个渠道的额外信息，但LOG/流水中还是记录下来以防需要查询
	 */
	public PayOrder(PayExtParam ext, String orderId, String money,
			String promoMask, String payWay, String paytime, String otherinfo) {
		this.ext = ext;
		this.orderId = orderId;
		this.money = money;
		this.promoMask = promoMask;
		this.payWay = payWay;
		this.paytime = paytime;
		this.otherinfo = otherinfo;
		payOrderID = new PayOrderID(this.ext.getPromoID(), this.orderId);
	}

	public PayOrder(String jsonstring) {
		try {
			JSONObject json = new JSONObject(jsonstring);
			this.ext = new PayExtParam(json.optString("ext"));
			this.orderId = json.optString("orderId");
			this.money = json.optString("money");
			this.promoMask = json.optString("promoMask");
			this.payWay = json.optString("payWay");
			this.paytime = json.optString("paytime");
			this.otherinfo = json.optString("otherinfo");

			this.orderStatus = OrderStatus.valueOf(json
					.optString("orderStatus"));
			
			//20131209增加
			goodsName = json.optString("goodsName");
			goodsPrice = json.optFloat("goodsPrice",0.10f);
			goodsCount = json.optInt("goodsCount");
			payCurrencyCode = json.optString("payCurrencyCode");
			
//			//20131210增加
//			this.coins = json.optInt("coins",0);

			payOrderID = new PayOrderID(this.ext.getPromoID(), this.orderId);
		} catch (JSONException e) {
			throw new IllegalArgumentException(jsonstring);
		}
	}

	/** CP自定义信息（含promoid、gsid、roleid），SDK服务器做透传 */
	public PayExtParam getExt() {
		return ext;
	}

	/** 订单号（渠道方） */
	public String getOrderId() {
		return orderId;
	}

	/** 支付通道，如果SDK服务器没此信息则默认是0 */
	public String getPayWay() {
		return payWay;
	}

	/**
	 * 支付时间（如果SDK服务器没此信息则为收到callback的时间） {@link #FORMAT_PAYTIME}
	 */
	public String getPaytime() {
		return paytime;
	}

	/** 其它附加信息，有些SDK服务器返回信息较多，我们LOG下来之后可能需要用于查询或DEBUG */
	public String getOtherinfo() {
		return otherinfo;
	}

	/** 支付金额，单位：RMB分 <b>【注意：用float类型来转换】<b> */
	public String getMoney() {
		return money;
	}

	/** 复合ID，通过promoID+渠道的orderId复合避免“不同渠道的orderId有可能一样”的隐患 */
	public PayOrderID getPayOrderID() {
		return payOrderID;
	}

	/**
	 * 其实就是用户ID，只是每个渠道都有不同，所以用一个promoMask来统一，结合promoID就可以从数据库获得对应的用户
	 * <b>【注：个别SDK服务器无返回，本值为""】</b>
	 */
	public String getPromoMask() {
		return promoMask;
	}

	/** 当前状态 */
	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	@Override
	public String toString() {
		return "PayOrder [ext=" + ext + ", orderId=" + orderId + ", money="
				+ money + ", promoMask=" + promoMask + ", payWay=" + payWay
				+ ", paytime=" + paytime + ", otherinfo=" + otherinfo
				+ ", goodsName=" + goodsName + ", goodsPrice=" + goodsPrice
				+ ", goodsCount=" + goodsCount + ", payCurrencyCode="
				+ payCurrencyCode +  ", orderStatus="
				+ orderStatus + "]";
	}

	/** 转换成JSON格式字符串，方便传递 */
	public String toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("ext", ext.toString());
		json.put("orderId", orderId);
		json.put("money", money);
		json.put("promoMask", promoMask);
		json.put("payWay", payWay);
		json.put("paytime", paytime);
		json.put("otherinfo", otherinfo);
		json.put("orderStatus", orderStatus);
		
		//20131209增加
		json.put("goodsName", goodsName);
		json.put("goodsPrice",goodsPrice);
		json.put("goodsCount",goodsCount);
		json.put("payCurrencyCode",payCurrencyCode);
		
//		//20131213增加
//		json.put("coins", coins);
		
		return json.toString();
	}

	public static String convert2PaytimeFormat(String timestring,
			String srctimeformat) {
		DateFormat sf = new SimpleDateFormat(srctimeformat);
		try {
			return DateUtil.format(sf.parse(timestring), FORMAT_PAYTIME);
		} catch (ParseException e) {
			return srctimeformat;
		}
	}

//	/**
//	 * 商品信息某些渠道（例如昆仑）需要用到的
//	 * 
//	 * @param goodsName
//	 *            商品名称
//	 * @param goodsPrice
//	 *            商品单价
//	 * @param goodsCount
//	 *            商品数量
//	 * @param payCurrencyCode
//	 *            支付货币代号（比如：CNY 中国 HKD 香港 TWD 台湾
//	 *            http://en.wikipedia.org/wiki/ISO_4217 可以查询）
//	 */
//	public void setGoodsInfos(String goodsName, String goodsPrice,
//			String goodsCount, String payCurrencyCode) {
//		this.goodsName = goodsName;
//		this.goodsPrice = goodsPrice;
//		this.goodsCount = goodsCount;
//		this.payCurrencyCode = payCurrencyCode;
//	}

	public void setGoodsName(String goodsName) {
		this.goodsName = goodsName;
	}

	public void setGoodsPrice(float goodsPrice) {
		this.goodsPrice = goodsPrice;
	}

	public void setGoodsCount(int goodsCount) {
		this.goodsCount = goodsCount;
	}
	
	public void setPayCurrencyCode(String payCurrencyCode) {
		this.payCurrencyCode = payCurrencyCode;
	}

	/**
	 * 商品名称（有些渠道可能要用到，某些没设置的渠道返回一个默认值）
	 * 
	 * @return
	 */
	public String getGoodsName() {
		return goodsName!=null?goodsName:KGameProtocol.CONST_PAYMENT_VALUE_PRODUCE;
	}

	/**
	 * 商品单价（有些渠道可能要用到，某些没设置的渠道返回一个默认值）
	 * 
	 * @return
	 */
	public float getGoodsPrice() {
		return goodsPrice!=0?goodsPrice:0.10f;
	}

	/**
	 * 商品数量（有些渠道可能要用到，某些没设置的渠道返回一个默认值）
	 * 
	 * @return
	 */
	public int getGoodsCount() {
		return goodsCount!=0?goodsCount:((int)(Float.parseFloat(money)/100/getGoodsPrice()));
	}

	/**
	 * 支付货币代号（比如：CNY 中国 HKD 香港 TWD 台湾 http://en.wikipedia.org/wiki/ISO_4217
	 * 可以查询）（有些渠道可能要用到，某些没设置的渠道返回一个默认值）
	 * 
	 * @return
	 */
	public String getPayCurrencyCode() {
		return payCurrencyCode!=null?payCurrencyCode:"CNY";
	}
	
//	public void setCoins(int coins){
//		this.coins = coins;
//	}
	
	/**
	 * 这个值是指推广渠道方要求的充值后为账号加的金币（即元宝）数。
	 * <pre>
	 * <b>注意：
	 * 1、一般情况下都不会使用此值来做加元宝操作，只有特殊渠道才需要用到。正常情况是根据{@link #getMoney()}值来操作的；
	 * 2、此值默认返回是0，只是某些特殊渠道才有大于0的值返回，GS在加元宝的时候需要判断此返回值再做操作；
	 * 3、此值是应加金币数，例如：某渠道充值5元，coins是70，即充值5元给70元宝。至于KOLA自身的赠送与此值无关，该怎么送就怎么送（根据{@link #getMoney()}值来操作）；
	 * </b>
	 * 跟{@link #getGoodsCount()}是一样的
	 * </pre>
	 * @return
	 */
	public int getCoins(){
		return getGoodsCount();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PayOrder) {
			return this.orderId == ((PayOrder) obj).orderId;
		}
		return false;
	}

	// public static void main(String[] args) {
	// System.out.println(OrderStatus.DONE_SUCCESS);
	// System.out.println(OrderStatus.valueOf("DONE_SUCCESS"));
	// }
}
