package de.yggdrasil128.rocketleague.mapmanager.config;

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
			//noinspection ConstantConditions
			if(configVersion == Config.CURRENT_CONFIG_VERSION) {
				return true;
			}
			
			// backup old config version
			//noinspection ResultOfMethodCallIgnored
			RLMapManager.FILE_CONFIG.renameTo(new File(RLMapManager.FILE_CONFIG.getAbsolutePath() + "." + configVersion));
			
			// upgrade1(json);
			
			data = Config.GSON.toJson(json);
			FileUtils.writeStringToFile(RLMapManager.FILE_CONFIG, data, StandardCharsets.UTF_8);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
}
