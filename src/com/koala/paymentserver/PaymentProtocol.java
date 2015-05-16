package com.koala.paymentserver;

import com.koala.game.KGameProtocol;

public interface PaymentProtocol extends KGameProtocol{

	/**
	 * 【支付服务器PS跟游戏服务器GS的通信】产生一笔支付/充值行为时，PS会通知GS并携带必须的信息。
	 */
	public final static int MID_PS2GS_PAY = 13;

//	/**
//	 * GS向PS获取paybefore要的参数
//	 */
//	public final static int MID_PS2GS_PAYBEFORE_PARAMS = 14;
}
