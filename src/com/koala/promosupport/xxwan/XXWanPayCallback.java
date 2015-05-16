package com.koala.promosupport.xxwan;

import java.net.URLDecoder;
import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class XXWanPayCallback implements IPayCallback {

	private static final KGameLogger logger = KGameLogger
			.getLogger(XXWanPayCallback.class);
	private XXWanChannel ch;
	private PayOrder payOrder;
	
	public static final String SUCCESS   = "success";//接收成功
	public static final String ERRORSIGN = "errorSign";//签名错误
	public static final String ERROR     = "error";//未知错误
	
	public static final int CLIENT_TYPE_ANDROID = 0;
	public static final int CLIENT_TYPE_IOS = 1;
	
	public static final String ENC = "UTF-8";

	private String userId       ;//a String 35 唯一标识，对应平台 id 1
	private String userAccount  ;//b String 35 对应平台帐号 2
	private String serverId     ;//c String 30 充值服务器，cp 充值时传入的值 3
	private String roleId       ;//d String 30 游戏角色 ID，cp 充值时传入的值 4
	private String roleName     ;//e String 30 游戏角色名称，cp 充值时传入的值 5
	private String orderId      ;//f String 20 梦想手游订单号 6
	private String orderStatus  ;//g String 4 订单状态，1-成功；其他为失败 7
	private String platformId   ;//h String 30 支付平台，参考：平台类型定义 8
	private String amount       ;//i String 10 成功充值金额，单位(分) 9
	private String remark       ;//j String 255 结果说明，描述订单状态 10
	private String callBackInfo ;//k String 255 合作商自定义参数，cp 充值时传入的值 11
	private String payTime      ;//l String 20 玩家充值时间，yyyyMMddHHmmss 12
	private String paySUTime    ;//m String 20 玩家充值成功时间，yyyyMMddHHmmss 13
	private String callBackUrl  ;//n String 100 回调 url，cp 充值时传入的值 14
	//private String appkey       ;//  String 32 由梦想手游提供，不通过请求传输 15
	private String sign         ;//o String 32 参数签名（用于验签对比） 不参与签名
	private int clientType      ;//p int 4 客户端类型 0-android 1-ios 不参与签名

	public XXWanPayCallback(XXWanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
//		this.userId       = URLDecoder.decode(params.get("userId"      ),ENC);//a String 35 唯一标识，对应平台 id 1                       
//		this.userAccount  = URLDecoder.decode(params.get("userAccount" ),ENC);//b String 35 对应平台帐号 2                                
//		this.serverId     = URLDecoder.decode(params.get("serverId"    ),ENC);//c String 30 充值服务器，cp 充值时传入的值 3               
//		this.roleId       = URLDecoder.decode(params.get("roleId"      ),ENC);//d String 30 游戏角色 ID，cp 充值时传入的值 4              
//		this.roleName     = URLDecoder.decode(params.get("roleName"    ),ENC);//e String 30 游戏角色名称，cp 充值时传入的值 5             
//		this.orderId      = URLDecoder.decode(params.get("orderId"     ),ENC);//f String 20 梦想手游订单号 6                              
//		this.orderStatus  = URLDecoder.decode(params.get("orderStatus" ),ENC);//g String 4 订单状态，1-成功；其他为失败 7                 
//		this.platformId   = URLDecoder.decode(params.get("platformId"  ),ENC);//h String 30 支付平台，参考：平台类型定义 8                
//		this.amount       = URLDecoder.decode(params.get("amount"      ),ENC);//i String 10 成功充值金额，单位(分) 9                      
//		this.remark       = URLDecoder.decode(params.get("remark"      ),ENC);//j String 255 结果说明，描述订单状态 10                    
//		this.callBackInfo = URLDecoder.decode(params.get("callBackInfo"),ENC);//k String 255 合作商自定义参数，cp 充值时传入的值 11       
//		this.payTime      = URLDecoder.decode(params.get("payTime"     ),ENC);//l String 20 玩家充值时间，yyyyMMddHHmmss 12               
//		this.paySUTime    = URLDecoder.decode(params.get("paySUTime"   ),ENC);//m String 20 玩家充值成功时间，yyyyMMddHHmmss 13           
//		this.callBackUrl  = URLDecoder.decode(params.get("callBackUrl" ),ENC);//n String 100 回调 url，cp 充值时传入的值 14               
//		//this.appkey     = URLDecoder.decode(params.get("appkey"      ),ENC);//  String 32 由梦想手游提供，不通过请求传输 15             
//		this.sign         = URLDecoder.decode(params.get("sign"        ),ENC);//o String 32 参数签名（用于验签对比） 不参与签名            
//		this.clientType   = Integer.parseInt(URLDecoder.decode(params.get("clientType"),ENC));

		// 2014-10-09 修改，xxwan采用post的方式，参数名字是a、b、c、d这种形式的
		this.userId       = URLDecoder.decode(params.get("a"),ENC);//a String 35 唯一标识，对应平台 id 1                       
		this.userAccount  = URLDecoder.decode(params.get("b"),ENC);//b String 35 对应平台帐号 2                                
		this.serverId     = URLDecoder.decode(params.get("c"),ENC);//c String 30 充值服务器，cp 充值时传入的值 3               
		this.roleId       = URLDecoder.decode(params.get("d"),ENC);//d String 30 游戏角色 ID，cp 充值时传入的值 4              
		this.roleName     = URLDecoder.decode(params.get("e"),ENC);//e String 30 游戏角色名称，cp 充值时传入的值 5             
		this.orderId      = URLDecoder.decode(params.get("f"),ENC);//f String 20 梦想手游订单号 6                              
		this.orderStatus  = URLDecoder.decode(params.get("g"),ENC);//g String 4 订单状态，1-成功；其他为失败 7                 
		this.platformId   = URLDecoder.decode(params.get("h"),ENC);//h String 30 支付平台，参考：平台类型定义 8                
		this.amount       = URLDecoder.decode(params.get("i"),ENC);//i String 10 成功充值金额，单位(分) 9                      
		this.remark       = URLDecoder.decode(params.get("j"),ENC);//j String 255 结果说明，描述订单状态 10                    
		this.callBackInfo = URLDecoder.decode(params.get("k"),ENC);//k String 255 合作商自定义参数，cp 充值时传入的值 11       
		this.payTime      = URLDecoder.decode(params.get("l"),ENC);//l String 20 玩家充值时间，yyyyMMddHHmmss 12               
		this.paySUTime    = URLDecoder.decode(params.get("m"),ENC);//m String 20 玩家充值成功时间，yyyyMMddHHmmss 13           
		this.callBackUrl  = URLDecoder.decode(params.get("n"),ENC);//n String 100 回调 url，cp 充值时传入的值 14               
		this.sign         = URLDecoder.decode(params.get("o"),ENC);//o String 32 参数签名（用于验签对比） 不参与签名            
		this.clientType   = Integer.parseInt(URLDecoder.decode(params.get("p"),ENC));// p String 客户端类型，不参与签名
		
		logger.info("【{}充值】生成callback {}", ch.getPromoID(), this);

		if(!"1".equals(orderStatus)){
			logger.info("【{}充值】SDK服务器返回失败状态.{}", ch.getPromoID(),orderStatus);
			return ERROR;
		}
		
		// 签名
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			StringBuilder sbs = new StringBuilder();
			sbs.append("a=").append(this.userId       );//a String 35 唯一标识，对应平台 id 1                
			sbs.append("&b=").append(this.userAccount  );//b String 35 对应平台帐号 2                         
			sbs.append("&c=").append(this.serverId     );//c String 30 充值服务器，cp 充值时传入的值 3        
			sbs.append("&d=").append(this.roleId       );//d String 30 游戏角色 ID，cp 充值时传入的值 4       
			sbs.append("&e=").append(this.roleName     );//e String 30 游戏角色名称，cp 充值时传入的值 5      
			sbs.append("&f=").append(this.orderId      );//f String 20 梦想手游订单号 6                       
			sbs.append("&g=").append(this.orderStatus  );//g String 4 订单状态，1-成功；其他为失败 7          
			sbs.append("&h=").append(this.platformId   );//h String 30 支付平台，参考：平台类型定义 8         
			sbs.append("&i=").append(this.amount       );//i String 10 成功充值金额，单位(分) 9               
			sbs.append("&j=").append(this.remark       );//j String 255 结果说明，描述订单状态 10             
			sbs.append("&k=").append(this.callBackInfo );//k String 255 合作商自定义参数，cp 充值时传入的值 11
			sbs.append("&l=").append(this.payTime      );//l String 20 玩家充值时间，yyyyMMddHHmmss 12        
			sbs.append("&m=").append(this.paySUTime    );//m String 20 玩家充值成功时间，yyyyMMddHHmmss 13    
			sbs.append("&n=").append(this.callBackUrl  );//n String 100 回调 url，cp 充值时传入的值 14        
			sbs.append("&appkey=").append(ch.getAppkey()    );//  String 32 由梦想手游提供，不通过请求传输 15      
			
			logger.info(sbs.toString());
			String gen_sign = MD5.MD5Encode(sbs.toString(), "UTF-8").toLowerCase(); // 2014-10-09 添加UTF-8的编码类型，原来是采用默认的编码的，但是xxwan是用UTF-8编码
			if (!gen_sign.equalsIgnoreCase(this.sign)) {
				logger.info("【{}充值】签名不一致.", ch.getPromoID());
				return ERRORSIGN;
			}
		}

		PayExtParam pext = new PayExtParam(callBackInfo);
		String paytime = PayOrder.convert2PaytimeFormat(payTime, "yyyyMMddHHmmss");
		StringBuilder otherinfo = new StringBuilder();
		otherinfo.append("userAccount=").append(userAccount);
		otherinfo.append("&serverId=").append(serverId);
		otherinfo.append("&roleId=").append(roleId);
		otherinfo.append("&roleName=").append(roleName);
		otherinfo.append("&platformId=").append(platformId);
		otherinfo.append("&remark=").append(remark);
		otherinfo.append("&paySUTime=").append(paySUTime);
		otherinfo.append("&callBackUrl=").append(callBackUrl);
		otherinfo.append("&clientType").append(clientType);
		this.payOrder = new PayOrder(pext,orderId , amount, userId, platformId,
				paytime,otherinfo.toString());
		this.payOrder.setPayCurrencyCode(ch.getPayCurrencyCode());

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
		return "XXWanPayCallback [userId=" + userId + ", userAccount="
				+ userAccount + ", serverId=" + serverId + ", roleId=" + roleId
				+ ", roleName=" + roleName + ", orderId=" + orderId
				+ ", orderStatus=" + orderStatus + ", platformId=" + platformId
				+ ", amount=" + amount + ", remark=" + remark
				+ ", callBackInfo=" + callBackInfo + ", payTime=" + payTime
				+ ", paySUTime=" + paySUTime + ", callBackUrl=" + callBackUrl
				+ ", sign=" + sign + ", clientType=" + clientType + "]";
	}


}
