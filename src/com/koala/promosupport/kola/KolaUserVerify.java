package com.koala.promosupport.kola;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.frontend.FEStatusMonitor;
import com.koala.game.frontend.KGameFrontend;
import com.koala.game.player.KGamePlayerSession;
import com.koala.game.player.KGamePlayerUtil;
import com.koala.game.tips.KGameTips;
import com.koala.game.tips.RespCode;
import com.koala.game.util.DateUtil;
import com.koala.promosupport.IUserVerify;
import com.koala.promosupport.PromoSupport;

public class KolaUserVerify implements IUserVerify{

	@Override
	public void request(KGamePlayerSession playersession,
			KGameMessage verifyResp, int promoID, Map<String, String> params, final Map<String, String> analysisInfoNeedSaveToDB)
			throws Exception {
		
		String verifyPName = params.get(PROMO_KEY_KOLA_PNAME);
		String verifyPPw = params.get(PROMO_KEY_KOLA_PW);
		if ((verifyPName == null || verifyPName.length() <= 0)
				| (verifyPPw == null || verifyPPw.length() <= 0)) {
			// 格式错误
			verifyResp
					.writeInt(PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG);
			verifyResp.writeUtf8String(KGameTips.get("PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG",
					promoID,PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG));
			playersession.send(verifyResp);
			return;
		}
		
		//TODO 验证账号密码
		// 已经握过手的情况
		RespCode pl_code = new RespCode();
		pl_code.set(PL_PASSPORT_VERIFY_SUCCEED,
				"PL_PASSPORT_VERIFY_SUCCEED");
		try {
			KGameDataAccessFactory.getInstance()
					.getPlayerManagerDataAccess()
					.verifyPlayerPassport(verifyPName, verifyPPw);
		} catch (PlayerAuthenticateException e1) {
			if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_PLAYER_NOT_FOUND) {
				pl_code.set(PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST,
						"NAMENOTEXIST");
			} else if (e1.getErrorCode() == PlayerAuthenticateException.CAUSE_WRONG_PASSWORD) {
				pl_code.set(PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG,
						"PASSWORDISWRONG");
			}
		} catch (KGameDBException e1) {
			pl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
					"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
		}
		// Response
		if (pl_code.v == PL_PASSPORT_VERIFY_SUCCEED) {
			// 如果验证成功:
			// 1.从数据库加载Player信息
			if (playersession.loadAndBindPlayer(verifyPName)) {
				// 检测是否有封号
				long banEndtime = playersession.getBoundPlayer()
						.getBanEndtime();
				if (banEndtime > System.currentTimeMillis()) {
					pl_code.set(PL_PASSPORT_VERIFY_FAILED_BAN, "BAN");
					verifyResp.writeInt(pl_code.v);
					verifyResp.writeUtf8String(KGameTips
							.get(pl_code.k,
									DateUtil.formatReadability(new Date(
											banEndtime))));
				} else {
					playersession.setAuthenticationPassed(true);// 验证通过
					// 2014-09-02 添加 保存analysisInfo
					boolean updateAnalysisInfo = false;
					if (analysisInfoNeedSaveToDB.size() > 0) {
						Map.Entry<String, String> entry;
						boolean temp;
						for (Iterator<Map.Entry<String, String>> itr = analysisInfoNeedSaveToDB.entrySet().iterator(); itr.hasNext();) {
							entry = itr.next();
							temp = playersession.getBoundPlayer().addAnalysisInfoToAttribute(entry.getKey(), entry.getValue());
							if(temp && !updateAnalysisInfo) {
								updateAnalysisInfo = true;
							}
						}
					}
					// 2014-09-02 END
					verifyResp.writeInt(pl_code.v);
					verifyResp.writeUtf8String("");
					verifyResp.writeUtf8String(verifyPName);
					verifyResp.writeUtf8String(verifyPPw);
					//----------------------------------------------------------
					//20130710新增附加返回参数KV形式 int paramN; for(paramN){ 
					//  String key;//具体看常量PROMO_KEY_??? 
					//  String value; 
					//}
					verifyResp.writeInt(0);
//					verifyResp.writeInt(extresponseparamsifsucceed.size());
//					for (String k : extresponseparamsifsucceed.keySet()) {
//						verifyResp.writeUtf8String(k);
//						verifyResp.writeUtf8String(extresponseparamsifsucceed
//								.get(k));
//					}
					// -----------------------------------------------------------
					// 2.直接携带‘服务器列表’的内容 2014-11-07 18:04 屏蔽
//					KGameFrontend.getInstance().getGSMgr().writeGsListOnResponseMsg(
//							verifyResp, playersession,PromoSupport.computeParentPromoID(promoID));
					if(updateAnalysisInfo) {
						// 先保存一次attribute，因为在sessionClose的时候保存，可能会来不及被GS读取
						KGamePlayerUtil.updatePlayerAttribute(playersession.getBoundPlayer());
					}
				}
			} else {
				pl_code.set(PL_UNKNOWNEXCEPTION_OR_SERVERBUSY,
						"PL_UNKNOWNEXCEPTION_OR_SERVERBUSY");
				verifyResp.writeInt(pl_code.v);
				verifyResp.writeUtf8String(KGameTips.get(pl_code.k));
			}
		} else {
			verifyResp.writeInt(pl_code.v);
			verifyResp.writeUtf8String(KGameTips.get(pl_code.k));
		}
		playersession.send(verifyResp);
		if (pl_code.v == PL_PASSPORT_VERIFY_SUCCEED) {
			FEStatusMonitor.commcounter.logined();
		}
	}

}
