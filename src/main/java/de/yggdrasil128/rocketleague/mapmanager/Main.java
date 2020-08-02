package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.webui.WebInterface;

public class Main {
	public static void main(String[] args) {
		RLMapManager rlMapManager = new RLMapManager();
		
		int port = rlMapManager.getConfig().getWebInterfacePort();
		if(WebInterface.isPortBound(port)) {
			System.err.println("App is already running! Opening a new browser tab, then exiting.");
			WebInterface.openInBrowser(port, Throwable::printStackTrace);
			System.exit(-1);
			return;
		}
		
		rlMapManager.start();
		rlMapManager.getWebInterface().openInBrowser();
	}
}
