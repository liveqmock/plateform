/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koala.game.timer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.netty.util.internal.ConcurrentIdentityHashMap;
import org.jboss.netty.util.internal.DetectionUtil;
import org.jboss.netty.util.internal.ReusableIterator;
import org.jdom.Element;

import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.util.DateUtil;
import com.koala.game.util.InstanceMisuseDetector;
import com.koala.game.util.MapBackedSet;
import com.koala.game.util.ThreadUtil;

/**
 * KGAME底层引擎的“时效任务”功能块主要类。<br>
 * <b> 注：使用者无须对本类的建立、start、stop生命周期做控制，只需要实现 {@link KGameTimerTask}并通过
 * {@link #newTimeSignal(KGameTimerTask, long, TimeUnit)}
 * 方法提交定时任务即可；另外所有‘时效任务’都会被一个‘报时信号’{@link KGameTimeSignal}
 * 所包装并提供一些cancel等实用方法。实例可通过{@link com.koala.game.KGame#getTimer()}获得 </b>
 * 
 * @author AHONG
 * @see KGameTimerTask
 * @see KGameTimeSignal
 */
public final class KGameTimer {
	
	public static final DateFormat FORMAT_DEBUG = new SimpleDateFormat(
			"MMdd:HH:mm:ss.SSS");
	
	private static final KGameLogger logger = KGameLogger
			.getLogger(KGameTimer.class);

	private int corePoolSize;
	private ExecutorService scheduledThreadPool;
	private final Set<KGameTimerHandler> handlers = new HashSet<KGameTimerHandler>();

	private static final InstanceMisuseDetector misuseDetector = new InstanceMisuseDetector(
			KGameTimer.class, 256);// 警告作用，256为暂时设定

	private final TickerWorker worker = new TickerWorker();
	final Thread workerThread;
	final AtomicBoolean shutdown = new AtomicBoolean();

	final long tickDuration;// ms, = 1s
	private final long roundDuration;
	final Set<KGameTimeSignal>[] wheel;// 将时间看作一个轮状，轮被分割成N个区间，就好像一个钟
	final ReusableIterator<KGameTimeSignal>[] iterators;
	final int mask;
	final ReadWriteLock lock = new ReentrantReadWriteLock();
	volatile int wheelCursor;

	/**
	 * 通过配置文件构建
	 * 
	 * @param eTimerTasks
	 *            org.jdom.Element对象，包含KGameTimer及tasks设置
	 */
	public KGameTimer(Element eTimer) {
		corePoolSize = Integer.parseInt(eTimer
				.getAttributeValue("corePoolSize"));
		scheduledThreadPool = Executors.newFixedThreadPool(corePoolSize);
		tickDuration = Long.parseLong(eTimer.getAttributeValue("tickDuration"));
		// int ticksPerWheel = 60;// 3600 * 24;// 临时的
		int ticksPerWheel = Integer.parseInt(eTimer
				.getAttributeValue("ticksPerWheel"));
		roundDuration = tickDuration * ticksPerWheel;

		wheel = createWheel(ticksPerWheel);
		iterators = createIterators(wheel);
		mask = wheel.length - 1;

		workerThread = Executors.defaultThreadFactory().newThread(worker);

		// Misuse check
		misuseDetector.increase();

		List<Element> ehandlers = eTimer.getChildren("handler");
		if (ehandlers != null) {
			for (Element ehandler : ehandlers) {
				String shandler = ehandler.getAttributeValue("clazz");
				if (shandler != null && shandler.length() > 0) {
					KGameTimerHandler handler = null;
					try {
						handler = (KGameTimerHandler) Class.forName(shandler)
								.newInstance();
						handlers.add(handler);
					} catch (InstantiationException e) {
						throw new IllegalArgumentException("handler class", e);
					} catch (IllegalAccessException e) {
						throw new IllegalArgumentException("handler class", e);
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("handler class", e);
					}
					if (handler != null) {
						List<Element> taskElementsConfigInXml = ehandler
								.getChildren("TimerTask");
						handler.init(this, taskElementsConfigInXml);
					}
				}
			}
			logger.info("timer handler size " + handlers.size());
		}
	}

