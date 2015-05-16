package com.koala.promosupport.duokoo;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.MD5;
import com.koala.promosupport.PromoSupport;
import com.koala.thirdpart.json.JSONObject;

public class DkUserVerify implements IUserVerify {

	class Response {
		String error_code;
		String error_msg;
	}

	private DkChannel ch;

	public DkUserVerify(DkChannel ch) {
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		final String uid = params.get(PROMO_KEY_DUOKOO_UID);
		final String session = params.get(PROMO_KEY_DUOKOO_SESSIONID);
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
					sb.append("appid=").append(ch.getAppId());
					sb.append("&appkey=").append(ch.getAppKey());
					sb.append("&uid=").append(uid);
					sb.append("&sessionid=").append(session);
					// 所有参数+ App_secret的MD5码（字母小写）appid值+ appkey值+ uid 值+
					// sessionid值+app_secret值(计算时无+号)
					sb.append("&clientsecret=").append(
							MD5.MD5Encode(
									(new StringBuilder()).append(ch.getAppId())
											.append(ch.getAppKey()).append(uid)
											.append(session)
											.append(ch.getAppSecret())
											.toString()).toLowerCase());
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUrlUserVerify(), sb.toString(),
									ch.getMethodUserVerify());

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

					// {"error_code":"100","error_msg":"Invalid parameter"}
					JSONObject jobj = new JSONObject(respstring);
					Response re = new Response();
					re.error_code = jobj.optString("error_code");
					re.error_msg = jobj.optString("error_msg");

					// 根据error_code值来判断，若为0，则有效
					if (!"0".equals(re.error_code)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID,
										re.error_code));
						playersession.send(pverifyResp);
						return;
					}
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_DUOKOO_UID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, uid,
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
