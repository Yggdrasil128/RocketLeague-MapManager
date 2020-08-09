package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.MapDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.SteamLibraryDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMapMetadata;
import de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.api.AbstractApiHttpHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
	
	public LastUpdated getLastUpdatedMaps() {
		return lastUpdatedMaps;
	}
	
	public ApiHttpHandler(RLMapManager rlMapManager) {
		super(rlMapManager.getLogger());
		
		this.rlMapManager = rlMapManager;
		
		super.registerHandler("getMapImage", this::handleMapImageRequest);
		
		super.registerFunction("getVersion", this::getVersion);
		super.registerFunction("getConfig", this::getConfig);
		super.registerFunction("discoverSteamLibrary", this::discoverSteamLibrary);
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
	}
	
	private RLMap getMapFromParameters(Map<String, String> parameters, @SuppressWarnings("SameParameterValue") boolean throwIfNotFound) {
		long id;
		try {
			id = Long.parseLong(parameters.get("mapID"));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException("Invalid id");
		}
		RLMap map = rlMapManager.getMaps().get(id);
		if(map == null && throwIfNotFound) {
			throw new NoSuchElementException("Unknown id");
		}
		return map;
	}
	
	private String getVersion(Map<String, String> parameters) {
		return RLMapManager.VERSION;
	}
	
	private String getConfig(Map<String, String> parameters) {
		return rlMapManager.getConfig().toJson();
	}
	
	private String discoverSteamLibrary(Map<String, String> parameters) {
		String path = parameters.get("postBody").trim();
		File file = new File(path);
		SteamLibraryDiscovery.Result result = rlMapManager.getSteamLibraryDiscovery().discoverSteamLibrary(file);
		if(result.isSuccess()) {
			result.saveToConfig(rlMapManager.getConfig());
		}
		
		JsonObject json = new JsonObject();
		json.addProperty("success", result.isSuccess());
		json.addProperty("message", result.getMessage());
		
		lastUpdatedMaps.now(parameters.get("btid"));
		return GSON.toJson(json);
	}
	
	private String getMaps(Map<String, String> parameters) {
		JsonArray array = new JsonArray();
		
		Map<Long, RLMap> maps = rlMapManager.getMaps();
		if(maps == null) {
			return GSON.toJson(array);
		}
		for(RLMap rlMap : maps.values()) {
			RLMapMetadata rlMapMetadata = rlMapManager.getConfig().getMapMetadata(rlMap.getID());
			
			JsonObject json = new JsonObject();
			json.addProperty("id", String.valueOf(rlMap.getID()));
			json.addProperty("name", rlMap.getUDKFilename());
			String title = rlMapMetadata.getTitle();
			if(title == null) {
				title = rlMap.getUDKFilename();
				title = title.substring(0, title.length() - 4);
			}
			json.addProperty("title", title);
			json.addProperty("description", rlMapMetadata.getDescription());
			json.addProperty("authorName", rlMapMetadata.getAuthorName());
			final File imageFile = rlMapMetadata.getImageFile();
			if(imageFile == null) {
				json.addProperty("hasImage", false);
				json.addProperty("imageMTime", 0);
			} else {
				json.addProperty("hasImage", true);
				json.addProperty("imageMTime", imageFile.lastModified());
			}
			json.addProperty("isFavorite", rlMapMetadata.isFavorite());
			json.addProperty("mapSize", String.valueOf(rlMap.getMapSize()));
			json.addProperty("lastLoadedTimestamp", rlMapMetadata.getLastLoadedTimestamp());
			
			array.add(json);
		}
		return GSON.toJson(array);
	}
	
	private String startMapDiscovery(Map<String, String> parameters) {
		MapDiscovery.start(rlMapManager);
		return "";
	}
	
	private String getMapDiscoveryStatus(Map<String, String> parameters) {
		if(MapDiscovery.get().isDone()) {
			lastUpdatedMaps.now(parameters.get("btid"));
		}
		return MapDiscovery.getStatusJson();
	}
	
	private String setFavorite(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		RLMapMetadata metadata = rlMapManager.getConfig().getMapMetadata(map.getID());
		
		boolean isFavorite = "1".equals(parameters.get("isFavorite"));
		metadata.setFavorite(isFavorite);
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
	
	private String refreshMapMetadata(Map<String, String> parameters) throws IOException {
		RLMap map = getMapFromParameters(parameters, true);
		rlMapManager.getConfig().getMapMetadata(map.getID()).fetchFromWorkshop();
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
		
		json.add("lastUpdatedMaps", lastUpdatedMaps.toJson());
		json.add("lastUpdatedConfig", lastUpdatedConfig.toJson());
		
		return GSON.toJson(json);
	}
	
	private void handleMapImageRequest(Map<String, String> parameters,
									   HttpExchange httpExchange,
									   OutputStream outputStream,
									   Logger logger,
									   String functionName) {
		mapImageRequestsThreadPool.execute(() -> {
			try {
				RLMap map = getMapFromParameters(parameters, true);
				RLMapMetadata metadata = rlMapManager.getConfig().getMapMetadata(map.getID());
				File mapImageFile = metadata.getImageFile();
				if(mapImageFile == null) {
					httpExchange.sendResponseHeaders(404, -1);
					return;
				}
				String mimeType = metadata.getImageFileMIMEType();
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
