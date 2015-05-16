package com.koala.game.resserver;

/**
 * <pre>
 * 资源下载服务器说明：
 * 1、HTTP方法： GET
 * 2、URL接受的参数定义请参考本接口常量
 * 最终URL格式如下： http://10.10.0.188:8080/d?i=1&v=1&o=0&l=4253609
 * 3、RESPONSE中携带一个Header客户端需要用到的"RESID"
 * </pre>
 */

public interface KGameHttpProtocol {

	/** 资源ID */
	public static final String URL_PARAM_RESID = "i";
	/** 资源的修正版本号 */
	public static final String URL_PARAM_REVISION = "v";
	/** 文件读取的起点字节，如果"<0或>=文件长度"都当是0 */
	public static final String URL_PARAM_OFFSET = "o";
	/** 文件读取长度，如果"l<0或(l+o)>=文件长度"都当是读取整个文件实际剩余长度 */
	public static final String URL_PARAM_LENGTH = "l";

	/** HTTP响应的一个HEADER表示当前返回的数据对于的资源ID */
	public static final String HEADER_RESID = "RESID";
}
