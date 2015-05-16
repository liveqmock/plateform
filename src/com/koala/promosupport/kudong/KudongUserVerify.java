package com.koala.promosupport.kudong;

import java.util.HashMap;
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
 * 请求参数 无
 * 服务器返回数据
 * 已经登陆返回：
 * {"head": null,"body": null,"res_info": {"response_code": "000","response_msg": "该用户已经登陆！"}}
 * 尚未登陆返回：
 * {"head": null,"body": null,"res_info": {"response_code": "303","response_msg": "该用户不存在或者没有登陆！"}}
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class KudongUserVerify implements IUserVerify {

	private KudongChannel ch;

	public KudongUserVerify(KudongChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		// mid/token
		final String token = params.get(PROMO_KEY_TOKEN);
		final String mid = params.get("mid");
		if (token == null || token.length() <= 0) {
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
					// 不携带参数，直接用Header传入token
					// StringBuilder sb = new StringBuilder();
					Map<String, String> headers = new HashMap<String, String>();
					if (token != null) {
						headers.put("Cookie", "JSESSIONID=" + token);
					}

					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUserVerifyUrl(), "",
									ch.getUserVerifyHttpMethod(), headers);

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
					// 已经登陆返回：
					// {"head": null,"body": null,"res_info": {"response_code":
					// "000","response_msg": "该用户已经登陆！"}}
					// 尚未登陆返回：
					// {"head": null,"body": null,"res_info": {"response_code":
					// "303","response_msg": "该用户不存在或者没有登陆！"}}
					JSONObject jobj = new JSONObject(respstring);
					String head = jobj.optString("head");
					String body = jobj.optString("body");
					JSONObject res_info = jobj.optJSONObject("res_info");
					String response_code = res_info.optString("response_code");
					String response_msg = res_info.optString("response_msg");

					if ("303".equals(response_code)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, respstring));
						playersession.send(pverifyResp);
						return;
					}

					String promoMask = mid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_DOWNJOY_MID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, mid,
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
