package com.koala.promosupport.muzhiwan;

import java.util.Date;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * 3、	用户支付成功后，拇指玩服务器会推送支付成功消息到接入方服务器指定接口地址，包含详细订单内容。接入方服务器接收成功并处理完成后，
 * 需要返回SUCCESS字符串给拇指玩服务器，区分大小写，拇指玩服务器接收到SUCCESS字符串视为一次完整支付流程的结束。
 * 若未成功推送到接入方服务器，或未接收到SUCCESS字符串，拇指玩服务器会在每日固定时间点（未定），统一处理掉单状态的订单，重新向接入方服务器发送支付成功消息。
 * 
 * 服务器接口定义：
 * 	支付结果回调接口（拇指玩服务器调用接入方服务器接口）
 * 		
 * URL	由接入方提供
 * 请求方式	GET
 * 请求参数	参数名	类型	描述
 * 	appkey	字符串	游戏唯一标记
 * 	orderID	字符串	订单唯一标记
 * 	productName	字符串	商品名称
 * 	productDesc	字符串	商品描述
 * 	productID	字符串	商品ID
 * 	money	字符串	金额，为整形，无小数点，元为单位
 * 	uid	字符串	充值用户ID
 * 	extern	字符串	扩展域
 * 	sign	字符串	签名，签名规则如下：
 * 1、	除了sign外的其它所有参数值，按照以下顺序拼合成新的字符串：
 * appkey
 * orderid
 * productName
 * productDesc
 * productID
 * money
 * uid
 * extern
 * 
 * 例如：
 * appkey=test&ordered=242323&productName=goods&productDesc=sds&productID=3434&money=100&uid=1232&extern=sdsd
 * 那么生成的新字符串应该为：
 * test242323goodssds34341001232sdsd
 * 
 * 2、	再加上拇指玩提供的一个唯一字符串，拼合成一个新字符串
 * 
 * 例如拇指玩提供的唯一字符串是testkey，那么新生成的字符串为：
 * test242323goodssds34341001232sdsdtestkey
 * 3、	最后对这个新字符串进行MD5，就是sign的值
 * 
 * 
 * 注意：
 * 1、	要求每个接入方都必须对sign进行校验，避免伪造数据
 * 2、	唯一字符串由拇指玩提供，请在接入时向工作人员索要
 * 返回数据	SUCCESS
 * 接收到信息并处理成功后，必须返回该字符串，区分大小写。
 * 
 * 
 * 支付返回码定义：
 * 		1、0：代表支付失败
 * 		2、1：代表支付成功
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class MuzhiwanPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(MuzhiwanPayCallback.class);
	private MuzhiwanChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "SUCCESS";
	private static final String FAILED = "FAILED";

	private String appkey	  ;//字符串	游戏唯一标记
	private String orderID	  ;//字符串	订单唯一标记
	private String productName;//字符串	商品名称
	private String productDesc;//字符串	商品描述
	private String productID  ;//字符串	商品ID
	private String money	  ;//字符串	金额，为整形，无小数点，元为单位
	private String uid	      ;//字符串	充值用户ID
	private String extern	  ;//字符串	扩展域

	public MuzhiwanPayCallback(MuzhiwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		
		appkey	   = params.get("appkey");//游戏唯一标记                     
		orderID	   = params.get("orderID");//订单唯一标记                     
		productName= params.get("productName");//商品名称                         
		productDesc= params.get("productDesc");//商品描述                         
		productID  = params.get("productID");//商品ID                           
		money	   = params.get("money");//金额，为整形，无小数点，元为单位 
		uid	       = params.get("uid");//充值用户ID                       
		extern	   = params.get("extern");//扩展域                           
		String sign= params.get("sign");//签名，签名规则如下：       

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		//签名验证
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			sb.append(appkey).append(orderID).append(productName).append(productDesc).append(productID).append(money).append(uid).append(extern);
			sb.append(ch.getSignkey());
			String gen_sign = MD5.MD5Encode(sb.toString(),"utf-8");
			if (!gen_sign.equalsIgnoreCase(sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(extern);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, orderID, String.valueOf(Float
				.parseFloat(money) * 100), uid, payWay, paytime, productName+productID+productDesc);

		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

		return SUCCESS;
	}

	@Override
	public String parse(String content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String responseOfRepeatCallback() {
		return SUCCESS;
	}

	@Override
	public String toString() {
		return "MuzhiwanPayCallback [appkey=" + appkey + ", orderID=" + orderID
				+ ", productName=" + productName + ", productDesc="
				+ productDesc + ", productID=" + productID + ", money=" + money
				+ ", uid=" + uid + ", extern=" + extern + "]";
	}

//	public static void main(String[] args) {
//		String s="529d3f437aafd3792033203b6674eb8946459cb81b551a870b67元宝游戏内货币113792033p1210g223r226529d3f6cda7ac";
//		System.out.println("e28918ffb488b682dfe77d10b86ffdae");
//		System.out.println(MD5.MD5Encode(s));
//		System.out.println(MD5.MD5Encode(s,"utf-8"));
//		System.out.println(MD5_System.getInstance().md5(s));
//		System.out.println(MD5_System.getInstance().Md5(s));
//	}
}
