package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.Main;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.game_discovery.GameDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.maps.CustomMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.LethamyrMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import de.yggdrasil128.rocketleague.mapmanager.tools.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("SameReturnValue")
public class ApiHttpHandler extends AbstractApiHttpHandler {
	private static final Gson GSON = Config.GSON;
	
	private final RLMapManager rlMapManager;
	private final Executor mapImageRequestsThreadPool = Executors.newFixedThreadPool(2);
	private final LastUpdated lastUpdatedMaps = new LastUpdated(), lastUpdatedConfig = new LastUpdated();
	
	public ApiHttpHandler(RLMapManager rlMapManager, String context) {
		super(context, rlMapManager.getLogger());
		
		this.rlMapManager = rlMapManager;
		
		super.registerHandler("getMapImage", this::handleMapImageRequest);
		super.registerHandler("gameDiscovery", this::handleGameDiscoveryRequest);
		
		super.registerFunction("steamWorkshopMapDiscovery_start", this::steamWorkshopMapDiscovery_start);
		super.registerFunction("steamWorkshopMapDiscovery_status", this::steamWorkshopMapDiscovery_status);
		super.registerFunction("steamWorkshopMapDiscovery_cancel", this::steamWorkshopMapDiscovery_cancel);
		
		super.registerFunction("steamWorkshopMapDownload_start", this::steamWorkshopMapDownload_start);
		super.registerFunction("steamWorkshopMapDownload_status", this::steamWorkshopMapDownload_status);
		super.registerFunction("steamWorkshopMapDownload_cancel", this::steamWorkshopMapDownload_cancel);
		
		super.registerFunction("lethamyrMapDownload_start", this::lethamyrMapDownload_start);
		super.registerFunction("lethamyrMapDownload_status", this::lethamyrMapDownload_status);
		super.registerFunction("lethamyrMapDownload_cancel", this::lethamyrMapDownload_cancel);
		
		super.registerFunction("workshopTextures_check", this::workshopTextures_check);
		super.registerFunction("workshopTextures_start", this::workshopTextures_start);
		super.registerFunction("workshopTextures_status", this::workshopTextures_status);
		
		super.registerFunction("getVersion", this::getVersion);
		super.registerFunction("getConfig", this::getConfig);
		super.registerFunction("getMaps", this::getMaps);
		super.registerFunction("setFavorite", this::setFavorite);
		super.registerFunction("getLoadedMapID", this::getLoadedMapID);
		super.registerFunction("loadMap", this::loadMap);
		super.registerFunction("unloadMap", this::unloadMap);
		super.registerFunction("editMap", this::editMap);
		super.registerFunction("deleteMap", this::deleteMap);
		super.registerFunction("importCustomMap", this::importCustomMap);
		super.registerFunction("refreshMapMetadata", this::refreshMapMetadata);
		super.registerFunction("isRocketLeagueRunning", this::isRocketLeagueRunning);
		super.registerFunction("startRocketLeague", this::startRocketLeague);
		super.registerFunction("stopRocketLeague", this::stopRocketLeague);
		super.registerFunction("patchConfig", this::patchConfig);
		super.registerFunction("exitApp", this::exitApp);
		super.registerFunction("getStatus", this::getStatus);
		super.registerFunction("getUpdateInfo", this::getUpdateInfo);
		super.registerFunction("installUpdate", this::installUpdate);
		super.registerFunction("hasDesktopIcon", this::hasDesktopIcon);
		super.registerFunction("createDesktopIcon", this::createDesktopIcon);
	}
	
	public LastUpdated getLastUpdatedMaps() {
		return lastUpdatedMaps;
	}
	
	private RLMap getMapFromParameters(Map<String, String> parameters, @SuppressWarnings("SameParameterValue") boolean throwIfNotFound) {
		RLMap map = rlMapManager.getConfig().getMaps().get(parameters.get("mapID"));
		if(map == null && throwIfNotFound) {
			throw new NoSuchElementException("Unknown id");
		}
		return map;
	}
	
	private String getVersion(Map<String, String> parameters) {
		return RLMapManager.VERSION.toString();
	}
	
	private String getConfig(Map<String, String> parameters) {
		return rlMapManager.getConfig().toJson();
	}
	
