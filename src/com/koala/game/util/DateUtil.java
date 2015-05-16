package com.koala.game.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.koala.game.timer.KGameTimer;

/**
 * 日期啊时间啊的工具类，主要针对{@link KGameTimer}功能块中与日期时间相关的方法
 * 
 * @author AHONG
 * 
 */
public final class DateUtil {

	/** KGAME程序内部默认的日期格式20120926160808(yyyyMMddHHmmss) */
	public static final DateFormat FORMAT_DEFAULT = new SimpleDateFormat(
			"yyyyMMddHHmmss");// 程序内部使用
	/**
	 * 只是为了可读性的日期格式2012-09-26 16:08:08(yyyy-MM-dd
	 * HH:mm:ss)，例如在打印或log中使用，不过一般建议使用{@link #FORMAT_DEFAULT}
	 */
	public static final DateFormat FORMAT_READABILITY = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");// readability

	// SUNDAY
	// MONDAY
	// TUESDAY
	// WEDNESDAY
	// THURSDAY
	// FRIDAY
	// SATURDAY

	// get ///////////////////////////////////////////////////////////

	public static synchronized String getCurrentTime(DateFormat format) {
		Calendar day = Calendar.getInstance();
		return format.format(day.getTime());
	}

	public static String getCurrentTimeDefault() {
		return getCurrentTime(FORMAT_DEFAULT);
	}

	// format/////////////////////////////////////////////////////////

	public static synchronized String format(Date date, DateFormat format) {
		return format.format(date);
	}

	public static String format(Date date, String format) {
		DateFormat df = new SimpleDateFormat(format);
		return format(date, df);
	}

	public static String formatDefault(Date date) {
		return format(date, FORMAT_DEFAULT);
	}

	public static String formatReadability(Date date) {
		return format(date, FORMAT_READABILITY);
	}

	public static synchronized Date parse(String formattedString,
			DateFormat format) throws ParseException {
		return format.parse(formattedString);
	}

	public static Date parseDefault(String defaultFormattedString)
			throws ParseException {
		return parse(defaultFormattedString, FORMAT_DEFAULT);
	}

	public static Date parseReadability(String readabilityFormatString)
			throws ParseException {
		return parse(readabilityFormatString, FORMAT_READABILITY);
	}

	// compute//////////////////////////////////////////////////////////

	/**
	 * 计算两个给定的时间间隔毫秒数
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public static synchronized long computeDurationMillis(Date startDate,
			Date endDate) {
		Calendar sc = Calendar.getInstance();
		sc.setTime(startDate);
		Calendar ec = Calendar.getInstance();
		ec.setTime(endDate);
		return ec.getTimeInMillis() - sc.getTimeInMillis();
	}

	/**
	 * 计算两个给定的时间间隔毫秒数
	 * 
	 * @param startDate
	 * @param endDate
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static long computeDurationMillis(String startDate, String endDate,
			DateFormat format) throws ParseException {
		return computeDurationMillis(format.parse(startDate),
				format.parse(endDate));
	}

	/**
	 * 计算两个给定的时间间隔毫秒数
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws ParseException
	 */
	public static long computeDurationMillisDefault(String startDate,
			String endDate) throws ParseException {
		return computeDurationMillis(FORMAT_DEFAULT.parse(startDate),
				FORMAT_DEFAULT.parse(endDate));
	}

	/**
	 * 计算给定日期跟当前时间的间隔毫秒数
	 * 
	 * @param endDate
	 * @param format
	 * @return
	 */
	public static long computeDurationMillis(Date endDate) {
		return computeDurationMillis(new Date(), endDate);
	}

	/**
	 * 计算给定日期跟当前时间的间隔毫秒数
	 * 
	 * @param endDate
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static long computeDurationMillis(String endDate, DateFormat format)
			throws ParseException {
		return computeDurationMillis(format.parse(endDate));
	}

	/**
	 * 计算给定日期跟当前时间的间隔毫秒数
	 * 
	 * @param endDate
	 * @return
	 * @throws ParseException
	 */
	public static long computeDurationMillisDefault(String endDate)
			throws ParseException {
		return computeDurationMillis(endDate, FORMAT_DEFAULT);
	}

	// 加减法//////////////////////////////////////////////////////////////////

