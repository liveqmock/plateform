package com.koala.game.timer;

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.koala.game.util.DateUtil;

/**
 * 一个报时信号，其实就是对{@link KGameTimerTask}的一个辅助包装。 <br>
 * 所以{@link KGameTimeSignal}和{@link KGameTimerTask}是一对的，不可分开
 * 
 * @author AHONG
 * @see KGameTimerTask
 * @see KGameTimer
 */
public final class KGameTimeSignal implements RunnableFuture<Object> {

	private KGameTimer timer;
	private KGameTimerTask task;
	private final Sync sync;// Synchronization control for FutureTask

	final long deadline;
	volatile int stopIndex;
	volatile long remainingRounds;

	final long createTimeMillis;

	public KGameTimeSignal(KGameTimer timer, KGameTimerTask task, long deadline) {
		if (task == null)
			throw new NullPointerException();
		createTimeMillis = System.currentTimeMillis();
		this.deadline = deadline;
		this.task = task;
		this.timer = timer;
		sync = new Sync(task);
	}

	@Override
	public String toString() {
		long currentTime = System.currentTimeMillis();
		long remaining = deadline - currentTime;

		StringBuilder buf = new StringBuilder(300);
		// buf.append(getClass().getSimpleName()).append("-");
		buf.append(task.getName()).append("@");
		buf.append(DateUtil.format(new Date(createTimeMillis), KGameTimer.FORMAT_DEBUG));
		buf.append(" -> ");
		buf.append(DateUtil.format(new Date(deadline), KGameTimer.FORMAT_DEBUG));
		buf.append('(');

		buf.append("deadline: ");
		if (remaining > 0) {
			buf.append(remaining);
			buf.append(" ms early, ");
		} else if (remaining < 0) {
			buf.append(-remaining);
			buf.append(" ms late, ");
		} else {
			buf.append("now, ");
		}

		buf.append(DateUtil.format(new Date(currentTime), KGameTimer.FORMAT_DEBUG));
//		buf.append("passed ").append(currentTime - createTimeMillis);

		if (isCancelled()) {
			buf.append(", cancelled");
		}

		return buf.append(')').toString();
	}

	// final functions/////////////////////////////////////////////////

	public KGameTimer getTimer() {
		return timer;
	}

	public KGameTimerTask getTask() {
		return task;
	}

	public boolean isCancelled() {
		return sync.innerIsCancelled();
	}

	public boolean isDone() {
		return sync.innerIsDone();
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (sync.innerCancel(mayInterruptIfRunning)) {
			timer.wheel[stopIndex].remove(this);
			return true;
		}
		return false;
	}

	/**
	 * @throws CancellationException
	 *             {@inheritDoc}
	 */
	public Object get() throws InterruptedException, ExecutionException {
		return sync.innerGet();
	}

