package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.tools.ProgressInputStream;
import de.yggdrasil128.rocketleague.mapmanager.tools.Task;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LethamyrMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(LethamyrMap.class.getName());
	private String urlName, udkFilename;
	
	public static LethamyrMap create(String urlName) {
		LethamyrMap map = new LethamyrMap();
		map.urlName = urlName;
		return map;
	}
	
	public String getUrlName() {
		return urlName;
	}
	
	public String getUrl() {
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
	
	@Override
	public String getAuthorName() {
		return "Lethamyr";
	}
	
	@Override
	public String getUdkFilename() {
		return udkFilename;
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
			Document doc = Jsoup.connect(getUrl()).get();
			fillMetadataFromJsoupDocument(doc);
		} catch(Exception e) {
			logger.warn("Uncaught exception during refreshMetadata()", e);
		}
	}
	
	public static class MapDownload extends Task {
		private static MapDownload task = null;
		private final RLMapManager rlMapManager;
		private final Runnable onFinish;
		private final String url;
		private File tempFile;
		private ProgressInputStream progressInputStream;
		
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
		
		@Override
		protected void run() throws Exception {
			statusMessage = "Checking URL...";
			
			String urlName = checkURL();
			LethamyrMap map = new LethamyrMap();
			map.urlName = urlName;
			map.udkFile = new File(RLMapManager.FILE_MAPS, map.getID() + ".udk");
			
			statusMessage = "Fetching map metadata from lethamyr.com...";
			
			Document doc = Jsoup.connect(map.getUrl()).get();
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
			tempFile = File.createTempFile("RLMM-download-lethamyr", map.getUrlName());
			
			statusMessage = "Downloading...";
			
			googleDriveURL = "https://drive.google.com/uc?export=download&id=" + googleDriveID;
			HttpsURLConnection con = (HttpsURLConnection) new URL(googleDriveURL).openConnection();
			final int responseCode = con.getResponseCode();
			if(responseCode != 200) {
				throw new IOException("Google drive API returned unexpected response code " + responseCode);
			}
			
			progressInputStream = new ProgressInputStream(con.getInputStream(), 0);
			FileUtils.copyInputStreamToFile(progressInputStream, tempFile);
			progressInputStream = null;
			
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
			
			if(onFinish != null) {
				onFinish.run();
			}
		}
		
		@Override
		protected void cleanup() {
			FileUtils.deleteQuietly(tempFile);
		}
		
		@Override
		protected void beforeStatusQuery() {
			if(progressInputStream != null) {
				statusMessage = "Downloading... " + progressInputStream.getStatusString();
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
