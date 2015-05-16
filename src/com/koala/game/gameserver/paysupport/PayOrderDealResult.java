package com.koala.game.gameserver.paysupport;

import com.koala.game.player.KGamePlayerSession;
import com.koala.paymentserver.PayOrder;

public interface PayOrderDealResult {

	/**
	 * 
	 * @return 处理结果，0表示成功，1表示失败原因1... TODO 待定义
	 */
	int getResult();

//	KGamePlayerSession getPlayerSession();

	PayOrder getPayOrder();

//	String getTipsToPlayer();

}
