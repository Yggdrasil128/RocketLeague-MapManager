import de.yggdrasil128.rocketleague.mapmanager.tools.GoogleDriveDownloader;

public class TestMain {
	@SuppressWarnings("SpellCheckingInspection")
	public static void main(String[] args) throws Exception {
		// has virus warning
		String id = "1ByRe2zLu4uqvK6zCXJBlEgYrq6tKB5ao";
		// has no virus warning
//		String id = "1rsbKLrrnQphOpul7NSZqmRqDj-cVNb4d";
		
		GoogleDriveDownloader gdd = new GoogleDriveDownloader(id, null);
		gdd.download();
	}
}
