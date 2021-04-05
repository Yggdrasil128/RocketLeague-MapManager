package de.yggdrasil128.rocketleague.mapmanager.game_discovery;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class SteamGameDiscovery implements GameDiscovery {
	private static final File DEFAULT_STEAMAPPS_FOLDER = new File("C:\\Program Files (x86)\\Steam\\steamapps");
	private static final String ROCKET_LEAGUE_APP_NAME = "rocketleague";
	private static final int ROCKET_LEAGUE_APP_ID = 252950;
	
	@SuppressWarnings("DuplicatedCode")
	@Override
	public Result discover(@Nullable File base, @Nullable RLMapManager rlMapManager) {
		if(base == null) {
			return discover(DEFAULT_STEAMAPPS_FOLDER, rlMapManager);
		}
		
		// steamappsFolder := base
		
		if(!base.exists()) {
			return Result.error("steamapps folder doesn't exist");
		}
		
		File rocketLeagueGameFolder = new File(base.getAbsolutePath() + "\\common\\" + ROCKET_LEAGUE_APP_NAME);
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
		
		File workshopFolder = new File(base.getAbsolutePath() + "\\workshop\\content\\" + ROCKET_LEAGUE_APP_ID);
		
		return Result.successSteam(exeFile, upkFile, base, workshopFolder);
	}
}