	private String getMaps(Map<String, String> parameters) {
		JsonArray array = new JsonArray();
		
		Map<String, RLMap> maps = rlMapManager.getConfig().getMaps();
		for(RLMap map : maps.values()) {
			JsonObject json = new JsonObject();
			json.addProperty("id", map.getID());
			json.addProperty("title", map.getDisplayName());
			json.addProperty("description", map.getDescription());
			json.addProperty("authorName", map.getAuthorName());
			json.addProperty("udkName", map.getUdkFilename());
			json.addProperty("url", map.getURL());
			final File imageFile = map.getImageFile();
			if(imageFile == null) {
				json.addProperty("hasImage", false);
				json.addProperty("imageMTime", 0);
			} else {
				json.addProperty("hasImage", true);
				json.addProperty("imageMTime", imageFile.lastModified());
			}
			json.addProperty("isFavorite", map.isFavorite());
			json.addProperty("mapSize", map.getSize());
			json.addProperty("addedTimestamp", map.getAddedTimestamp());
			json.addProperty("lastLoadedTimestamp", map.getLastLoadedTimestamp());
			json.addProperty("canBeDeleted", map.canBeDeleted());
			
			array.add(json);
		}
		return GSON.toJson(array);
	}
	
	private String steamWorkshopMapDiscovery_start(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDiscovery.create(rlMapManager);
		task.registerOnFinishRunnable(() -> lastUpdatedMaps.now(parameters.get("btid")));
		task.start();
		return task.getStatusJson();
	}
	
	private String steamWorkshopMapDiscovery_status(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDiscovery.get();
		if(task == null) {
			return "";
		}
		return task.getStatusJson();
	}
	
	private String steamWorkshopMapDiscovery_cancel(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDiscovery.get();
		if(task != null) {
			task.cancel();
		}
		return "";
	}
	
	private String steamWorkshopMapDownload_start(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDownload.create(parameters.get("url"), rlMapManager);
		task.registerOnFinishRunnable(() -> lastUpdatedMaps.now(parameters.get("btid")));
		task.start();
		return task.getStatusJson();
	}
	
	private String steamWorkshopMapDownload_status(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDownload.get();
		if(task == null) {
			return "";
		}
		return task.getStatusJson();
	}
	
	private String steamWorkshopMapDownload_cancel(Map<String, String> parameters) {
		final Task task = SteamWorkshopMap.MapDownload.get();
		if(task != null) {
			task.cancel();
		}
		return "";
	}
	
	private String lethamyrMapDownload_start(Map<String, String> parameters) {
		final Task task = LethamyrMap.MapDownload.create(parameters.get("url"), rlMapManager);
		task.registerOnFinishRunnable(() -> lastUpdatedMaps.now(parameters.get("btid")));
		task.start();
		return task.getStatusJson();
	}
	
	private String lethamyrMapDownload_status(Map<String, String> parameters) {
		final Task task = LethamyrMap.MapDownload.get();
		if(task == null) {
			return "";
		}
		return task.getStatusJson();
	}
	
	private String lethamyrMapDownload_cancel(Map<String, String> parameters) {
		final Task task = LethamyrMap.MapDownload.get();
		if(task != null) {
			task.cancel();
		}
		return "";
	}
	
	private String workshopTextures_check(Map<String, String> parameters) {
		boolean result = WorkshopTextures.checkIfInstalled(rlMapManager.getConfig());
		return result ? "1" : "0";
	}
	
