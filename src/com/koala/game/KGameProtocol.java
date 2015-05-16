package com.koala.game;

/**
 * 平台级的消息协议。<br>
 * 常量命名的约定：<br>
 * (1)<b>{@code 'MID_'}</b>开头代表‘msgID’<br>
 * (2)<b>{@code 'PL_'}</b>开头代表某条消息里面的Payload中的常量定义，例如<strong>
 * {@code PL_LOGIN_SUCCEED}</strong>表示登录结果中的‘成功’标识
 * 
 * @author AHONG
 * 
 */
public interface KGameProtocol {
	
	/**
	 * 跨服消息，20131209
	 * <pre>
	 * int 目标GS的ID;
	 * byte[] 待传递的KGameMessage转换成的字节数组
	 * </pre>
	 */
	public final static int MID_CROSS_SERVER_MSG = -1;

	/** 心跳消息 pl(long curms) */
	public final static int MID_PING = 0;// ping

	/**
	 * 握手消息
	 * 
	 * <pre>
	 * REQUEST:
	 *   String code;//密匙，如果不符合 服务器将判断为非法客户端并断开链接
	 *   String clientmodel;
	 * RESPONSE:
	 *   String serverinfo;//发回一些服务器信息（预留）
	 *   
	 * 【注：内部通信的HANDSHAKE内容是完全不同的，通过ClientType判断，游戏逻辑无须理会】
	 * </pre>
	 */
	public final static int MID_HANDSHAKE = 1;// handshake

	/**
	 * 系统公告，客户端收到此消息必须放到最高优先级处理，例如停服前的通知 <br>
	 * <b>服务器主动PUSH给客户端的消息，内容只有一个String</b>
	 */
	public final static int MID_SYSTEM_NOTICE = 4;

	/**
	 * S2C 通知客户端连接已经被断开 <br>
	 * PL：int cause = {@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #CAUSE_PLAYEROUT_KICK}/
	 * {@link #CAUSE_PLAYEROUT_SERVERSHUTDOWN}/{@link #CAUSE_PLAYEROUT_OVERLAP};
	 * String tips;
	 */
	public final static int MID_DISCONNECT = 5;
	/** 服务异常或一些未知错误 */
	public final static int PL_UNKNOWNEXCEPTION_OR_SERVERBUSY = -1;
	/** 如果需要先握手但未通过握手验证的消息返回此结果 */
	public final static int PL_ILLEGAL_SESSION_OR_MSG = -2;
	/** 服务器产生异常,通知客户端的消息 pl(int code;String info) */
	public final static int MID_EXCEPTION = 96;
	/** 服务器状态,各种客户端(WEB/GM/PHONE...)获取服务器当前状态 pl(string) */
	public final static int MID_SERVERSTATUS = 97;
	/** DEBUG消息 */
	public final static int MID_DEBUG = 98;
	/** 关闭服务器的指令,由关机程序发送,加密 */
	public final static int MID_SHUTDOWN = 99;

	// 版本验证/////////////////////////////////////////////////////////////
	/**
	 * 版本检测（FE）- 【20130611更新了协议】【20130724更新了协议添加md5】
	 * 
	 * <pre>
	 * 请求PL:
	 *   int promoid;  //渠道ID
	 *   String appVer;//应用版本号（客户端和服务器协商自定义）
	 *   int resVer;//资源版本号（必须为整型值）
	 * 响应PL:
	 *   int responsecode = 
	 *     {@link #PL_VERCHECK_NEWEST}/ 
	 *     {@link #PL_VERCHECK_UPDATE_APK}/
	 *     {@link #PL_VERCHECK_UPDATE_APK_SYS}/
	 *     {@link #PL_VERCHECK_UPDATE_RESPACKS}/
	 *     {@link #PL_VERSIONFORMAT_ERROR}/
	 *     {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   String tips;//版本更新结果提示
	 *   if({@link #PL_VERCHECK_UPDATE_APK}){
	 *     String lastestAppVer; //当前最新版本号
	 *     long apkfilesize;     //apk文件大小
	 *     String apkdownloadurl;//下载地址
	 *     String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
	 *   }
	 *   else if({@link #PL_VERCHECK_UPDATE_RESPACKS}){
	 *     int updatefileN;//需要更新的文件数量
	 *     for(updatefileN){
	 *       String filename;//文件名
	 *       int patchver;//版本号,为整型值
	 *       long filesize;//文件大小
	 *       String downloadurl;//HTTP下载地址
	 *       String md5;//20130724每个文件增加md5校验码，发给客户端自己对比
	 *     }
	 *   }
	 * </pre>
	 */
	public final static int MID_VERCHECK = 100;
	/** PL 版本检测结果-最新,无须更新 */
	public final static int PL_VERCHECK_NEWEST = 0;
	/** PL 版本检测结果-需要更新APK */
	public final static int PL_VERCHECK_UPDATE_APK = 1;
	/** PL 版本检测结果-需要更新APK，通知客户端由系统系在，而不是在应用内下载，主要是防止客户端出现问题而采取的紧急处理 办法 */
	public final static int PL_VERCHECK_UPDATE_APK_SYS = 4;
	/** PL 版本检测结果-需要更新资源包 */
	public final static int PL_VERCHECK_UPDATE_RESPACKS = 2;
	/** PL 版本检测结果-客户端版本号格式错误 */
	public final static int PL_VERSIONFORMAT_ERROR = 3;

