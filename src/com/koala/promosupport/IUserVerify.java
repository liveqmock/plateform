package com.koala.promosupport;

import java.util.Map;

import com.koala.game.KGameMessage;
import com.koala.game.KGameProtocol;
import com.koala.game.player.KGamePlayerSession;

/**
 * 用户合法性验证，多在登录跟SDK服务器同步时发生
 * 
 * @author AHONG
 * 
 */
public interface IUserVerify extends KGameProtocol {

	/**
	 * 向渠道方SDK服务器发出请求做用户验证
	 * 
	 * @param playersession
	 * @param pverifyResp
	 * @param promoID
	 * @param params
	 * @param analysisInfoNeedSaveToDB 2014-09-02 最新添加，需要保存到db的analysisInfo
	 * @throws Exception
	 */
	void request(KGamePlayerSession playersession, KGameMessage pverifyResp,
			int promoID, Map<String, String> params, Map<String, String> analysisInfoNeedSaveToDB) throws Exception;

}
