package com.koala.promosupport.ng91;

import java.util.Map;

import com.koala.game.logging.KGameLogger;
import com.koala.paymentserver.IPayCallback;
import com.koala.paymentserver.PayExtParam;
import com.koala.paymentserver.PayOrder;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONException;
import com.koala.thirdpart.json.JSONObject;

/**
 * 91的支付回调
 * 
 * <pre>
 * 参数名  说明(没有说明可选的参数，为必传参数) 
 * -----------------------------------
 * AppId  应用ID 
 * Act  1 
 * ProductName  应用名称 
 * ConsumeStreamId  消费流水号 
 * CooOrderSerial  商户订单号 
 * Uin  91账号ID 
 * GoodsId  商品ID 
 * GoodsInfo  商品名称 
 * GoodsCount  商品数量 
 * OriginalMoney  原始总价(格式：0.00) 
 * OrderMoney  实际总价(格式：0.00) 
 * Note  即支付描述（客户端API参数中的payDescription字段） 购买时客户端应用通过API传入，原样返回给应用服务器 开发者可以利用该字段，定义自己的扩展数据。例如区分游戏服务器 
 * PayStatus  支付状态：0=失败，1=成功 
 * CreateTime  创建时间(yyyy-MM-dd HH:mm:ss) 
 * Sign  以上参数的MD5值，其中AppKey为91SNS平台分配的应用密钥 
 * String.Format("{0}{1}{2}{3}{4}{5}{6}{7}{8}{9:0.00}{10:0.00}{11}{12}{13:yyyy-MM-dd HH:mm:ss}{14}", 
 * AppId,Act, ProductName,ConsumeStreamId, CooOrderSerial, Uin, GoodsId, GoodsInfo, GoodsCount, OriginalMoney, OrderMoney, Note, PayStatus, CreateTime, AppKey).HashToMD5Hex()
 * 
 * -------------------------------------
 * 对亍用户完成正常支付后的通知，移动开发平台服务器会间隔时间，向应用服务器发送
 * 支付结果通知，直到应用服务器确认接收成功。 
 * 由亍网络等原因，可能存在同一笔订单，重复通知，业务方服务器若收到重复通知，需
 * 要回复成功（ErrorCode=1），避免下次再次通知。
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class NG91PayCallback implements IPayCallback {
	
	private static final KGameLogger logger = KGameLogger.getLogger(NG91PayCallback.class);
	private NG91Channel ch;
	private PayOrder payOrder;

	//-----------------------------------------------------
	private String AppId                   ;//应用ID 
	private String Act                     ;//1 
	private String ProductName             ;//应用名称 
	private String ConsumeStreamId         ;//消费流水号 
	private String CooOrderSerial          ;//商户订单号 
	private String Uin                     ;//91账号ID 
	private String GoodsId                 ;//商品ID 
	private String GoodsInfo               ;//商品名称 
	private String GoodsCount              ;//商品数量 
	private String OriginalMoney           ;//原始总价(格式：0.00) 
	private String OrderMoney              ;//实际总价(格式：0.00) 
	private String Note                    ;//即支付描述（客户端API参数中的payDescription字段） 购买时客户端应用通过API传入，原样返回给应用服务器 开发者可以利用该字段，定义自己的扩展数据。例如区分游戏服务器 
	private String PayStatus               ;//支付状态：0=失败，1=成功 
	private String CreateTime              ;//创建时间(yyyy-MM-dd HH:mm:ss) 
	private String Sign                    ;//以上参数的MD5值，其中AppKey为91SNS平台分配的应用密钥 
	//-----------------------------------------------------
	
	
	public NG91PayCallback(NG91Channel ch) {
		this.ch = ch;
	}

	@Override
	public String parse(Map<String, String> params) throws Exception {
		// 应用服务端处理
		// 1. 验证Sign是否有效
		// 2. 读取订单数据
		// 3. 返回结果给91移动开发平台服务器
		// 注：验证Sign时，所拼接的字符串，需要用91服务器传回的原样数据，否则Sign会
		// 不一致。OriginalMoney 和OrderMoney拼接时需要保留两位小数，例如15个豆用
		// 15.00
		
		this.AppId            = params.get("AppId");          
		this.Act              = params.get("Act");            
		this.ProductName      = params.get("ProductName");    
		this.ConsumeStreamId  = params.get("ConsumeStreamId");
		this.CooOrderSerial   = params.get("CooOrderSerial"); 
		this.Uin              = params.get("Uin");            
		this.GoodsId          = params.get("GoodsId");        
		this.GoodsInfo        = params.get("GoodsInfo");      
		this.GoodsCount       = params.get("GoodsCount");     
		this.OriginalMoney    = params.get("OriginalMoney");  
		this.OrderMoney       = params.get("OrderMoney");     
		this.Note             = params.get("Note");           
		this.PayStatus        = params.get("PayStatus");      
		this.CreateTime       = params.get("CreateTime");     
		this.Sign             = params.get("Sign");           
		
		logger.info("【{}充值】生成callback {}",ch.getPromoID(), this);
		
		EC ec = new EC("1", "接收成功");

		if (AppId == null || Act == null || ProductName == null
				|| ConsumeStreamId == null || CooOrderSerial == null
				|| Uin == null || GoodsId == null || GoodsInfo == null
				|| GoodsCount == null || OriginalMoney == null
				|| OrderMoney == null || Note == null || PayStatus == null
				|| CreateTime == null) {
			ec.setE("4", "参数无效");
			logger.info("【{}充值】失败。 {}",ch.getPromoID(), ec);
			return ec.toString();
		}
		// 判断AppId有没有错
		if (!String.valueOf(ch.getAppId()).equals(this.AppId)) {
			ec.setE("2", "AppId无效");
			logger.info("【{}充值】失败。 {}",ch.getPromoID(), ec);
			return ec.toString();
		}
		// 判断Act有没有错
		if (!"1".equals(this.Act)) {
			ec.setE("3", "Act无效");
			logger.info("【{}充值】失败。 {}",ch.getPromoID(), ec);
			return ec.toString();
		}
		
		if (!PromoSupport.getInstance().isDebugPayNoSign()) {
			// 验证sign
			// String.Format("{0}{1}{2}{3}{4}{5}{6}{7}{8}{9:0.00}{10:0.00}{11}{12}{13:yyyy-MM-dd HH:mm:ss}{14}",
			// AppId,Act, ProductName,ConsumeStreamId,
			// CooOrderSerial, Uin, GoodsId, GoodsInfo, GoodsCount,
			// OriginalMoney, OrderMoney, Note, PayStatus,
			// CreateTime, AppKey).HashToMD5Hex()
			StringBuilder buf = new StringBuilder();
			buf.append(AppId);
			buf.append(Act);
			buf.append(ProductName);
			buf.append(ConsumeStreamId);
			buf.append(CooOrderSerial);
			buf.append(Uin);
			buf.append(GoodsId);
			buf.append(GoodsInfo);
			buf.append(GoodsCount);
			buf.append(OriginalMoney);
			buf.append(OrderMoney);
			buf.append(Note);
			buf.append(PayStatus);
			buf.append(CreateTime);
			buf.append(ch.getAppKey());

			if (!ch.md5(buf.toString()).toLowerCase()
					.equals(this.Sign.toLowerCase())) {
				ec.setE("5", "Sign无效");
				logger.info("【{}充值】失败。 {}",ch.getPromoID(), ec);
				return ec.toString();
			}
			// sign通过
		}
		
		// 判断支付状态是不是成功
		if (!"1".equals(this.PayStatus)) {// 支付成功
			ec.setE("0", "支付状态失败");
			logger.info("【{}充值】失败。 {}",ch.getPromoID(), ec);
			return ec.toString();
		}

		// 将从渠道支付通知的内容整理成一个订单PayOrder，然后发到GS
		PayExtParam pext = new PayExtParam(this.Note);
		String payWay = "0";// downjoy没有返回payWay就用0代替了
		StringBuilder otherinfo = new StringBuilder()
				.append("ConsumeStreamId=").append(ConsumeStreamId)
				.append("&GoodsId=").append(GoodsId).append("&GoodsInfo=")
				.append(GoodsInfo).append("&GoodsCount").append(GoodsCount);
		payOrder = new PayOrder(pext, CooOrderSerial, String.valueOf(Float
				.parseFloat(OrderMoney) * 100), Uin, payWay, CreateTime,
				otherinfo.toString());
		logger.info("【{}充值】生成订单  {}", ch.getPromoID(), payOrder);



//		ErrorCode  错误码(0=失败，1=成功（已处理过的，也当作成功返回），2= AppId无效，3= Act无效，4=参数无效，5= Sign无效，其他的应用自己定义，幵在错误描述中体现) 
//		ErrorDesc  错误描述 
//		例如 
//		{"ErrorCode":"0","ErrorDesc":"接收失败"} 
//		{"ErrorCode":"1","ErrorDesc":"接收成功"} 
		ec.setE("1", "接收成功");
		logger.info("【{}充值】结果  {}",ch.getPromoID(), ec);
		return ec.toString();
	}

	@Override
	public String parse(String data) throws Exception {
		return null;
	}
	
	@Override
	public String responseOfRepeatCallback() {
		return new EC("1","接收成功").toString();
	}

	@Override
	public PayOrder getGeneratedPayOrder() {
		return payOrder;
	}

	@Override
	public String toString() {
		return "NG91PayCallback [AppId=" + AppId + ", Act=" + Act
				+ ", ProductName=" + ProductName + ", ConsumeStreamId="
				+ ConsumeStreamId + ", CooOrderSerial=" + CooOrderSerial
				+ ", Uin=" + Uin + ", GoodsId=" + GoodsId + ", GoodsInfo="
				+ GoodsInfo + ", GoodsCount=" + GoodsCount + ", OriginalMoney="
				+ OriginalMoney + ", OrderMoney=" + OrderMoney + ", Note="
				+ Note + ", PayStatus=" + PayStatus + ", CreateTime="
				+ CreateTime + ", Sign=" + Sign + "]";
	}



	private class EC {
		String ec;
		String ed;

		EC(String ec, String ed) {
			this.ec = ec;
			this.ed = ed;
		}

		void setE(String ec, String ed) {
			this.ec = ec;
			this.ed = ed;
		}

		@Override
		public String toString() {
			JSONObject j = new JSONObject();
			try {
				j.put("ErrorCode", ec);
				j.put("ErrorDesc", ed);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return j.toString();
		}
	}
	
}
