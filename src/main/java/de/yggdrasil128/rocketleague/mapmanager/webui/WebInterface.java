package de.yggdrasil128.rocketleague.mapmanager.webui;

import com.sun.net.httpserver.HttpServer;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.ApiHttpHandler;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.SetupApiHttpHandler;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.StaticFilesHttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WebInterface {
	private final Logger logger;
	private final RLMapManager rlMapManager;
	private final int port;
	private String browserTabID = null;
	private HttpServer httpServer;
	
	public WebInterface(RLMapManager rlMapManager, int port) {
		logger = LoggerFactory.getLogger(WebInterface.class);
		this.rlMapManager = rlMapManager;
		this.port = port;
	}
	
	public static boolean isPortBound(int port) {
		try(ServerSocket ignored = new ServerSocket(port)) {
			return false;
		} catch(IOException e) {
			return true;
		}
	}
	
	public void start(boolean isSetupMode) {
		logger.info("Starting web server on port " + port);
		try {
			httpServer = HttpServer.create(new InetSocketAddress(port), 0);
			httpServer.setExecutor(Executors.newFixedThreadPool(2));
			httpServer.createContext("/", new StaticFilesHttpHandler(isSetupMode));
			if(isSetupMode) {
				httpServer.createContext("/api/", new SetupApiHttpHandler(rlMapManager));
			} else {
				httpServer.createContext("/api/", new ApiHttpHandler(rlMapManager));
			}
			httpServer.start();
		} catch(Exception e) {
			logger.warn("Couldn't start web UI", e);
		}
	}
	
	public void stop() {
		httpServer.stop(0);
	}
	
	public String getBrowserTabID() {
		return browserTabID;
	}
	
	public void setBrowserTabID(String browserTabID) {
		this.browserTabID = browserTabID;
	}
	
	public void openInBrowser() {
		openInBrowser(port, e -> logger.warn("Couldn't open web UI in browser", e));
	}
	
	public static void openInBrowser(int port, Consumer<Exception> onError) {
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI("http://localhost:" + port));
			} catch(Exception e) {
				onError.accept(e);
			}
		}
	}
}