	// 如果日历字段值中存在任何冲突，则 Calendar
	// 将为最近设置的日历字段提供优先权。以下是日历字段的默认组合。将使用由最近设置的单个字段所确定的最近组合。
	// 对于日期字段：
	// YEAR + MONTH + DAY_OF_MONTH
	// YEAR + MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
	// YEAR + MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
	// YEAR + DAY_OF_YEAR
	// YEAR + DAY_OF_WEEK + WEEK_OF_YEAR
	// //////////////////////////////////////////////////////FIELD
	public static final int YEAR = Calendar.YEAR;
	public static final int MONTH = Calendar.MONTH;
	public static final int WEEK_OF_MONTH = Calendar.WEEK_OF_MONTH;
	public static final int DAY = Calendar.DAY_OF_MONTH;
	public static final int HOUR = Calendar.HOUR_OF_DAY;
	public static final int MINUTE = Calendar.MINUTE;
	public static final int SECOND = Calendar.SECOND;
	// //////////////////////////////////////////////////////MONTH
	/** the same of {@link Calendar.JANUARY  } */
	public static final int MONTH_1 = Calendar.JANUARY;
	/** the same of {@link Calendar.FEBRUARY } */
	public static final int MONTH_2 = Calendar.FEBRUARY;
	/** the same of {@link Calendar.MARCH    } */
	public static final int MONTH_3 = Calendar.MARCH;
	/** the same of {@link Calendar.APRIL    } */
	public static final int MONTH_4 = Calendar.APRIL;
	/** the same of {@link Calendar.MAY      } */
	public static final int MONTH_5 = Calendar.MAY;
	/** the same of {@link Calendar.JUNE     } */
	public static final int MONTH_6 = Calendar.JUNE;
	/** the same of {@link Calendar.JULY     } */
	public static final int MONTH_7 = Calendar.JULY;
	/** the same of {@link Calendar.AUGUST   } */
	public static final int MONTH_8 = Calendar.AUGUST;
	/** the same of {@link Calendar.SEPTEMBER} */
	public static final int MONTH_9 = Calendar.SEPTEMBER;
	/** the same of {@link Calendar.OCTOBER  } */
	public static final int MONTH_10 = Calendar.OCTOBER;
	/** the same of {@link Calendar.NOVEMBER } */
	public static final int MONTH_11 = Calendar.NOVEMBER;
	/** the same of {@link Calendar.DECEMBER } */
	public static final int MONTH_12 = Calendar.DECEMBER;
	// //////////////////////////////////////////////////////WEEK
	/** 星期一 */
	public static final int WEEK_1 = Calendar.MONDAY;
	/** 星期二 */
	public static final int WEEK_2 = Calendar.TUESDAY;
	/** 星期三 */
	public static final int WEEK_3 = Calendar.WEDNESDAY;
	/** 星期四 */
	public static final int WEEK_4 = Calendar.THURSDAY;
	/** 星期五 */
	public static final int WEEK_5 = Calendar.FRIDAY;
	/** 星期六 */
	public static final int WEEK_6 = Calendar.SATURDAY;
	/** 星期日 */
	public static final int WEEK_7 = Calendar.SUNDAY;

	/**
	 * 获取默认格式的日期中的某一个项，年、月、月中的第几星期、日、小时、分钟、秒。<br>
	 * 注：如果想获取星期几应使用{@link #getDayOfWeek(String)}
	 * 
	 * @param defaultformatteddate
	 *            默认格式{@link #FORMAT_DEFAULT}的日期字符串
	 * @param field
	 *            {@link #YEAR}/{@link #MONTH}/{@link #WEEK_OF_MONTH}/
	 *            {@link #DAY}/{@link #HOUR}/{@link #MINUTE}/{@link #SECOND}/
	 *            {@link Calendar}.XXX
	 * @return 该项的值，注意：如果{@code field}为{@link #MONTH}时返回值是常量{@link #MONTH_1}~
	 *         {@link #MONTH_12}，而值为{@value #MONTH_1}~{@value #MONTH_12}
	 * @throws ParseException
	 *             如果{@code defaultformatteddate}不符合默认格式则抛异常
	 * 
	 * @see Calendar#get(int)
	 * @see #MONTH_1
	 * @see #MONTH_2
	 * @see #MONTH_3
	 * @see #MONTH_4
	 * @see #MONTH_5
	 * @see #MONTH_6
	 * @see #MONTH_7
	 * @see #MONTH_8
	 * @see #MONTH_9
	 * @see #MONTH_10
	 * @see #MONTH_11
	 * @see #MONTH_12
	 */
	public static int get(String defaultformatteddate, int field)
			throws ParseException {
		Calendar day = Calendar.getInstance();
		// day.setFirstDayOfWeek(Calendar.MONDAY);
		day.setTime(parseDefault(defaultformatteddate));
		int n = day.get(field);
		// return field == MONTH ? (n + 1) : (field == WEEK ? (n == 1 ? 7 : n -
		// 1)
		// : n);
		return n;
	}

	/**
	 * 获取当前时间的某些项
	 * 
	 * @param field
	 * @return
	 * @see #get(String, int)
	 */
	public static int get(int field) {
		Calendar day = Calendar.getInstance();
		return day.get(field);
	}

