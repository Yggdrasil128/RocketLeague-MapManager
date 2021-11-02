package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.Main;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.game_discovery.GameDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import de.yggdrasil128.rocketleague.mapmanager.tools.DesktopShortcutHelper;
import de.yggdrasil128.rocketleague.mapmanager.tools.Task;
import de.yggdrasil128.rocketleague.mapmanager.tools.WorkshopTextures;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("SameReturnValue")
public class SetupApiHttpHandler extends AbstractApiHttpHandler {
	private static final Gson GSON = Config.GSON;
	
	private final RLMapManager rlMapManager;
	
	public SetupApiHttpHandler(RLMapManager rlMapManager, String context) {
		super(context, rlMapManager.getLogger());
		this.rlMapManager = rlMapManager;
		
		super.registerHandler("gameDiscovery", this::handleGameDiscoveryRequest);
		
		super.registerFunction("workshopTextures_check", this::workshopTextures_check);
		super.registerFunction("workshopTextures_start", this::workshopTextures_start);
		super.registerFunction("workshopTextures_status", this::workshopTextures_status);
		
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
	
	private void handleGameDiscoveryRequest(Map<String, String> parameters,
											HttpExchange httpExchange,
											OutputStream outputStream,
											Logger logger,
											@SuppressWarnings("unused") String functionName) {
//		ApiHttpHandler.handleChooseSteamLibraryRequest(parameters, httpExchange, outputStream, logger, functionName, rlMapManager, null, false);
		new Thread(() -> {
			try {
				boolean disableAlert = "1".equals(parameters.get("disableAlert"));
				boolean useDefaultDirectory = "1".equals(parameters.get("useDefaultDirectory"));
				Config.Platform platform = Config.Platform.fromInt(Integer.parseInt(parameters.get("platform")));
				
				GameDiscovery.Result result;
				if(useDefaultDirectory) {
					result = GameDiscovery.discover(platform, null, rlMapManager);
				} else {
					result = GameDiscovery.chooseFolderAndDiscover(platform, rlMapManager);
					if(result == null) {
						httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NO_CONTENT, -1);
						return;
					}
				}
				
				if(result.isSuccess()) {
					result.saveToConfig(rlMapManager.getConfig(), false);
				}
				
				final String data = GSON.toJson(result);
				httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_OK, data.length());
				outputStream.write(data.getBytes(StandardCharsets.UTF_8));
				
				if(!disableAlert) {
					result.showResultMessage();
				}
			} catch(IOException e) {
				logger.warn("Uncaught exception", e);
			} finally {
				try {
					outputStream.flush();
					outputStream.close();
				} catch(IOException ignored) {
				}
			}
		}).start();
	}
	
	private String workshopTextures_check(Map<String, String> parameters) {
		boolean result = WorkshopTextures.checkIfInstalled(rlMapManager.getConfig());
		return result ? "1" : "0";
	}
	
	private String workshopTextures_start(Map<String, String> parameters) {
		Task task = WorkshopTextures.InstallTask.create(rlMapManager.getConfig());
		task.start();
		return task.getStatusJson();
	}
	
	private String workshopTextures_status(Map<String, String> parameters) {
		final Task task = WorkshopTextures.InstallTask.get();
		if(task == null) {
			return "";
		}
		return task.getStatusJson();
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
		final SteamWorkshopMap.MapDiscovery mapDiscovery = SteamWorkshopMap.MapDiscovery.create(rlMapManager);
		mapDiscovery.start();
		return mapDiscovery.getStatusJson();
	}
	
	private String getMapDiscoveryStatus(Map<String, String> parameters) {
		final SteamWorkshopMap.MapDiscovery mapDiscovery = SteamWorkshopMap.MapDiscovery.get();
		if(mapDiscovery == null) {
			return "";
		}
		return mapDiscovery.getStatusJson();
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
