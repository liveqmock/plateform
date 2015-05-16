package com.koala.game.frontend;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.koala.game.communication.KGameCommCounter;
import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;

public class FEStatusMonitor implements KGameTimerTask {

	private final static KGameLogger logger = KGameLogger
			.getLogger(FEStatusMonitor.class);
	public static final KGameCommCounter commcounter = new KGameCommCounter("FE");
	private final long printIntervalSeconds;
	private final long writtenAmount2FileSeconds;
	private long lastprintMS;
	private long lastwrittenMS;

	public FEStatusMonitor(long printIntervalSeconds,
			long writtenAmount2FileSeconds) {
		this.printIntervalSeconds = printIntervalSeconds;
		this.writtenAmount2FileSeconds = writtenAmount2FileSeconds;
	}

	@Override
	public String getName() {
		return FEStatusMonitor.class.getSimpleName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		long ct = System.currentTimeMillis();
		if (printIntervalSeconds > 0) {
			if (((ct - lastprintMS) / 1000) > printIntervalSeconds) {
				lastprintMS = ct;
				logger.warn("FE's Mem= {}/{} M;handshakedplayersessions= {} ",
						(Runtime.getRuntime().freeMemory() / 1024 / 1024),
						(Runtime.getRuntime().totalMemory() / 1024 / 1024),
						KGameFrontendHandler.getHandshakedPsSize());
				logger.warn(commcounter.toString());
			}
		}
		if (writtenAmount2FileSeconds > 0) {
			if (((ct - lastwrittenMS) / 1000) > writtenAmount2FileSeconds) {
				lastwrittenMS = ct;
				commcounter.printMapping("fe_commcount",true);
			}
		}

		long next;
		if (printIntervalSeconds > 0 && writtenAmount2FileSeconds > 0) {
			next = Math.min(printIntervalSeconds, writtenAmount2FileSeconds);
		} else {
			next = Math.max(printIntervalSeconds, writtenAmount2FileSeconds);
		}

		timeSignal.getTimer().newTimeSignal(this, next, TimeUnit.SECONDS);

		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {

	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public void rejected(RejectedExecutionException e) {

	}

}
