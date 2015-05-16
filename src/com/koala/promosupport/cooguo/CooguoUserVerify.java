package com.koala.promosupport.cooguo;

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
public class CooguoUserVerify implements IUserVerify {

	private CooguoChannel ch;

	public CooguoUserVerify(CooguoChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		final String userId = params.get(PROMO_KEY_360_USERID);
		final String userName = params.get(PROMO_KEY_USERNAME);
		if (StringUtil.hasNullOr0LengthString(userId, userName)) {
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
					// 注：cooguo的登录验证时客户端直接做的，这里直接通过

					String promoMask = String.valueOf(userId);

					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					params.put(KGameProtocol.PROMO_KEY_360_USERID, userId);
					params.put(KGameProtocol.PROMO_KEY_USERNAME, userName);

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