	@SuppressWarnings("unchecked")
	private static Set<KGameTimeSignal>[] createWheel(int ticksPerWheel) {
		if (ticksPerWheel <= 0) {
			throw new IllegalArgumentException(
					"ticksPerWheel must be greater than 0: " + ticksPerWheel);
		}
		if (ticksPerWheel > 1073741824) {
			throw new IllegalArgumentException(
					"ticksPerWheel may not be greater than 2^30: "
							+ ticksPerWheel);
		}

		ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
		Set<KGameTimeSignal>[] wheel = new Set[ticksPerWheel];
		for (int i = 0; i < wheel.length; i++) {
			wheel[i] = new MapBackedSet<KGameTimeSignal>(
					new ConcurrentIdentityHashMap<KGameTimeSignal, Boolean>(16,
							0.95f, 4));
		}
		return wheel;
	}

	@SuppressWarnings("unchecked")
	private static ReusableIterator<KGameTimeSignal>[] createIterators(
			Set<KGameTimeSignal>[] wheel) {
		ReusableIterator<KGameTimeSignal>[] iterators = new ReusableIterator[wheel.length];
		for (int i = 0; i < wheel.length; i++) {
			iterators[i] = (ReusableIterator<KGameTimeSignal>) wheel[i]
					.iterator();
		}
		return iterators;
	}

	//2的平方数： 2 4 8 16 32 64 128 256 512...
	private static int normalizeTicksPerWheel(int ticksPerWheel) {
		int normalizedTicksPerWheel = 1;
		while (normalizedTicksPerWheel < ticksPerWheel) {
			normalizedTicksPerWheel <<= 1;
		}
		return normalizedTicksPerWheel;
//		return ticksPerWheel;
	}

	/**
	 * 启动定时器，启动后除非调用{@link #stop()}否则将一直运行
	 */
	public synchronized void start() {
		if (shutdown.get()) {
			throw new IllegalStateException("cannot be started once stopped");
		}

		if (!workerThread.isAlive()) {
			workerThread.start();
		}
	}

