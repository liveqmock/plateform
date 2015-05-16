package com.koala.promosupport.kunlun;

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
 * 检验结果:
 * {"retcode":0,"retmsg":"success","data":{"uid":"2","ipv4":"61.148.75.238","indulge":0,"ulevel":0,"timestamp":"2012-11-1515:59:49"}}
 * {"retcode":500,"retmsg":"klssoisnull."}
 * {"retcode":501,"retmsg":"parseklssoerror."}
 * {"retcode":501,"retmsg":"klssoexpired."}
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class KunlunUserVerify implements IUserVerify {

	private KunlunChannel ch;

	public KunlunUserVerify(KunlunChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {
		final String klsso = params.get("klsso");
		final String userId = params.get("userId");
		final String userName = params.get("userName");
		if (StringUtil.hasNullOr0LengthString(klsso,userId)) {
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
					// 发起HTTP连接
					String respstring = PromoSupport
							.getInstance()
							.getHttp()
							.request(ch.getUserVerifyUrl(), klsso,
									ch.getUserVerifyHttpMethod());

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

//					 * {"retcode":0,"retmsg":"success","data":{"uid":"2","ipv4":"61.148.75.238","indulge":0,"ulevel":0,"timestamp":"2012-11-1515:59:49"}}
//					 * {"retcode":500,"retmsg":"klssoisnull."}
//					 * {"retcode":501,"retmsg":"parseklssoerror."}
//					 * {"retcode":501,"retmsg":"klssoexpired."}
					JSONObject jobj = new JSONObject(respstring);
					int retcode = jobj.optInt("retcode");
					String retmsg = jobj.optString("retmsg");
					// 根据retcode值来判断，若为0，则有效
					if (retcode!=0) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, retmsg+"("+retcode+")"));
						playersession.send(pverifyResp);
						return;
					}
					JSONObject jdata = jobj.optJSONObject("data");
					String uid = null;
					if (jdata != null) {
						uid = jdata.optString("uid");
						String ipv4 = jdata.optString("ipv4");
						int indulge = jdata.optInt("indulge");
						int ulevel = jdata.optInt("ulevel");
						String timestamp = jdata.optString("timestamp");
					}
					
					if (!userId.equals(uid)) {
						// 与第三方服务器验证失败
						pverifyResp
								.writeInt(KGameProtocol.PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR);
						pverifyResp.writeUtf8String(KGameTips
								.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR",
										promoID, uid));
						playersession.send(pverifyResp);
						return;
					}
					
					String promoMask = uid;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_DUOKOO_UID, promoMask);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, userName,
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
