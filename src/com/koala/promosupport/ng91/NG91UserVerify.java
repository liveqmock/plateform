package com.koala.promosupport.ng91;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

/**
 * <pre>
 * 功能描述  检查用户登录SessionId是否有效 
 * 【应用服务器传给91服务器参数】 :
 * 	参数名  说明(没有说明可选的参数，为必传参数) 
 * 	AppId  应用ID 
 * 	Act  4 
 * 	Uin  用户的91Uin 
 * 	SessionId  用户登录SessionId 
 * 	Sign  参数值不AppKey的MD5值 
 * 	String.Format("{0}{1}{2}{3}{4}", AppId, Act, Uin,SessionId ,AppKey).HashToMD5Hex();
 * 	例如：AppId=100010&Act=4&Uin=11323356&Sign=76bd057af04298da8379359810b8c9f6&SessionID=5e9c3844563640daa9b9d4846031bbd4 
 * 【91服务器返回应用服务器参数 】
 * 	ErrorCode  错误码(0=失败，1=成功(SessionId 有效)，2= AppId无效，3= Act无效，4=参数无效，5= Sign无效，11=SessionId无效) 
 * 	ErrorDesc  错误描述 
 * 	例如：{"ErrorCode":"1","ErrorDesc":"有效"}
 * </pre>
 * 
 * @param uin
 * @param sessionId
 * @return
 */
public class NG91UserVerify implements IUserVerify {

	class Response {
		int errorCode;
		String errorDesc;
	}

	private NG91Channel ch;

	public	NG91UserVerify(NG91Channel ch) {
		this.ch = ch;
	}

	/**
	 * <pre>
	 * 功能描述  检查用户登录SessionId是否有效 
	 * 【应用服务器传给91服务器参数】 :
	 * 	参数名  说明(没有说明可选的参数，为必传参数) 
	 * 	AppId  应用ID 
	 * 	Act  4 
	 * 	Uin  用户的91Uin 
	 * 	SessionId  用户登录SessionId 
	 * 	Sign  参数值不AppKey的MD5值 
	 * 	String.Format("{0}{1}{2}{3}{4}", AppId, Act, Uin,SessionId ,AppKey).HashToMD5Hex();
	 * 	例如：AppId=100010&Act=4&Uin=11323356&Sign=76bd057af04298da8379359810b8c9f6&SessionID=5e9c3844563640daa9b9d4846031bbd4 
	 * 【91服务器返回应用服务器参数 】
	 * 	ErrorCode  错误码(0=失败，1=成功(SessionId 有效)，2= AppId无效，3= Act无效，4=参数无效，5= Sign无效，11=SessionId无效) 
	 * 	ErrorDesc  错误描述 
	 * 	例如：{"ErrorCode":"1","ErrorDesc":"有效"}
	 * </pre>
	 */
	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String uin = params.get(PROMO_KEY_91_UIN);
		final String sessionId = params.get(PROMO_KEY_91_SESSIONID);
		if (uin == null || uin.length() <= 0 || sessionId == null
				|| sessionId.length() <= 0) {
			// 格式错误
			pverifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			pverifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
					promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(pverifyResp);
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append("AppId=").append(ch.getAppId());
					sb.append("&");
					sb.append("Act=").append(NG91Channel.ACT_USERVERIFY);
					sb.append("&");
					sb.append("Uin=").append(uin);
					sb.append("&");
					sb.append("SessionID=").append(sessionId);
					sb.append("&");
					StringBuilder sign = new StringBuilder();
					sign.append(ch.getAppId())
							.append(NG91Channel.ACT_USERVERIFY).append(uin)
							.append(sessionId).append(ch.getAppKey());
					sb.append("Sign=").append(ch.md5(sign.toString()));

					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getSdkServer(), sb.toString(),
									ch.getHttpMethod());

					// 分析返回结果
					if (respstring == null || respstring.length() <= 0) {
						// 渠道服务器无返回
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										167167));
						playersession.send(pverifyResp);
						return;
					}

					JSONObject obj = new JSONObject(respstring);
					Response re = new Response();
					re.errorCode = obj.optInt("ErrorCode");
					re.errorDesc = obj.optString("ErrorDesc");

					// 通过返回值中调用{@link DownjoyUserVerify#getError_code()}==0
					// 判断是否成功。
					if (re.errorCode != 1) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.errorCode));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = uin;
					
					//返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_UC_UCID, promoMask);
					
					PromoSupport.getInstance().afterUserVerified(
							playersession, promoID, ch.getPromoID(), promoMask,
							uin, pverifyResp, params, analysisInfoNeedSaveToDB);
				} catch (Exception e) {
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
