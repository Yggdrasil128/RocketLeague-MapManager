import de.yggdrasil128.rocketleague.mapmanager.config.RLMapMetadata;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RlMapMetadataTest {
	@Test
	public void testRLMapMetadata() {
		RLMapMetadata rlMapMetadata = new RLMapMetadata(2142821184L);
		try {
			rlMapMetadata.fetchFromWorkshop();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		Assert.assertEquals("Minigolf", rlMapMetadata.getTitle());
		Assert.assertTrue(rlMapMetadata.getDescription().startsWith("Another Minigolf map. 9 holes in total. Hole scored = next course"));
		Assert.assertEquals("FroYo", rlMapMetadata.getAuthorName());
		Assert.assertEquals("image/png", rlMapMetadata.getImageFileMIMEType());
	}
}
