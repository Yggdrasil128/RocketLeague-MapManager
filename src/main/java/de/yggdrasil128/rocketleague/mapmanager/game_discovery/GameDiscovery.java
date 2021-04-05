package de.yggdrasil128.rocketleague.mapmanager.game_discovery;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

public interface GameDiscovery {
	static GameDiscovery forInstallationType(Config.InstallationType installationType) {
		switch(installationType) {
			case STEAM:
				return new SteamGameDiscovery();
			case EPIC:
				return new EpicGameDiscovery();
			default:
				throw new IllegalStateException("Unexpected value: " + installationType);
		}
	}
	
	static Result discover(Config.InstallationType installationType, File base, RLMapManager rlMapManager) {
		return forInstallationType(installationType).discover(base, rlMapManager);
	}
	
	static Result chooseFolderAndDiscover(Config.InstallationType installationType, RLMapManager rlMapManager) {
		File folder = chooseFolder(installationType);
		if(folder == null) {
			return null;
		}
		
		return discover(installationType, folder, rlMapManager);
	}
	
	static File chooseFolder(Config.InstallationType installationType) {
		// we need this JFrame in order for the JFileChooser to be always on top of other applications
		final JFrame jFrame = new JFrame();
		jFrame.setAlwaysOnTop(true);
		jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jFrame.setLocationRelativeTo(null);
		jFrame.requestFocus();
		
		final JFileChooser chooser = new JFileChooser(System.getenv("SystemDrive"));
		
		String title;
		if(installationType == Config.InstallationType.STEAM) {
			title = "Choose your steamapps folder";
		} else {
			title = "Choose your Epic Games folder";
		}
		chooser.setDialogTitle(title);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		final int result = chooser.showOpenDialog(jFrame);
		jFrame.dispose();
		
		if(result == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		
		return null;
	}
	
	static File findRocketLeagueExe(File installationLocation) {
		final String path = installationLocation.getAbsolutePath();
		
		File exeFile = new File(path + "\\Binaries\\RocketLeague.exe");
		if(!exeFile.exists()) {
			exeFile = new File(path + "\\Binaries\\Win64\\RocketLeague.exe");
		}
		if(!exeFile.exists()) {
			exeFile = new File(path + "\\Binaries\\Win32\\RocketLeague.exe");
		}
		if(!exeFile.exists()) {
			return null;
		} else {
			return exeFile;
		}
	}
	
	static String getDefaultUPKFilename(@Nullable RLMapManager rlMapManager) {
		return rlMapManager == null ? Config.DEFAULT_UPK_FILENAME : rlMapManager.getConfig().getUpkFilename();
	}
	
	Result discover(File base, RLMapManager rlMapManager);
	
	class Result {
		private boolean success;
		private String message;
		
		private Config.InstallationType installationType;
		private File exeFile;
		private File upkFile;
		
		private File steamappsFolder;
		private File steamWorkshopFolder;
		
		private Result() {
		}
		
		public static Result error(String message) {
			Result result = new Result();
			result.success = false;
			result.message = message;
			
			return result;
		}
		
		public static Result successSteam(File exeFile, File upkFile, File steamappsFolder, File steamWorkshopFolder) {
			Result result = new Result();
			result.success = true;
			result.message = "OK";
			
			result.installationType = Config.InstallationType.STEAM;
			result.exeFile = exeFile;
			result.upkFile = upkFile;
			result.steamappsFolder = steamappsFolder;
			result.steamWorkshopFolder = steamWorkshopFolder;
			
			return result;
		}
		
		public static Result successEpic(File exeFile, File upkFile) {
			Result result = new Result();
			result.success = true;
			result.message = "OK";
			
			result.installationType = Config.InstallationType.EPIC;
			result.exeFile = exeFile;
			result.upkFile = upkFile;
			
			return result;
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public String getMessage() {
			return message;
		}
		
		public File getSteamappsFolder() {
			return steamappsFolder;
		}
		
		public File getExeFile() {
			return exeFile;
		}
		
		public File getUpkFile() {
			return upkFile;
		}
		
		public File getSteamWorkshopFolder() {
			return steamWorkshopFolder;
		}
		
		public void saveToConfig(Config config, boolean saveConfig) {
			if(!success) {
				throw new IllegalStateException();
			}
			
			config.setInstallationType(installationType);
			config.setSteamappsFolder(steamappsFolder);
			config.setExeFile(exeFile);
			config.setUpkFile(upkFile);
			config.setWorkshopFolder(steamWorkshopFolder);
			
			if(saveConfig) {
				config.save();
			}
		}
	}
}
