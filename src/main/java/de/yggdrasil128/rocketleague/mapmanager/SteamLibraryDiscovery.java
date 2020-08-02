package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.config.Config;

import java.io.File;

public class SteamLibraryDiscovery {
	public static final File DEFAULT_STEAMAPPS_FOLDER = new File("C:\\Program Files (x86)\\Steam\\steamapps");
	private static final String ROCKET_LEAGUE_APP_NAME = "rocketleague";
	private static final int ROCKET_LEAGUE_APP_ID = 252950;
	//	private final Logger logger;
	private final RLMapManager rlMapManager;
	
	public SteamLibraryDiscovery(RLMapManager rlMapManager) {
//		logger = LoggerFactory.getLogger(StaticFilesHttpHandler.class);
		this.rlMapManager = rlMapManager;
	}
	
	public Result discoverSteamLibrary() {
		return discoverSteamLibrary(DEFAULT_STEAMAPPS_FOLDER);
	}
	
	public Result discoverSteamLibrary(File steamappsFolder) {
		if(!steamappsFolder.exists()) {
			return new Result(false, "steamapps folder doesn't exist");
		}
		
		File rocketLeagueGameFolder = new File(steamappsFolder.getAbsolutePath() + "\\common\\" + ROCKET_LEAGUE_APP_NAME);
		if(!rocketLeagueGameFolder.exists()) {
			return new Result(false, "Rocket League installation not found");
		}
		
		File exeFile = new File(rocketLeagueGameFolder.getAbsolutePath() + "\\Binaries\\RocketLeague.exe");
		if(!exeFile.exists()) {
			return new Result(false, "Rocket League installation not found");
		}
		
		File upkFile = new File(rocketLeagueGameFolder.getAbsolutePath() + "\\TAGame\\CookedPCConsole\\mods\\" + rlMapManager.getConfig().getUpkFilename());
		//noinspection ResultOfMethodCallIgnored
		upkFile.getParentFile().mkdirs();
		
		File workshopFolder = new File(steamappsFolder.getAbsolutePath() + "\\workshop\\content\\" + ROCKET_LEAGUE_APP_ID);
		
		return new Result(steamappsFolder, exeFile, upkFile, workshopFolder);
	}
	
	public static class Result {
		private final boolean success;
		private final String message;
		
		private File steamappsFolder;
		private File exeFile;
		private File upkFile;
		private File workshopFolder;
		
		private Result(boolean success, String message) {
			this.success = success;
			this.message = message;
		}
		
		private Result(File steamappsFolder, File exeFile, File upkFile, File workshopFolder) {
			this(true, "OK");
			
			this.steamappsFolder = steamappsFolder;
			this.exeFile = exeFile;
			this.upkFile = upkFile;
			this.workshopFolder = workshopFolder;
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public String getMessage() {
			return message;
		}
		
		public void saveToConfig(Config config) {
			if(!success) {
				throw new IllegalStateException();
			}
			
			config.setSteamappsFolder(steamappsFolder);
			config.setExeFile(exeFile);
			config.setUpkFile(upkFile);
			config.setWorkshopFolder(workshopFolder);
			config.save();
		}
	}
}
