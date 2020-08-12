package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.DesktopShortcutHelper;
import de.yggdrasil128.rocketleague.mapmanager.Main;
import de.yggdrasil128.rocketleague.mapmanager.MapDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.api.AbstractApiHttpHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@SuppressWarnings("SameReturnValue")
public class SetupApiHttpHandler extends AbstractApiHttpHandler {
//	private static final Gson GSON = Config.GSON;
	
	private final RLMapManager rlMapManager;
	
	public SetupApiHttpHandler(RLMapManager rlMapManager) {
		super(rlMapManager.getLogger());
		this.rlMapManager = rlMapManager;
		
		super.registerHandler("chooseSteamLibrary", this::handleChooseSteamLibraryRequest);
		
		super.registerFunction("getVersion", this::getVersion);
		super.registerFunction("getAppPath", this::getAppPath);
		super.registerFunction("getConfig", this::getConfig);
		super.registerFunction("patchConfig", this::patchConfig);
		super.registerFunction("startMapDiscovery", this::startMapDiscovery);
		super.registerFunction("getMapDiscoveryStatus", this::getMapDiscoveryStatus);
		super.registerFunction("exit", this::exit);
		super.registerFunction("install", this::install);
		super.registerFunction("cancel", this::cancel);
	}
	
	private void handleChooseSteamLibraryRequest(Map<String, String> parameters,
												 HttpExchange httpExchange,
												 OutputStream outputStream,
												 Logger logger,
												 @SuppressWarnings("unused") String functionName) {
		ApiHttpHandler.handleChooseSteamLibraryRequest(parameters, httpExchange, outputStream, logger, functionName, rlMapManager, null, false);
	}
	
	private String getVersion(Map<String, String> parameters) {
		return RLMapManager.VERSION.toString();
	}
	
	private String getAppPath(Map<String, String> parameters) {
		return RLMapManager.FILE_ROOT.getAbsolutePath();
	}
	
	private String getConfig(Map<String, String> parameters) {
		return rlMapManager.getConfig().toJson();
	}
	
	private String patchConfig(Map<String, String> parameters) {
		rlMapManager.getConfig().patchFromJson(parameters.get("postBody"));
		return "";
	}
	
	private String install(Map<String, String> parameters) throws IOException {
		Main.deleteInstalledJarFiles();
		
		// copy jar file to app folder
		File source = Main.getThisJarFile();
		assert source != null;
		File target = new File(RLMapManager.FILE_ROOT, source.getName());
		
		FileUtils.copyFile(source, target);
		
		DesktopShortcutHelper.saveAppIcon(rlMapManager);
		
		return "";
	}
	
	private String startMapDiscovery(Map<String, String> parameters) {
		MapDiscovery.start(rlMapManager);
		return MapDiscovery.getStatusJson();
	}
	
	private String getMapDiscoveryStatus(Map<String, String> parameters) {
		return MapDiscovery.getStatusJson();
	}
	
	private String exit(Map<String, String> parameters) {
		boolean startApp = "1".equals(parameters.get("startApp"));
		boolean createDesktopShortcut = "1".equals(parameters.get("createDesktopShortcut"));
		
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch(InterruptedException ignored) {
			}
			
			rlMapManager.getWebInterface().stop();
			rlMapManager.getConfig().save();
			
			File jar = Main.findInstalledJarFile();
			if(createDesktopShortcut && jar != null) {
				DesktopShortcutHelper.createOrUpdateShortcut(jar);
			} else {
				DesktopShortcutHelper.deleteIfExists();
			}
			
			if(!startApp) {
				System.exit(0);
				return;
			}
			
			try {
				Thread.sleep(1000);
			} catch(InterruptedException ignored) {
			}
			
			if(jar != null) {
				String command = "\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\" -jar \"" + jar.getAbsolutePath() + "\"";
				try {
					Runtime.getRuntime().exec(command);
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
			
			System.exit(0);
		}).start();
		
		return "";
	}
	
	private String cancel(Map<String, String> parameters) {
		System.exit(0);
		return "";
	}
}
