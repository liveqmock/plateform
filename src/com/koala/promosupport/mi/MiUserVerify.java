package com.koala.promosupport.mi;

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
 * 【请求参数】
 * appId 必须 应用ID
 * session 必须 用户sessionID
 * uid 必须 用户ID
 * signature 必须 签名，签名方法见后面说明
 * 
 * 【响应参数】
 * errcode 必须 状态码 200 验证正确、1515 appId错误、 1516 uid错误、1520 session错误、 1525 signature错误
 * errMsg 可选 错误信息描述
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class MiUserVerify implements IUserVerify{

	class Response {
		int errcode;
		String errMsg;
	}

	private MiChannel ch;

	public MiUserVerify(MiChannel ch) {
		this.ch = ch;
	}

	/**
	 * <pre>
	 * 【请求参数】
	 * appId 必须 应用ID
	 * session 必须 用户sessionID
	 * uid 必须 用户ID
	 * signature 必须 签名，签名方法见后面说明
	 * 
	 * 【响应参数】
	 * errcode 必须 状态码 200 验证正确、1515 appId错误、 1516 uid错误、1520 session错误、 1525 signature错误
	 * errMsg 可选 错误信息描述
	 * </pre>
	 */
	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String,String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		final String uid = params.get(PROMO_KEY_MI_UID);
		final String session = params.get(PROMO_KEY_MI_SESSION);
		if (uid == null || uid.length() <= 0 || session == null
				|| session.length() <= 0) {
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
					sb.append("appId=").append(ch.getAppId());
					sb.append("&session=").append(session);
					sb.append("&uid=").append(uid);
					String encryptText = sb.toString();
					sb.append("&signature=").append(
							HmacSHA1Encryption.hmacSHA1Encrypt(encryptText,
									ch.getAppKey()));
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUserVerifyUrl(), sb.toString(),
									ch.getUserVerifyHttpMethod());

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
					JSONObject jobj = new JSONObject(respstring);
					Response re = new Response();
					re.errcode = jobj.optInt("errcode");
					re.errMsg = jobj.optString("errMsg");

					// errcode 必须 状态码 200 验证正确、1515 appId错误、 1516 uid错误、1520
					// session错误、 1525 signature错误
					if (re.errcode != 200) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.errcode));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = uid;
					
					//返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_MI_UID, promoMask);
					
					PromoSupport.getInstance().afterUserVerified(
							playersession, promoID, ch.getPromoID(), promoMask,
							uid, pverifyResp, params, analysisInfoNeedSaveToDB);
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
