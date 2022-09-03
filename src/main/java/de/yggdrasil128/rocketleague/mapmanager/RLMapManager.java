package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.tools.RLProcessWatcher;
import de.yggdrasil128.rocketleague.mapmanager.tools.RegistryHelper;
import de.yggdrasil128.rocketleague.mapmanager.tools.UpdateChecker;
import de.yggdrasil128.rocketleague.mapmanager.webui.WebInterface;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class RLMapManager {
	public static final UpdateChecker.Version VERSION = new UpdateChecker.Version(2, 1, 2);
	public static final File FILE_ROOT;
	public static final File FILE_CONFIG;
	public static final File FILE_MAPS;
	public static final File FILE_MAP_IMAGES;
	private static final boolean USE_DEV_ENVIRONMENT = false;
	public static final String REGISTRY_AUTOSTART_KEY = "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run";
	public static final String REGISTRY_AUTOSTART_VALUE = "RL Map Manager";
	static final File FILE_LOG;
	
	static {
		String home = System.getProperty("user.home");
		FILE_ROOT = new File(home, USE_DEV_ENVIRONMENT ? "RL-MapManager DEV" : "RL-MapManager");
		FILE_CONFIG = new File(FILE_ROOT, "config.json");
		FILE_MAPS = new File(FILE_ROOT, "maps");
		FILE_MAP_IMAGES = new File(FILE_ROOT, "mapImages");
		FILE_LOG = new File(FILE_ROOT, "log.txt");
		//noinspection ResultOfMethodCallIgnored
		FILE_MAPS.mkdirs();
		
		// set up logging
		System.setProperty("org.slf4j.simpleLogger.logFile", FILE_LOG.getAbsolutePath());
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
		System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
	}
	
	private final Logger logger;
	private final Config config;
	private final WebInterface webInterface;
	private final UpdateChecker updateChecker;
	private final RLProcessWatcher rlProcessWatcher;
	private final boolean isSetupMode;
	private SysTray sysTray;
	
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
		updateChecker = new UpdateChecker();
		rlProcessWatcher = new RLProcessWatcher();
		
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
	
	public UpdateChecker getUpdateChecker() {
		return updateChecker;
	}
	
	public RLProcessWatcher getRLProcessWatcher() {
		return rlProcessWatcher;
	}
	
	public SysTray getSysTray() {
		return sysTray;
	}
	
	public void loadMap(RLMap map) {
		Integer pid = rlProcessWatcher.getPID();
		
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
		
		try {
			FileUtils.copyFile(map.getUdkFile(), config.getUpkFile());
		} catch(IOException e) {
			logger.error("Couldn't copy map to destination", e);
		}
		
		config.setLoadedMapID(map.getID());
		map.setLastLoadedNow();
		config.save();
		
		if(config.getRenameOriginalUnderpassUPK()) {
			renameOriginalUnderpassUPK(false);
		}
		
		if(sysTray != null) {
			sysTray.updateLoadFavoriteMapMenu();
		}
		
		if(startRL) {
			startRocketLeague(false);
		}
	}
	
	public void unloadMap() {
		if(config.getLoadedMapID() == null) {
			return;
		}
		//noinspection ResultOfMethodCallIgnored
		config.getUpkFile().delete();
		config.setLoadedMapID(null);
		config.save();
		
		if(config.getRenameOriginalUnderpassUPK()) {
			renameOriginalUnderpassUPK(true);
		}
		
		if(sysTray != null) {
			sysTray.updateLoadFavoriteMapMenu();
		}
	}
	
	public void stopRocketLeague() {
		Integer pid = rlProcessWatcher.getPID(); // prevent TOC/TOU
		if(pid != null) {
			stopRocketLeague(pid);
		}
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
		if(checkIfRunning && rlProcessWatcher.isRunning()) {
			return;
		}
		
		try {
			if(config.getPlatform() == Config.Platform.EPIC) {
				URI uri = new URI("com.epicgames.launcher://apps/Sugar?action=launch&silent=true");
				if(Desktop.isDesktopSupported()) {
					Desktop.getDesktop().browse(uri);
				}
			} else {
				Runtime.getRuntime().exec(config.getExeFile().getAbsolutePath());
			}
		} catch(Exception e) {
			logger.warn("startRocketLeague", e);
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
}