	/**
	 * 获取默认格式的日期是星期几
	 * 
	 * @param defaultformatteddate
	 * @return 注意返回值并非1代表星期一，请看常量{@link #WEEK_1} ~ {@link #WEEK_7}
	 * @throws ParseException
	 * 
	 * @see #WEEK_1
	 * @see #WEEK_2
	 * @see #WEEK_3
	 * @see #WEEK_4
	 * @see #WEEK_5
	 * @see #WEEK_6
	 * @see #WEEK_7
	 */
	public static int getDayOfWeek(String defaultformatteddate)
			throws ParseException {
		Calendar day = Calendar.getInstance();
		// day.setFirstDayOfWeek(Calendar.MONDAY);
		day.setTime(parseDefault(defaultformatteddate));
		return day.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 获取今天是星期几
	 * 
	 * @param defaultformatteddate
	 * @return 注意返回值并非1代表星期一，请看常量{@link #WEEK_1} ~ {@link #WEEK_7}
	 * @throws ParseException
	 * 
	 * @see #WEEK_1
	 * @see #WEEK_2
	 * @see #WEEK_3
	 * @see #WEEK_4
	 * @see #WEEK_5
	 * @see #WEEK_6
	 * @see #WEEK_7
	 */
	public static int getDayOfWeek() {
		Calendar day = Calendar.getInstance();
		// day.setFirstDayOfWeek(Calendar.MONDAY);
		return day.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 改变默认格式的日期的某个项
	 * 
	 * @param defaultformatteddate
	 * @param field
	 * @param value
	 * @return 修改后的默认格式日期字符串
	 * @throws ParseException
	 */
	public static String set(String defaultformatteddate, int field, int value)
			throws ParseException {
		Calendar day = Calendar.getInstance();
		day.setTime(parseDefault(defaultformatteddate));
		day.set(field, value);
		return formatDefault(day.getTime());
	}

	/**
	 * 获得此刻并修改后的默认格式日期字符串
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public static String set(int field, int value) {
		Calendar day = Calendar.getInstance();
		day.set(field, value);
		return formatDefault(day.getTime());
	}

	/**
	 * 对日期的加减法，例如20120901010101加1天就是20120902010101，加-1天就是20120831010101
	 * 
	 * @param theDay
	 * @param field
	 * @param value
	 * @return
	 * @throws ParseException
	 */
	public static String add(String theDay, int field, int value)
			throws ParseException {
		Calendar day = Calendar.getInstance();
		day.setTime(parseDefault(theDay));
		day.add(field, value);
		return formatDefault(day.getTime());
	}

	/**
	 * 对此刻做加减法
	 * 
	 * @param field
	 * @param value
	 * @return
	 */
	public static String add(int field, int value) {
		Calendar day = Calendar.getInstance();
		day.add(field, value);
		return formatDefault(day.getTime());
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		try {

			System.out.println(DateUtil.computeDurationMillis("20120926010203",
					"20120927010203", FORMAT_DEFAULT));
			long ms = DateUtil.computeDurationMillis("20120926000000",
					"20130926000000", FORMAT_DEFAULT);
			System.out.println("day " + ms / 1000 / 60 / 60 / 24);

			// System.out.println(addDay("20120125000000",10));//20120204000000
			// //20120204000000
			// System.out.println(addDay("20120225000000",10));//20120306000000
			// //20120306000000
			// System.out.println(addDay("20120325000000",10));//20120404000000
			// //20120404000000
			// System.out.println(addDay("20120425000000",10));//20120505000000
			// //20120505000000
			// System.out.println(addDay("20120525000000",10));//20120604000000
			// //20120604000000
			// System.out.println(addDay("20120625000000",10));//20120705000000
			// //20120705000000
			// System.out.println(addDay("20120725000000",10));//20120804000000
			// //20120804000000
			// System.out.println(addDay("20120825000000",10));//20120904000000
			// //20120904000000
			// System.out.println(addDay("20120925000000",10));//20121005000000
			// //20121005000000
			// System.out.println(addDay("20121025000000",10));//20121104000000
			// //20121104000000
			// System.out.println(addDay("20121125000000",10));//20121205000000
			// //20121205000000
			// System.out.println(addDay("20121225000000",10));//20130104000000
			// //20130104000000

			System.out.println("---------------------------------add");
			System.out.println(add("20120125000000", Calendar.YEAR, 1)); // 20130125000000
			System.out.println(add("20120125000000", Calendar.MONTH, 1)); // 20120225000000
			System.out.println(add("20120125000000", Calendar.WEEK_OF_YEAR, 1)); // 20120201000000即加了7天
			System.out
					.println(add("20120125000000", Calendar.WEEK_OF_MONTH, 1)); // 20120201000000
			System.out.println(add("20120125000000", Calendar.DAY_OF_YEAR, 10)); // 20120204000000
			System.out
					.println(add("20120125000000", Calendar.DAY_OF_MONTH, 10)); // 20120204000000
			System.out.println(add("20120125000000", Calendar.HOUR_OF_DAY, 1)); // 20120125010000
			System.out.println(add("20120125000000", Calendar.MINUTE, 61)); // 20120125010100
			System.out.println(add("20120125000000", Calendar.SECOND, 61)); // 20120125000101
			System.out
					.println(add("20120125000000", Calendar.MILLISECOND, 1001)); // 20120125000001

			System.out.println("---------------------------------set");
			System.out.println(set("20120125000000", DAY, 1));
			System.out.println(set("20120125000000", HOUR, 1));
			System.out.println(set("20120125000000", MINUTE, 1));
			System.out.println(set("20120125000000", SECOND, 1));

			System.out.println("---------------------------------get");
			System.out.println(get("20120125010203", YEAR));
			System.out.println(get("20120125010203", MONTH));
			System.out.println(get("20120125010203", DAY));
			System.out.println(get("20120125010203", HOUR));
			System.out.println(get("20120125010203", MINUTE));
			System.out.println(get("20120125010203", SECOND));

			System.out.println("---------------------------------week");
			System.out.println(get("20120924010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120925010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120926010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120927010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120928010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120929010203", Calendar.DAY_OF_WEEK));
			System.out.println(get("20120930010203", Calendar.DAY_OF_WEEK));

			_printInfo();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	static void _printInfo() {
		Calendar calendar = new GregorianCalendar();
		Date trialTime = new Date();
		calendar.setTime(trialTime);
		// print out a bunch of interesting things
		System.out.println("----------------printInfo-----------------");
		System.out.println("ERA: " + calendar.get(Calendar.ERA));
		System.out.println("YEAR: " + calendar.get(Calendar.YEAR));
		System.out.println("MONTH: " + calendar.get(Calendar.MONTH));
		System.out.println("WEEK_OF_YEAR: "
				+ calendar.get(Calendar.WEEK_OF_YEAR));
		System.out.println("WEEK_OF_MONTH: "
				+ calendar.get(Calendar.WEEK_OF_MONTH));
		System.out.println("DATE: " + calendar.get(Calendar.DATE));
		System.out.println("DAY_OF_MONTH: "
				+ calendar.get(Calendar.DAY_OF_MONTH));
		System.out
				.println("DAY_OF_YEAR: " + calendar.get(Calendar.DAY_OF_YEAR));
		System.out
				.println("DAY_OF_WEEK: " + calendar.get(Calendar.DAY_OF_WEEK));
		System.out.println("DAY_OF_WEEK_IN_MONTH: "
				+ calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
		System.out.println("AM_PM: " + calendar.get(Calendar.AM_PM));
		System.out.println("HOUR: " + calendar.get(Calendar.HOUR));
		System.out
				.println("HOUR_OF_DAY: " + calendar.get(Calendar.HOUR_OF_DAY));
		System.out.println("MINUTE: " + calendar.get(Calendar.MINUTE));
		System.out.println("SECOND: " + calendar.get(Calendar.SECOND));
		System.out
				.println("MILLISECOND: " + calendar.get(Calendar.MILLISECOND));
		System.out.println("ZONE_OFFSET: "
				+ (calendar.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000)));
		System.out.println("DST_OFFSET: "
				+ (calendar.get(Calendar.DST_OFFSET) / (60 * 60 * 1000)));
		System.out.println("Current Time, with hour reset to 3");
		calendar.clear(Calendar.HOUR_OF_DAY); // so doesn't override
		calendar.set(Calendar.HOUR, 3);
		System.out.println("ERA: " + calendar.get(Calendar.ERA));
		System.out.println("YEAR: " + calendar.get(Calendar.YEAR));
		System.out.println("MONTH: " + calendar.get(Calendar.MONTH));
		System.out.println("WEEK_OF_YEAR: "
				+ calendar.get(Calendar.WEEK_OF_YEAR));
		System.out.println("WEEK_OF_MONTH: "
				+ calendar.get(Calendar.WEEK_OF_MONTH));
		System.out.println("DATE: " + calendar.get(Calendar.DATE));
		System.out.println("DAY_OF_MONTH: "
				+ calendar.get(Calendar.DAY_OF_MONTH));
		System.out
				.println("DAY_OF_YEAR: " + calendar.get(Calendar.DAY_OF_YEAR));
		System.out
				.println("DAY_OF_WEEK: " + calendar.get(Calendar.DAY_OF_WEEK));
		System.out.println("DAY_OF_WEEK_IN_MONTH: "
				+ calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
		System.out.println("----------------printInfo-----------------");
	}
}
