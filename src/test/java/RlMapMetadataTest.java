import de.yggdrasil128.rocketleague.mapmanager.maps.LethamyrMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import org.junit.Assert;
import org.junit.Test;

public class RlMapMetadataTest {
	@Test
	public void testSteamWorkshopMapMetadata() {
		SteamWorkshopMap map = SteamWorkshopMap.create(2142821184L, null, null, false);
		map.refreshMetadata();
		
		Assert.assertEquals("Minigolf", map.getTitle());
		Assert.assertTrue(map.getDescription().startsWith("Another Minigolf map. 9 holes in total. Hole scored = next course"));
		Assert.assertEquals("FroYo", map.getAuthorName());
		Assert.assertEquals("image/png", map.getImageFileMimeType());
	}
	
	@Test
	public void testLethamyrMapMetadata() {
		LethamyrMap map = LethamyrMap.create("grand-hall");
		map.refreshMetadata();
		
		Assert.assertEquals("Grand Hall", map.getTitle());
		Assert.assertTrue(map.getDescription().startsWith("A freestyler’s paradise…"));
		Assert.assertTrue(map.getDescription().contains("Recommended Settings"));
		Assert.assertEquals("Lethamyr", map.getAuthorName());
		Assert.assertEquals("image/jpeg", map.getImageFileMimeType());
	}
}
