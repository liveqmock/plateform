package com.koala.promosupport.changwan;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.game.util.StringUtil;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * 注：cooguo的登录验证时客户端直接做的，这里直接通过
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class ChangwanUserVerify implements IUserVerify {

	private ChangwanChannel ch;

	public ChangwanUserVerify(ChangwanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		//openId/sign
		final String openId = params.get("openId");
		final String sign = params.get("sign");
		if (StringUtil.hasNullOr0LengthString(openId, sign)) {
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
					// 注：登录验证时客户端直接做的，这里直接通过

					String promoMask = openId;

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put("openId", openId);

					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, ch.getPromoID(), promoMask, openId,
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
