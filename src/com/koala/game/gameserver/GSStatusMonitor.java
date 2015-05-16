package com.koala.game.gameserver;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.koala.game.KGame;
import com.koala.game.KGameMessage;
import com.koala.game.communication.KGameCommCounter;
import com.koala.game.dataaccess.KGameDataAccessFactory;
import com.koala.game.exception.KGameServerException;
import com.koala.game.gameserver.crossserversupport.KGameCrossServerSupport;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;

public class GSStatusMonitor implements KGameTimerTask {

	private final static KGameLogger logger = KGameLogger
			.getLogger(GSStatusMonitor.class);
	public static final KGameCommCounter commcounter = new KGameCommCounter(
			"GS");
	private final long printIntervalSeconds;
	private final long writtenAmount2FileSeconds;
	private long lastprintMS;
	private long lastwrittenMS;

	private final long recordonlineflowseconds;
	private long lastrecordonlineflowseconds;

	public GSStatusMonitor(long printIntervalSeconds,
			long writtenAmount2FileSeconds, long recordonlineflowseconds) {
		this.printIntervalSeconds = printIntervalSeconds;
		this.writtenAmount2FileSeconds = writtenAmount2FileSeconds;
		this.recordonlineflowseconds = recordonlineflowseconds;
	}

	@Override
	public String getName() {
		return GSStatusMonitor.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		long ct = System.currentTimeMillis();
		if (printIntervalSeconds > 0) {
			if (((ct - lastprintMS) / 1000) > printIntervalSeconds) {
				lastprintMS = ct;
				logger.warn(
						"GS's Mem= {}/{} M;handshakedplayersessions= {} ;online= {} ",
						(Runtime.getRuntime().freeMemory() / 1024 / 1024),
						(Runtime.getRuntime().totalMemory() / 1024 / 1024),
						KGameServerHandler.handshakedplayersessions.size(),
						KGameServer.getInstance().getPlayerManager()
								.getCachedPlayerSessionSize());
				logger.warn(commcounter.toString());
			}
		}
		if (writtenAmount2FileSeconds > 0) {
			if (((ct - lastwrittenMS) / 1000) > writtenAmount2FileSeconds) {
				lastwrittenMS = ct;
				commcounter.printMapping("gs_commcount", true);
			}
		}
		if (recordonlineflowseconds > 0) {
			if (((ct - lastrecordonlineflowseconds) / 1000) > recordonlineflowseconds) {
				lastrecordonlineflowseconds = ct;
				KGameDataAccessFactory
						.getInstance()
						.getPlayerManagerDataAccess()
						.addServerOnlineRecord(
								0,
								KGameServer.getInstance().getGSID(),
								KGameServerHandler.handshakedplayersessions
										.size(),
								lastrecordonlineflowseconds,
								KGameServer.getInstance()
										.getPlayerManager()
										.getCachedPlayerSessionSize());
			}
		}

		long next;
		if (printIntervalSeconds > 0 && writtenAmount2FileSeconds > 0) {
			next = Math.min(printIntervalSeconds, writtenAmount2FileSeconds);
		} else {
			next = Math.max(printIntervalSeconds, writtenAmount2FileSeconds);
		}
		
//		//TEST
//		System.out.println("111111111111111111111111111111111111");
//		KGameMessage mm = KGame.newLogicMessage(1111111);
//		mm.writeInt(88);
//		KGameCrossServerSupport.sendGrossGs(1, 1, mm);
//		System.out.println("222222222222222222222222222222222222");
		
		timeSignal.getTimer().newTimeSignal(this, next, TimeUnit.SECONDS);

		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {

	}

	@Override
	public void rejected(RejectedExecutionException e) {

	}

}
