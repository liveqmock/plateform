package com.koala.game.gameserver.paysupport;

import com.koala.game.player.KGamePlayerSession;
import com.koala.paymentserver.PayOrder;

public class ExamplePaymentListener implements KGamePaymentListener{

	@Override
	public PayOrderDealResult dealPayOrder(PayOrder payOrder) {
		
		System.out.println("ExamplePaymentListener dealPayOrder: "+payOrder);
		
		return new KGamePayOrderDealResult(payOrder);
	}

	public class KGamePayOrderDealResult implements PayOrderDealResult{
		
		PayOrder payOrder;
		
		public KGamePayOrderDealResult(PayOrder payOrder) {
			super();
			this.payOrder = payOrder;
		}

//		@Override
//		public String getTipsToPlayer() {
//			return "tips to player";
//		}
		
		@Override
		public int getResult() {
			return 0;
		}
		
//		@Override
//		public KGamePlayerSession getPlayerSession() {
//			return null;
//		}
		
		@Override
		public PayOrder getPayOrder() {
			return payOrder;
		}
	}

//	@Override
//	public String getYuanBaoPrice() {
//		// TODO Auto-generated method stub
//		return "0.10";
//	}
}
