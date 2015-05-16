package com.koala.game.example;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.koala.game.exception.KGameServerException;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;

public class _ExampleTimerTask implements KGameTimerTask {

	int c = 0;

	@Override
	public String getName() {
		return _ExampleTimerTask.class.getSimpleName();
	}
	
	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		System.out.println("onTimeSignal " + timeSignal + ",@"
				+ Thread.currentThread());
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (c > 5) {
			final KGameTimer timer = timeSignal.getTimer();
			new Thread(new Runnable() {

				@Override
				public void run() {
					Set<KGameTimeSignal> stop = timer.stop();
					for (KGameTimeSignal kGameTimeSignal : stop) {
						System.err.println("--not finish task-- "
								+ kGameTimeSignal);
					}
				}

			}).start();
		}
		c++;

		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
		System.out.println("done " + timeSignal);

		timeSignal.getTimer().newTimeSignal(this,
				Math.abs((new Random()).nextInt() % 10), TimeUnit.SECONDS);
	}

	public static void test(KGameTimer timer) {
//		KGameTimer timer = new KGameTimer(null);
		timer.newTimeSignal(new _ExampleTimerTask(), 5, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 7, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 7, TimeUnit.DAYS);
		timer.newTimeSignal(new _ExampleTimerTask(), 2, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 4, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 37, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 55, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 43, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 65, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 9, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 4, TimeUnit.SECONDS);
		timer.newTimeSignal(new _ExampleTimerTask(), 8, TimeUnit.SECONDS);
	}

	@Override
	public void rejected(RejectedExecutionException e) {
		
	}

}
