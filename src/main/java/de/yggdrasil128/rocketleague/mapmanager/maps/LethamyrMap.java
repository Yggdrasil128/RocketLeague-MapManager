package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.tools.GoogleDriveDownloader;
import de.yggdrasil128.rocketleague.mapmanager.tools.Task;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LethamyrMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(LethamyrMap.class.getName());
	private String urlName;
	
	public static LethamyrMap create(String urlName) {
		LethamyrMap map = new LethamyrMap();
		map.urlName = urlName;
		return map;
	}
	
	public String getUrlName() {
		return urlName;
	}
	
	@Override
	public String getURL() {
		return "https://lethamyr.com/mymaps/" + urlName;
	}
	
	@Override
	protected String getDiscriminator() {
		return urlName;
	}
	
	@Override
	public MapType getType() {
		return MapType.LETHAMYR;
	}
	
	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	private void fillMetadataFromJsoupDocument(Document doc) {
		Elements elements;
		
		elements = doc.getElementsByTag("h1");
		if(!elements.isEmpty()) {
			title = elements.first().text();
		}
		
		elements = doc.getElementsByTag("h3");
		String description = null;
		for(Element element : elements) {
			if(element.text().startsWith("Description") && description == null) {
				description = element.siblingElements().first().html();
			} else if(element.text().startsWith("Recommended Settings") && description != null) {
				//noinspection StringConcatenationInLoop
				description += "<br>Recommended Settings:<br>" + element.siblingElements().first().html();
			}
		}
		if(description != null) {
			this.description = description;
		}
		
		elements = doc.getElementsByClass("thumb-image");
		if(!elements.isEmpty()) {
			String src = elements.first().attr("data-src");
			downloadImage(src);
		}
	}
	
	@Override
	public void refreshMetadata() {
		try {
			Document doc = Jsoup.connect(getURL()).get();
			fillMetadataFromJsoupDocument(doc);
		} catch(Exception e) {
			logger.warn("Uncaught exception during refreshMetadata()", e);
		}
	}
	
	public static class MapDownload extends Task {
		private static final transient Logger logger = LoggerFactory.getLogger(MapDownload.class.getName());
		private static MapDownload task = null;
		private final RLMapManager rlMapManager;
		private final Runnable onFinish;
		private final String url;
		private File tempFile;
		private GoogleDriveDownloader googleDriveDownloader;
		
		private MapDownload(String url, RLMapManager rlMapManager, Runnable onFinish) {
			super();
			this.url = url;
			this.rlMapManager = rlMapManager;
			this.onFinish = onFinish;
		}
		
		public synchronized static MapDownload get() {
			return task;
		}
		
		public synchronized static MapDownload start(String url, RLMapManager rlMapManager) {
			return start(url, rlMapManager, null);
		}
		
		public synchronized static MapDownload start(String url, RLMapManager rlMapManager, Runnable onFinish) {
			if(task != null && task.isRunning()) {
				throw new IllegalStateException("Already running");
			}
			task = new MapDownload(url, rlMapManager, onFinish);
			task.start();
			return task;
		}
		
		public static boolean isTaskRunning() {
			if(task == null) {
				return false;
			}
			return task.isRunning();
		}
		
		@Override
		public Logger getLogger() {
			return logger;
		}
		
		@Override
		protected void run() throws Exception {
			statusMessage = "Checking URL...";
			
			String urlName = checkURL();
			LethamyrMap map = new LethamyrMap();
			map.urlName = urlName;
			map.udkFile = new File(RLMapManager.FILE_MAPS, map.getID() + ".udk");
			map.authorName = "Lethamyr";
			
			checkIfTaskIsCancelled();
			statusMessage = "Fetching map metadata from lethamyr.com...";
			
			Document doc = Jsoup.connect(map.getURL()).get();
			map.fillMetadataFromJsoupDocument(doc);
			
			Elements elements = doc.select("a[href*=drive.google.com]");
			if(elements.isEmpty()) {
				throw new Exception("Google Drive URL not found on lethamyr.com page");
			}
			
			String googleDriveURL = elements.first().attr("href");
			Pattern pattern = Pattern.compile("https?://drive\\.google\\.com/file/d/([^/]+)/");
			Matcher matcher = pattern.matcher(googleDriveURL);
			if(!matcher.find()) {
				throw new Exception("Google Drive URL not recognized");
			}
			String googleDriveID = matcher.group(1);
			tempFile = File.createTempFile("RLMM-download-lethamyr-" + map.getUrlName(), null);
			
			checkIfTaskIsCancelled();
			statusMessage = "Downloading...";
			
			googleDriveDownloader = new GoogleDriveDownloader(googleDriveID, tempFile);
			googleDriveDownloader.download();
			googleDriveDownloader = null;
			
			checkIfTaskIsCancelled();
			resetProgress();
			statusMessage = "Unzipping...";
			
			FileUtils.copyFile(tempFile, new File("C:\\Users\\Yggdrasil128\\temp\\" + map.urlName + ".zip"));
			
			ZipFile zipFile = new ZipFile(tempFile);
			ZipEntry zipEntry = SteamWorkshopMap.MapDownload.findUdkFile(zipFile);
			File targetFile = new File(RLMapManager.FILE_MAPS, map.getID() + ".udk");
			FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), targetFile);
			
			String udkFilename = zipEntry.getName();
			if(udkFilename.contains("/")) {
				udkFilename = udkFilename.substring(udkFilename.lastIndexOf('/') + 1);
			}
			map.udkFilename = udkFilename;
			map.udkFile = targetFile;
			
			rlMapManager.getConfig().registerMap(map);
			rlMapManager.getConfig().save();
			
			statusMessage = "Done. Successfully imported '" + map.getDisplayName() + "'.";
			
			if(onFinish != null) {
				onFinish.run();
			}
		}
		
		@Override
		protected void onCancel() {
			if(googleDriveDownloader != null) {
				googleDriveDownloader.cancel();
			}
		}
		
		@Override
		protected void cleanup() {
			FileUtils.deleteQuietly(tempFile);
		}
		
		@Override
		protected void beforeStatusQuery() {
			if(googleDriveDownloader != null) {
				statusMessage = googleDriveDownloader.getStatus();
			}
		}
		
		private String checkURL() throws Exception {
			if(url == null || url.isEmpty()) {
				throw new Exception("Please provide a valid URL to a lethamyr.com map");
			}
			
			Pattern pattern = Pattern.compile("https?://lethamyr\\.com/mymaps/([^/]+)");
			Matcher matcher = pattern.matcher(url);
			if(!matcher.find()) {
				throw new Exception("Please provide a valid URL to a lethamyr.com map");
			}
			return matcher.group(1);
		}
	}
}
