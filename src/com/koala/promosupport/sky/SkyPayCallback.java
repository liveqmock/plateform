package com.koala.promosupport.sky;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.QueryStringDecoder;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * 斯凯支付回调
 * 
 * <pre>
 * 参数名称  参数含义  类型  长度  是否必填  说明 
 * orderId  订单号  String  30  是 与请求的订单号一致，CP平台需保存 
 * cardType  付费方式  String  10  是  0：短信付费 
 * skyId  用户id  Long    是  用户id，与请求一致 
 * resultCode  支付状态  Int  1  是  支付状态,0 成功，1失败 
 * payNum  斯凯支付流水号  String  50  是  CP平台需保存 
 * realAmount  实际支付金额  Int    是 整型数字以人民币分为单位。比方10 元，实际支付金额为1000  
 * payTime  支付时间  String 固定14 是 resultCode=0 时必填，格式： YYYYMMDDHHMMSS 
 * failure  错误代码  String  10  是  成功为0，失败为其它值 
 * failDesc  失败原因  String  256  否  失败原因说明   
 * ext1  扩展字段      否  与提交相同 
 * ext2  扩展字段      否  与提交相同 
 * ext3  扩展字段      否  与提交相同 
 * signMsg  签名字符串  String  32  是 Md5(URL参数&key=xxx),其中KEY由斯凯平台分配, 具体说明在表格下方。
 * -------------------------------------------------
 * CP收到订单号支付结果的处理原则是：先判断该订单是否有处理成功过的记录，
 * 如果记录存在，则不作处理，返回result=0，表明已经处理无需重发。如果不存在，
 * 则给相应用户加上游戏币，然后返回result=0。如果期间因异常，无法保存记录，
 * 则返回result=1.
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class SkyPayCallback implements IPayCallback {

	//////////////////////////////////////////////////
	private String orderId     ;//订单号  String  30  是 与请求的订单号一致，CP平台需保存 
	private String cardType    ;//付费方式  String  10  是  0：短信付费 
	private String skyId       ;//用户id  Long    是  用户id，与请求一致 
	private String resultCode  ;//支付状态  Int  1  是  支付状态,0 成功，1失败 
	private String payNum      ;//斯凯支付流水号  String  50  是  CP平台需保存 
	private String realAmount  ;//实际支付金额  Int    是 整型数字以人民币分为单位。比方10 元，实际支付金额为1000  
	private String payTime     ;//支付时间  String 固定14 是 resultCode=0 时必填，格式： YYYYMMDDHHMMSS 
	private String failure     ;//错误代码  String  10  是  成功为0，失败为其它值 
	private String failDesc    ;//失败原因  String  256  否  失败原因说明   
	private String ext1        ;//扩展字段      否  与提交相同 
	private String ext2        ;//扩展字段      否  与提交相同 
	private String ext3        ;//扩展字段      否  与提交相同 
	private String signMsg     ;//签名字符串  String  32  是 Md5(URL参数&key=xxx),其中KEY由斯凯平台分配, 具体说明在表格下方。
	//////////////////////////////////////////////////
	private static final KGameLogger logger = KGameLogger
			.getLogger(SkyPayCallback.class);
	private SkyChannel ch;
	private PayOrder payOrder;
	
	public SkyPayCallback(SkyChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		return "";
	}

	@Override
	public String parse(String content) throws Exception {
		
		if(content==null||content.length()==0){
			return "result=1";
		}
		
		Map<String, String> params = new HashMap<String, String>();
		QueryStringDecoder decoderQuery = new QueryStringDecoder(
				content, false);
		Map<String, List<String>> uriAttributes = decoderQuery
				.getParameters();
		for (String key : uriAttributes.keySet()) {
			for (String valuen : uriAttributes.get(key)) {
				params.put(key, valuen);
				logger.debug("[Param] {} = {}", key, valuen);
			}
		}
		
		this.orderId   = params.get("orderId")   ;//订单号  String  30  是 与请求的订单号一致，CP平台需保存                                    
		this.cardType  = params.get("cardType")  ;//付费方式  String  10  是  0：短信付费                                                      
		this.skyId     = params.get("skyId")     ;//用户id  Long    是  用户id，与请求一致                                                     
		this.resultCode= params.get("resultCode");//支付状态  Int  1  是  支付状态,0 成功，1失败                                               
		this.payNum    = params.get("payNum")    ;//斯凯支付流水号  String  50  是  CP平台需保存                                               
		this.realAmount= params.get("realAmount");//实际支付金额  Int    是 整型数字以人民币分为单位。比方10 元，实际支付金额为1000            
		this.payTime   = params.get("payTime")   ;//支付时间  String 固定14 是 resultCode=0 时必填，格式： YYYYMMDDHHMMSS                      
		this.failure   = params.get("failure")   ;//错误代码  String  10  是  成功为0，失败为其它值                                            
		this.failDesc  = params.get("failDesc")  ;//失败原因  String  256  否  失败原因说明                                                    
		this.ext1      = params.get("ext1")      ;//扩展字段      否  与提交相同                                                               
		this.ext2      = params.get("ext2")      ;//扩展字段      否  与提交相同                                                               
		this.ext3      = params.get("ext3")      ;//扩展字段      否  与提交相同                                                               
		this.signMsg   = params.get("signMsg")   ;//签名字符串  String  32  是 Md5(URL参数&key=xxx),其中KEY由斯凯平台分配, 具体说明在表格下方。
		
		logger.info("【{}充值】生成callback {}",ch.getPromoID(), this);
		
		if(!"0".equals(this.resultCode)){
			logger.info("【{}充值】失败：SDK方返回状态是失败的. {}",ch.getPromoID(), this.failDesc);
			return "result=1";//sky方失败
		}
		
		if (!PromoSupport.getInstance().isDebugPayNoSign()){
			//MD5签名验证
			StringBuilder buf = new StringBuilder();
			buf.append(content.substring(0,content.lastIndexOf("&signMsg=")));
			buf.append("&key=").append(ch.getAppKey());
			String signGen = MD5.MD5Encode(buf.toString());
			if(!signGen.equalsIgnoreCase(this.signMsg)){
				logger.info("【{}充值】失败：签名验证 Src【{}】 Gen【{}】", ch.getPromoID(),
						buf.toString(), signGen);
				return "result=1";//验证失败
			}
		}
		
		// 将从渠道支付通知的内容整理成一个订单PayOrder，然后发到GS
		PayExtParam pext = new PayExtParam(ext1);
		String payWay = this.cardType;// downjoy没有返回payWay就用0代替了
		payOrder = new PayOrder(pext, orderId, String.valueOf(Integer
				.parseInt(realAmount)), skyId, payWay,
				PayOrder.convert2PaytimeFormat(payTime, "yyyyMMddHHmmss"),
				"payNum=" + payNum);
		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);
		
		return "result=0";
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String responseOfRepeatCallback() {
		return "result=0";
	}

	@Override
	public String toString() {
		return "SkyPayCallback [orderId=" + orderId + ", cardType=" + cardType
				+ ", skyId=" + skyId + ", resultCode=" + resultCode
				+ ", payNum=" + payNum + ", realAmount=" + realAmount
				+ ", payTime=" + payTime + ", failure=" + failure
				+ ", failDesc=" + failDesc + ", ext1=" + ext1 + ", ext2="
				+ ext2 + ", ext3=" + ext3 + ", signMsg=" + signMsg + "]";
	}

}
