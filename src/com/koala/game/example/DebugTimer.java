package com.koala.game.example;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.RejectedExecutionException;

import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;


/**
 * Timer时间段跨得严重的时候有延时，需要DEBUG：直接提交多个任务， 每个整点都打印，看每个小时延时多少
 * @author AHONG
 *
 */
public class DebugTimer implements KGameTimerTask{

	private final KGameLogger logger = KGameLogger.getLogger(DebugTimer.class);

	
	@Override
	public String getName() {
		return DebugTimer.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		logger.info(">>>>>>>>>>{}",timeSignal);
		
//		try {
//			Thread.sleep(1);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
	}

	@Override
	public void rejected(RejectedExecutionException e) {
		logger.warn("RejectedExecutionException {}",e);
	}

}
