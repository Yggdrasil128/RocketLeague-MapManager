package de.yggdrasil128.rocketleague.mapmanager.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
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
	private static final transient Logger logger = LoggerFactory.getLogger(Config.class);
	private final HashMap<Long, RLMapMetadata> mapMetadata = new HashMap<>();
	@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
	private int configVersion = CURRENT_CONFIG_VERSION;
	private File steamappsFolder = SteamLibraryDiscovery.DEFAULT_STEAMAPPS_FOLDER;
	private File exeFile;
	private File upkFile;
	private File workshopFolder;
	private int webInterfacePort = 16016;
	private long loadedMapID = 0;
	
	private boolean renameOriginalUnderpassUPK = false;
	private BehaviorWhenRLIsStopped behaviorWhenRLIsStopped = BehaviorWhenRLIsStopped.DO_NOTHING;
	private BehaviorWhenRLIsRunning behaviorWhenRLIsRunning = BehaviorWhenRLIsRunning.RESTART_RL;
	
	private MapLayout mapLayout = MapLayout.DETAILED_LIST;
	private int mapSorting = 1;
	private boolean showLoadedMapAtTop = false;
	private boolean showFavoritesAtTop = false;
	
	public static Config load() {
		try {
			String data = FileUtils.readFileToString(RLMapManager.FILE_CONFIG, StandardCharsets.UTF_8);
			Config config = GSON.fromJson(data, Config.class);
			
			if(config.configVersion == CURRENT_CONFIG_VERSION) {
				return config;
			}
			
			if(ConfigUpgrader.upgrade()) {
				data = FileUtils.readFileToString(RLMapManager.FILE_CONFIG, StandardCharsets.UTF_8);
				config = GSON.fromJson(data, Config.class);
				return config;
			}
			
			return new Config();
		} catch(FileNotFoundException e) {
			return new Config();
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
}
