package de.yggdrasil128.rocketleague.mapmanager.game_discovery;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EpicGameDiscovery implements GameDiscovery {
	private static final File DEFAULT_EPIC_GAMES_FOLDER = new File("C:\\Program Files (x86)\\Epic Games");
	private static final String ROCKET_LEAGUE_APP_NAME = "rocketleague";
	
	public static File findDefaultAppInstallLocation() {
		File iniFile = new File(System.getenv("LOCALAPPDATA") + "\\EpicGamesLauncher\\Saved\\Config\\Windows\\GameUserSettings.ini");
		if(!iniFile.exists()) {
			return null;
		}
		final String data;
		try {
			data = FileUtils.readFileToString(iniFile, StandardCharsets.UTF_16);
		} catch(IOException e) {
			return null;
		}
		
		Pattern pattern = Pattern.compile("DefaultAppInstallLocation=(.*)[\\n\\r]");
		Matcher matcher = pattern.matcher(data);
		if(!matcher.find()) {
			return null;
		}
		String path = matcher.group(1).trim();
		return new File(path);
	}
	
	@SuppressWarnings("DuplicatedCode")
	@Override
	public Result discover(@Nullable File base, @Nullable RLMapManager rlMapManager) {
		if(base == null) {
			Result result = discover(DEFAULT_EPIC_GAMES_FOLDER, rlMapManager);
			if(result.isSuccess()) {
				return result;
			}
			File location = findDefaultAppInstallLocation();
			if(location != null) {
				return discover(location, rlMapManager);
			}
			return Result.error("Rocket League installation not found");
		}
		
		if(!base.exists()) {
			return Result.error("Epic Games folder doesn't exist");
		}
		
		File rocketLeagueGameFolder = new File(base.getAbsolutePath() + "\\" + ROCKET_LEAGUE_APP_NAME);
		if(!rocketLeagueGameFolder.exists()) {
			return Result.error("Rocket League installation not found");
		}
		
		File exeFile = GameDiscovery.findRocketLeagueExe(rocketLeagueGameFolder);
		if(exeFile == null) {
			return Result.error("Rocket League installation not found");
		}
		
		final String upkFilename = GameDiscovery.getDefaultUPKFilename(rlMapManager);
		
		File upkFile = new File(rocketLeagueGameFolder.getAbsolutePath() + "\\TAGame\\CookedPCConsole\\mods\\" + upkFilename);
		//noinspection ResultOfMethodCallIgnored
		upkFile.getParentFile().mkdirs();
		
		return Result.successEpic(exeFile, upkFile);
	}
}
