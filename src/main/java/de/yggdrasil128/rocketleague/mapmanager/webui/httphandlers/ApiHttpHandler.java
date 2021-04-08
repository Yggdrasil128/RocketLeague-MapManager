package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.DesktopShortcutHelper;
import de.yggdrasil128.rocketleague.mapmanager.Main;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.RegistryHelper;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.game_discovery.GameDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.api.AbstractApiHttpHandler;
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
	
	public ApiHttpHandler(RLMapManager rlMapManager) {
		super(rlMapManager.getLogger());
		
		this.rlMapManager = rlMapManager;
		
		super.registerHandler("getMapImage", this::handleMapImageRequest);
		super.registerHandler("gameDiscovery", this::handleGameDiscoveryRequest);
		
		super.registerFunction("getVersion", this::getVersion);
		super.registerFunction("getConfig", this::getConfig);
		super.registerFunction("getMaps", this::getMaps);
		super.registerFunction("startMapDiscovery", this::startMapDiscovery);
		super.registerFunction("getMapDiscoveryStatus", this::getMapDiscoveryStatus);
		super.registerFunction("setFavorite", this::setFavorite);
		super.registerFunction("getLoadedMapID", this::getLoadedMapID);
		super.registerFunction("loadMap", this::loadMap);
		super.registerFunction("unloadMap", this::unloadMap);
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
		if(maps == null) {
			return GSON.toJson(array);
		}
		for(RLMap map : maps.values()) {
			JsonObject json = new JsonObject();
			json.addProperty("id", String.valueOf(map.getID()));
			json.addProperty("name", map.getUdkFilename());
			json.addProperty("title", map.getDisplayName());
			json.addProperty("description", map.getDescription());
			json.addProperty("authorName", map.getAuthorName());
			final File imageFile = map.getImageFile();
			if(imageFile == null) {
				json.addProperty("hasImage", false);
				json.addProperty("imageMTime", 0);
			} else {
				json.addProperty("hasImage", true);
				json.addProperty("imageMTime", imageFile.lastModified());
			}
			json.addProperty("isFavorite", map.isFavorite());
			json.addProperty("mapSize", String.valueOf(map.getSize()));
			json.addProperty("lastLoadedTimestamp", map.getLastLoadedTimestamp());
			
			array.add(json);
		}
		return GSON.toJson(array);
	}
	
	private String startMapDiscovery(Map<String, String> parameters) {
		SteamWorkshopMap.MapDiscovery.start(rlMapManager);
		return "";
	}
	
	private String getMapDiscoveryStatus(Map<String, String> parameters) {
		if(SteamWorkshopMap.MapDiscovery.get().isDone()) {
			lastUpdatedMaps.now(parameters.get("btid"));
		}
		return SteamWorkshopMap.MapDiscovery.getStatusJson();
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
	
	private String loadMap(Map<String, String> parameters) throws IOException {
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
	
	private String refreshMapMetadata(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		map.refreshMetadata();
		rlMapManager.getConfig().save();
		lastUpdatedMaps.now(parameters.get("btid"));
		return "";
	}
	
	private String isRocketLeagueRunning(Map<String, String> parameters) {
		return rlMapManager.isRocketLeagueRunning() ? "true" : "false";
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
		
		json.addProperty("isRLRunning", rlMapManager.isRocketLeagueRunning());
		if(rlMapManager.getUpdateChecker().getJson() == null) {
			json.add("updateAvailable", null);
		} else if(rlMapManager.getUpdateChecker().isUpdateAvailable()) {
			json.addProperty("updateAvailable", rlMapManager.getUpdateChecker().getLatestVersion().toString());
		} else {
			json.addProperty("updateAvailable", false);
		}
		
		json.add("lastUpdatedMaps", lastUpdatedMaps.toJson());
		json.add("lastUpdatedConfig", lastUpdatedConfig.toJson());
		
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
						int choice = JOptionPane.showConfirmDialog(
								null,
								"Couldn't automatically detect your Rocket League installation. Select folder manually?",
								"Rocket League Map Manager",
								JOptionPane.YES_NO_OPTION);
						if(choice != 0) {
							httpExchange.sendResponseHeaders(204, -1);
							return;
						}
					}
				}
				
				result = GameDiscovery.chooseFolderAndDiscover(platform, rlMapManager);
				if(result == null) {
					httpExchange.sendResponseHeaders(204, -1);
					return;
				}
				
				if(result.isSuccess()) {
					result.saveToConfig(rlMapManager.getConfig(), true);
				}
				
				lastUpdatedMaps.now(parameters.get("btid"));
				
				final String data = GSON.toJson(result);
				httpExchange.sendResponseHeaders(200, data.length());
				outputStream.write(data.getBytes(StandardCharsets.UTF_8));
				
				result.showResultMessage();
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
