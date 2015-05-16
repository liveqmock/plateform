package com.koala.promosupport.zhidiantong;

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
 * 
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class BXPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(BXPayCallback.class);
	private BXChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "success";
	private static final String FAILED = "failure";

	private String cp_id;// 游戏合作商id
	private String game_id;// 游戏id
	private String server_id;// 服务器id
	private String bill_no;// 订单号
	private String price;// 金额.(单位到分.例如5.00是5元)
	private String user_id;// 帐号id
	private String trade_status;// TRADE_SUCCESS(固定)
	private String sign;// 签名(game_id+ server_id+ user_id+ bill_no+price+
						// trade_status)字符串连接起来,注意没有+号之后
	private String cp_bill_no;// 游戏订单号 ()
	private String extra;// 扩展

	public BXPayCallback(BXChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.cp_id = params.get("cp_id");// 游戏合作商id
		this.game_id = params.get("game_id");// 游戏id
		this.server_id = params.get("server_id");// 服务器id
		this.bill_no = params.get("bill_no");// 订单号
		this.price = params.get("price");// 金额.(单位到分.例如5.00是5元)//其实就是元为单位！！！
		this.user_id = params.get("user_id");// 帐号id
		this.trade_status = params.get("trade_status");// TRADE_SUCCESS(固定)
		this.sign = params.get("sign");// 签名(game_id+ server_id+ user_id+
										// bill_no+price+
										// trade_status)字符串连接起来,注意没有+号之后
		this.cp_bill_no = params.get("cp_bill_no");// 游戏订单号 ()
		this.extra = params.get("extra");// 扩展

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		// 签名 
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sb = new StringBuilder();
			sb.append(ch.getCp_id());
			sb.append(game_id).append(server_id).append(user_id).append(bill_no).append(price).append(trade_status);
			sb.append(ch.getCp_key());
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(this.extra);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, bill_no, String.valueOf(Float.parseFloat(price)*100), user_id, payWay,
				paytime, cp_bill_no);

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
		return "BXPayCallback [cp_id=" + cp_id + ", game_id=" + game_id
				+ ", server_id=" + server_id + ", bill_no=" + bill_no
				+ ", price=" + price + ", user_id=" + user_id
				+ ", trade_status=" + trade_status + ", sign=" + sign
				+ ", cp_bill_no=" + cp_bill_no + ", extra=" + extra + "]";
	}

}
