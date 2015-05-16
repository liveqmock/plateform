package com.koala.game.player;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 对应玩家的货币账户，即用于记录玩家余额等信息 <br>
 * <br>
 * 
 * <b>!!!必须保证线程安全!!!</b>
 * 
 * @author AHONG
 * 
 */
public final class KGameAccount {

	// private int accountType;
	// public final static byte ACCOUNT_TYPE_SAVINGS = 0;
	// public final static byte ACCOUNT_TYPE_CREDIT = 1;

	private boolean canOverdraft;// 是否可以透支

	private long moneyCanOverdraft;// 可透支额度
	// private long moneyOverdrafted;//已透支金额

	/** 当前账号总额 ,如果可以透支,则此值有可能为负数 */
	private final AtomicLong totalMoney;
	/** 当前可用额度,如果可以透支,则此值{@code = (totalMoney+moneyCanOverdraft-freezeMoney) } */
	private final AtomicLong canuseMoney;
	/** 冻结住的额度 */
	private final AtomicLong freezeMoney;

	/** 历史总收入 */
	private final AtomicLong historyIncomeMoney;
	/** 历史总支出 */
	private final AtomicLong historyUsedMoney;

	/**
	 * 构造函数，所有值默认为0
	 * 
	 * @param canOverdraft
	 *            是否可以透支
	 */
	public KGameAccount(boolean canOverdraft) {
		this(canOverdraft, 0L, 0L, 0L, 0L, 0L);
	}

	/**
	 * 构造函数，对所有值有指定
	 * 
	 * @param canOverdraft
	 *            是否可以透支
	 * @param totalMoney
	 *            当前账号总额 ,如果可以透支,则此值有可能为负数
	 * @param canuseMoney
	 *            当前可用额度,如果可以透支,则此值
	 *            {@code = (totalMoney+moneyCanOverdraft-freezeMoney) }
	 * @param freezeMoney
	 *            冻结住的额度
	 * @param historyIncomeMoney
	 *            历史总收入
	 * @param historyUsedMoney
	 *            历史总支出
	 */
	public KGameAccount(boolean canOverdraft, long totalMoney,
			long canuseMoney, long freezeMoney, long historyIncomeMoney,
			long historyUsedMoney) {
		this.canOverdraft = canOverdraft;
		this.totalMoney = new AtomicLong(totalMoney);
		this.canuseMoney = new AtomicLong(canuseMoney);
		this.freezeMoney = new AtomicLong(freezeMoney);
		this.historyIncomeMoney = new AtomicLong(historyIncomeMoney);
		this.historyUsedMoney = new AtomicLong(historyUsedMoney);
	}

	public boolean isCanOverdraft() {
		return canOverdraft;
	}

	public long getMoneyCanOverdraft() {
		return moneyCanOverdraft;
	}

	public AtomicLong getTotalMoney() {
		return totalMoney;
	}

	public AtomicLong getCanuseMoney() {
		return canuseMoney;
	}

	public AtomicLong getFreezeMoney() {
		return freezeMoney;
	}

	public AtomicLong getHistoryIncomeMoney() {
		return historyIncomeMoney;
	}

	public AtomicLong getHistoryUsedMoney() {
		return historyUsedMoney;
	}

}
