package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.webui.WebInterface;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RLMapManager {
	public static final File FILE_ROOT;
	public static final File FILE_CONFIG;
	public static final UpdateChecker.Version VERSION = new UpdateChecker.Version(1, 2);
	static final File FILE_LOG;
	public static final String REGISTRY_AUTOSTART_KEY = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
	public static final String REGISTRY_AUTOSTART_VALUE = "RL Map Manager";
	private static final long IS_RL_RUNNING_CACHE_TTL = 4800;
	
	static {
		String home = System.getProperty("user.home");
		FILE_ROOT = new File(home, "RL-MapManager");
		FILE_CONFIG = new File(FILE_ROOT, "config.json");
		FILE_LOG = new File(FILE_ROOT, "log.txt");
		
		// set up logging
		//noinspection ResultOfMethodCallIgnored
		FILE_ROOT.mkdirs();
		System.setProperty("org.slf4j.simpleLogger.logFile", FILE_LOG.getAbsolutePath());
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
		System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
	}
	
	private final Logger logger;
	private final Config config;
	private final WebInterface webInterface;
	private final SteamLibraryDiscovery steamLibraryDiscovery;
	private final UpdateChecker updateChecker;
	private final boolean isSetupMode;
	private SysTray sysTray;
	private Map<Long, RLMap> maps;
	private boolean isRLRunningCache = false;
	private long isRLRunningCacheExpiry = 0;
	
	RLMapManager(boolean isSetupMode) {
		this.isSetupMode = isSetupMode;
		logger = LoggerFactory.getLogger(RLMapManager.class.getName());
		
		if(isSetupMode) {
			config = new Config(this);
		} else {
			config = Config.load(this);
		}
		assert config != null;
		
		webInterface = new WebInterface(this, config.getWebInterfacePort());
		steamLibraryDiscovery = new SteamLibraryDiscovery(this);
		updateChecker = new UpdateChecker();
	}
	
	RLMapManager() {
		this(false);
	}
	
	public void start() {
		if(isSetupMode) {
			webInterface.start(true);
			return;
		}
		
		config.save();
		
		if(!config.needsSetup()) {
			MapDiscovery.start(this);
		}
		
		webInterface.start(false);
		
		try {
			sysTray = new SysTray(this);
		} catch(Exception e) {
			logger.error("Couldn't create SysTray icon", e);
		}
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public WebInterface getWebInterface() {
		return webInterface;
	}
	
	public SteamLibraryDiscovery getSteamLibraryDiscovery() {
		return steamLibraryDiscovery;
	}
	
	public UpdateChecker getUpdateChecker() {
		return updateChecker;
	}
	
	public SysTray getSysTray() {
		return sysTray;
	}
	
	public Map<Long, RLMap> getMaps() {
		return maps;
	}
	
	void setMaps(HashMap<Long, RLMap> maps) {
		this.maps = Collections.unmodifiableMap(maps);
	}
	
	public void loadMap(RLMap map) throws IOException {
		Integer pid = getRocketLeaguePID();
		boolean stopRL, startRL;
		if(pid == null) {
			// Rocket League is stopped
			stopRL = false;
			startRL = config.getBehaviorWhenRLIsStopped() == Config.BehaviorWhenRLIsStopped.START_RL;
		} else {
			// Rocket League is running
			stopRL = config.getBehaviorWhenRLIsRunning() != Config.BehaviorWhenRLIsRunning.DO_NOTHING;
			startRL = config.getBehaviorWhenRLIsRunning() == Config.BehaviorWhenRLIsRunning.RESTART_RL;
		}
		
		if(stopRL) {
			stopRocketLeague(pid);
		}
		
		FileUtils.copyFile(map.getUdkFile(), config.getUpkFile());
		config.setLoadedMapID(map.getID());
		config.getMapMetadata(map.getID()).setLastLoadedTimestamp(System.currentTimeMillis());
		config.save();
		
		if(config.getRenameOriginalUnderpassUPK()) {
			renameOriginalUnderpassUPK(false);
		}
		
		if(startRL) {
			startRocketLeague(false);
		}
	}
	
	public void unloadMap() {
		if(config.getLoadedMapID() == 0) {
			return;
		}
		//noinspection ResultOfMethodCallIgnored
		config.getUpkFile().delete();
		config.setLoadedMapID(0);
		config.save();
		
		if(config.getRenameOriginalUnderpassUPK()) {
			renameOriginalUnderpassUPK(true);
		}
	}
	
	public boolean isRocketLeagueRunning() {
		long now = System.currentTimeMillis();
		if(now > isRLRunningCacheExpiry) {
			isRLRunningCache = getRocketLeaguePID() != null;
			isRLRunningCacheExpiry = now + IS_RL_RUNNING_CACHE_TTL;
		}
		return isRLRunningCache;
	}
	
	public void stopRocketLeague() {
		Integer pid = getRocketLeaguePID();
		if(pid == null) {
			return;
		}
		
		stopRocketLeague(pid);
	}
	
	private void stopRocketLeague(int pid) {
		try {
			Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
		} catch(IOException e) {
			logger.warn("stopRocketLeague", e);
		}
	}
	
	public void startRocketLeague() {
		startRocketLeague(true);
	}
	
	private void startRocketLeague(boolean checkIfRunning) {
		if(checkIfRunning && isRocketLeagueRunning()) {
			return;
		}
		
		try {
			Runtime.getRuntime().exec(config.getExeFile().getAbsolutePath());
		} catch(IOException e) {
			logger.warn("startRocketLeague_noCheck", e);
		}
	}
	
	public boolean isAutostartEnabled() {
		return RegistryHelper.query(REGISTRY_AUTOSTART_KEY, REGISTRY_AUTOSTART_VALUE) != null;
	}
	
	public void setAutostartEnabled(boolean enabled) {
		if(!enabled) {
			RegistryHelper.delete(REGISTRY_AUTOSTART_KEY, REGISTRY_AUTOSTART_VALUE);
			return;
		}
		File installedJarFile = Main.findInstalledJarFile();
		assert installedJarFile != null;
		String command = "\"" + System.getProperty("java.home") + "\\bin\\javaw.exe\" -jar \"" + installedJarFile.getAbsolutePath() + "\" --autostart";
		RegistryHelper.add(REGISTRY_AUTOSTART_KEY, REGISTRY_AUTOSTART_VALUE, command);
	}
	
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void renameOriginalUnderpassUPK(boolean undo) {
		File mapsFolder = config.getUpkFile().getParentFile().getParentFile();
		
		File originalUPK = new File(mapsFolder, "Labs_Underpass_P.upk");
		File renamedUPK = new File(mapsFolder, "Labs_Underpass_P.upk.UNUSED");
		
		File source = undo ? renamedUPK : originalUPK;
		File target = undo ? originalUPK : renamedUPK;
		
		if(target.exists()) {
			if(source.exists()) {
				source.delete();
			}
			return;
		}
		if(source.exists()) {
			source.renameTo(target);
		}
	}
	
	private Integer getRocketLeaguePID() {
		try {
			Process process = Runtime.getRuntime().exec("tasklist");
			try(BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while((line = input.readLine()) != null) {
					if(!line.startsWith("RocketLeague.exe")) {
						continue;
					}
					line = line.substring("RocketLeague.exe".length()).trim();
					int index = line.indexOf(' ');
					line = line.substring(0, index);
					return Integer.parseInt(line);
				}
			}
		} catch(Exception e) {
			logger.warn("getRocketLeaguePID", e);
		}
		return null;
	}
}
