package de.yggdrasil128.rocketleague.mapmanager.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.SteamLibraryDiscovery;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Config {
	public static final Gson GSON = new GsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.registerTypeAdapter(File.class, new FileTypeAdapter())
			.create();
	public static final String DEFAULT_UPK_FILENAME = "Labs_Underpass_P.upk";
	static final int CURRENT_CONFIG_VERSION = 1;
	private static final transient Logger logger = LoggerFactory.getLogger(Config.class.getName());
	private final HashMap<Long, RLMapMetadata> mapMetadata = new HashMap<>();
	@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
	private int configVersion = CURRENT_CONFIG_VERSION;
	private File steamappsFolder = SteamLibraryDiscovery.DEFAULT_STEAMAPPS_FOLDER;
	private File exeFile;
	private File upkFile;
	private File workshopFolder;
	private int webInterfacePort = 16016;
	private long loadedMapID = 0;
	
	private boolean autostartOpenBrowser = false;
	private boolean renameOriginalUnderpassUPK = false;
	private BehaviorWhenRLIsStopped behaviorWhenRLIsStopped = BehaviorWhenRLIsStopped.DO_NOTHING;
	private BehaviorWhenRLIsRunning behaviorWhenRLIsRunning = BehaviorWhenRLIsRunning.RESTART_RL;
	
	private MapLayout mapLayout = MapLayout.DETAILED_LIST;
	private int mapSorting = 1;
	private boolean showLoadedMapAtTop = false;
	private boolean showFavoritesAtTop = false;
	
	private transient RLMapManager rlMapManager;
	
	public Config(RLMapManager rlMapManager) {
		this.rlMapManager = rlMapManager;
	}
	
	public static Config load(RLMapManager rlMapManager) {
		try {
			String data = FileUtils.readFileToString(RLMapManager.FILE_CONFIG, StandardCharsets.UTF_8);
			Config config = GSON.fromJson(data, Config.class);
			config.rlMapManager = rlMapManager;
			
			if(config.configVersion == CURRENT_CONFIG_VERSION) {
				return config;
			}
			
			if(ConfigUpgrader.upgrade()) {
				data = FileUtils.readFileToString(RLMapManager.FILE_CONFIG, StandardCharsets.UTF_8);
				config = GSON.fromJson(data, Config.class);
				config.rlMapManager = rlMapManager;
				return config;
			}
			
			return new Config(rlMapManager);
		} catch(FileNotFoundException e) {
			return new Config(rlMapManager);
		} catch(Exception e) {
			logger.error("Couldn't load config", e);
			return null;
		}
	}
	
	public synchronized void save() {
		try {
			File directory = RLMapManager.FILE_CONFIG.getParentFile();
			if(!directory.exists() && !RLMapManager.FILE_CONFIG.getParentFile().mkdirs()) {
				throw new IOException("Couldn't create RL-MapManager directory in home folder.");
			}
			String data = GSON.toJson(this);
			FileUtils.writeStringToFile(RLMapManager.FILE_CONFIG, data, StandardCharsets.UTF_8);
		} catch(Exception e) {
			logger.error("Couldn't save config", e);
		}
	}
	
	public boolean needsSetup() {
		if(steamappsFolder == null || !steamappsFolder.exists()) {
			return true;
		}
		if(exeFile == null || !exeFile.exists()) {
			return true;
		}
		if(upkFile == null) {
			return true;
		}
		if(workshopFolder == null) {
			return true;
		}
		return false;
	}
	
	public File getSteamappsFolder() {
		return steamappsFolder;
	}
	
	public void setSteamappsFolder(File steamappsFolder) {
		this.steamappsFolder = steamappsFolder;
	}
	
	public File getExeFile() {
		return exeFile;
	}
	
	public void setExeFile(File exeFile) {
		this.exeFile = exeFile;
	}
	
	public File getWorkshopFolder() {
		return workshopFolder;
	}
	
	public void setWorkshopFolder(File workshopFolder) {
		this.workshopFolder = workshopFolder;
	}
	
	public File getUpkFile() {
		return upkFile;
	}
	
	public void setUpkFile(File upkFile) {
		this.upkFile = upkFile;
	}
	
	public String getUpkFilename() {
		if(upkFile == null) {
			return DEFAULT_UPK_FILENAME;
		}
		return upkFile.getName();
	}
	
	public void setUpkFilename(String upkFilename) {
		upkFile = new File(upkFile.getParent(), upkFilename);
	}
	
	public int getWebInterfacePort() {
		return webInterfacePort;
	}
	
	public void setWebInterfacePort(int webInterfacePort) {
		if(webInterfacePort <= 0) {
			throw new IllegalArgumentException();
		}
		this.webInterfacePort = webInterfacePort;
	}
	
	public long getLoadedMapID() {
		return loadedMapID;
	}
	
	public void setLoadedMapID(long loadedMapID) {
		this.loadedMapID = loadedMapID;
	}
	
	public boolean getAutostartOpenBrowser() {
		return autostartOpenBrowser;
	}
	
	public void setAutostartOpenBrowser(boolean autostartOpenBrowser) {
		this.autostartOpenBrowser = autostartOpenBrowser;
	}
	
	public boolean getRenameOriginalUnderpassUPK() {
		return renameOriginalUnderpassUPK;
	}
	
	public void setRenameOriginalUnderpassUPK(boolean renameOriginalUnderpassUPK) {
		this.renameOriginalUnderpassUPK = renameOriginalUnderpassUPK;
	}
	
	public BehaviorWhenRLIsStopped getBehaviorWhenRLIsStopped() {
		return behaviorWhenRLIsStopped;
	}
	
	public void setBehaviorWhenRLIsStopped(BehaviorWhenRLIsStopped behaviorWhenRLIsStopped) {
		this.behaviorWhenRLIsStopped = behaviorWhenRLIsStopped;
	}
	
	public BehaviorWhenRLIsRunning getBehaviorWhenRLIsRunning() {
		return behaviorWhenRLIsRunning;
	}
	
	public void setBehaviorWhenRLIsRunning(BehaviorWhenRLIsRunning behaviorWhenRLIsRunning) {
		this.behaviorWhenRLIsRunning = behaviorWhenRLIsRunning;
	}
	
	public MapLayout getMapLayout() {
		return mapLayout;
	}
	
	public void setMapLayout(MapLayout mapLayout) {
		this.mapLayout = mapLayout;
	}
	
	public int getMapSorting() {
		return mapSorting;
	}
	
	public void setMapSorting(int mapSorting) {
		if(mapSorting < -5 || mapSorting == 0 || mapSorting > 5) {
			throw new IllegalArgumentException();
		}
		this.mapSorting = mapSorting;
	}
	
	public boolean getShowLoadedMapAtTop() {
		return showLoadedMapAtTop;
	}
	
	public void setShowLoadedMapAtTop(boolean showLoadedMapAtTop) {
		this.showLoadedMapAtTop = showLoadedMapAtTop;
	}
	
	public boolean getShowFavoritesAtTop() {
		return showFavoritesAtTop;
	}
	
	public void setShowFavoritesAtTop(boolean showFavoritesAtTop) {
		this.showFavoritesAtTop = showFavoritesAtTop;
	}
	
	public RLMapMetadata getMapMetadata(long mapID) {
		return mapMetadata.computeIfAbsent(mapID, mapID_ -> {
			RLMapMetadata rlMapMetadata = new RLMapMetadata(mapID_);
			try {
				rlMapMetadata.fetchFromWorkshop();
			} catch(IOException e) {
				logger.error("Couldn't fetch map metadata from workshop", e);
			}
			return rlMapMetadata;
		});
	}
	
	public String toJson() {
		JsonObject json = new JsonObject();
		
		json.addProperty("needsSetup", needsSetup());
		
		JsonObject jsonPaths = new JsonObject();
		json.add("paths", jsonPaths);
		TriConsumer<JsonObject, String, File> addPathProperty = (jsonObject, propertyName, file) -> {
			if(file == null) {
				jsonObject.add(propertyName, JsonNull.INSTANCE);
			} else {
				jsonObject.addProperty(propertyName, file.getAbsolutePath());
			}
		};
		addPathProperty.accept(jsonPaths, "steamappsFolder", getSteamappsFolder());
		addPathProperty.accept(jsonPaths, "exeFile", getExeFile());
		addPathProperty.accept(jsonPaths, "upkFile", getUpkFile());
		addPathProperty.accept(jsonPaths, "workshopFolder", getWorkshopFolder());
		
		json.addProperty("renameOriginalUnderpassUPK", getRenameOriginalUnderpassUPK());
		json.addProperty("behaviorWhenRLIsStopped", getBehaviorWhenRLIsStopped().toInt());
		json.addProperty("behaviorWhenRLIsRunning", getBehaviorWhenRLIsRunning().toInt());
		if(rlMapManager.isAutostartEnabled()) {
			json.addProperty("autostart", getAutostartOpenBrowser() ? 2 : 1);
		} else {
			json.addProperty("autostart", 0);
		}
		
		json.addProperty("upkFilename", getUpkFilename());
		json.addProperty("webInterfacePort", getWebInterfacePort());
		
		json.addProperty("mayLayout", getMapLayout().toInt());
		json.addProperty("mapSorting", getMapSorting());
		json.addProperty("showLoadedMapAtTop", getShowLoadedMapAtTop());
		json.addProperty("showFavoritesAtTop", getShowFavoritesAtTop());
		
		return GSON.toJson(json);
	}
	
	public void patchFromJson(String jsonString) {
		JsonObject json = GSON.fromJson(jsonString, JsonObject.class);
		
		if(json.has("renameOriginalUnderpassUPK")) {
			boolean oldValue = getRenameOriginalUnderpassUPK();
			boolean value = json.get("renameOriginalUnderpassUPK").getAsBoolean();
			setRenameOriginalUnderpassUPK(value);
			
			if(oldValue ^ value) {
				// this setting has changed
				if(!value) {
					// has been disabled
					rlMapManager.renameOriginalUnderpassUPK(true);
				} else if(getLoadedMapID() != 0) {
					// has been enabled and a map currently loaded
					rlMapManager.renameOriginalUnderpassUPK(false);
				}
			}
		}
		if(json.has("behaviorWhenRLIsStopped")) {
			Config.BehaviorWhenRLIsStopped value = Config.BehaviorWhenRLIsStopped.fromInt(json.get("behaviorWhenRLIsStopped").getAsInt());
			setBehaviorWhenRLIsStopped(value);
		}
		if(json.has("behaviorWhenRLIsRunning")) {
			Config.BehaviorWhenRLIsRunning value = Config.BehaviorWhenRLIsRunning.fromInt(json.get("behaviorWhenRLIsRunning").getAsInt());
			setBehaviorWhenRLIsRunning(value);
		}
		if(json.has("autostart")) {
			int value = json.get("autostart").getAsInt();
			switch(value) {
				case 0:
					rlMapManager.setAutostartEnabled(false);
					break;
				case 1:
					rlMapManager.setAutostartEnabled(true);
					setAutostartOpenBrowser(false);
					break;
				case 2:
					rlMapManager.setAutostartEnabled(true);
					setAutostartOpenBrowser(true);
					break;
			}
		}
		
		if(json.has("upkFilename")) {
			String value = json.get("upkFilename").getAsString();
			setUpkFilename(value);
		}
		if(json.has("webInterfacePort")) {
			int value = json.get("webInterfacePort").getAsInt();
			setWebInterfacePort(value);
		}
		
		if(json.has("mapLayout")) {
			Config.MapLayout value = Config.MapLayout.fromInt(json.get("mapLayout").getAsInt());
			setMapLayout(value);
		}
		if(json.has("mapSorting")) {
			int value = json.get("mapSorting").getAsInt();
			setMapSorting(value);
		}
		if(json.has("showLoadedMapAtTop")) {
			boolean value = json.get("showLoadedMapAtTop").getAsBoolean();
			setShowLoadedMapAtTop(value);
		}
		if(json.has("showFavoritesAtTop")) {
			boolean value = json.get("showFavoritesAtTop").getAsBoolean();
			setShowFavoritesAtTop(value);
		}
		
		save();
	}
	
	public enum BehaviorWhenRLIsStopped {
		DO_NOTHING,
		START_RL;
		
		public static BehaviorWhenRLIsStopped fromInt(int i) {
			return i == 0 ? DO_NOTHING : START_RL;
		}
		
		public int toInt() {
			return this == DO_NOTHING ? 0 : 1;
		}
	}
	
	public enum BehaviorWhenRLIsRunning {
		DO_NOTHING,
		STOP_RL,
		RESTART_RL;
		
		public static BehaviorWhenRLIsRunning fromInt(int i) {
			if(i == 0) {
				return DO_NOTHING;
			}
			return i == 1 ? STOP_RL : RESTART_RL;
		}
		
		public int toInt() {
			if(this == DO_NOTHING) {
				return 0;
			}
			return this == STOP_RL ? 1 : 2;
		}
	}
	
	public enum MapLayout {
		COMPACT_LIST,
		DETAILED_LIST,
		GRID_VIEW;
		
		public static MapLayout fromInt(int i) {
			if(i == 0) {
				return COMPACT_LIST;
			}
			return i == 1 ? DETAILED_LIST : GRID_VIEW;
		}
		
		public int toInt() {
			if(this == COMPACT_LIST) {
				return 0;
			}
			return this == DETAILED_LIST ? 1 : 2;
		}
	}
	
	private static class FileTypeAdapter extends TypeAdapter<File> {
		@Override
		public File read(JsonReader in) throws IOException {
			if(in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			return new File(in.nextString());
		}
		
		@Override
		public void write(JsonWriter out, File value) throws IOException {
			if(value == null) {
				out.nullValue();
			} else {
				out.value(value.getAbsolutePath());
			}
		}
	}
	
	@FunctionalInterface
	private interface TriConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}
}
