package com.koala.game.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * 统一的日志服务。保存的方式和位置可以多变，有可能本地文件有可能传输到其它专用日志服务器<br>
 * 需要导入lib： slf4j-api-1.7.0.jar;slf4j-ext-1.7.0.jar;slf4j-xxx-1.7.0.jar
 * 
 * @author AHONG
 * 
 */
public final class KGameLogger implements Logger {

	private Logger logger;

	private KGameLogger(Logger logger) {
		this.logger = logger;
	}

	public static KGameLogger getLogger(String loggerName) {
		Logger logger = LoggerFactory.getLogger(loggerName);
		return new KGameLogger(logger);
	}

	public static KGameLogger getLogger(Class<?> clazz) {
		Logger logger = LoggerFactory.getLogger(clazz);
		return new KGameLogger(logger);
	}
	
	public void printSwitch(){
		StringBuilder sb = new StringBuilder("【Logger Switch】");
		sb.append("\nisDebugEnabled ").append(isDebugEnabled());
		sb.append("\nisTraceEnabled ").append(isTraceEnabled());
		sb.append("\nisInfoEnabled ").append(isInfoEnabled());
		sb.append("\nisWarnEnabled ").append(isWarnEnabled());
		sb.append("\nisErrorEnabled ").append(isErrorEnabled());
		System.out.println(sb);
	}

//////////////////////////////////////////////////////////////////////////////////

	public String getName() {
		return logger.getName();
	}

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public void trace(String msg) {
		logger.trace(msg);
	}

	public void trace(String format, Object arg) {
		logger.trace(format, arg);
	}

	public void trace(String format, Object arg1, Object arg2) {
		logger.trace(format, arg1, arg2);
	}

	public void trace(String format, Object... arguments) {
		logger.trace(format, arguments);
	}

	public void trace(String msg, Throwable t) {
		logger.trace(msg, t);
	}

	public boolean isTraceEnabled(Marker marker) {
		return logger.isTraceEnabled(marker);
	}

	public void trace(Marker marker, String msg) {
		logger.trace(marker, msg);
	}

