package com.koala.paymentserver;

/**
 * 自定义的支付订单ID（复合）- 通过promoID+渠道的orderId复合避免“不同渠道的orderId有可能一样”的隐患
 * 
 * @author AHONG
 * 
 */
public class PayOrderID {

	private int promoID;
	private String orderId;

	public PayOrderID(int promoID, String orderId) {
		this.promoID = promoID;
		this.orderId = orderId;
	}

	public int getPromoID() {
		return promoID;
	}

	public String getOrderId() {
		return orderId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orderId == null) ? 0 : orderId.hashCode());
		result = prime * result + promoID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PayOrderID other = (PayOrderID) obj;
		if (orderId == null) {
			if (other.orderId != null)
				return false;
		} else if (!orderId.equals(other.orderId))
			return false;
		if (promoID != other.promoID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PayOrderID [promoID=" + promoID + ", orderId=" + orderId + "]";
	}

}
