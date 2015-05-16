package com.koala.game.gameserver.paysupport;

import com.koala.paymentserver.PayOrder;

/**
 * 【支付/充值】监听器。
 * 
 * <pre>
 * <b>游戏逻辑中<u>负责元宝账户的模块</u>需要监听本接口所带来的所有事件</b>
 * 
 * 相关配置：
 * KGameServer.xml中修改"PaySupport"标签下的内容
 * 
 * </pre>
 * 
 * @author AHONG
 */
public interface KGamePaymentListener {

	/**
	 * 处理一个支付/充值订单（就是发货/发元宝）
	 * 
	 * @param payOrder
	 * @return 处理结果{@link PayOrderDealResult}
	 */
	PayOrderDealResult dealPayOrder(PayOrder payOrder);

//	/**
//	 * 元宝价格，单位：元
//	 * 
//	 * <pre>
//	 * <b>
//	 * ！！！！！！！！！！！！！！！！！
//	 * 
//	 * 这个非常重要，而且一经确定不再修改。
//	 * 
//	 * <u>如目前是1元兑换10个元宝，即元宝价格为0.10</u>
//	 * 
//	 * 客户端会将此值转换成float类型的，要保留小数点后两位
//	 * </b>
//	 * </pre>
//	 * 
//	 * @return 元宝价格，单位：元
//	 */
//	String getYuanBaoPrice();
}
