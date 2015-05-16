package com.koala.promosupport.qh360;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.game.util.DateUtil;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * 360的支付回调
 * 
 * <pre>
 * 应用在接收到通知消息后, 需回应ok, 表示通知已经接收.  如果回应其他值或者不回应,  则被认为
 * 通知失败, 360 会尝试多次通知.  这个机制用来避免掉单.
 * 
 * 应用应做好接收到多次通知的准备, 防止多次加钱. 同时,  需要特别注意的是,  回应的ok表示应用
 * 已经正常接到消息,  无需继续发送通知. 它不表示订单成功与否,  或者应用处理成功与否.  对于重复
 * 的通知, 应用可能发现订单已经成功处理完毕,  无需继续处理,  也要返回ok. 否则, 360会认为未成
 * 功通知, 会继续发送通知
 * 
 * -----------------------------
 * app_key  Y  应用app key  Y 
 * product_id  Y  所购商品id  Y 
 * amount  Y  总价,以分为单位  Y 
 * app_uid  Y  应用内用户id   Y 
 * app_ext1  N  应用扩展信息1原样返回  Y 
 * app_ext2  N  应用扩展信息2原样返回  Y 
 * user_id  N  360账号id  Y 
 * order_id  Y  360返回的支付订单号  Y 
 * gateway_flag N  如果支付返回成功，返回success 应用需要确认是success才给用户加钱 Y 
 * sign_type  Y  定值 md5  Y 
 * app_order_id  N 应用订单号 支付请求时若传递就原样返回 Y 
 * sign_return  Y 应用回传给订单核实接口的参数 不加入签名校验计算 N 
 * sign  Y  签名  N
 * -----------------------------
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class QH360PayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(QH360PayCallback.class);
	
	private QH360Channel ch;
	private PayOrder payOrder;
	
	private String app_key      ;//Y  应用app key  Y 
	private String product_id   ;//Y  所购商品id  Y 
	private String amount       ;//Y  总价,以分为单位  Y 
	private String app_uid      ;//Y  应用内用户id   Y 
	private String app_ext1     ;//N  应用扩展信息1原样返回  Y 
	private String app_ext2     ;//N  应用扩展信息2原样返回  Y 
	private String user_id      ;//N  360账号id  Y 
	private String order_id     ;//Y  360返回的支付订单号  Y 
	private String gateway_flag ;//N  如果支付返回成功，返回success 应用需要确认是success才给用户加钱 Y 
	private String sign_type    ;//Y  定值 md5  Y 
	private String app_order_id ;//N  应用订单号 支付请求时若传递就原样返回 Y 
	private String sign_return  ;//Y  应用回传给订单核实接口的参数 不加入签名校验计算 N 
	private String sign         ;//Y  签名  N

	public QH360PayCallback(QH360Channel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		// 将参数读取出来
		this.app_key = params.get("app_key");           //Y  应用app key  Y                                                 
		this.product_id = params.get("product_id");     //Y  所购商品id  Y                                                    
		this.amount = params.get("amount");             //Y  总价,以分为单位  Y                                                  
		this.app_uid = params.get("app_uid");           //Y  应用内用户id   Y                                                  
		this.app_ext1 = params.get("app_ext1");         //N  应用扩展信息1原样返回  Y                                               
		this.app_ext2 = params.get("app_ext2");         //N  应用扩展信息2原样返回  Y                                               
		this.user_id = params.get("user_id");           //N  360账号id  Y                                                   
		this.order_id = params.get("order_id");         //Y  360返回的支付订单号  Y                                               
		this.gateway_flag = params.get("gateway_flag"); //N  如果支付返回成功，返回success 应用需要确认是success才给用户加钱 Y                    
		this.sign_type = params.get("sign_type");       //Y  定值 md5  Y                                                    
		this.app_order_id = params.get("app_order_id"); //N  应用订单号 支付请求时若传递就原样返回 Y                                        
		this.sign_return = params.get("sign_return");   //Y  应用回传给订单核实接口的参数 不加入签名校验计算 N                                   
		this.sign = params.get("sign");                 //Y  签名  N                                                        
		
		logger.info("【{}充值】生成callback {}",ch.getPromoID(), this);
		
		if (gateway_flag != null && "success".equalsIgnoreCase(gateway_flag)) {
			// 生成签名字符串///////////////////////////////////////////////
			// 6.1 签名算法：
			// 签名算法不区分前后端，只要在需要签名的地方，均采用如下的算法
			// 1. 必选参数必须有值, 而且参数值必须不为空，不为0. 字符集为utf-8
			// 2. 所有不为空，不为0的参数都需要加入签名，参数必须为做urlencode之前的原始数值. 如中
			// 文 金币, 作为参数传输时编码为%E9%87%91%E5%B8%81, 做签名时则要用其原始中文值
			// 金币 (注意字符集必须是UTF-8)
			List<String> signkeys = new ArrayList<String>();
			for (String k : params.keySet()) {
				if ((!"sign_return".equals(k)) && (!"sign".equals(k))) {
					signkeys.add(k);
				}
			}
			Collections.sort(signkeys);// 根据参数名首字母升序
			StringBuilder buf = new StringBuilder();
			for (String k : signkeys) {
				String v = params.get(k);
				// 1. 必选参数必须有值, 而且参数值必须不为空，不为0.
				if (v != null && v.length() > 0 && (!"0".equals(v))) {
					buf.append(v).append("#");
				}
			}
			buf.append(ch.getAppSecret());
			String gen_sign = MD5.MD5Encode(buf.toString()).toLowerCase();
			if (PromoSupport.getInstance().isDebugPayNoSign()||gen_sign.equals(this.sign)) {// 签名验证通过
				
				// 将从渠道支付通知的内容整理成一个订单PayOrder，然后发到GS
				PayExtParam pext = new PayExtParam(app_ext1);
				String paytime = DateUtil.formatReadability(new Date(System.currentTimeMillis()));
				String payWay = "0";// downjoy没有返回payWay就用0代替了
				payOrder = new PayOrder(pext, order_id, amount, user_id,
						payWay, paytime, "app_order_id=" + app_order_id);
				logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);

//				// 通知GS进行发货
//				PS2GS p2g = PaymentServer.getIntance().getPS2GS(pext.getGsID());
//				if (p2g != null) {
//					p2g.sendPayOrder2GS(pOrder);
//				} else {
//					logger.error("【{}充值】找不到PS2GS(gsid={}),{}",ch.getPromoID(), pext.getGsID(),
//							this);
//				}
				
			} else {
				// sign验证失败
				logger.warn("【{}充值】sign验证失败 {}",ch.getPromoID(), this);
			}
		} else {
			logger.warn("【{}充值】失败,{}",ch.getPromoID(), this);
		}
		
		return "ok";
	}

	@Override
	public String parse(String data) throws Exception {
		return null;
	}

	@Override
	public String responseOfRepeatCallback() {
		return "ok";
	}
	
	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String toString() {
		return "QH360PayCallback [app_key=" + app_key + ", product_id="
				+ product_id + ", amount=" + amount + ", app_uid=" + app_uid
				+ ", app_ext1=" + app_ext1 + ", app_ext2=" + app_ext2
				+ ", user_id=" + user_id + ", order_id=" + order_id
				+ ", gateway_flag=" + gateway_flag + ", sign_type=" + sign_type
				+ ", app_order_id=" + app_order_id + ", sign_return="
				+ sign_return + ", sign=" + sign + "]";
	}

//	public static void main(String[] args) {
//		String[] ss={"app_key",
//				"product_id",
//				"amount",
//				"app_uid",
//				"app_ext2",
//				"app_ext1",
//				"user_id",
//				"order_id",
//				"gateway_flag",
//				"sign_type",
//				"app_order_id"};
//		List<String> list = new ArrayList<String>();
//		for (String string : ss) {
//			list.add(string);
//		}
//		Collections.sort(list);
//		for (String string : list) {
//			System.out.println(string);
//		}
//		
//		System.out.println(String.valueOf(Float.parseFloat("199")/100));
//	}
	
}
