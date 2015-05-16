package com.koala.promosupport.downjoy;

import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * 当乐渠道支付/充值通知
 * 
 * <pre>
 * ·流程
 * ==========================================
 * 1.用户打开支付界面，提交订单
 * 2.支付完成，回到游戏
 * 3.计费结果通知
 * 4.response写回‘success’
 * 5.为用户发货
 * 6.若无法发货，调用退款接口
 * 8.退款成功
 * 7.退款到用户账号
 * 
 * ·SDK服务器通知CP支付服务器的http请求内容：
 * ===========================================
 * 参数名            说明 
 * result  支付结果，固定值。“1”代表成功，“0”代表失败 
 * money   支付金额，单位：元。 
 * order   本次支付的订单号。 
 * mid     本次支付用户的乐号，既登录后返回的mid参数。 
 * time    时间戳，格式：yyyymmddHH24mmss月日小时分秒小于10前面补充0 
 * signature MD5验证串，用于与接口生成的验证串做比较，保证计费通知的合法性。order=_&money=_&mid=_&time=_&result=_&ext=_&key=_  
 * ext     发起支付请求时传递的eif参数，此处原样返回。
 * =============================================
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class DownjoyPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(DownjoyPayCallback.class);

	// ////////////////////////////////////////////////////////
	public String result;// 支付结果，固定值。“1”代表成功，“0”代表失败
	public String money;// 支付金额，单位：元。
	public String order;// 本次支付的订单号。
	public String mid;// 本次支付用户的乐号，既登录后返回的mid参数。
	public String time;// 时间戳，格式：yyyymmddHH24mmss月日小时分秒小于10前面补充0
	public String signature;// MD5验证串，用于与接口生成的验证串做比较，保证计费通知的合法性。order=_&money=_&mid=_&time=_&result=_&ext=_&key=_
	public String ext;// 发起支付请求时传递的eif参数，此处原样返回。

	// ////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return "DownjoyPayCallback [result=" + result + ", money=" + money
				+ ", order=" + order + ", mid=" + mid + ", time=" + time
				+ ", signature=" + signature + ", ext=" + ext + "]";
	}

	private DownjoyChannel ch;
	private PayOrder payOrder;

	public DownjoyPayCallback(DownjoyChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.result = params.get("result");
		this.money = params.get("money");
		this.order = params.get("order");
		this.mid = params.get("mid");
		this.time = params.get("time");
		this.signature = params.get("signature");
		this.ext = params.get("ext");
		
		logger.info("【{}充值】生成callback {}",ch.getPromoID(), this);

		// 验证通过
		if ("1".equals(this.result)) {// 支付成功

			StringBuilder sb = new StringBuilder();
			sb.append("order=").append(this.order);
			sb.append("&money=").append(this.money);
			sb.append("&mid=").append(this.mid);
			sb.append("&time=").append(this.time);
			sb.append("&result=").append(this.result);
			sb.append("&ext=").append(this.ext);
			sb.append("&key=").append(ch.getPaymentKey());

			String sig = MD5.MD5Encode(sb.toString()).toLowerCase();
			if (PromoSupport.getInstance().isDebugPayNoSign()||(this.signature != null && sig.equalsIgnoreCase(this.signature))) {

				// 将从渠道支付通知的内容整理成一个订单PayOrder，然后发到GS
				PayExtParam pext = new PayExtParam(ext);
				String payWay = "0";// downjoy没有返回payWay就用0代替了
				payOrder = new PayOrder(pext, order, String.valueOf(Float
						.parseFloat(money) * 100), mid, payWay,
						PayOrder.convert2PaytimeFormat(time, "yyyyMMddHHmmss"),
						"");
				logger.info("【{}充值】生成订单  {}",ch.getPromoID(), payOrder);

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
		} else {// 支付失败
			logger.warn("【{}充值】失败,{}",ch.getPromoID(), this);
		}
		// http请求的response中回写‘success’，表明通知接收成功。
		// 如果计费通知接口没有回写success，当乐的通知发送程序会认为通知发送失败，在一
		// 定的间隔时间后重发通知。每笔订单的重复通知次数为10次，时间间隔为15秒。
		// 注：此处的success表示通知接收成功，与本次支付的结果无关。
		return "success";
	}

	@Override
	public String parse(String data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String responseOfRepeatCallback() {
		return "success";
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

}
