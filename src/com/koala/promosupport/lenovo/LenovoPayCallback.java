package com.koala.promosupport.lenovo;

import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.PromoSupport;
import com.koala.promosupport.lenovo.util.CpTransSyncSignValid;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * 联想的支付回调
 * <pre>
 * 【接口约定】 
 * 1、 接口URL。商户可以在自服务界面配置接收同步数据的notifyurl或者在客户端集成的时候通过notifyurl 参数来设置同步的URL。
 * 如果两个都配置了，以客户端的参数notifyurl为准。 
 * 2、 接口均采用http协议，POST方法。 
 * 3、 POST参数为transdata、sign。transdata 为本次支付的具体业务参数，数据格式为json格式；sign为transdata 的签名数据。
 * 具体呈现方式为transdata=xxxx&sign=yyyy，其中yyyy就是对xxxx的签名数据。 
 * 4、 平台发送到商户的数据需要根据应用的支付密钥进行验签。验证通过则给支付平台返回SUCCESS应答，应答不需签名。 
 * 5、 交易结果在同步失败的情况下，会定时重发。但是重发一定次数后，将不再进行重发。
 * 
 * 【示例】-----------------
 * 同步数据具体呈现方式（http包体数据）： 
 * transdata={"exorderno":"1","transid":"2","appid":"3","waresid":31,"feetype":4,"money":5,"co
 * unt":6,"result":0,"transtype":0,"transtime":"2012-12-12 12:11:10","cpprivate":"7"}
 * &sign=d91cbc584316b9d99919921a9
 * 
 * 【同步数据参数详解】 ------------------
 * 
 * exorderno  外部订单号  String  Max(50)   商户订单号 
 * transid  交易流水号  String  Max(32)   计费支付平台的交易流水号 
 * appid  应用编号  String  Max(20)   平台为商户应用分配的唯一编号 
 * waresid  商品编号  integer  Max(8)   平台为应用内需计费商品分配的编号 
 * feetype  计费方式  integer  Max(3)   计费类型： 
 *                                             0 – 开放价格 
 *                                             1 – 免费 
 *                                             2 – 按次 
 *                                             3 – 包自然时长 
 *                                             4 – 包账期 
 *                                             5 – 买断 
 *                                             6 – 包次数 
 *                                             7 – 按时长 
 *                                             8 – 包活跃时长 
 *                                             9 – 批量购买 
 *                                             100 – 按次免费试用 
 *                                             101 – 按时长免费试用 
 * money  交易金额  integer  Max(10)   本次交易的金额，单位：分 
 * count    购买数量  integer  Max(10)   本次购买的商品数量 
 * result  交易结果  integer  Max(2)   交易结果： 
 *                                             0 – 交易成功 
 *                                             1 – 交易失败 
 * transtype  交易类型  integer  Max(2)   交易类型： 
 *                                             0 – 交易 
 *                                             1 – 冲正 
 * transtime  交易时间  String  Max(20)   交易时间格式： yyyy-mm-dd hh24:mi:ss 
 * cpprivate  商户私有信息  String  Max(128)  商户私有信息
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class LenovoPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(LenovoPayCallback.class);
	private static final String SUCCESS = "SUCCESS";
	private static final String FAILURE = "FAILURE";
	// ------------------------------------
	private String exorderno  ;//外部订单号  String  Max(50)   商户订单号 
	private String transid    ;//交易流水号  String  Max(32)   计费支付平台的交易流水号 
	private String appid      ;//应用编号  String  Max(20)   平台为商户应用分配的唯一编号 
	private int    waresid    ;//商品编号  integer  Max(8)   平台为应用内需计费商品分配的编号 
	private int    feetype    ;//计费方式  integer  Max(3)   计费类型：0 – 开放价格 1 – 免费 2 – 按次 3 – 包自然时长 4 – 包账期 5 – 买断 6 – 包次数 7 – 按时长 8 – 包活跃时长 9 – 批量购买 100 – 按次免费试用 101 – 按时长免费试用 
	private int    money      ;//交易金额  integer  Max(10)   本次交易的金额，单位：分 
	private int    count      ;//购买数量  integer  Max(10)   本次购买的商品数量 
	private int    result     ;//交易结果  integer  Max(2)   交易结果： 0 – 交易成功 1 – 交易失败 
	private int    transtype  ;//交易类型  integer  Max(2)   交易类型： 0 – 交易 1 – 冲正 
	private String transtime  ;//交易时间  String  Max(20)   交易时间格式： yyyy-mm-dd hh24:mi:ss 
	private String cpprivate  ;//商户私有信息  String  Max(128)  商户私有信息 
	// ------------------------------------
	private LenovoChannel ch;
	private PayOrder payOrder;

	public LenovoPayCallback(LenovoChannel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {

		// 1/获取信息
		String transdata = params.get("transdata");
		String sign = params.get("sign");

		logger.info("【{}充值】获取到参数内容 transdata={} ,sign={}", ch.getPromoID(),
				transdata, sign);

		// 2/验证数据有效性
		if (transdata == null || "".equalsIgnoreCase(transdata)) {
			logger.info("【{}充值】错误.transdata=null", ch.getPromoID());
			return FAILURE;
		}
		if (sign == null || "".equalsIgnoreCase(sign)) {
			logger.info("【{}充值】错误.sign=null", ch.getPromoID());
			return FAILURE;
		}

		// 3/验签操作
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			if (!CpTransSyncSignValid
					.validSign(transdata, sign, ch.getPayKey())) {
				logger.info("【{}充值】sign错误.", ch.getPromoID());
				return FAILURE;
			}
		}

		// 4/业务处理
		try {
			JSONObject json = new JSONObject(transdata);
			this.exorderno = json.optString("exorderno");//商户订单号                                                                                                                                            
			this.transid = json.optString("transid");    //计费支付平台的交易流水号                                                                                                                                     
			this.appid = json.optString("appid");        //平台为商户应用分配的唯一编号                                                                                                                                    
			this.waresid = json.optInt("waresid");       //平台为应用内需计费商品分配的编号                                                                                                                                  
			this.feetype = json.optInt("feetype");       //计费类型：0 – 开放价格 1 – 免费 2 – 按次 3 – 包自然时长 4 – 包账期 5 – 买断 6 – 包次数 7 – 按时长 8 – 包活跃时长 9 – 批量购买 100 – 按次免费试用 101 – 按时长免费试用                                
			this.money = json.optInt("money");           //本次交易的金额，单位：分                                                                                                                                     
			this.count = json.optInt("count");           //本次购买的商品数量                                                                                                                                        
			this.result = json.optInt("result");         //交易结果： 0 – 交易成功 1 – 交易失败                                                                                                                           
			this.transtype = json.optInt("transtype");   //交易类型： 0 – 交易 1 – 冲正                                                                                                                               
			this.transtime = json.optString("transtime");//交易时间格式： yyyy-mm-dd hh24:mi:ss                                                                                                                     
			this.cpprivate = json.optString("cpprivate");//商户私有信息                                                                                                                                          
			
			logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);
			
		} catch (JSONException je) {
			logger.info("【{}充值】错误.transdata内容错误{}", ch.getPromoID(),transdata);
			return FAILURE;
		}
		
		if (!ch.getAppId().equalsIgnoreCase(this.appid)) {
			logger.info("【{}充值】AppId({})错误.正确是{}", ch.getPromoID(),this.appid,ch.getAppId());
			return FAILURE;
		}
		
		if (this.result != 0) {
			logger.info("【{}充值】交易失败.", ch.getPromoID());
			return FAILURE;
		}
		
		//生成订单
		PayExtParam pext = new PayExtParam(cpprivate);
		String payWay ="0";//支付渠道没有返回payWay就用0代替了
		String otherinfo = (new StringBuilder().append("transid=")
				.append(transid).append("&waresid=").append(waresid)
				.append("&feetype=").append(feetype)
				.append("&count=").append(count)).toString();
		this.payOrder = new PayOrder(
				pext,
				this.exorderno,
				String.valueOf(money),
				"",
				payWay,
				transtime,
				otherinfo);
		
		logger.info("【{}充值】生成订单  {}",ch.getPromoID(), payOrder);

		// 5/成功返回
		return SUCCESS;
	}

	@Override
	public String parse(String content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String responseOfRepeatCallback() {
		return SUCCESS;
	}
	
	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String toString() {
		return "LenovoPayCallback [exorderno=" + exorderno + ", transid="
				+ transid + ", appid=" + appid + ", waresid=" + waresid
				+ ", feetype=" + feetype + ", money=" + money + ", count="
				+ count + ", result=" + result + ", transtype=" + transtype
				+ ", transtime=" + transtime + ", cpprivate=" + cpprivate + "]";
	}

	
}
