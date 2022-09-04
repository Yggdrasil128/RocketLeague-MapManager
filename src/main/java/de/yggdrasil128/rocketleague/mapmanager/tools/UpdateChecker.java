package de.yggdrasil128.rocketleague.mapmanager.tools;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateChecker {
	private static final Gson GSON = Config.GSON;
	private static final String GITHUB_API_URL = "https://api.github.com/repos/Yggdrasil128/RocketLeague-MapManager/releases/latest";
	private static final long INTERVAL = 1000 * 60 * 60; // 1 hour
	private final Logger logger;
	private String json;
	private JsonObject response;
	private Version latestVersion;
	private boolean isUpdateAvailable;
	
	public UpdateChecker() {
		logger = LoggerFactory.getLogger(UpdateChecker.class.getName());
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				check();
			}
		}, 0, INTERVAL);
	}
	
	private void check() {
		try {
			URL url = new URL(GITHUB_API_URL);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			final int responseCode = con.getResponseCode();
			if(responseCode != 200) {
				throw new IOException("Unexpected response code " + responseCode);
			}
			InputStream inputStream = con.getInputStream();
			String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
			inputStream.close();
			
			JsonObject response = GSON.fromJson(json, JsonObject.class);
			Version latestVersion = Version.parse(response.get("tag_name").getAsString());
			boolean isUpdateAvailable = latestVersion.compareTo(RLMapManager.VERSION) > 0;
			
			this.json = json;
			this.response = response;
			this.latestVersion = latestVersion;
			this.isUpdateAvailable = isUpdateAvailable;
		} catch(Exception e) {
			logger.warn("Error while checking for updates", e);
			this.json = null;
			this.response = null;
			this.latestVersion = null;
			this.isUpdateAvailable = false;
		}
	}
	
	public String getJson() {
		return json;
	}
	
	public Version getLatestVersion() {
		return latestVersion;
	}
	
	public boolean isUpdateAvailable() {
		return isUpdateAvailable;
	}
	
	public JsonObject findJarAsset() {
		for(JsonElement assetElement : response.get("assets").getAsJsonArray()) {
			JsonObject asset = assetElement.getAsJsonObject();
			String name = asset.get("name").getAsString();
			if(name.toLowerCase().endsWith(".jar")) {
				return asset;
			}
		}
		return null;
	}
	
	public static class Version implements Comparable<Version> {
		private final int major, minor, patch;
		
		public Version(int major, int minor, int patch) {
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}
		
		public static Version parse(String s) {
			if(s == null || s.isEmpty()) {
				return null;
			}
			if(s.toLowerCase().startsWith("v")) {
				s = s.substring(1);
			}
			String[] parts = s.split("\\.");
			if(parts.length == 0 || parts.length > 3) {
				return null;
			}
			int major, minor = 0, patch = 0;
			try {
				major = Integer.parseInt(parts[0]);
				if(parts.length >= 2) {
					minor = Integer.parseInt(parts[1]);
				}
				if(parts.length == 3) {
					patch = Integer.parseInt(parts[2]);
				}
			} catch(NumberFormatException e) {
				return null;
			}
			return new Version(major, minor, patch);
		}
		
		public int getMajor() {
			return major;
		}
		
		public int getMinor() {
			return minor;
		}
		
		public int getPatch() {
			return patch;
		}
		
		@Override
		public String toString() {
			if(patch == 0) {
				return major + "." + minor;
			} else {
				return major + "." + minor + "." + patch;
			}
		}
		
		@Override
		public boolean equals(Object that) {
			if(this == that) return true;
			if(that == null || getClass() != that.getClass()) return false;
			Version version = (Version) that;
			return major == version.major &&
				   minor == version.minor &&
				   patch == version.patch;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(major, minor, patch);
		}
		
		@Override
		public int compareTo(Version that) {
			if(this.major != that.major) {
				return Integer.compare(this.major, that.major);
			}
			if(this.minor != that.minor) {
				return Integer.compare(this.minor, that.minor);
			}
			return Integer.compare(this.patch, that.patch);
		}
	}
}
