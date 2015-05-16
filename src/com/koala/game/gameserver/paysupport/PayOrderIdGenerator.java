package com.koala.game.gameserver.paysupport;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 部分渠道的orderId是要CP生成的
 * <pre>
 * <b>规则：promoid-roleid-nanoTime
 * 如：1001-123456-12104978178330
 * </b>
 * </pre>
 * @author AHONG
 * 
 */
public class PayOrderIdGenerator {

	private static final DateFormat DF = new SimpleDateFormat(
			"yyyyMMddHHmmssSSS");
	private static final AtomicLong FLOW = new AtomicLong();
	
	/**
	 * 生成一个订单号（保证唯一的）
	 * @param promoid 渠道ID
	 * @param roleid 角色ID
	 * @return 订单号，类似P1001R123456T20130712003241950N16
	 */
	public static String gen(int promoid, long roleid) {
		StringBuilder buf = new StringBuilder("P");
		buf.append(promoid).append("R");
		buf.append(roleid).append("T");
		buf.append(DF.format(new Date(System.currentTimeMillis()))).append("N");
		buf.append(FLOW.incrementAndGet());

		return buf.toString();
	}

	public static void main(String[] args) {
		for (int i = 0; i < 20; i++) {
			System.out.println(gen(1001, 123456));
			// System.out.println(DF.format(new
			// Date(System.currentTimeMillis()+System.nanoTime()/1000/1000/1000/1000)));
		}
		System.out.println(DF.format(new Date(System.currentTimeMillis()
				+ System.nanoTime() / 1000 / 1000 / 1000 / 1000)));

	}
}