	/**
	 * @throws CancellationException
	 *             {@inheritDoc}
	 */
	public Object get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return sync.innerGet(unit.toNanos(timeout));
	}

	/**
	 * Protected method invoked when this task transitions to state
	 * <tt>isDone</tt> (whether normally or via cancellation). The default
	 * implementation does nothing. Subclasses may override this method to
	 * invoke completion callbacks or perform bookkeeping. Note that you can
	 * query status inside the implementation of this method to determine
	 * whether this task has been cancelled.
	 */
	protected void done() {
		task.done(this);
	}

	/**
	 * Sets the result of this Future to the given value unless this future has
	 * already been set or has been cancelled. This method is invoked internally
	 * by the <tt>run</tt> method upon successful completion of the computation.
	 * 
	 * @param v
	 *            the value
	 */
	protected void set(Object v) {
		sync.innerSet(v);
	}

	/**
	 * Causes this future to report an <tt>ExecutionException</tt> with the
	 * given throwable as its cause, unless this Future has already been set or
	 * has been cancelled. This method is invoked internally by the <tt>run</tt>
	 * method upon failure of the computation.
	 * 
	 * @param t
	 *            the cause of failure
	 */
	protected void setException(Throwable t) {
		sync.innerSetException(t);
	}

	// The following (duplicated) doc comment can be removed once
	//
	// 6270645: Javadoc comments should be inherited from most derived
	// superinterface or superclass
	// is fixed.
	/**
	 * Sets this Future to the result of its computation unless it has been
	 * cancelled.
	 */
	public void run() {
		sync.innerRun();
	}

	/**
	 * Executes the computation without setting its result, and then resets this
	 * Future to initial state, failing to do so if the computation encounters
	 * an exception or is cancelled. This is designed for use with tasks that
	 * intrinsically execute more than once.
	 * 
	 * @return true if successfully run and reset
	 */
	protected boolean runAndReset() {
		return sync.innerRunAndReset();
	}

	/**
	 * Synchronization control for FutureTask. Note that this must be a
	 * non-static inner class in order to invoke the protected <tt>done</tt>
	 * method. For clarity, all inner class support methods are same as outer,
	 * prefixed with "inner".
	 * 
	 * Uses AQS sync state to represent run status
	 */
	private final class Sync extends AbstractQueuedSynchronizer {
		private static final long serialVersionUID = -7828117401763700385L;

		/** State value representing that task is running */
		private static final int RUNNING = 1;
		/** State value representing that task ran */
		private static final int RAN = 2;
		/** State value representing that task was cancelled */
		private static final int CANCELLED = 4;

		/** The underlying callable */
		private final KGameTimerTask callable;
		/** The result to return from get() */
		private Object result;
		/** The exception to throw from get() */
		private Throwable exception;

		/**
		 * The thread running task. When nulled after set/cancel, this indicates
		 * that the results are accessible. Must be volatile, to ensure
		 * visibility upon completion.
		 */
		private volatile Thread runner;

		Sync(KGameTimerTask callable2) {
			this.callable = callable2;
		}

		private boolean ranOrCancelled(int state) {
			return (state & (RAN | CANCELLED)) != 0;
		}

		/**
		 * Implements AQS base acquire to succeed if ran or cancelled
		 */
		protected int tryAcquireShared(int ignore) {
			return innerIsDone() ? 1 : -1;
		}

		/**
		 * Implements AQS base release to always signal after setting final done
		 * status by nulling runner thread.
		 */
		protected boolean tryReleaseShared(int ignore) {
			runner = null;
			return true;
		}

		boolean innerIsCancelled() {
			return getState() == CANCELLED;
		}

		boolean innerIsDone() {
			return ranOrCancelled(getState()) && runner == null;
		}

		Object innerGet() throws InterruptedException, ExecutionException {
			acquireSharedInterruptibly(0);
			if (getState() == CANCELLED)
				throw new CancellationException();
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}

		Object innerGet(long nanosTimeout) throws InterruptedException,
				ExecutionException, TimeoutException {
			if (!tryAcquireSharedNanos(0, nanosTimeout))
				throw new TimeoutException();
			if (getState() == CANCELLED)
				throw new CancellationException();
			if (exception != null)
				throw new ExecutionException(exception);
			return result;
		}

		void innerSet(Object v) {
			for (;;) {
				int s = getState();
				if (s == RAN)
					return;
				if (s == CANCELLED) {
					// aggressively release to set runner to null,
					// in case we are racing with a cancel request
					// that will try to interrupt runner
					releaseShared(0);
					return;
				}
				if (compareAndSetState(s, RAN)) {
					result = v;
					releaseShared(0);
					done();
					return;
				}
			}
		}

		void innerSetException(Throwable t) {
			for (;;) {
				int s = getState();
				if (s == RAN)
					return;
				if (s == CANCELLED) {
					// aggressively release to set runner to null,
					// in case we are racing with a cancel request
					// that will try to interrupt runner
					releaseShared(0);
					return;
				}
				if (compareAndSetState(s, RAN)) {
					exception = t;
					result = null;
					releaseShared(0);
					done();
					return;
				}
			}
		}

		boolean innerCancel(boolean mayInterruptIfRunning) {
			for (;;) {
				int s = getState();
				if (ranOrCancelled(s))
					return false;
				if (compareAndSetState(s, CANCELLED))
					break;
			}
			if (mayInterruptIfRunning) {
				Thread r = runner;
				if (r != null)
					r.interrupt();
			}
			releaseShared(0);
			done();
			return true;
		}

		void innerRun() {
			if (!compareAndSetState(0, RUNNING))
				return;
			try {
				runner = Thread.currentThread();
				if (getState() == RUNNING) // recheck after setting thread
					innerSet(callable.onTimeSignal(KGameTimeSignal.this));
				else
					releaseShared(0); // cancel
			} catch (Throwable ex) {
				innerSetException(ex);
			}
		}

		boolean innerRunAndReset() {
			if (!compareAndSetState(0, RUNNING))
				return false;
			try {
				runner = Thread.currentThread();
				if (getState() == RUNNING)
					callable.onTimeSignal(KGameTimeSignal.this); // don't
																	// set
																	// result
				runner = null;
				return compareAndSetState(RUNNING, 0);
			} catch (Throwable ex) {
				innerSetException(ex);
				return false;
			}
		}
	}

}
