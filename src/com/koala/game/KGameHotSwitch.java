package com.koala.game;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.jdom.Document;
import org.jdom.Element;

import com.koala.game.exception.KGameServerException;
import com.koala.game.logging.KGameLogger;
import com.koala.game.timer.KGameTimeSignal;
import com.koala.game.timer.KGameTimerTask;
import com.koala.game.util.XmlUtil;

public class KGameHotSwitch implements KGameTimerTask {

	private final static KGameLogger logger = KGameLogger
			.getLogger(KGameHotSwitch.class);
	private static KGameHotSwitch instance;
	private static long lastModifiedOfXml;
	private int modifyCheckSeconds;
	private final static File xml = new File("res/config/hotswitch.xml");
	// private final ConcurrentHashMap<String, String> switchs = new
	// ConcurrentHashMap<String, String>();

	private int allowPingPerMin=6;
	private int pingJudgeMin=3;
	private long cheatBanMin=30;

	public static KGameHotSwitch getInstance() {
		if (instance == null)
			instance = new KGameHotSwitch();
		return instance;
	}

	private KGameHotSwitch() {
		load(false);
	}

	private void load(boolean reload) {

		lastModifiedOfXml = xml.lastModified();
		Document doc = XmlUtil.openXml(xml);
		Element root = doc.getRootElement();
		modifyCheckSeconds = Integer.parseInt(root
				.getAttributeValue("modifyCheckSeconds"));

		// 真正的开关内容
		allowPingPerMin = Integer.parseInt(root
				.getChildTextTrim("AllowPingPerMin"));
		pingJudgeMin = Integer.parseInt(root.getChildTextTrim("PingJudgeMin"));
		cheatBanMin = Long.parseLong(root.getChildTextTrim("CheatBanMin"));
	}

	// /////////////////////////////////////////////////////////////////////
	public int getAllowPingPerMin() {
		return allowPingPerMin;
	}

	public int getPingJudgeMin() {
		return pingJudgeMin;
	}

	public long getCheatBanMin() {
		return cheatBanMin;
	}

	// /////////////////////////////////////////////////////////////////////

	@Override
	public String getName() {
		return KGameHotSwitch.class.getName();
	}

	@Override
	public Object onTimeSignal(KGameTimeSignal timeSignal)
			throws KGameServerException {
		try {
			if (xml.exists() && (xml.lastModified() != lastModifiedOfXml)) {
				logger.info("{} had modified! reload...", xml.getName());
				load(true);
			}
		} finally {
			timeSignal.getTimer().newTimeSignal(this, modifyCheckSeconds,
					TimeUnit.SECONDS);
		}
		return null;
	}

	@Override
	public void done(KGameTimeSignal timeSignal) {
	}

	@Override
	public void rejected(RejectedExecutionException e) {
		logger.warn("RejectedExecutionException {}", e);
	}

	public static void main(String[] args) {
		System.out.println(KGameHotSwitch.getInstance().getAllowPingPerMin());
		System.out.println(KGameHotSwitch.getInstance().getPingJudgeMin());
		System.out.println(KGameHotSwitch.getInstance().getCheatBanMin());
	}
}