	// /账号登录、注册、密码服务等////////////////////////////////////////////////
	/**
	 * 做账号验证（FE）
	 * 
	 * <pre>
	 * 请求PL:
	 * String pName; 
	 * String pPassword
	 * 响应PL:
	 * String pName;
	 * int responsecode =
	 * {@link #PL_PASSPORT_VERIFY_SUCCEED} / 
	 * {@link #PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST} /
	 * {@link #PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 * String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 * 注意：如果responsecode==PL_PASSPORT_VERIFY_SUCCEED，后面将携带服务器列表数据
	 * </pre>
	 */
	public final static int MID_PASSPORT_VERIFY = 200;

	/**
	 * 通过接入第三方推广渠道SDK的账号验证
	 * 
	 * <pre>
	 * 【REQUEST】:
	 *  int promoID;//推广渠道ID（我们自己定义好的不能变的）
	 *  int paramN;//参数KV对数量
	 *  for(paramN){
	 *    String key;//参数KEY，根据不同渠道而定，定义请看协议文件中PROMO_KEY_???_???
	 *    String value;//参数值，根据不同渠道而定
	 *  }
	 * 【RESPONSE】:
	 *  int responsecode =
	 *  {@link #PL_PASSPORT_VERIFY_SUCCEED} / 
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST} /
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG}/
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_BAN}/
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND}/
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG}/
	 *  {@link #PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR}/
	 *  {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *  String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 *  if(responsecode==PL_PASSPORT_VERIFY_SUCCEED){
	 *    String kName;//我们自己的kola账号（客户端要保留真正登录GS时用）
	 *    String kPassword;//我们自己的kola密码（客户端要保留真正登录GS时用）
	 *    --------------------------
	 *    //20130710新增附加返回参数KV形式
	 *    int paramN;
	 *    for(paramN){
	 *      String key;//具体看常量PROMO_KEY_???
	 *      String value;
	 *    }
	 *    --------------------------
	 *    【后面携带服务器列表数据】
	 *    --------------------------
	 *  }
	 * </pre>
	 */
	public final static int MID_PASSPORT_VERIFY_BY_PROMOCHANNEL = 201;
	/** 账号密码验证成功 */
	public final static int PL_PASSPORT_VERIFY_SUCCEED = 0;
	/** 账号密码验证失败-用户名不存在 */
	public final static int PL_PASSPORT_VERIFY_FAILED_NAMENOTEXIST = 1;
	/** 账号密码验证失败-密码错误 */
	public final static int PL_PASSPORT_VERIFY_FAILED_PASSWORDISWRONG = 2;
	/** 账号密码验证失败-封号中 */
	public final static int PL_PASSPORT_VERIFY_FAILED_BAN = 3;
	public final static int PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELNOTFOUND = 4;
	public final static int PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELPARAMSWRONG = 5;
	public final static int PL_PASSPORT_VERIFY_FAILED_PROMOCHANNELSERVERERROR = 6;

	/**
	 * 登录到服务器（GS）
	 * 
	 * <pre>
	 * 请求PL:
	 * String pName; 
	 * String pPassword
	 * 响应PL:
	 * int responsecode = 
	 * {@link #PL_LOGIN_SUCCEED}/
	 * {@link #PL_LOGIN_FAILED_PASSWORDISWRONG}/
	 * {@link #PL_LOGIN_FAILED_IDNOTEXIST}/
	 * {@link #PL_LOGIN_FAILED_NAMENOTEXIST}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY};
	 * long passportID;//if(responsecode==PL_LOGIN_SUCCEED)
	 * String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 * </pre>
	 */
	public final static int MID_LOGIN_BY_NAME = 210;
	/**
	 * @deprecated 暂时不用
	 */
	public final static int MID_LOGIN_BY_ID = 211;
	/** 登录成功 */
	public final static int PL_LOGIN_SUCCEED = 0;
	/** 登录失败-密码错误 */
	public final static int PL_LOGIN_FAILED_PASSWORDISWRONG = 1;
	/** 登录失败-账号ID不存在 */
	public final static int PL_LOGIN_FAILED_IDNOTEXIST = 2;
	/** 登录失败-账号名不存在 */
	public final static int PL_LOGIN_FAILED_NAMENOTEXIST = 3;

