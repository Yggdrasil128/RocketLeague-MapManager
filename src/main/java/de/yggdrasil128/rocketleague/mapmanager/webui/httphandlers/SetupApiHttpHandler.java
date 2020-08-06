package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import de.yggdrasil128.rocketleague.mapmanager.Main;
import de.yggdrasil128.rocketleague.mapmanager.MapDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.SteamLibraryDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.api.AbstractApiHttpHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@SuppressWarnings("SameReturnValue")
public class SetupApiHttpHandler extends AbstractApiHttpHandler {
	private static final Gson GSON = Config.GSON;
	
	private final RLMapManager rlMapManager;
	
	public SetupApiHttpHandler(RLMapManager rlMapManager) {
		super(rlMapManager.getLogger());
		this.rlMapManager = rlMapManager;
		
		super.registerFunction("getVersion", this::getVersion);
		super.registerFunction("getAppPath", this::getAppPath);
		super.registerFunction("steamLibraryDiscovery", this::steamLibraryDiscovery);
		super.registerFunction("getConfig", this::getConfig);
		super.registerFunction("patchConfig", this::patchConfig);
		super.registerFunction("startMapDiscovery", this::startMapDiscovery);
		super.registerFunction("getMapDiscoveryStatus", this::getMapDiscoveryStatus);
		super.registerFunction("exit", this::exit);
		super.registerFunction("install", this::install);
	}
	
	private String getVersion(Map<String, String> parameters) {
		return RLMapManager.VERSION;
	}
	
	private String getAppPath(Map<String, String> parameters) {
		return RLMapManager.FILE_ROOT.getAbsolutePath();
	}
	
	private String steamLibraryDiscovery(Map<String, String> parameters) {
		SteamLibraryDiscovery.Result result;
		if(parameters.containsKey("postBody")) {
			result = rlMapManager.getSteamLibraryDiscovery().discoverSteamLibrary(new File(parameters.get("postBody")));
		} else {
			result = rlMapManager.getSteamLibraryDiscovery().discoverSteamLibrary();
		}
		
		if(result.isSuccess()) {
			result.saveToConfig(rlMapManager.getConfig());
		}
		
		return GSON.toJson(result);
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
		
		new Thread(() -> {
			try {
				Thread.sleep(1000);
			} catch(InterruptedException ignored) {
			}
			
			rlMapManager.getWebInterface().stop();
			rlMapManager.getConfig().save();
			
			if(!startApp) {
				System.exit(0);
				return;
			}
			
			try {
				Thread.sleep(1000);
			} catch(InterruptedException ignored) {
			}
			
			File jar = Main.findInstalledJarFile();
			if(jar != null) {
				String command = "\"" + System.getProperty("java.home") + "\\bin\\java.exe\" -jar \"" + jar.getAbsolutePath() + "\"";
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
}
