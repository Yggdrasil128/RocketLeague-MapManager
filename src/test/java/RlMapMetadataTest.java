import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RlMapMetadataTest {
	@Test
	public void testRLMapMetadata() {
		SteamWorkshopMap map = SteamWorkshopMap.create(2142821184L, null);
		try {
			map.fetchDataFromWorkshop();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		Assert.assertEquals("Minigolf", map.getTitle());
		Assert.assertTrue(map.getDescription().startsWith("Another Minigolf map. 9 holes in total. Hole scored = next course"));
		Assert.assertEquals("FroYo", map.getAuthorName());
		Assert.assertEquals("image/png", map.getImageFileMimeType());
	}
}