	/**
	 * 注册新账号（FE）
	 * 
	 * <pre>
	 * 请求PL:String registername;
	 * String registerpassword;
	 * String registerremark;//20130806这里内容定义promoID（无自身登录SDK的渠道）
	 * 响应PL:
	 * int responsecode = 
	 * {@link #PL_REGISTER_SUCCEED}/
	 * {@link #PL_REGISTER_FAILED_DUPLICATIONOFNAME}/
	 * {@link #PL_REGISTER_FAILED_UN_INVALID_LENGTH}/
	 * {@link #PL_REGISTER_FAILED_PASSWORD_INVALID}/
	 * {@link #PL_REGISTER_FAILED_MOBILE_INVALID}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 * long newpassportid;//if(responsecode==PL_REGISTER_SUCCEED)
	 * String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 * </pre>
	 */
	public final static int MID_REGISTER_NEWPASSPORT = 220;

	/**
	 * @deprecated 联运方式，这个已经没用
	 */
	public final static int MID_REGISTER_NEWPASSPORT_AUTO = 221;
	/**
	 * @deprecated 联运方式，全自动注册，这个已经没用
	 */
	public final static int MID_REGISTER_NEWPASSPORT_BY_PROMOCHANNEL = 222;
	public final static int PL_REGISTER_SUCCEED = 0;
	/** 代表用户名已经被使用 */
	public final static int PL_REGISTER_FAILED_DUPLICATIONOFNAME = 1;
	/** 代表用户名长度不符合要求（4 - 20 个字符） */
	public static final int PL_REGISTER_FAILED_UN_INVALID_LENGTH = 2;
	/** 代表密码长度不符合要求（6 - 20 个字符）或密码字段和密码确认字段不匹配。 */
	public static final int PL_REGISTER_FAILED_PASSWORD_INVALID = 3;
	/** 代表手机号码不符合要求（只能为数字，长度为 11 位） */
	public static final int PL_REGISTER_FAILED_MOBILE_INVALID = 4;

	/**
	 * 修改账号密码（FE）
	 * 
	 * <pre>
	 * 请求PL: string pname;string oldpw;string newpw
	 * 响应PL: 
	 * int responsecode = 
	 * {@link #PL_CHANGE_PASSWORD_SUCCEED}/
	 * {@link #PL_CHANGE_PASSWORD_FAILED_PASSWORDISWRONG}/
	 * {@link #PL_CHANGE_PASSWORD_FAILED_IDNOTEXIST}/
	 * {@link #PL_CHANGE_PASSWORD_FAILED_NAMENOTEXIST}/
	 * {@link #PL_CHANGE_PASSWORD_FAILED_NEWPASSWORDINVALID}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 * String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 * </pre>
	 * 
	 * @deprecated 联运方式，这个已经没用
	 */
	public final static int MID_CHANGE_PASSWORD = 230;
	public final static int PL_CHANGE_PASSWORD_SUCCEED = 0;
	public final static int PL_CHANGE_PASSWORD_FAILED_PASSWORDISWRONG = 1;
	public final static int PL_CHANGE_PASSWORD_FAILED_IDNOTEXIST = 2;
	public final static int PL_CHANGE_PASSWORD_FAILED_NAMENOTEXIST = 3;
	public final static int PL_CHANGE_PASSWORD_FAILED_NEWPASSWORDINVALID = 4;

	/**
	 * 忘记密码/取回密码（FE）
	 * 
	 * <pre>
	 * 请求PL: string pname;int questionid;string answer
	 * 响应PL: 
	 * int responsecode = 
	 * {@link #PL_FORGET_PASSWORD_SUCCEED}/
	 * {@link #PL_FORGET_PASSWORD_FAILED_NAMENOTEXIST}/
	 * {@link #PL_FORGET_PASSWORD_FAILED_UNMATCHED}/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 * String passwordgot;//if(responsecode==PL_FORGET_PASSWORD_SUCCEED)
	 * String tips;//20130222添加的提示语，有可能是0长度客户端要注意
	 * </pre>
	 * 
	 * @deprecated 联运方式，这个已经没用
	 */
	public final static int MID_FORGET_PASSWORD = 240;
	public final static int PL_FORGET_PASSWORD_SUCCEED = 0;
	public final static int PL_FORGET_PASSWORD_FAILED_NAMENOTEXIST = 1;
	public final static int PL_FORGET_PASSWORD_FAILED_UNMATCHED = 2;

