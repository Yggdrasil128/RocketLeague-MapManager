import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.game_discovery.GameDiscovery;

public class TestMain {
	public static void main(String[] args) {
		GameDiscovery.Result result;
		
		System.out.println("Steam:");
		result = GameDiscovery.discover(Config.InstallationType.STEAM, null, null);
		if(!result.isSuccess()) {
			System.out.println("Error: " + result.getMessage());
		} else {
			System.out.println(result.getExeFile());
		}
		
		System.out.println("Epic:");
		result = GameDiscovery.discover(Config.InstallationType.EPIC, null, null);
		if(!result.isSuccess()) {
			System.out.println("Error: " + result.getMessage());
		} else {
			System.out.println(result.getExeFile());
		}
	}
}
