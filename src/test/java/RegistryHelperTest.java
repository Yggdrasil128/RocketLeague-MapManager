import de.yggdrasil128.rocketleague.mapmanager.RegistryHelper;
import org.junit.Assert;
import org.junit.Test;

public class RegistryHelperTest {
	@Test
	public void test() {
		String key = "HKCU\\Test";
		
		RegistryHelper.addKey(key);
		
		RegistryHelper.add(key, "foo", "bar");
		Assert.assertEquals("bar", RegistryHelper.query(key, "foo"));
		
		RegistryHelper.add(key, "foo", "\"bas\"");
		Assert.assertEquals("\"bas\"", RegistryHelper.query(key, "foo"));
		
		RegistryHelper.delete(key, "foo");
		Assert.assertNull(RegistryHelper.query(key, "foo"));
	}
}