	// 服务器列表///////////////////////////////////////////////////////////

	/** 游戏服务器GS状态：维护中 */
	public final static byte CONST_GS_STATUS_MAINTENANCE = -1;
	/** 游戏服务器GS状态：启动但未对外开放 */
	public final static byte CONST_GS_STATUS_STARTBUTNOTOPEN = 0;
	/** 游戏服务器GS状态：开放 */
	public final static byte CONST_GS_STATUS_OPEN = 1;
	/** 游戏服务器GS历史：新区 */
	public final static byte CONST_GS_HISTORY_NEW = 0;
	/** 游戏服务器GS历史：老区（正常） */
	public final static byte CONST_GS_HISTORY_OLD = 1;
	/** 游戏服务器GS历史：合区 */
	public final static byte CONST_GS_HISTORY_MERGER = 2;

	public final static int CONST_JOBTYPE_NAN_ZHANSHI = 1;
	public final static int CONST_JOBTYPE_NAN_FASHI = 2;
	public final static int CONST_JOBTYPE_NAN_CIKE = 3;
	public final static int CONST_JOBTYPE_NV_ZHANSHI = 4;
	public final static int CONST_JOBTYPE_NV_FASHI = 5;
	public final static int CONST_JOBTYPE_NV_CIKE = 6;
	
	/**
	 * <pre>
	 * 服务器通知客户端下线
	 * String 原因
	 * </pre>
	 */
	public final static int MID_INFO_CLIENT_OFFLINE = 250;

	/** 客户端请求获取服务器列表（FE） */
	/**
	 * 服务器返回
	 * byte 大区的数量 n
	 *    for(0~n) {
	 *        int 大区id
	 *        String 大区名称
	 *        byte 服务器数量 m
	 *        for(0~m) {
	 *            int 服务器id
	 *            String 服务器标题
	 *            String 服务器颜色值
	 *            String 服务器名字
	 *            String 服务器描述
	 *            byte 服务器状态
	 *            byte 服务器历史
	 *            int 允许的在线人数
	 *            int 当前在线人数
	 *            String 服务器ip
	 *            int 服务器端口
	 *            byte 角色的数量count
	 *            for(0~count) {
	 *                int 角色职业类型
	 *                int 角色等级
	 *            }
	 *        }
	 *    }
	 *    --------------------------
	 *    【后面携带登陆公告】
	 *    --------------------------
	 *    byte 公告的数量
	 *    for(0~n) {
	 *        byte 公告类型（1=公告，2=活动）
	 *        byte 公告状态（0=普通，1=新，2=热门）
	 *        String 公告标题
	 *        String 公告内容
	 *    } 
	 */
	public final static int MID_GET_GSLIST = 300;
	public final static int PL_GET_GSLIST_SUCCEED = 0;

	/**
	 * 向客户端推送一个默认的游戏服务器（FE）<br>
	 * 
	 * <pre>
	 * responsemsg.writeBoolean(b);
	 * if (b) {
	 * 	responsemsg.writeUtf8String(gs.zone.label);
	 * 	responsemsg.writeInt(gs.gsID);
	 * 	responsemsg.writeUtf8String(labelheader);
	 * 	responsemsg.writeUtf8String(labelheaderfontcolor);
	 * 	responsemsg.writeUtf8String(gs.label);
	 * 	responsemsg.writeUtf8String(gs.remark);
	 * 	responsemsg.writeInt(gs.status);
	 * 	responsemsg.writeInt(gs.allowedOnline);
	 * 	responsemsg.writeInt(gs.currentOnline);
	 * 	responsemsg.writeUtf8String(gs.gsIP);
	 * 	responsemsg.writeInt(gs.gsSocketPort);
	 * }
	 * </pre>
	 */
	public final static int MID_DEFAULT_GAMESERVER = 301;
	
	/**
	 * <pre>
	 * 获取登录公告
	 * 
	 * 返回以下信息：
	 * 
	 * byte 登录公告数量
	 * for(0~n) {
	 *     int 登录公告id
	 *     byte 公告类型（1=公告，2=活动）
	 *     byte 公告状态（0=普通，1=新，2=热门）
	 *     String 公告标题
	 *     String 公告内容
	 * }
	 * 
	 * </pre>
	 */
	public final static int MID_GET_LOGIN_NOTICE_LIST = 302;
	
