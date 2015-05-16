package com.koala.game.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

	public static boolean shutdownAndAwaitTermination(ExecutorService pool,
			int awaitSeconds) /* throws Throwable */{
		pool.shutdown();
		try {
			if (!pool.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
				// throw new TimeoutException("Service did not terminate");
				System.err
						.println("WARN shutdownAndAwaitTermination,service did not terminate, await "
								+ awaitSeconds + "s throw TimeoutException.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			pool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		return pool.isTerminated();
	}

}
