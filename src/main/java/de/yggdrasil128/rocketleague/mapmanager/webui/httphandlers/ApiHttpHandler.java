package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.yggdrasil128.rocketleague.mapmanager.MapDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.SteamLibraryDiscovery;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMapMetadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@SuppressWarnings("SameReturnValue")
public class ApiHttpHandler implements HttpHandler {
	private static final Gson GSON = Config.GSON;
	
	private final RLMapManager rlMapManager;
	private final HashMap<String, ApiFunctionRaw> functions;
	
	public ApiHttpHandler(RLMapManager rlMapManager) {
		this.rlMapManager = rlMapManager;
		
		functions = new HashMap<>();
		functions.put("getVersion", ApiFunctionRaw.of(this::getVersion));
		functions.put("getConfig", ApiFunctionRaw.of(this::getConfig));
		functions.put("discoverSteamLibrary", ApiFunctionRaw.of(this::discoverSteamLibrary));
		functions.put("getMaps", ApiFunctionRaw.of(this::getMaps));
		functions.put("startMapDiscovery", ApiFunctionRaw.of(this::startMapDiscovery));
		functions.put("getMapDiscoveryStatus", ApiFunctionRaw.of(this::getMapDiscoveryStatus));
		functions.put("getMapImage", this::getMapImage);
		functions.put("setFavorite", ApiFunctionRaw.of(this::setFavorite));
		functions.put("getLoadedMapID", ApiFunctionRaw.of(this::getLoadedMapID));
		functions.put("loadMap", ApiFunctionRaw.of(this::loadMap));
		functions.put("unloadMap", ApiFunctionRaw.of(this::unloadMap));
		functions.put("refreshMapMetadata", ApiFunctionRaw.of(this::refreshMapMetadata));
		functions.put("isRocketLeagueRunning", ApiFunctionRaw.of(this::isRocketLeagueRunning));
		functions.put("startRocketLeague", ApiFunctionRaw.of(this::startRocketLeague));
		functions.put("stopRocketLeague", ApiFunctionRaw.of(this::stopRocketLeague));
		functions.put("patchConfig", ApiFunctionRaw.of(this::patchConfig));
		functions.put("exitApp", ApiFunctionRaw.of(this::exitApp));
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
		JsonObject json = new JsonObject();
		Config config = rlMapManager.getConfig();
		
		json.addProperty("needsSetup", config.needsSetup());
		
		JsonObject jsonPaths = new JsonObject();
		json.add("paths", jsonPaths);
		TriConsumer<JsonObject, String, File> addPathProperty = (jsonObject, propertyName, file) -> {
			if(file == null) {
				jsonObject.add(propertyName, JsonNull.INSTANCE);
			} else {
				jsonObject.addProperty(propertyName, file.getAbsolutePath());
			}
		};
		addPathProperty.accept(jsonPaths, "steamappsFolder", config.getSteamappsFolder());
		addPathProperty.accept(jsonPaths, "exeFile", config.getExeFile());
		addPathProperty.accept(jsonPaths, "upkFile", config.getUpkFile());
		addPathProperty.accept(jsonPaths, "workshopFolder", config.getWorkshopFolder());
		
		json.addProperty("renameOriginalUnderpassUPK", config.getRenameOriginalUnderpassUPK());
		json.addProperty("behaviorWhenRLIsStopped", config.getBehaviorWhenRLIsStopped().toInt());
		json.addProperty("behaviorWhenRLIsRunning", config.getBehaviorWhenRLIsRunning().toInt());
		
		json.addProperty("upkFilename", config.getUpkFilename());
		json.addProperty("webInterfacePort", config.getWebInterfacePort());
		
		return GSON.toJson(json);
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
			json.addProperty("hasImage", rlMapMetadata.getImageFile() != null);
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
		MapDiscovery mapDiscovery = MapDiscovery.get();
		JsonObject json = new JsonObject();
		if(mapDiscovery == null) {
			json.addProperty("progressFloat", 0);
			json.addProperty("isDone", true);
			json.addProperty("message", "Not started");
			return GSON.toJson(json);
		}
		
		boolean isDone = mapDiscovery.isDone();
		json.addProperty("isDone", isDone);
		
		if(isDone) {
			Throwable throwable = mapDiscovery.getThrowable();
			if(throwable == null) {
				int mapCount = rlMapManager.getMaps().size();
				String s = "Successfully discovered " + mapCount + (mapCount == 1 ? " map." : " maps.");
				json.addProperty("message", s);
				return GSON.toJson(json);
			}
			
			json.addProperty("message", "Error:<br />" + throwable.toString());
			return GSON.toJson(json);
		}
		
		json.addProperty("message", "Discovering maps, please wait...");
		json.addProperty("progress", mapDiscovery.getProgress());
		json.addProperty("progressTarget", mapDiscovery.getProgressTarget());
		return GSON.toJson(json);
	}
	
