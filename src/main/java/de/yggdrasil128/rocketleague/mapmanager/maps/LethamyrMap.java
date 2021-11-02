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
	public boolean canBeDeleted() {
		return true;
	}
	
	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	private boolean fillMetadataFromJsoupDocument(Document doc) {
		boolean result = true;
		Elements elements;
		Element element;
		
		authorName = "Lethamyr";
		
		element = doc.getElementsByTag("h1").first();
		if(element != null) {
			title = element.text();
		} else {
			result = false;
		}
		
		elements = doc.getElementsByTag("h3");
		String description = null;
		for(Element element2 : elements) {
			if(element2.text().startsWith("Description") && description == null) {
				element = element2.siblingElements().first();
				if(element != null) {
					description = element.html();
				}
			} else if(element2.text().startsWith("Recommended Settings") && description != null) {
				element = element2.siblingElements().first();
				if(element != null) {
					//noinspection StringConcatenationInLoop
					description += "<br>Recommended Settings:<br>" + element.html();
				}
			}
		}
		if(description != null) {
			this.description = description;
		} else {
			result = false;
		}
		
		element = doc.getElementsByClass("thumb-image").first();
		if(element != null) {
			String src = element.attr("data-src");
			downloadImage(src);
		} else {
			result = false;
		}
		
		if(!result) {
			logger.warn("Raw page HTML: ");
			logger.warn(doc.html());
		}
		
		return result;
	}
	
	@Override
	public boolean refreshMetadata() {
		try {
			Document doc = Jsoup.connect(getURL()).get();
			return fillMetadataFromJsoupDocument(doc);
		} catch(Exception e) {
			logger.warn("Uncaught exception during refreshMetadata()", e);
			return false;
		}
	}
	
	public static class MapDownload extends Task {
		private static final transient Logger logger = LoggerFactory.getLogger(MapDownload.class.getName());
		private static MapDownload task = null;
		private final RLMapManager rlMapManager;
		private final String url;
		private File tempFile;
		private GoogleDriveDownloader googleDriveDownloader;
		
		private MapDownload(String url, RLMapManager rlMapManager) {
			super();
			this.url = url;
			this.rlMapManager = rlMapManager;
		}
		
		public synchronized static MapDownload get() {
			return task;
		}
		
		public synchronized static MapDownload create(String url, RLMapManager rlMapManager) {
			if(task != null && task.isRunning()) {
				throw new IllegalStateException("Already running");
			}
			task = new MapDownload(url, rlMapManager);
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
			if(rlMapManager.getConfig().getMaps().containsKey(MapType.LETHAMYR.getAbbreviation() + "-" + urlName)) {
				throw new Exception("Map is already downloaded.");
			}
			LethamyrMap map = new LethamyrMap();
			map.urlName = urlName;
			map.udkFile = new File(RLMapManager.FILE_MAPS, map.getID() + ".udk");
			
			checkIfTaskIsCancelled();
			statusMessage = "Fetching map metadata from lethamyr.com...";
			
			Document doc = Jsoup.connect(map.getURL()).get();
			map.fillMetadataFromJsoupDocument(doc);
			
			Element element = doc.select("a[href*=drive.google.com]").first();
			if(element == null) {
				throw new Exception("Google Drive URL not found on lethamyr.com page");
			}
			
			String googleDriveURL = element.attr("href");
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
			
			Pattern pattern = Pattern.compile("https?://lethamyr\\.com/mymaps/([^/?]+)");
			Matcher matcher = pattern.matcher(url);
			if(!matcher.find()) {
				throw new Exception("Please provide a valid URL to a lethamyr.com map");
			}
			return matcher.group(1);
		}
	}
}