	/**
	 * <pre>
	 * 修改登录公告
	 * 
	 * byte 新增或修改的登录公告数量
	 * for(0~n) {
	 *     int 登录公告id（-1表示新增，否则表示修改）
	 *     byte 公告类型（1=公告，2=活动）
	 *     byte 公告状态（0=普通，1=新，2=热门）
	 *     String 公告标题
	 *     String 公告内容
	 * }
	 * 
	 * byte 删除的登录公告数量
	 * for(0~n) {
	 *     int 登录公告id
	 * }
	 * 
	 * 返回以下信息：
	 * boolean 成功或失败
	 * </pre>
	 */
	public final static int MID_UPDATE_LOGIN_NOTICE = 303;
	
	/**
	 * <pre>
	 * 维护管理系统获取FE服务器列表
	 * int 服务器数量
	 * for(0~n) {
	 *     int 服务器id
	 *     String 服务器名称
	 *     byte 服务器状态
	 * }
	 * </pre>
	 */
	public final static int MID_GET_GS_LIST_FOR_MAINTENANCE_SYSTEM = 304;
	
	/**
	 * <pre>
	 * 通知所有服务器进入维护状态
	 * </pre>
	 */
	public final static int MID_CHG_SERVER_TO_MAINTENANCE_STATUS = 305;
	
	/**
	 * <pre>
	 * 通知所有服务器进入开放状态
	 * </pre>
	 */
	public final static int MID_CHG_SERVER_TO_OPEN_STATUS = 306;
	
	/**
	 * <pre>
	 * 更新服务器状态
	 * int 服务器id
	 * int 状态
	 * </pre>
	 */
	public final static int MID_UPDATE_STATUS_OF_SERVER = 307;
	
	/**
	 * <pre>
	 * 获取所有服务器的状态
	 * 客户端不需要发送任何信息
	 * 服务器返回：
	 * int server数量
	 * for(0~n) {
	 * 	   int 大区id
	 *     int serverId
	 *     byte server状态
	 * }
	 * </pre>
	 */
	public final static int MID_GET_STATUS_OF_ALL_SERVER = 308;

	/**
	 * 选择某个游戏服务器GS并进入（FE）
	 * 
	 * <pre>
	 * 请求PL: int selectedGSID
	 * 响应PL: int responsecode = 
	 * {@link #PL_SELECT_GS_SUCCEED} 成功/
	 * {@link #PL_SELECT_GS_FAILED_UNOPENED} 维护中或未开放/
	 * {@link #PL_SELECT_GS_FAILED_REJECTED} 直接拒绝了/
	 * {@link #PL_SELECT_GS_FAILED_QUEUE} 要排队/
	 * {@link #PL_SELECT_GS_FAILED_OPENSOON} 即将开放（白名单的是可以进入的）/
	 * {@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 * String tips;//20130628增加
	 * if(responsecode=={@link #PL_SELECT_GS_SUCCEED}){
	 *   String gsip;
	 *   int gsport;
	 * }
	 * if(responsecode=={@link #PL_SELECT_GS_FAILED_QUEUE}){
	 *   int index;//排队排第几位
	 *   int avgpolltimeseconds;//每个排位平均等待时间s
	 * }
	 * </pre>
	 */
	public final static int MID_SELECT_GS = 310;// 选择GS，返回有可能是成功/失败/排队
	public final static int PL_SELECT_GS_SUCCEED = 0;
	public final static int PL_SELECT_GS_FAILED_UNOPENED = 1;// GS维护中的时候
	public final static int PL_SELECT_GS_FAILED_REJECTED = 2;// 拒绝连接，这种情况是找不到对应的GS
	public final static int PL_SELECT_GS_FAILED_QUEUE = 3;// 要排队，排队有排队信息
	public final static int PL_SELECT_GS_FAILED_OPENSOON = 4;// 即将开放（白名单的是可以进入的）

	/**
	 * 客户端主动放弃排队（FE）
	 * 
	 * <pre>
	 * 响应PL：跟{@link #MID_GET_GSLIST}一样
	 * </pre>
	 * 
	 * @see #MID_GET_GSLIST
	 */
	public final static int MID_CANCEL_LOGINQUEUE = 320;

	// // 选择角色////////////////////////////////////////////////////////////
	// public final static int MID_SELECTROLE_AND_ENTERGAME = 400;
	// public final static int MID_CREATE_NEW_ROLE = 410;
	// public final static int MID_DELETE_ROLE = 420;

	/** 客户端登出游戏 */
	public final static int MID_LOGOUT = 290;