	private byte[] getMapImage(Map<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws IOException {
		RLMap map = getMapFromParameters(parameters, true);
		RLMapMetadata metadata = rlMapManager.getConfig().getMapMetadata(map.getID());
		File mapImageFile = metadata.getImageFile();
		if(mapImageFile == null) {
			throw new NoSuchElementException("Map has no image");
		}
		String mimeType = metadata.getImageFileMIMEType();
		if(mimeType != null) {
			httpExchange.getResponseHeaders().set("Content-Type", mimeType);
		}
		return FileUtils.readFileToByteArray(mapImageFile);
	}
	
	private String setFavorite(Map<String, String> parameters) {
		RLMap map = getMapFromParameters(parameters, true);
		RLMapMetadata metadata = rlMapManager.getConfig().getMapMetadata(map.getID());
		
		boolean isFavorite = "1".equals(parameters.get("isFavorite"));
		metadata.setFavorite(isFavorite);
		rlMapManager.getConfig().save();
		
		return "";
	}
	
	private String getLoadedMapID(Map<String, String> parameters) {
		return String.valueOf(rlMapManager.getConfig().getLoadedMapID());
	}
	
	private String loadMap(Map<String, String> parameters) throws IOException {
		RLMap map = getMapFromParameters(parameters, true);
		rlMapManager.loadMap(map);
		return "";
	}
	
	private String unloadMap(Map<String, String> parameters) {
		rlMapManager.unloadMap();
		return "";
	}
	
	private String refreshMapMetadata(Map<String, String> parameters) throws IOException {
		RLMap map = getMapFromParameters(parameters, true);
		rlMapManager.getConfig().getMapMetadata(map.getID()).fetchFromWorkshop();
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
		JsonObject json = GSON.fromJson(parameters.get("postBody"), JsonObject.class);
		Config config = rlMapManager.getConfig();
		
		if(json.has("renameOriginalUPK")) {
			boolean oldValue = config.getRenameOriginalUnderpassUPK();
			boolean value = json.get("renameOriginalUPK").getAsBoolean();
			config.setRenameOriginalUnderpassUPK(value);
			
			if(oldValue ^ value) {
				// this setting has changed
				if(!value) {
					// has been disabled
					rlMapManager.renameOriginalUnderpassUPK(true);
				} else if(config.getLoadedMapID() != 0) {
					// has been enabled and a map currently loaded
					rlMapManager.renameOriginalUnderpassUPK(false);
				}
			}
		}
		if(json.has("behaviorWhenRLIsStopped")) {
			Config.BehaviorWhenRLIsStopped value = Config.BehaviorWhenRLIsStopped.fromInt(json.get("behaviorWhenRLIsStopped").getAsInt());
			config.setBehaviorWhenRLIsStopped(value);
		}
		if(json.has("behaviorWhenRLIsRunning")) {
			Config.BehaviorWhenRLIsRunning value = Config.BehaviorWhenRLIsRunning.fromInt(json.get("behaviorWhenRLIsRunning").getAsInt());
			config.setBehaviorWhenRLIsRunning(value);
		}
		
		if(json.has("upkFilename")) {
			String value = json.get("upkFilename").getAsString();
			config.setUpkFilename(value);
		}
		if(json.has("webInterfacePort")) {
			int value = json.get("webInterfacePort").getAsInt();
			config.setWebInterfacePort(value);
		}
		
		config.save();
		
		return "";
	}
	
	private String exitApp(Map<String, String> parameters) {
		System.exit(0);
		return "";
	}
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		OutputStream outputStream = httpExchange.getResponseBody();
		
		String functionName = httpExchange.getRequestURI().toString().substring(5);
		HashMap<String, String> parameters = new HashMap<>();
		int index = functionName.indexOf('?');
		if(index != -1) {
			String s = functionName.substring(index + 1);
			functionName = functionName.substring(0, index);
			String[] parts = s.split("&");
			for(String part : parts) {
				part = part.trim();
				if(part.isEmpty()) {
					continue;
				}
				index = part.indexOf('=');
				if(index == -1) {
					parameters.put(part, null);
				} else {
					parameters.put(part.substring(0, index), part.substring(index + 1));
				}
			}
		}
		if(functionName.endsWith("/")) {
			functionName = functionName.substring(0, functionName.length() - 1);
		}
		// POST body
		if(httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
			String postBody = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
			parameters.put("postBody", postBody);
		}
		
		ApiFunctionRaw function = functions.get(functionName);
		if(function == null) {
			httpExchange.sendResponseHeaders(404, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		byte[] output;
		
		try {
			output = function.apply(parameters, httpExchange, outputStream);
		} catch(AssertionError | IllegalArgumentException e) {
			httpExchange.sendResponseHeaders(400, -1);
			outputStream.flush();
			outputStream.close();
			return;
		} catch(NoSuchElementException e) {
			httpExchange.sendResponseHeaders(404, -1);
			outputStream.flush();
			outputStream.close();
			return;
		} catch(Exception e) {
			rlMapManager.getLogger().error("Uncaught exception on ApiHttpHandler command '" + function + "'", e);
			
			httpExchange.sendResponseHeaders(500, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		if(output.length == 0) {
			httpExchange.sendResponseHeaders(204, -1);
		} else {
			httpExchange.sendResponseHeaders(200, output.length);
			outputStream.write(output);
		}
		outputStream.flush();
		outputStream.close();
	}
	
	@FunctionalInterface
	private interface ApiFunction {
		String apply(Map<String, String> parameters) throws Exception;
	}
	
	@FunctionalInterface
	private interface ApiFunctionRaw {
		static ApiFunctionRaw of(ApiFunction apiFunction) {
			return (parameters, httpExchange, outputStream) -> {
				String result = apiFunction.apply(parameters);
				if(result == null || result.isEmpty()) {
					return new byte[0];
				}
				return result.getBytes(StandardCharsets.UTF_8);
			};
		}
		
		byte[] apply(Map<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception;
	}
	
	@FunctionalInterface
	private interface TriConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}
	
}
