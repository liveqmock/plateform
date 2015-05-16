package com.koala.promosupport.baoruan;

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
 * 4.4.	服务端充值通知
 * 1）、通知地址：游戏提供notify_url(充值回调地址)
 * 		（游戏客户端拼接上的参数会透传给游戏服务器）
 * 2）、通知方式：POST
 * 3）、参数
 * 参数名	参数类型	必须	参数说明	备注信息
 * cid	int	是	充值合作号	
 * uid	int	是	一号通用户UID	
 * order_id	string	是	宝软订单号	
 * amount	string	是	充值的宝币	1元=1宝币,客户端收到通知自己换算
 * 格式：1.00
 * verifystring	string	是	校验串	md5(cid+uid+order_id+amount+key)
 * 
 * 3、通知说明
 * 1）、由于网络有可能出现不稳定，通知有可能出现多次通知，客户端自己要能控制多次通知不会给用户多次加游戏币。
 * 2）、Amount=0说明没有充值成功，>0 充值成功了。要查看充值是否成功用户要自己查看充值记录。
 * 3）、Verifystring 校验不一致不能更新用户的游戏币。
 * 4）、成功处理完后需要返回 1 ，通知金额是0如果接收成功处理后还是返回1。
 * 5）、cid、key是另外分配的，跟注册登录接口是不一的。
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class BaoruanPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(BaoruanPayCallback.class);
	private BaoruanChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "1";
	private static final String FAILED = "0";
	
	private String cid;
	private String uid;
	private String order_id;
	private String amount;
	private String verifystring;
	private String ext;

	public BaoruanPayCallback(BaoruanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.cid = params.get("cid");
		this.uid = params.get("uid");
		this.order_id = params.get("order_id");
		this.amount = params.get("amount");
		this.verifystring = params.get("verifystring");
		this.ext = params.get("ext");
		
		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);
		
		float f_amount = Float.parseFloat(amount);
		if(f_amount<=0){//amount=0说明没有充值成功，>0 充值成功了
			logger.info("【{}充值】订单状态失败,{}", ch.getPromoID(), amount);
			return SUCCESS;
		}
		
		// 签名 md5(cid+uid+order_id+amount+key)
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			String gen_sign = MD5.MD5Encode(cid+uid+order_id+amount+ch.getKey());
			if(!gen_sign.equalsIgnoreCase(verifystring)){
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}
		
		PayExtParam pext = new PayExtParam(ext);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, order_id, String.valueOf(Float.parseFloat(amount)*100), uid, payWay, paytime, "");

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
		return "BaoruanPayCallback [cid=" + cid + ", uid=" + uid
				+ ", order_id=" + order_id + ", amount=" + amount
				+ ", verifystring=" + verifystring + ", ext=" + ext + "]";
	}

	
}