	// /////////////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////
	// 玩家退出的N种原因
	/** 玩家退出的N种原因:未知原因 */
	public final static int CAUSE_PLAYEROUT_UNKNOWN = 0;
	/** 玩家退出的N种原因:用户客户端主动退出 */
	public final static int CAUSE_PLAYEROUT_USEROP = 1;
	/** 玩家退出的N种原因:网络断连 */
	public final static int CAUSE_PLAYEROUT_DISCONNECT = 2;
	/** 玩家退出的N种原因:客户端空闲（超出既定时间没有收到客户端的心跳消息） */
	public final static int CAUSE_PLAYEROUT_IDLE = 3;
	/** 玩家退出的N种原因:服务器将客户端踢出（可能是GM操作） */
	public final static int CAUSE_PLAYEROUT_KICK = 4;
	/** 玩家退出的N种原因:服务器关机 */
	public final static int CAUSE_PLAYEROUT_SERVERSHUTDOWN = 5;
	/** 玩家退出的N种原因:异常 */
	public final static int CAUSE_PLAYEROUT_EXCEPTION = 6;
	/** 玩家退出的N种原因:相同的账号在别处登录 */
	public final static int CAUSE_PLAYEROUT_OVERLAP = 7;
	/** 玩家退出的N种原因:重连（游戏逻辑可以不处理这种情况，因为接着又会重连上来）*/
	public final static int CAUSE_PLAYEROUT_RECONNECT = 8;

	/**
	 * 断线重连。-20130530新增
	 * 
	 * <pre>
	 * <u>注意：这个功能分两步走，第一是连到FE成功拿到GS的IP端口，然后第二步是连到GS（都是发本MID）</u>
	 * 【request】:
	 *   String code;//密匙，如果不符合 服务器将判断为非法客户端并断开链接
	 *   String clientmodel;//终端型号
	 *   String pName; //账号名
	 *   String pPassword;//账号密码
	 * 【FE response】:
	 *   int result;//连接结果{@link PL_RECONNECT_SUCCEED}/{@link PL_RECONNECT_FAILED_VERIFY}/{@link PL_RECONNECT_FAILED_GSSHUTDOWN}/{@link PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   if(result==PL_RECONNECT_SUCCEED){
	 *     String gsIP;
	 *     int gsPort;
	 *   }
	 * 【GS response】:
	 *   int result;//连接结果{@link PL_RECONNECT_SUCCEED}/{@link PL_RECONNECT_FAILED_VERIFY}/{@link PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   if(result==PL_RECONNECT_SUCCEED){
	 *     long playerID;
	 *   }
	 * </pre>
	 */
	public final static int MID_RECONNECT = 6;
	public final static int PL_RECONNECT_SUCCEED = 0;
	public final static int PL_RECONNECT_FAILED_VERIFY = 1;
	public final static int PL_RECONNECT_FAILED_GSSHUTDOWN = 2;

	/**
	 * 设定禁言或解禁
	 * 
	 * <pre>
	 * 【request】：
	 *   long playerID;
	 *   long endTimeMillis;//解封设为0
	 * 【response】:
	 *   int code;//{@link #PL_GAG_SUCCEED}/{@link #PL_GAG_FAILED_PLAYERNOTEXIST}/{@link #PL_GAG_FAILED_TIMEFORMATERROR}/{@link PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   long playerID;
	 *   long endTimeMillis;
	 * </pre>
	 */
	public final static int MID_GAG = 7;
	public final static int PL_GAG_SUCCEED = 0;
	public final static int PL_GAG_FAILED_PLAYERNOTEXIST = 1;
	public final static int PL_GAG_FAILED_TIMEFORMATERROR = 2;

	/**
	 * 设定封号或解封
	 * 
	 * <pre>
	 * 【request】：
	 *   long playerID;
	 *   long endTimeMillis;//解封设为0
	 * 【response】:
	 *   int code;//{@link #PL_BAN_SUCCEED}/{@link #PL_BAN_FAILED_PLAYERNOTEXIST}/{@link #PL_BAN_FAILED_TIMEFORMATERROR}/{@link PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   long playerID;
	 *   long endTimeMillis;
	 * </pre>
	 */
	public final static int MID_BAN = 8;
	public final static int PL_BAN_SUCCEED = 0;
	public final static int PL_BAN_FAILED_PLAYERNOTEXIST = 1;
	public final static int PL_BAN_FAILED_TIMEFORMATERROR = 2;

	/**
	 * 查询用户的禁言时间。
	 * 
	 * <pre>
	 * 【request】 
	 *   long playerID;
	 * 【response】
	 *   long playerID;
	 *   long endTimeMillis;//如果返回-1表示没查到
	 * </pre>
	 */
	public final static int MID_QUERY_GAG = 9;