	private String workshopTextures_start(Map<String, String> parameters) {
		Task task = WorkshopTextures.InstallTask.create(rlMapManager.getConfig());
		task.registerOnFinishRunnable(() -> lastUpdatedConfig.now(parameters.get("btid")));
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
	
	private String setFavorite(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		
		boolean isFavorite = "1".equals(parameters.get("isFavorite"));
		map.setFavorite(isFavorite);
		rlMapManager.getConfig().save();
		
		lastUpdatedMaps.now(parameters.get("btid"));
		if(rlMapManager.getSysTray() != null) {
			rlMapManager.getSysTray().updateLoadFavoriteMapMenu();
		}
		return "";
	}
	
	private String getLoadedMapID(Map<String, String> parameters) {
		return String.valueOf(rlMapManager.getConfig().getLoadedMapID());
	}
	
	private String loadMap(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		rlMapManager.loadMap(map);
		lastUpdatedMaps.now(parameters.get("btid"));
		return "";
	}
	
	private String unloadMap(Map<String, String> parameters) {
		rlMapManager.unloadMap();
		lastUpdatedMaps.now(parameters.get("btid"));
		return "";
	}
	
	private String editMap(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		String title = parameters.get("title").trim();
		String authorName = parameters.get("authorName").trim();
		String description = parameters.get("description").trim();
		
		map.setTitle(title);
		if(authorName.isEmpty()) {
			map.setAuthorName(null);
		} else {
			map.setAuthorName(authorName);
		}
		map.setDescription(description);
		
		rlMapManager.getConfig().save();
		rlMapManager.getSysTray().updateLoadFavoriteMapMenu();
		
		lastUpdatedMaps.now(parameters.get("btid"));
		return "";
	}
	
	private String deleteMap(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		rlMapManager.getConfig().deleteMap(map);
		rlMapManager.getConfig().save();
		rlMapManager.getSysTray().updateLoadFavoriteMapMenu();
		
		lastUpdatedMaps.now(parameters.get("btid"));
		return "";
	}
	
	private String importCustomMap(Map<String, String> parameters) {
		try {
			String response = CustomMap.importMap(rlMapManager);
			lastUpdatedMaps.now(parameters.get("btid"));
			return response;
		} catch(Exception e) {
			return "Error: " + e;
		}
	}
	
	private String refreshMapMetadata(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		boolean result = map.refreshMetadata();
		rlMapManager.getConfig().save();
		rlMapManager.getSysTray().updateLoadFavoriteMapMenu();
		lastUpdatedMaps.now(parameters.get("btid"));
		if(!result) {
			return "Error: Couldn't update map data.";
		}
		JsonObject json = new JsonObject();
		json.addProperty("title", map.getDisplayName());
		json.addProperty("authorName", map.getAuthorName());
		json.addProperty("description", map.getDescription());
		return GSON.toJson(json);
	}
	
	private String isRocketLeagueRunning(Map<String, String> parameters) {
		return rlMapManager.getRLProcessWatcher().isRunning() ? "true" : "false";
	}
	
	private String startRocketLeague(Map<String, String> parameters) {
		rlMapManager.startRocketLeague();
		return "";
	}
	
	private String stopRocketLeague(Map<String, String> parameters) {
		rlMapManager.stopRocketLeague();
		return "";
	}
	
	private String patchConfig(Map<String, String> parameters) {
		rlMapManager.getConfig().patchFromJson(parameters.get("postBody"));
		lastUpdatedConfig.now(parameters.get("btid"));
		return "";
	}
	
	private String exitApp(Map<String, String> parameters) {
		System.exit(0);
		return "";
	}
	
	private String getStatus(Map<String, String> parameters) {
		JsonObject json = new JsonObject();
		
		json.addProperty("isRLRunning", rlMapManager.getRLProcessWatcher().isRunning());
		if(rlMapManager.getUpdateChecker().getJson() == null) {
			json.add("updateAvailable", null);
		} else if(rlMapManager.getUpdateChecker().isUpdateAvailable()) {
			json.addProperty("updateAvailable", rlMapManager.getUpdateChecker().getLatestVersion().toString());
		} else {
			json.addProperty("updateAvailable", false);
		}
		
		json.add("lastUpdatedMaps", lastUpdatedMaps.toJson());
		json.add("lastUpdatedConfig", lastUpdatedConfig.toJson());
		
		JsonObject tasksRunning = new JsonObject();
		tasksRunning.addProperty("steamWorkshopMapDiscovery", SteamWorkshopMap.MapDiscovery.isTaskRunning());
		tasksRunning.addProperty("steamWorkshopMapDownload", SteamWorkshopMap.MapDownload.isTaskRunning());
		tasksRunning.addProperty("lethamyrMapDownload", LethamyrMap.MapDownload.isTaskRunning());
		tasksRunning.addProperty("workshopTextures", WorkshopTextures.InstallTask.isTaskRunning());
		json.add("tasksRunning", tasksRunning);
		
		return GSON.toJson(json);
	}
	
	private String getUpdateInfo(Map<String, String> parameters) {
		return rlMapManager.getUpdateChecker().getJson();
	}
	
	private String installUpdate(Map<String, String> parameters) throws Exception {
		final List<File> oldInstalledJarFiles = Main.findInstalledJarFiles();
		
		JsonObject jarAssetInfo = rlMapManager.getUpdateChecker().findJarAsset();
		assert jarAssetInfo != null;
		
		URL downloadURL = new URL(jarAssetInfo.get("browser_download_url").getAsString());
		File newJarFile = new File(RLMapManager.FILE_ROOT, jarAssetInfo.get("name").getAsString());
		
		oldInstalledJarFiles.remove(newJarFile);
		
		FileUtils.copyURLToFile(downloadURL, newJarFile);
		
		String launchCommand = "\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\" -jar \"" + newJarFile.getAbsolutePath() + "\"";
		
		final File shortcut = DesktopShortcutHelper.findShortcut();
		if(shortcut != null) {
			DesktopShortcutHelper.createShortcut(shortcut, newJarFile);
		}
		
		if(rlMapManager.isAutostartEnabled()) {
			// update registry entry
			RegistryHelper.add(RLMapManager.REGISTRY_AUTOSTART_KEY, RLMapManager.REGISTRY_AUTOSTART_VALUE, launchCommand + " --autostart");
		}
		
		rlMapManager.getWebInterface().stop();
		Thread.sleep(500);
		
		Runtime.getRuntime().exec(launchCommand);
		for(File file : oldInstalledJarFiles) {
			// delay deletion by 5 seconds
			String command = "cmd /c ping localhost -n 6 > nul && del \"" + file.getAbsolutePath() + "\"";
			Runtime.getRuntime().exec(command);
		}
		System.exit(0);
		
		return "";
	}
	
	private String hasDesktopIcon(Map<String, String> parameters) {
		return DesktopShortcutHelper.findShortcut() != null ? "1" : "0";
	}
	
	private String createDesktopIcon(Map<String, String> parameters) {
		DesktopShortcutHelper.saveAppIcon(rlMapManager);
		
		final File jarFile = Main.findInstalledJarFile();
		assert jarFile != null;
		DesktopShortcutHelper.createOrUpdateShortcut(jarFile);
		
		return "";
	}
	
	private void handleMapImageRequest(Map<String, String> parameters,
									   HttpExchange httpExchange,
									   OutputStream outputStream,
									   Logger logger,
									   String functionName) {
		mapImageRequestsThreadPool.execute(() -> {
			try {
				RLMap map = getMapFromParameters(parameters, true);
				File mapImageFile = map.getImageFile();
				if(mapImageFile == null) {
					httpExchange.sendResponseHeaders(404, -1);
					return;
				}
				String mimeType = map.getImageFileMimeType();
				if(mimeType != null) {
					httpExchange.getResponseHeaders().set("Content-Type", mimeType);
				}
				httpExchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
				
				byte[] data = FileUtils.readFileToByteArray(mapImageFile);
				httpExchange.sendResponseHeaders(200, data.length);
				outputStream.write(data);
			} catch(IOException e) {
				logger.warn("Uncaught exception", e);
			} finally {
				try {
					outputStream.flush();
					outputStream.close();
				} catch(IOException ignored) {
				}
			}
		});
	}
	
	private void handleGameDiscoveryRequest(Map<String, String> parameters,
											HttpExchange httpExchange,
											OutputStream outputStream,
											Logger logger,
											String functionName) {
		new Thread(() -> {
			try {
				Config.Platform platform = Config.Platform.fromInt(Integer.parseInt(parameters.get("platform")));
				boolean tryDefaultDirectoryFirst = "1".equals(parameters.get("tryDefaultDirectoryFirst"));
				
				GameDiscovery.Result result;
				
				if(tryDefaultDirectoryFirst) {
					result = GameDiscovery.discover(platform, null, rlMapManager);
					if(!result.isSuccess()) {
						JFrame jFrame = JavaXSwingTools.makeModalFrame();
						
						int choice = JOptionPane.showConfirmDialog(
								jFrame,
								"Couldn't automatically detect your Rocket League installation. Select folder manually?",
								"Rocket League Map Manager",
								JOptionPane.YES_NO_OPTION);
						
						jFrame.dispose();
						
						if(choice != 0) {
							httpExchange.sendResponseHeaders(204, -1);
							return;
						}
						
						result = GameDiscovery.chooseFolderAndDiscover(platform, rlMapManager);
					}
				} else {
					result = GameDiscovery.chooseFolderAndDiscover(platform, rlMapManager);
				}
				
				if(result == null) {
					httpExchange.sendResponseHeaders(204, -1);
					return;
				}
				
				if(result.isSuccess()) {
					result.saveToConfig(rlMapManager.getConfig(), true);
				}
				
				lastUpdatedMaps.now(parameters.get("btid"));
				
				result.showResultMessage();
				
				final String data = GSON.toJson(result);
				httpExchange.sendResponseHeaders(200, data.length());
				outputStream.write(data.getBytes(StandardCharsets.UTF_8));
			} catch(Exception e) {
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
	
	public static class LastUpdated {
		private String browserTabID = null;
		private long timestamp = 0;
		
		public void now(String browserTabID) {
			this.browserTabID = browserTabID;
			timestamp = System.currentTimeMillis();
		}
		
		private JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("browserTabID", browserTabID);
			json.addProperty("timestamp", timestamp);
			return json;
		}
	}
}
