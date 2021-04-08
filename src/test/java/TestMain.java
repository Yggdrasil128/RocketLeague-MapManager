import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;

import java.io.IOException;

public class TestMain {
	public static void main(String[] args) throws IOException, InterruptedException {
		long time = System.currentTimeMillis();
		String json = "{\"mapType\": \"LETHAMYR\", \"title\": \"Test\"}";
		RLMap map = Config.GSON.fromJson(json, RLMap.class);
		System.out.println("[" + (System.currentTimeMillis() - time) + "] " + (map instanceof SteamWorkshopMap));
		json = Config.GSON.toJson(map, RLMap.class);
		System.out.println("[" + (System.currentTimeMillis() - time) + "] " + json);
		
		time = System.currentTimeMillis();
		for(int i = 0; i < 100; i++) {
			map = Config.GSON.fromJson(json, RLMap.class);
			System.out.println(i);
		}
		System.out.println("Deserialization: " + (System.currentTimeMillis() - time) / 100);
		
		time = System.currentTimeMillis();
		for(int i = 0; i < 100; i++) {
			json = Config.GSON.toJson(map, RLMap.class);
			System.out.println(i);
		}
		System.out.println("Serialization: " + (System.currentTimeMillis() - time) / 100);
	}
}
