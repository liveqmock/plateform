package com.koala.game.tips;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.exception.KGameServerException;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimer;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.StringUtil;
import com.koala.game.util.XmlUtil;

public class KGameTips {

	private final static Map<String, String> tips = new HashMap<String, String>();
	public static final DateFormat DATE_FORMAT_TIPS = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm");
	private static File _tipsFile;
	private static long _tipsLastModifyTime;
	private static long _delaySeconds = 30;

	public static void load() {
		String xml = "res/config/Tips.xml";
		_tipsFile = new File(xml);
		_tipsLastModifyTime = _tipsFile.lastModified();
		loadTips();
	}
	
	@SuppressWarnings("unchecked")
	private static void loadTips() {
		Document doc = XmlUtil.openXml(_tipsFile);
		Element root = doc.getRootElement();
		List<Element> ts = root.getChildren("tips");
		for (Element element : ts) {
			String key = element.getAttributeValue("key");
			String value = element.getTextTrim();
			System.out.println(key + " : " + value);
			tips.put(key, value);
		}
	}

	public static String get(String key) {
		String v = tips.get(key);
		return v == null ? "" : v;
	}

	/**
	 * 根据KEY获取一个TIPS字符串，并对字符串中的动态参数进行填充
	 * 
	 * <pre>
	 * 如：
	 * 
	 * </pre>
	 * 
	 * @param key
	 * @param arguments
	 * @return
	 */
	public static String get(String key, Object... arguments) {
		String str = get(key);
		return StringUtil.format(str, arguments);
	}
	
	public static void submitWatcher(KGameTimer timer) {
		timer.newTimeSignal(new KGameTipsWatcher(), _delaySeconds, TimeUnit.SECONDS);
	}
	
	private static class KGameTipsWatcher implements KGameTimerTask {

		@Override
		public String getName() {
			return "KGameTipsWatcher";
		}

		@Override
		public Object onTimeSignal(KGameTimeSignal timeSignal) throws KGameServerException {
			if(_tipsFile.lastModified() != _tipsLastModifyTime) {
				loadTips();
				_tipsLastModifyTime = _tipsFile.lastModified();
			}
			timeSignal.getTimer().newTimeSignal(this, _delaySeconds, TimeUnit.SECONDS);
			return "SUCCESS";
		}

		@Override
		public void done(KGameTimeSignal timeSignal) {
			
		}

		@Override
		public void rejected(RejectedExecutionException e) {
			
		}
		
	}

	// private static String replace(String str, Object... arguments) {
	// // for (Object object : arguments) {
	// // str = str.replace("{}", object.toString());
	// str = java.text.MessageFormat.format(str, arguments);
	// // }
	//
	// return str;
	// }
	//
//	 public static void main(String[] args) {
//	 // String s = "该账号于{0}在{1}手机上登录游戏，请核实并确保帐号安全！";
//	 // System.out.println(replace(s, "2013/2/22", "SONY ONE"));
//	 KGameTips.load();
//	 System.out.println(KGameTips.get("login_safe_warning", "2013/2/22",
//	 "SONY ONE"));
//	 }
}
