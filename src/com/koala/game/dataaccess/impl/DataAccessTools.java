package com.koala.game.dataaccess.impl;

import com.koala.game.dataaccess.KGameDBException;

public class DataAccessTools {
	protected final long db_timeout_mills = 10000;
	
	protected long[] initRestTime() {
        return new long[]{db_timeout_mills, System.currentTimeMillis()};
    }

    protected int getStatmentTimeout(long[] time) {
        if (time[0] >= 1000) {
            return ((int) (time[0] / 1000));
        } else {
            return 1;
        }
    }

    protected void getRestTimeout(long[] time) throws KGameDBException {
        long currTime = System.currentTimeMillis();
        time[0] = time[0] - (currTime - time[1]);
        if (time[0] <= 0) {
            throw new KGameDBException("数据访问超时", null, KGameDBException.CAUSE_DB_TIMEOUT, "操作异常，数据访问超时");
        }
        time[1] = currTime;
    }

}