	public void trace(Marker marker, String format, Object arg) {
		logger.trace(marker, format, arg);
	}

	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		logger.trace(marker, format, arg1, arg2);
	}

	public void trace(Marker marker, String format, Object... argArray) {
		logger.trace(marker, format, argArray);
	}

	public void trace(Marker marker, String msg, Throwable t) {
		logger.trace(marker, msg, t);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public void debug(String msg) {
		logger.debug(msg);
	}

	public void debug(String format, Object arg) {
		logger.debug(format, arg);
	}

	public void debug(String format, Object arg1, Object arg2) {
		logger.debug(format, arg1, arg2);
	}

	public void debug(String format, Object... arguments) {
		logger.debug(format, arguments);
	}

	public void debug(String msg, Throwable t) {
		logger.debug(msg, t);
	}
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		logger.debug(marker, format, arg1, arg2);
	}
	public boolean isDebugEnabled(Marker marker) {
		return logger.isDebugEnabled(marker);
	}

	public void debug(Marker marker, String msg) {
		logger.debug(marker, msg);
	}

	public void debug(Marker marker, String format, Object arg) {
		logger.debug(marker, format, arg);
	}

	public void debug(Marker marker, String format, Object... arguments) {
		logger.debug(marker, format, arguments);
	}

	public void debug(Marker marker, String msg, Throwable t) {
		logger.debug(marker, msg, t);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void info(String format, Object arg) {
		logger.info(format, arg);
	}

	public void info(String format, Object arg1, Object arg2) {
		logger.info(format, arg1, arg2);
	}

	public void info(String format, Object... arguments) {
		logger.info(format, arguments);
	}

	public void info(String msg, Throwable t) {
		logger.info(msg, t);
	}

	public boolean isInfoEnabled(Marker marker) {
		return logger.isInfoEnabled(marker);
	}

	public void info(Marker marker, String msg) {
		logger.info(marker, msg);
	}

	public void info(Marker marker, String format, Object arg) {
		logger.info(marker, format, arg);
	}

	public void info(Marker marker, String format, Object arg1, Object arg2) {
		logger.info(marker, format, arg1, arg2);
	}

	public void info(Marker marker, String format, Object... arguments) {
		logger.info(marker, format, arguments);
	}

	public void info(Marker marker, String msg, Throwable t) {
		logger.info(marker, msg, t);
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	public void warn(String msg) {
		logger.warn(msg);
	}

	public void warn(String format, Object arg) {
		logger.warn(format, arg);
	}

	public void warn(String format, Object... arguments) {
		logger.warn(format, arguments);
	}

	public void warn(String format, Object arg1, Object arg2) {
		logger.warn(format, arg1, arg2);
	}

	public void warn(String msg, Throwable t) {
		logger.warn(msg, t);
	}

	public boolean isWarnEnabled(Marker marker) {
		return logger.isWarnEnabled(marker);
	}

	public void warn(Marker marker, String msg) {
		logger.warn(marker, msg);
	}

	public void warn(Marker marker, String format, Object arg) {
		logger.warn(marker, format, arg);
	}

	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		logger.warn(marker, format, arg1, arg2);
	}

	public void warn(Marker marker, String format, Object... arguments) {
		logger.warn(marker, format, arguments);
	}

	public void warn(Marker marker, String msg, Throwable t) {
		logger.warn(marker, msg, t);
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public void error(String msg) {
		logger.error(msg);
	}

	public void error(String format, Object arg) {
		logger.error(format, arg);
	}

	public void error(String format, Object arg1, Object arg2) {
		logger.error(format, arg1, arg2);
	}

	public void error(String format, Object... arguments) {
		logger.error(format, arguments);
	}

	public void error(String msg, Throwable t) {
		logger.error(msg, t);
	}

	public boolean isErrorEnabled(Marker marker) {
		return logger.isErrorEnabled(marker);
	}

	public void error(Marker marker, String msg) {
		logger.error(marker, msg);
	}

	public void error(Marker marker, String format, Object arg) {
		logger.error(marker, format, arg);
	}

	public void error(Marker marker, String format, Object arg1, Object arg2) {
		logger.error(marker, format, arg1, arg2);
	}

	public void error(Marker marker, String format, Object... arguments) {
		logger.error(marker, format, arguments);
	}

	public void error(Marker marker, String msg, Throwable t) {
		logger.error(marker, msg, t);
	}

	//
	// public enum LogLevel {
	// /**
	// * 'DEBUG' log level.
	// */
	// DEBUG,
	// /**
	// * 'INFO' log level.
	// */
	// INFO,
	// /**
	// * 'WARN' log level.
	// */
	// WARN,
	// /**
	// * 'ERROR' log level.
	// */
	// ERROR;
	// }
	//
	// public void debug(String msg) {
	// logger.debug(msg);
	// }
	//
	// public void debug(String msg, Throwable cause) {
	// logger.debug(msg, cause);
	// }
	//
	// public void error(String msg) {
	// logger.error(msg);
	// }
	//
	// public void error(String msg, Throwable cause) {
	// logger.error(msg, cause);
	// }
	//
	// public void info(String msg) {
	// logger.info(msg);
	// }
	//
	// public void info(String msg, Throwable cause) {
	// logger.info(msg, cause);
	// }
	//
	// public boolean isDebugEnabled() {
	// return logger.isDebugEnabled();
	// }
	//
	// public boolean isErrorEnabled() {
	// return logger.isErrorEnabled();
	// }
	//
	// public boolean isInfoEnabled() {
	// return logger.isInfoEnabled();
	// }
	//
	// public boolean isWarnEnabled() {
	// return logger.isWarnEnabled();
	// }
	//
	// public void warn(String msg) {
	// logger.warn(msg);
	// }
	//
	// public void warn(String msg, Throwable cause) {
	// logger.warn(msg, cause);
	// }
	//
	// @Override
	// public String toString() {
	// return String.valueOf(logger.getName());
	// }
	// public boolean isEnabled(LogLevel level) {
	// switch (level) {
	// case DEBUG:
	// return isDebugEnabled();
	// case INFO:
	// return isInfoEnabled();
	// case WARN:
	// return isWarnEnabled();
	// case ERROR:
	// return isErrorEnabled();
	// default:
	// throw new Error();
	// }
	// }
	//
	// public void log(LogLevel level, String msg, Throwable cause) {
	// switch (level) {
	// case DEBUG:
	// debug(msg, cause);
	// break;
	// case INFO:
	// info(msg, cause);
	// break;
	// case WARN:
	// warn(msg, cause);
	// break;
	// case ERROR:
	// error(msg, cause);
	// break;
	// default:
	// throw new Error();
	// }
	// }
	//
	// public void log(LogLevel level, String msg) {
	// switch (level) {
	// case DEBUG:
	// debug(msg);
	// break;
	// case INFO:
	// info(msg);
	// break;
	// case WARN:
	// warn(msg);
	// break;
	// case ERROR:
	// error(msg);
	// break;
	// default:
	// throw new Error();
	// }
	// }

}
