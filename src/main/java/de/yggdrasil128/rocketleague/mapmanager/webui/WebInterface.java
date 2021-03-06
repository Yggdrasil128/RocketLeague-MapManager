package de.yggdrasil128.rocketleague.mapmanager.webui;

import com.sun.net.httpserver.HttpServer;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.*;
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
	private HttpServer httpServer;
	private final IPWhitelist ipWhitelist;
	private StaticFilesHttpHandler staticFilesHttpHandler;
	private ApiHttpHandler apiHttpHandler;
	
	public WebInterface(RLMapManager rlMapManager, int port) {
		logger = LoggerFactory.getLogger(WebInterface.class.getName());
		this.rlMapManager = rlMapManager;
		this.port = port;
		ipWhitelist = new IPWhitelist();
		ipWhitelist.setWhitelist(rlMapManager.getConfig().getIpWhitelist());
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
	
	public static boolean isPortBound(int port) {
		try(ServerSocket ignored = new ServerSocket(port)) {
			return false;
		} catch(IOException e) {
			return true;
		}
	}
	
	public IPWhitelist getIpWhitelist() {
		return ipWhitelist;
	}
	
	public StaticFilesHttpHandler getStaticFilesHttpHandler() {
		return staticFilesHttpHandler;
	}
	
	public ApiHttpHandler getApiHttpHandler() {
		return apiHttpHandler;
	}
	
	public void start(boolean isSetupMode) {
		logger.info("Starting web server on port " + port);
		try {
			httpServer = HttpServer.create(new InetSocketAddress(port), 0);
			httpServer.setExecutor(Executors.newFixedThreadPool(2));
			
			staticFilesHttpHandler = new StaticFilesHttpHandler(isSetupMode);
			httpServer.createContext("/", ipWhitelist.forHttpHandler(staticFilesHttpHandler));
			if(isSetupMode) {
				SetupApiHttpHandler apiHttpHandler = new SetupApiHttpHandler(rlMapManager, "/api/");
				httpServer.createContext("/api/", ipWhitelist.forHttpHandler(apiHttpHandler));
			} else {
				apiHttpHandler = new ApiHttpHandler(rlMapManager, "/api/");
				httpServer.createContext("/api/", ipWhitelist.forHttpHandler(apiHttpHandler));
				
				ApiUploadHttpHandler apiUploadHttpHandler = new ApiUploadHttpHandler(rlMapManager, "/api/upload/");
				httpServer.createContext("/api/upload/", ipWhitelist.forHttpHandler(apiUploadHttpHandler));
			}
			httpServer.start();
		} catch(Exception e) {
			logger.warn("Couldn't start web UI", e);
		}
	}
	
	public void stop() {
		httpServer.stop(0);
	}
	
	public void openInBrowser() {
		openInBrowser(port, e -> logger.warn("Couldn't open web UI in browser", e));
	}
}
