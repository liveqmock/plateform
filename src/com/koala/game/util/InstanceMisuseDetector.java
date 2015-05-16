package com.koala.game.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.koala.game.logging.KGameLogger;

/**
 * Warn when user creates too many instances to avoid {@link OutOfMemoryError}.
 * 
 * @author AHONG
 * 
 */
public class InstanceMisuseDetector {

	private final int maxActiveInstances;
	private final Class<?> type;
	private final AtomicLong activeInstances = new AtomicLong();
	private final AtomicBoolean logged = new AtomicBoolean();
	private static final KGameLogger logger = KGameLogger
			.getLogger(InstanceMisuseDetector.class);

	public InstanceMisuseDetector(Class<?> type, int maxActiveInstances) {
		if (type == null) {
			throw new NullPointerException("type");
		}
		this.type = type;
		this.maxActiveInstances = maxActiveInstances;
	}

	public void increase() {
		if (activeInstances.incrementAndGet() > maxActiveInstances) {
			if (logger.isWarnEnabled()) {
				if (logged.compareAndSet(false, true)) {
					logger.warn("You are creating too many "
							+ type.getSimpleName()
							+ " instances.  "
							+ type.getSimpleName()
							+ " is a shared resource that must be reused across the"
							+ " application, so that only a few instances are created.");
				}
			}
		}
	}

	public void decrease() {
		activeInstances.decrementAndGet();
	}
}