	/**
	 * 查询用户的封号时间。
	 * 
	 * <pre>
	 * 【request】 
	 *   long playerID;
	 * 【response】
	 *   long playerID;
	 *   long endTimeMillis;//如果返回-1表示没查到
	 * </pre>
	 */
	public final static int MID_QUERY_BAN = 10;

	/*****************************************************************************
	 * 支付/充值前客户端向GS获取必须的信息以满足不同渠道SDK的支付/充值数据需要。（C <-> GS）
	 * 
	 * <pre>
	 * <b>支付流程：
	 * 1. C-(MID_PAY_BEFORE)->GS; 
	 * 2. GS-(MID_PAY_BEFORE)->C;
	 * 3. C-(申请支付)->SDKServer; 
	 * 4. SDKServer-(callback)->PaymentServer;
	 * 5. PaymentServer-(MID_PS2GS_PAY)->GS; 
	 * 6. GS-(MID_PS2GS_PAY)->PaymentServer;
	 * 7. GS-(MID_PAY_RESULT)->C .
	 * </b>
	 * </pre>
	 * 
	 * =============
	 * 
	 * <pre>
	 * 【request】 
	 *   int promoid;
	 *   long roleid;
	 *   String roleName;
	 * 【response】
	 *   int promoid;
	 *   int responsecode;//{@link #PL_PAY_BEFORE_SUCCEED}/{@link #PL_PAY_BEFORE_FAILED_PROMONOSUPPORT}/{@link #PL_PAY_BEFORE_FAILED_PROMOSTOPPAY}/{@link #PL_ILLEGAL_SESSION_OR_MSG}/{@link #PL_UNKNOWNEXCEPTION_OR_SERVERBUSY}
	 *   String tips;
	 *   if(responsecode=={@link #PL_PAY_BEFORE_SUCCEED}){
	 *     boolean openpriceui;//是否打开我们自己的金额界面
	 *     int paramN;//参数KV对数量
	 *     for(paramN){
	 *       String key;//参数KEY，根据不同渠道而定
	 *       String value;//参数值，根据不同渠道而定
	 *     }
	 *   }
	 *  
	 *  
	 * 以下是对各渠道的param说明：
	 * ============================================
	 * 当乐 (1001)
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：元 --0.1
	 * String productName = info.get("product");  // 商品名称 --元宝
	 * String extInfo = info.get("ext");  // CP自定义信息，多为CP订单号 -- 字符串pid1001gid1rid1234
	 * ------------------------------------------------------------------------------------
	 * UC(1002)
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：元 --0.1
	 * String callbackInfo = info.get("ext"); //CP自定义信息 --字符串pid1001gid1rid1234
	 * ------------------------------------------------------------------------------------
	 * 91(1003)
	 * String orderId = info.get("orderId");  //CP订单号，必须保证唯一
	 * float money = Float.parseFloat(info.get("price")); // 商品价格，单位：在91服务器配置里改兑换率
	 * String payDescription = info.get("ext");//即支付描述（客户端API参数中的payDescription字段） 购买时客户端应用通过API传入，原样返回给应用服务器 开发者可以利用该字段，定义自己的扩展数据。例如区分游戏服务器 
	 * -------------------------------------------------------------------------------------
	 * 360(1004)
	 *               -充值S2C的参数MAP：orderId、ext、price、product、productId、exchange_ratio、accessToken、userId、notifyUri、appName、appUserName、appUserId
	 * --------------------------------------------------------------------------------------
	 * Lenovo(1005)  -登录时c2s的参数MAP：lpsust
	 *               -充值S2C的参数MAP：orderId、ext、appKey、notifyUri、productCode、price(单位分)									
	 * --------------------------------------------------------------------------------------										
	 * Xiaomi(1006)  -登录时c2s的参数MAP：uid,sessionId
	 *               -充值S2C的参数MAP：orderId、ext、price								
	 * --------------------------------------------------------------------------------------
	 * Duokoo(1007)  -登录时c2s的参数MAP：uid,sessionId
	 *               -充值S2C的参数MAP：orderId、ext、price、product、exchange_ratio(人民币与游戏充值币的默认比例，例如2，代表1元人民币可以兑换2个游戏币，整数)
	 * ==============================================
	 * 20131105接入的渠道与客户端的约定
	 * --------------------------------------------------
	 * 渠道		        c->s				   s->c
	 * 5G		    sign/token			    orderId/ext
	 * AGames		sessionId/accountId		orderId/notifyUrl/productName
	 * AnZhi		sid				        orderNo(整型)/productDesc/ext
	 * BaoRuan		token				    orderId/notifyUrl
	 * BXGame		userId/ticket			orderId/notifyUrl/ext
	 * Sohu		    uid/sessionId			orderId/productName/ext
	 * UMeng		uid/sessionId			orderId/ratio/coinName
	 * ---------------------------------------------------
	 * </pre>
	 */
	public final static int MID_PAY_BEFORE = 11;
	public final static int PL_PAY_BEFORE_SUCCEED = 0;
	public final static int PL_PAY_BEFORE_FAILED_PROMONOSUPPORT = 1;// 未支持渠道，其实就是渠道ID错误
	public final static int PL_PAY_BEFORE_FAILED_PROMOSTOPPAY = 2;// 渠道暂停充值，一般是渠道方的问题或发现严重漏洞我们暂停某个渠道的充值

