package com.koala.game.example;

import org.jdom.Element;

import com.koala.game.KGameModule;
import com.koala.game.communication.KGameMessageEvent;
import com.koala.game.exception.KGameServerException;

public class _ExampleGameLogicModule implements KGameModule{

	private boolean isPlayerSessionListener;
	
	@Override
	public void init(String moduleName, boolean isPlayerSessionListener,
			Element eModuleSelfDefiningConfig) {
		this.isPlayerSessionListener=isPlayerSessionListener;
	}

	@Override
	public boolean isPlayerSessionListener() {
		return isPlayerSessionListener;
	}

	@Override
	public String getModuleName() {
		return null;
	}

	@Override
	public void serverStartCompleted() {
		
	}

	@Override
	public boolean messageReceived(KGameMessageEvent msgEvent)
			throws KGameServerException {
//		System.out.println("GOT GLLLLLLLLLLLLLLLLLLLLLLL MSG... ");
		return false;
	}

	@Override
	public void exceptionCaught(KGameServerException ex) {
		
	}

	@Override
	public void serverShutdown() {
		
	}

	@Override
	public boolean isInitFinished() {
		// TODO Auto-generated method stub
		return true;
	}

}
