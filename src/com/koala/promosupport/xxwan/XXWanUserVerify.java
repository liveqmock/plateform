package com.koala.promosupport.xxwan;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.tips.KGameTips;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;

/**
 * <pre>
 * </pre>
 * 
 * @author AHONG
 * 
 */
public class XXWanUserVerify implements IUserVerify {

	private XXWanChannel ch;

	public XXWanUserVerify(XXWanChannel ch) {
		super();
		this.ch = ch;
	}

	@Override
	public void request(final KGamePlayerSession playersession,
			final KGameMessage pverifyResp, final int promoID,
			final Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB) throws Exception {

		String tempAccountid = params.get(PROMO_KEY_ACCOUNTID);
		final String userId = params.get(PROMO_KEY_UID);
		if (/*accountid == null || accountid.length() <= 0 ||*/ userId == null
				|| userId.length() <= 0) {
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
		if (tempAccountid == null || tempAccountid.length() == 0) {
			tempAccountid = userId;
		}
		final String accountid = tempAccountid;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					
					//由于XXWAN已经把其它渠道的SDK的用户验证做过了，这里不做验证
					
					String promoMask = userId;
					
					// 返回信息中添加参数（直接重用上面的Map对象）
					params.clear();
					//params.put(KGameProtocol.PROMO_KEY_ACCOUNTID, accountid);

//					PromoSupport.getInstance().afterUserVerified(playersession,
//							promoID, ch.getPromoID(), promoMask, accountid,
//							pverifyResp, params);
					PromoSupport.getInstance().afterUserVerified(playersession,
							promoID, PromoSupport.computeParentPromoID(ch.getPromoID()), promoMask, accountid,
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
