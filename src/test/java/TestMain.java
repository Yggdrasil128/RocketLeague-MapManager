import de.yggdrasil128.rocketleague.mapmanager.tools.GoogleDriveDownloader;

public class TestMain {
	@SuppressWarnings("SpellCheckingInspection")
	public static void main(String[] args) throws Exception {
		GoogleDriveDownloader gdd = new GoogleDriveDownloader("1rsbKLrrnQphOpul7NSZqmRqDj-cVNb4d", null);
		// https://drive.google.com/file/d/1rsbKLrrnQphOpul7NSZqmRqDj-cVNb4d/view?usp=sharing
		gdd.download();
	}
}
