package com.koala.game.player;

import org.slf4j.Logger;

import com.koala.game.dataaccess.KGameDBException;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.dataaccess.PlayerAuthenticateException;
import com.koala.game.logging.KGameLogger;

/**
 * 
 * @author PERRY CHAN
 */
public class KGamePlayerUtil {

	private static final Logger logger = KGameLogger.getLogger(KGamePlayerUtil.class);
	
	public static void updatePlayerAttribute(KGamePlayer player) {
		try {
			KGameDataAccessFactory.getInstance().getPlayerManagerDataAccess().updatePlayerAttributeById(player.getID(), player.encodeAttribute());
		} catch (PlayerAuthenticateException e) {
			e.printStackTrace();
		} catch (KGameDBException e) {
			e.printStackTrace();
		}
		logger.debug("updatePlayerAttributeAndRemark {}", player);
	}
}
