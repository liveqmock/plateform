package com.koala.promosupport.souhu;

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
 * 4.3.2  支付结果通知 
 *   本接口用于交易完成之后，由支付平台服务端向  CP  服务端主动发起交易结果通知。本
 * 接口使用要求如下： 
 * 1. 如果 CP  服务端需要实时获取交易结果，则 CP  需要在商品配置的信息同步地址中填
 * 写接收同步数据的 URL 。 
 * 2. 交易结果在同步失败的情况下，会定时重发。但是重发一定次数后，将不再进行重发。 
 * 3. 本接口由支付平台服务端向 CP  服务端发起请求。支付平台会对同步的交易结果数据
 * 以商户密钥进行签名，商户在接收到同步数据之后需要对签名进行验证，验证通过，给支付
 * 平台服务端返回  SUCCESS  应答，应答不需签名。 
 * 4. 支付平台服务端以 POST方式向 CP服务端发送请求。
 *    
 *    
 * 以下为交易结果同步接口的请求参数列表： 
 * 参数名称  参数含义  数据类型  参数长度  参数说明 
 * appid  应用id   String  50  应用在支付平台的编号 
 * param  用户自定义参数 String  200  用户自定义参数 
 * payMoney   支付金额  String  10  订单的支付金额(单位为 分) 
 * orderno  订单号  String  25  商户的订单号(必须保证唯一) 
 * result  支付结果  String  3  订单的支付结果(-1 支付失败 1 支付成功) 
 * verifyType   签名类型  String  3  签名类型 1 为MD5 
 * sign  签名 String 20  数字签名(签名规则见附录二)
 * 
 * 
 * 附录二  签名生成规则 
 * Sign的的生成规则如下： 
 * 将参数拼接成如下的字符串appid=1211090012&param=自定义参数&payMoney=10 
 * &orderno=10001&result=1&verifyType=1&key=ad32hsdddf33sss3322  
 * 其中key所对应的值为该应用的AppSecret。 
 * 用MD5 生成 sign 与传入的 sign 做验证。
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class SouhuPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(SouhuPayCallback.class);
	private SouhuChannel ch;
	private PayOrder payOrder;
	private static final String SUCCESS = "SUCCESS";
	private static final String FAILED = "failure";

	private String appid      ;//应用id   String  50  应用在支付平台的编号 
	private String param      ;//用户自定义参数 String 200  用户自定义参数 
	private String payMoney   ;//支付金额  String  10  订单的支付金额(单位为 分) 
	private String orderno    ;//订单号  String  25  商户的订单号(必须保证唯一) 
	private String result     ;//支付结果  String  3  订单的支付结果(-1 支付失败 1 支付成功) 
	private String verifyType ;//签名类型  String  3  签名类型 1 为MD5 
	private String sign       ;//签名 String 20  数字签名(签名规则见附录二)     
	private String uid;

	public SouhuPayCallback(SouhuChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		this.appid      = params.get("appid");//应用id   String  50  应用在支付平台的编号                     
		String ext      = params.get("param");//用户自定义参数 String 200  用户自定义参数                                           
		this.payMoney   = params.get("payMoney");//支付金额  String  10  订单的支付金额(单位为 分)               
		this.orderno    = params.get("orderno");//订单号  String  25  商户的订单号(必须保证唯一)                
		this.result     = params.get("result");//支付结果  String  3  订单的支付结果(-1 支付失败 1 支付成功)   
		this.verifyType = params.get("verifyType");//签名类型  String  3  签名类型 1 为MD5                         
		this.sign       = params.get("sign");//签名 String 20  数字签名(签名规则见附录二)     
		//this.uid = params.get("uid");
		//因为搜狐没有传uid过来，只能在自定义参数里面包含uid，所以这里要把uid拆出来
		this.uid = ext.substring(ext.indexOf('&')+"&uid=".length());
		this.param = ext.substring(0, ext.indexOf('&'));

		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		if(!"1".equalsIgnoreCase(result)){
			logger.info("【{}充值】SDK服务器返回失败状态.{}", ch.getPromoID(), result);
			return FAILED;
		}
		
		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			//appid=1211090012&param=自定义参数&payMoney=10&orderno=10001&result=1&verifyType=1&key=ad32hsdddf33sss3322
			StringBuilder sb = new StringBuilder();
			sb.append("appid=").append(appid);
			sb.append("&param=").append(ext);
			sb.append("&payMoney=").append(payMoney);
			sb.append("&orderno=").append(orderno);
			sb.append("&result=").append(result);
			sb.append("&verifyType=").append(verifyType);
			sb.append("&key=").append(ch.getAppSecret());
			
			String gen_sign = MD5.MD5Encode(sb.toString());
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return FAILED;// ?
			}
		}

		PayExtParam pext = new PayExtParam(param);
		String payWay = "0";// 支付渠道没有返回payWay就用0代替了
		String paytime = PayOrder.FORMAT_PAYTIME.format(new Date());
		this.payOrder = new PayOrder(pext, orderno, payMoney, uid, payWay,
				paytime, "");//XXX 这里没有uid之类返回，要修改自定义参数ext=xxx&uid=xx

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
		return "SouhuPayCallback [appid=" + appid + ", param=" + param
				+ ", payMoney=" + payMoney + ", orderno=" + orderno
				+ ", result=" + result + ", verifyType=" + verifyType
				+ ", sign=" + sign + ", uid=" + uid + "]";
	}

}
