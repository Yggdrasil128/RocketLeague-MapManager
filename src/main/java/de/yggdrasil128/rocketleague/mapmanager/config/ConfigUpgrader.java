package de.yggdrasil128.rocketleague.mapmanager.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.game_discovery.GameDiscovery;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ConfigUpgrader {
	static boolean upgrade() {
		try {
			String data = FileUtils.readFileToString(RLMapManager.FILE_CONFIG, StandardCharsets.UTF_8);
			JsonObject json = Config.GSON.fromJson(data, JsonObject.class);
			if(!json.has("configVersion")) {
				return false;
			}
			int configVersion = json.get("configVersion").getAsInt();
			if(configVersion <= 0 || configVersion > Config.CURRENT_CONFIG_VERSION) {
				return false;
			}
			if(configVersion == Config.CURRENT_CONFIG_VERSION) {
				return true;
			}
			
			// backup old config version
			// noinspection ResultOfMethodCallIgnored
			RLMapManager.FILE_CONFIG.renameTo(new File(RLMapManager.FILE_CONFIG.getAbsolutePath() + "." + configVersion));
			
			// deliberately no breaks here
			switch(configVersion) {
				case 1:
					upgrade1(json);
				case 2:
					upgrade2(json);
			}
			
			json.addProperty("configVersion", Config.CURRENT_CONFIG_VERSION);
			data = Config.GSON.toJson(json);
			FileUtils.writeStringToFile(RLMapManager.FILE_CONFIG, data, StandardCharsets.UTF_8);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	/**
	 * Fixes the path to the RocketLeague.exe file by re-running SteamLibraryDiscovery.
	 *
	 * @param json The current config json
	 */
	private static void upgrade1(JsonObject json) {
		final JsonElement jsonElement = json.get("steamappsFolder");
		if(jsonElement.isJsonNull()) {
			// SteamLibraryDiscovery hasn't been run yet.
			return;
		}
		File steamappsFolder = new File(jsonElement.getAsString());
		GameDiscovery.Result result = GameDiscovery.discover(Config.Platform.STEAM, steamappsFolder, null);
		if(!result.isSuccess()) {
			// SteamLibraryDiscovery has failed.
			// Our best course of action is to ignore this and continue with broken Steam Library paths,
			// so the other settings won't be deleted.
			return;
		}
		json.addProperty("exeFile", result.getExeFile().getAbsolutePath());
	}
	
	private static void upgrade2(JsonObject json) {
		json.addProperty("platform", "STEAM");
		
		final File[] mapImages = new File(RLMapManager.FILE_ROOT, "mapImages").listFiles();
		assert mapImages != null;
		
		File workshopFolder = new File(json.get("workshopFolder").getAsString());
		
		JsonObject newMaps = new JsonObject();
		for(Map.Entry<String, JsonElement> mapMetadata : json.getAsJsonObject("mapMetadata").entrySet()) {
			String mapIDString = mapMetadata.getKey();
			JsonObject map = mapMetadata.getValue().getAsJsonObject();
			long mapID = map.get("id").getAsLong();
			
			map.remove("id");
			map.addProperty("workshopID", mapID);
			map.addProperty("isManuallyDownloaded", false);
			map.addProperty("mapType", "STEAM_WORKSHOP");
			
			if(map.get("imageFileMIMEType").isJsonNull()) {
				map.add("imageFileMimeType", null);
				map.add("imageFile", null);
			} else {
				File mapImage = null;
				for(File file : mapImages) {
					String filename = file.getName();
					if(filename.equals(mapIDString) || filename.startsWith(mapIDString + ".")) {
						mapImage = file;
						break;
					}
				}
				if(mapImage != null) {
					File mapImageRenamed = new File(mapImage.getParentFile(), "S-" + mapImage.getName());
					//noinspection ResultOfMethodCallIgnored
					mapImage.renameTo(mapImageRenamed);
					
					map.addProperty("imageFileMimeType", map.get("imageFileMIMEType").getAsString());
					map.addProperty("imageFile", mapImageRenamed.getAbsolutePath());
				} else {
					map.add("imageFileMimeType", null);
					map.add("imageFile", null);
				}
			}
			map.remove("imageFileMIMEType");
			
			File folder = new File(workshopFolder, mapIDString);
			if(!folder.exists()) {
				continue;
			}
			map.addProperty("addedTimestamp", folder.lastModified());
			File[] files = folder.listFiles();
			if(files == null) {
				continue;
			}
			File udkFile = null;
			for(File file : files) {
				if(file.getName().endsWith(".udk")) {
					udkFile = file;
					break;
				}
			}
			if(udkFile == null) {
				continue;
			}
			
			map.addProperty("udkFile", udkFile.getAbsolutePath());
			map.addProperty("udkFilename", udkFile.getName());
			
			newMaps.add("S-" + mapIDString, map);
		}
		
		json.add("maps", newMaps);
		json.remove("mapMetadata");
	}
}
