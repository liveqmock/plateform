package com.koala.promosupport.souhu;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.StringUtil;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * 1.4  请求数据 
 * rid  接口请求 ID   L ong  Y   Unix  时 间 戳，例：1330395827 
 * appId  对接应用 ID   String  Y   对接前搜狐分配 
 * appKey  对接应用 Key  String  Y   对接前搜狐分配 
 * sessionI d  会话ID   String  Y   对接应用客户端登录后从SDK客户端获得 
 * sign   签名参数  String  Y   计 算 公 式：MD5(appId+ sessionId  + appKey)
 * 
 * 1.6  返回数据 
 * status   返回状态  Int   Y   0:成功  1:appId、appKey、sessionId和sign不能为空 10：内部错误  21:登陆状态已失效  22：签名参数不正确 
 * info   验证结果说明  String  N  仅验证失败时有此数据 
 * uid  用户uid  String  Y    
 * nickname   用户昵称  String  N
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class SouhuUserVerify implements IUserVerify {

	private SouhuChannel ch;

	public SouhuUserVerify(SouhuChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String uid0 = params.get(PROMO_KEY_UID);
		final String sessionId = params.get(PROMO_KEY_SESSIONID);
		if (StringUtil.hasNullOr0LengthString(uid0,sessionId)) {
			// 格式错误
			pverifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			pverifyResp
					.writeUtf8String(KGameTips
							.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
									promoID,
									PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(pverifyResp);
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
//					{ 
//						"rid":1336399826, 
//						"appId":"001",  
//						"appKey":"key001",  
//						"sessionId":"66b327c83c2440bd93c6cbb5ec405fd5",  
//						"sign":"a4f1ca5eca94d3dcf7f3e67b51f25a86" 
//					} 
					//sign计 算 公 式：MD5(appId+ sessionId  + appKey)
					JSONObject jreq = new JSONObject();
					jreq.putOpt("rid", System.currentTimeMillis()/1000);
					jreq.putOpt("appId", ch.getAppId());
					jreq.putOpt("appKey", ch.getAppKey());
					jreq.putOpt("sessionId", sessionId);
					jreq.putOpt("sign", MD5.MD5Encode(ch.getAppId()+sessionId+ch.getAppKey()));
					System.out.println(jreq.toString());
					// 发起HTTP连接
					Map<String,String> header = new HashMap<String,String>();
					header.put("Content-Type", "text/html");
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(
									ch.getUserVerifyUrl(),
									URLEncoder.encode(jreq.toString(), "UTF-8"),
									ch.getUserVerifyHttpMethod(),header);

					respstring = URLDecoder.decode(respstring, "UTF-8");
					System.out.println(respstring);
					// 分析返回结果
					if (respstring == null || respstring.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, 167167));
						playersession.send(pverifyResp);
						return;
					}
//					 * status   返回状态  Int   Y   0:成功  1:appId、appKey、sessionId和sign不能为空 10：内部错误  21:登陆状态已失效  22：签名参数不正确 
//					 * info   验证结果说明  String  N  仅验证失败时有此数据 
//					 * uid  用户uid  String  Y     (在userinfo中)
//					 * nickname   用户昵称  String  N (在userinfo中)
					JSONObject jobj = new JSONObject(respstring);
					int status = jobj.optInt("status");
					if (status != 0) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, "("+status+")"+jobj.optString("info")));
						playersession.send(pverifyResp);
						return;
					}
					String userinfo = jobj.optString("userinfo");
					JSONObject juserinfo = new JSONObject(userinfo);
					String uid = juserinfo.optString("uid");
					String nickname = juserinfo.optString("nickname");
					
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UID, promoMask);
					params.put("nickname", nickname);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, nickname,
							pverifyResp, params, analysisInfoNeedSaveToDB);
				} catch (Exception e) {
					e.printStackTrace();
					// 其它失败情况
					pverifyResp
							.writeInt(KGameProtocol.PL_UNKNOWNEXCEPTION_OR_SERVERBUSY);
					pverifyResp.writeUtf8String(KGameTips
							.get("PL_UNKNOWNEXCEPTION_OR_SERVERBUSY"));
					playersession.send(pverifyResp);
				}
			}
		}).start();
	}

}
