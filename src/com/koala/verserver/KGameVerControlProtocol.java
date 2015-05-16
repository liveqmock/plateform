package com.koala.verserver;

/**
 * <pre>
 * 客户端请求采用HTTP GET方法
 * 携带参数范例：pid=1&appver=1.2&sover=11&resver=99&sign=jh3jlsd234jl42l3j
 * 【注】sign的生成采用MD5加密，MD5("pid=1&appver=1.2&sover=11&resver=99&key=kola")
 * 
 * --------------------------------------
 * 响应PL: <b>xml</b>
 * </pre>
 * 
 * @author AHONG
 * 
 */
public interface KGameVerControlProtocol {

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>客户端请求的参数KEY
	public static final String PKEY_PLATFORM = "p";
	public static final String PKEY_PROMOID = "pid";
	public static final String PKEY_APPVER = "appver";
	public static final String PKEY_SOVER = "sover";
	public static final String PKEY_RESVER = "resver";
	public static final String PKEY_SIGN = "sign";
	public static final String SIGN_KEY = "kola";

	/** PL 版本检测结果-最新,无须更新 */
	public final static int PL_VERCHECK_NEWEST = 0;
	/** PL 版本检测结果-需要更新APK */
	public final static int PL_VERCHECK_UPDATE_APK = 1;
	/** PL 版本检测结果-需要更新APK，通知客户端由系统系在，而不是在应用内下载，主要是防止客户端出现问题而采取的紧急处理 办法 */
	public final static int PL_VERCHECK_UPDATE_APK_SYS = 4;
	/** PL 版本检测结果-需要更新资源包 */
	public final static int PL_VERCHECK_UPDATE_RESPACKS = 2;
	/** PL 版本检测结果-客户端参数错误 */
	public final static int PL_VERCHECK_PARAM_ERROR = 3;
	/** PL 版本检测结果-客户端签名错误 */
	public final static int PL_VERCHECK_SIGN_ERROR = 5;
	/** PL 版本检测结果-找不到对应渠道的更新包 */
	public final static int PL_VERCHECK_NOTFOUND = 6;

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>服务器响应的XML方式
	public final static String XML_TAG_ROOT = "VerCheckResult";
	public final static String XML_TAG_RESPONSECODE = "responsecode";
	public final static String XML_TAG_TIPS = "tips";
	
	public final static String XML_TAG_APK = "apk";
	public final static String XML_TAG_APPVER = PKEY_APPVER;
	public final static String XML_TAG_FILESIZE = "filesize";
	public final static String XML_TAG_DOWNURL = "downurl";
	public final static String XML_TAG_MD5 = "md5";
	
	public final static String XML_TAG_FILENAME = "name";
	
	public final static String XML_TAG_RESPATCH = "patch";
	public final static String XML_TAG_RESVER =PKEY_RESVER;
	
	public final static String XML_TAG_SOPATCH = "sopatch";
	public final static String XML_TAG_SOVER =PKEY_SOVER;
	
	public final static String XML_TAG_FE = "fe";
	public final static String XML_TAG_FE_IP = "ip";
	public final static String XML_TAG_FE_PORT = "port";
	
	public final static String XML_TAG_NOTIFICATION = "Notification";
	public final static String XML_TAG_NOTIFICATION_NOTICE = "notice";
	public final static String XML_TAG_NOTIFICATION_MSG = "msg";
	public final static String XML_TAG_NOTIFICATION_YEAR = "year";
	public final static String XML_TAG_NOTIFICATION_MONTH= "month";
	public final static String XML_TAG_NOTIFICATION_DAY = "day";
	public final static String XML_TAG_NOTIFICATION_HOUR = "hour";
	public final static String XML_TAG_NOTIFICATION_MINUTE = "min";
	public final static String XML_TAG_NOTIFICATION_REPEATTYPE = "repeat";
	
	public final static String XML_TAG_SERVER_CONFIG = "ServerConfig";
}
