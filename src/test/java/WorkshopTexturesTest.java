import de.yggdrasil128.rocketleague.mapmanager.tools.WorkshopTextures;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class WorkshopTexturesTest {
	private static File getTestFolder() {
		String home = System.getProperty("user.home");
		return new File(home, "RLMM WorkshopTexturesTest");
	}
	
	@Test
	public void test() throws InterruptedException {
		File cookedFolder = getTestFolder();
		
		FileUtils.deleteQuietly(cookedFolder);
		
		Assert.assertFalse(WorkshopTextures.checkIfInstalled(cookedFolder));
		
		WorkshopTextures.InstallTask task = WorkshopTextures.InstallTask.create(cookedFolder);
		task.start();
		while(task.isRunning()) {
			//noinspection BusyWait
			Thread.sleep(500);
			System.out.println(task.getStatusMessage());
		}
		
		Assert.assertTrue(WorkshopTextures.checkIfInstalled(cookedFolder));
		
		FileUtils.deleteQuietly(cookedFolder);
	}
}
