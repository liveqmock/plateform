package com.koala.game;

import org.jdom.Element;

/**
 * 
 * @author PERRY CHAN
 */
public class KGameMsgCompressConfig {
	
	private static int _msgLengthForAutoCompress = 1024 * 1024; // 自动压缩的长度（默认长度1M）
	private static byte _autoCompressType = KGameMessage.ENCRYPTION_NONE; // 自动压缩的类型
	
	public static void init(Element element) {

		_msgLengthForAutoCompress = Integer.parseInt(element.getChildText("msgLengthForAutoCompress"));
		String sCompressType = element.getChildTextTrim("autoCompressType").toLowerCase();
		if (sCompressType.equals("base64")) {
			_autoCompressType = KGameMessage.ENCRYPTION_BASE64;
		} else if (sCompressType.equals("none")) {
			_autoCompressType = KGameMessage.ENCRYPTION_NONE;
		} else {
			_autoCompressType = KGameMessage.ENCRYPTION_ZIP;
		}
	}
	
	public static int getMsgLengthForAutoCompress() {
		return _msgLengthForAutoCompress;
	}
	
	public static byte getAutoCompressType() {
		return _autoCompressType;
	}
}
