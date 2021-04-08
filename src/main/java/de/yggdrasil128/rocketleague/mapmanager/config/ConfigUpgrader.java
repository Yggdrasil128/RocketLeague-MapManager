package de.yggdrasil128.rocketleague.mapmanager.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
			
			upgrade1(json);
			
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
		final SteamLibraryDiscovery steamLibraryDiscovery = new SteamLibraryDiscovery(null);
		final SteamLibraryDiscovery.Result result = steamLibraryDiscovery.discoverSteamLibrary(steamappsFolder);
		if(!result.isSuccess()) {
			// SteamLibraryDiscovery has failed.
			// Our best course of action is to ignore this and continue with broken Steam Library paths,
			// so the other settings won't be deleted.
			return;
		}
		json.addProperty("exeFile", result.getExeFile().getAbsolutePath());
	}
}