	/**
	 * 停止定时器。调用本方法后内部的及时线程先被关闭，然后关闭处理线程池（提交了还没执行的任务直接取消，运行中的则等待Termination）
	 * 
	 * @return 提交了又还没开始执行的任务的集合，如果是很重要的任务，使用者可以对此做保存或其它处理
	 */
	public synchronized Set<KGameTimeSignal> stop() {

		if (Thread.currentThread() == workerThread) {
			throw new IllegalStateException(KGameTimer.class.getSimpleName()
					+ ".stop() cannot be called from "
					+ KGameTimeSignal.class.getSimpleName());
		}

		if (!shutdown.compareAndSet(false, true)) {
			logger.warn(KGameTimer.class.getSimpleName()
					+ ".stop() repeated call.");
			return Collections.emptySet();
		}

		logger.info(KGameTimer.class.getSimpleName() + " stopping ......");

		boolean interrupted = false;
		while (workerThread.isAlive()) {
			workerThread.interrupt();
			try {
				workerThread.join(100);
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		logger.info(KGameTimer.class.getSimpleName()
				+ " stopping ...... workerThread interrup " + interrupted);

		if (interrupted) {
			Thread.currentThread().interrupt();
		}

		boolean isTerminated = false;
		try {
			isTerminated = ThreadUtil.shutdownAndAwaitTermination(
					scheduledThreadPool, 60);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		logger.info(KGameTimer.class.getSimpleName()
				+ " stopped !!!!!!!!!!!!scheduledThreadPool shutdowned "
				+ scheduledThreadPool.isShutdown() + ",isTerminated "
				+ isTerminated);

		misuseDetector.decrease();

		Set<KGameTimeSignal> unprocessedTimeSignals = new HashSet<KGameTimeSignal>();
		for (Set<KGameTimeSignal> bucket : wheel) {
			unprocessedTimeSignals.addAll(bucket);
			bucket.clear();
		}

		Set<KGameTimeSignal> unprocessed = Collections
				.unmodifiableSet(unprocessedTimeSignals);

		// 通知Handler
		for (KGameTimerHandler handler : handlers) {
			handler.timerStopped(this, unprocessed);
		}

		return unprocessed;
	}

	// public KGameTimeSignal scheduleTaskAtFixedRate(KGameTimerTask task,
	// long initialDelay, long period, int loop, TimeUnit unit) {
	//
	// return null;
	// }

	/**
	 * 向定时器{@link KGameTimer}提交一个新的定时任务{@link KGameTimerTask}
	 * 。加入后引擎底层将这个任务包装成一个报时信号{@link KGameTimeSignal}，当时间到达时产生信号，并通过
	 * {@link KGameTimerTask#onTimeSignal(KGameTimeSignal)}向使用者回调通知
	 * 
	 * @param task
	 *            待执行的定时任务
	 * @param delay
	 *            第一次执行延迟，从提交这一刻开始算起
	 * @param unit
	 *            延迟的时间单位
	 * @return 新的报时信号，报时信号中包含一些常用的方法
	 * @throws IllegalStateException
	 *             if this timer has been {@linkplain #stop() stopped} already
	 */
	public KGameTimeSignal newTimeSignal(KGameTimerTask task, long delay,
			TimeUnit unit) {
		final long currentTime = System.currentTimeMillis();

		if (task == null) {
			throw new NullPointerException("task");
		}
		if (unit == null) {
			throw new NullPointerException("unit");
		}

		if (!workerThread.isAlive()) {
			start();
		}

		delay = unit.toMillis(delay);
		KGameTimeSignal timeSignal = new KGameTimeSignal(this, task,
				currentTime + delay);
		scheduleTimeSignal(timeSignal, delay);

		// logger.info("newTimeSignal({}),{} {}", task.getName(), currentTime,
		// timeSignal);

		return timeSignal;
	}

	void scheduleTimeSignal(KGameTimeSignal timeSignal, long delay) {
		// delay must be equal to or greater than tickDuration so that the
		// worker thread never misses the TimeSignal.
		if (delay < tickDuration) {
			delay = tickDuration;
		}

		// Prepare the required parameters to schedule the TimeSignal object.
		final long lastRoundDelay = delay % roundDuration;
		final long lastTickDelay = delay % tickDuration;
		final long relativeIndex = lastRoundDelay / tickDuration
				+ (lastTickDelay != 0 ? 1 : 0);
		final long remainingRounds = delay / roundDuration
				- (delay % roundDuration == 0 ? 1 : 0);

		// Add the TimeSignal to the wheel.
		lock.readLock().lock();
		try {
			int stopIndex = (int) ((wheelCursor + relativeIndex) & mask);
			timeSignal.stopIndex = stopIndex;
			timeSignal.remainingRounds = remainingRounds;

			//logger.debug("lastRoundDelay={},remainingRounds={},relativeIndex={},stopIndex={},wheelCursor={}",lastRoundDelay,remainingRounds,relativeIndex,stopIndex,wheelCursor);
			
			wheel[stopIndex].add(timeSignal);
		} finally {
			lock.readLock().unlock();
		}

	}

	@Override
	public String toString() {
		return "KGameTimer [corePoolSize=" + corePoolSize + ", tickDuration="
				+ tickDuration + ", roundDuration=" + roundDuration + "]";
	}

	public void printwheel(){
		for(int i=0;i<wheel.length;i++){
			System.out.print(" "+i);
		}
		System.out.println();
		for (int i=0;i<wheel.length;i++) {
			System.out.print(" "+wheel[i].size());
		}
		System.out.println();
	}


	/*************************************************************************************
	 * Inner class {@link TickerWorker} 报时信号线程，就像一个时钟，每个固定时间间隔发送一次信号
	 */
	private final class TickerWorker implements Runnable {

		private long startTime;
		private long tick;
		private Date tickdate;

		TickerWorker() {
			super();
		}

		@Override
		public void run() {
			List<KGameTimeSignal> expiredTimeSignals = new ArrayList<KGameTimeSignal>();

			startTime = System.currentTimeMillis();
			tick = 0;//指针起始位置

			// DEBUG>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
			tickdate = new Date(startTime);
			logger.info("KGameTimer Ticker Start @ {}",
					DateUtil.format(tickdate,FORMAT_DEBUG));
			// DEBUG<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

			while (!shutdown.get()) {
				final long deadline = waitForNextTick();

//				// DEBUG>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
//				tickdate.setTime(deadline);
//				logger.info(
//						"===============tick={},date={},wantms-actual={},now={}",
//						tick, DateUtil.format(tickdate,FORMAT_DEBUG),
//						((startTime + (tick-1) * tickDuration) - System
//								.currentTimeMillis()), DateUtil
//								.formatReadability(new Date()));
//				// DEBUG<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

				if (deadline > 0) {
					fetchExpiredTimeSignals(expiredTimeSignals, deadline);
					notifyExpiredTimeSignals(expiredTimeSignals);
				}
			}
		}

		private void fetchExpiredTimeSignals(
				List<KGameTimeSignal> expiredTimeSignals, long deadline) {
			lock.writeLock().lock();
			try {
				int newWheelCursor = wheelCursor = (wheelCursor + 1) & mask;
				ReusableIterator<KGameTimeSignal> i = iterators[newWheelCursor];
				fetchExpiredTimeSignals(expiredTimeSignals, i, deadline);
			} finally {
				lock.writeLock().unlock();
			}
		}

		private void fetchExpiredTimeSignals(
				List<KGameTimeSignal> expiredTimeSignals,
				ReusableIterator<KGameTimeSignal> i, long deadline) {

			List<KGameTimeSignal> slipped = null;
			i.rewind();
			while (i.hasNext()) {
				KGameTimeSignal timesignal = i.next();
				if (timesignal.remainingRounds <= 0) {
					i.remove();
					if (timesignal.deadline <= deadline) {
						expiredTimeSignals.add(timesignal);
					} else {
						// Handle the case where the timesignal is put into a
						// wrong place, usually one tick earlier. For now, just add
						// it to a temporary list - we will reschedule it in a
						// separate loop.
						if (slipped == null) {
							slipped = new ArrayList<KGameTimeSignal>();
						}
						slipped.add(timesignal);
					}
				} else {
					timesignal.remainingRounds--;
				}
			}

			// Reschedule the slipped timesignals.
			if (slipped != null) {
				for (KGameTimeSignal timesignal : slipped) {
					scheduleTimeSignal(timesignal, timesignal.deadline
							- deadline);
				}
			}
		}

		private void notifyExpiredTimeSignals(
				List<KGameTimeSignal> expiredTimeSignals) {
			// Notify the expired timesignals.
			for (int i = expiredTimeSignals.size() - 1; i >= 0; i--) {
				// 提交到处理线程池处理
				KGameTimeSignal signal = null;
				try {
					signal = expiredTimeSignals.get(i);
					scheduledThreadPool.execute(signal);
				} catch (NullPointerException e) {
					continue;
				} catch (RejectedExecutionException e) {
					if (signal != null) {
						signal.getTask().rejected(e);
					}
				}
			}

			// Clean up the temporary list.
			expiredTimeSignals.clear();
		}

		private long waitForNextTick() {
			//long deadline = startTime + tickDuration * tick;

			for (;;) {
				final long currentTime = System.currentTimeMillis();
				long sleepTime = tickDuration * tick
						- (currentTime - startTime);

				// Check if we run on windows, as if thats the case we will need
				// to round the sleepTime as workaround for a bug that only
				// affect
				// the JVM if it runs on windows.
				//
				// See https://github.com/netty/netty/issues/356
				if (DetectionUtil.isWindows()) {
					sleepTime = sleepTime / 10 * 10;
				}

				if (sleepTime <= 0) {
					break;
				}
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					if (shutdown.get()) {
						return -1;
					}
					logger.warn(TickerWorker.class.getName()
							+ " caught a InterruptedException. ");
				}
				// logger.info("tick {},sleep {},used {}",tick,sleepTime,(System.currentTimeMillis()-currentTime));
			}// ~for

			// Increase the tick.
			tick++;

			long deadline = startTime + tickDuration * tick;
			return deadline;
		}
	}

}