	/************************************* 以下常量值严禁修改 ****************************************/
	public final static String CONST_PAYMENT_VALUE_PRODUCE = "元宝";
	public final static String CONST_PAYMENT_VALUE_PRODUCEID = "1";
	public final static String CONST_PAYMENT_KEY_ORDERID = "orderId";
	public final static String CONST_PAYMENT_KEY_EXT = "ext";

	public final static String PROMO_KEY_KOLA_PNAME = "pn";
	public final static String PROMO_KEY_KOLA_PW = "pw";

	public final static String PROMO_KEY_UID = "uid";
	public final static String PROMO_KEY_SESSIONID = "sessionId";
	public final static String PROMO_KEY_TOKEN = "token";
	public final static String PROMO_KEY_TICKET = "ticket";
	public final static String PROMO_KEY_ACCOUNTID = "accountId";
	public final static String PROMO_KEY_USERNAME = "userName";
	public final static String PROMO_KEY_NOTIFYURL = "notifyUrl";

	public final static String PROMO_KEY_DOWNJOY_MID = "mid";
	public final static String PROMO_KEY_DOWNJOY_TOKEN = PROMO_KEY_TOKEN;
	public final static String PROMO_KEY_UC_SID = "sid";
	public final static String PROMO_KEY_UC_UCID = "ucid";
	public final static String PROMO_KEY_91_UIN = "uin";
	public final static String PROMO_KEY_91_SESSIONID = PROMO_KEY_SESSIONID;
	public final static String PROMO_KEY_360_AUTHCODE = "authcode";
	public final static String PROMO_KEY_360_USERID = "userId";
	public final static String PROMO_KEY_360_ACCESSTOKEN = "accessToken";
	public final static String PROMO_KEY_MI_UID = PROMO_KEY_UID;
	public final static String PROMO_KEY_MI_SESSION = PROMO_KEY_SESSIONID;
	public final static String PROMO_KEY_LENOVO_LPSUST = "lpsust";
	public final static String PROMO_KEY_LENOVO_REALM = "realm";
	public final static String PROMO_KEY_LENOVO_ACCOUNTID = "AccountID";
	public final static String PROMO_KEY_DUOKOO_SESSIONID = PROMO_KEY_SESSIONID;
	public final static String PROMO_KEY_DUOKOO_UID = PROMO_KEY_UID;
	/************************************* 以上常量值严禁修改 ****************************************/

	/**
	 * 支付/充值结果。由GS主动PUSH给客户端。
	 * 
	 * <pre>
	 * 【response】
	 *   String orderId;//订单号
	 *   int payresultcode;//充值结果
	 *   String payresulttips;//充值结果提示（客户端弹窗照显示）
	 * </pre>
	 */
	public final static int MID_PAY_RESULT = 12;

	/**
	 * 客户端主动询问服务器充值的结果（C <-> GS）
	 * 
	 * <pre>
	 * 【request】 
	 *   int promoID;
	 *   String orderId;
	 * 【response】
	 *   String orderId;//订单号
	 *   int payresultcode;//充值结果
	 *   String payresulttips;//充值结果提示（客户端弹窗照显示）
	 * </pre>
	 */
	public final static int MID_QUERY_PAYRESULT = 13;
	
	/** 此消息发给客户端，服务器已经断开其连接 */
	public final static int MID_SEND_IDLE_TO_CLIENT = 14;
	
	/** 此消息发给客户端，服务器已经关闭 */
	public final static int MID_SEND_SHUTDOWN_TO_CLIENT = 15;
	
	
	/**
	 * 关闭gs的key
	 */
	public final static String KEY_SHUTDOWN_GS = "16c288adbbbae89d02d61fed2e26bb36";
	
	/**
	 * 关闭FE的key
	 */
	public final static String KEY_SHUTDOWN_FE = "16c288adbbbae89d02d61fed2e26bb36";
}
