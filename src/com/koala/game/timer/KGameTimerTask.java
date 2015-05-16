package com.koala.game.timer;

import java.util.concurrent.RejectedExecutionException;

import com.koala.game.exception.KGameServerException;
import com.koala.game.util.DateUtil;

/**
 * 一个时效任务。使用者将要处理的内容在本类实现，并通过
 * {@link KGameTimer#newTimeSignal(KGameTimerTask, long, java.util.concurrent.TimeUnit)}
 * 提交到定时器等待执行。 <br>
 * 当到达预设时间点，将收到定时器的报时信号并通过{@link #onTimeSignal(KGameTimeSignal)}
 * 方法通知，此方法可以返回值，目前定义Object，使用者可根据自身实际需要定义。<br>
 * <br>
 * PS：{@link KGameTimeSignal}和{@link KGameTimerTask}是一对的，不可分开
 * 
 * <p>
 * <b> 友情提示：<br>
 * 1、有很多时效任务都是循环或按一定的时间计划表执行的，但当调用
 * {@linkplain KGameTimer#newTimeSignal(KGameTimerTask, long, java.util.concurrent.TimeUnit)}
 * 方法时，其实只是执行一次本任务。
 * 那么应该如何处理这个问题呢？使用者可以在本接口实现类中定义如loop、remainingLoop、period之类的变量，然后通过在
 * {@link #onTimeSignal(KGameTimeSignal)}或 {@link #done(KGameTimeSignal)}
 * 方法中去计算改变这些变量，并按实际情况再调用
 * {@linkplain KGameTimer#newTimeSignal(KGameTimerTask, long, java.util.concurrent.TimeUnit)}
 * 方法（注：参数中的task直接就是this），从而达到自动循环的效果。（PS：done方法的时间要比onTimeSignal迟至少1s，
 * 所以如果对时间要求很精确的例如要在2013/3/3
 * 12:00执行，那么就要放到onTimeSignal方法里面提交；done一般作为完成统计或一些需要上次任务执行完再提交的情况）<br>
 * 2、时间计划表可以用long[]来实现，例如long[]{10,3,35,23,24,22}可以表示不相同的时间间隔。<br>
 * 3、有很多日期时间星期之类的计算，可通过{@link DateUtil}工具类中所提供的方法简便达到目的。 <br>
 * 4、注意配置文件的灵活设计。 </b>
 * </p>
 * 
 * @author AHONG
 * @see com.koala.game.util.DateUtil 
 * @see KGameTimer
 * @see KGameTimeSignal
 */
public interface KGameTimerTask {

	/**
	 * 时效任务的名称。没实际作用，只是作为跟踪辨别之用。
	 * 
	 * @return 时效任务的名称
	 */
	String getName();

	/**
	 * 到达预设的时间，收到定时器发来的定时信号，在本方法实现业务逻辑
	 * 
	 * @param timeSignal
	 *            报时信号
	 * @return 目前定义Object，使用者可根据自身实际需要定义
	 * @throws KGameServerException
	 */
	Object onTimeSignal(KGameTimeSignal timeSignal) throws KGameServerException;

	/**
	 * 当一轮逻辑处理完成后的通知，其实就是线程池执行完{@linkplain #onTimeSignal(KGameTimeSignal)
	 * onTimeSignal}方法时调用。<br>
	 * 本方法未必有实际的业务逻辑作用，或可以用于对任务执行的时间和结果的检测。。。<br>
	 * <br>
	 * PS：可以调用{@link KGameTimeSignal#get()}方法获取到返回值，也就是
	 * {@linkplain #onTimeSignal(KGameTimeSignal) onTimeSignal}返回的Object
	 * 
	 * @param timeSignal
	 */
	void done(KGameTimeSignal timeSignal);
	
	/**
	 * 执行线程池拒绝执行，此异常一般不会出现，除非执行线程池出现严重问题
	 * @param e 拒绝执行异常
	 */
	void rejected(RejectedExecutionException e);
}
