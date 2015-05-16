package com.koala.game.timer;

import java.util.List;
import java.util.Set;

/**
 * KGAME定时器事件处理器，由游戏逻辑实现
 * 
 * @author AHONG
 * 
 */
public interface KGameTimerHandler {
	
	void init(KGameTimer timer, List<org.jdom.Element> taskElementsConfigInXml);

	void timerStopped(KGameTimer timer,Set<KGameTimeSignal> unprocessed);
}
