package de.yggdrasil128.rocketleague.mapmanager.maps;

import com.google.gson.JsonObject;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.tools.SteamWorkshopDownloader;
import de.yggdrasil128.rocketleague.mapmanager.tools.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SteamWorkshopMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(SteamWorkshopMap.class.getName());
	
	private long workshopID;
	private boolean isManuallyDownloaded;
	private String udkFilename;
	
	public static SteamWorkshopMap create(long workshopID, File udkFile, String udkFilename, boolean isManuallyDownloaded) {
		SteamWorkshopMap map = new SteamWorkshopMap();
		map.workshopID = workshopID;
		map.udkFile = udkFile;
		map.udkFilename = udkFilename;
		map.isManuallyDownloaded = isManuallyDownloaded;
		return map;
	}
	
	public long getWorkshopID() {
		return workshopID;
	}
	
	@Override
	protected String getDiscriminator() {
		return String.valueOf(workshopID);
	}
	
	@Override
	public MapType getType() {
		return MapType.STEAM_WORKSHOP;
	}
	
	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	@Override
	public String getUdkFilename() {
		return udkFilename;
	}
	
	public boolean isManuallyDownloaded() {
		return isManuallyDownloaded;
	}
	
	@Override
	public void refreshMetadata() {
		try {
			fetchDataFromWorkshop();
		} catch(Exception e) {
			logger.warn("Uncaught exception during fetchDataFromWorkshop()", e);
		}
	}
	
	public void fetchDataFromWorkshop() throws IOException {
		Document doc;
		try {
			doc = Jsoup.connect("https://steamcommunity.com/sharedfiles/filedetails/?id=" + workshopID).timeout(5000).get();
		} catch(IOException e) {
			title = null;
			description = "Error: Couldn't fetch information about this map from the Steam workshop because an error occurred.";
			authorName = "Unknown";
			throw e;
		}
		
		Elements elements;
		
		elements = doc.getElementsByClass("workshopItemTitle");
		if(!elements.isEmpty()) {
			title = elements.first().text();
		} else {
			logger.warn("Couldn't find title for workshop item " + workshopID);
			title = null;
		}
		
		elements = doc.getElementsByClass("workshopItemDescription");
		if(!elements.isEmpty()) {
			description = elements.first().text();
		} else {
			logger.warn("Couldn't find description for workshop item " + workshopID);
			description = "Error: Couldn't fetch information about this map from the Steam workshop " +
						  "because the associated workshop page no longer exists.";
		}
		
		elements = doc.getElementsByClass("friendBlockContent");
		if(!elements.isEmpty()) {
			authorName = elements.first().ownText();
		} else {
			logger.warn("Couldn't find authorName for workshop item " + workshopID);
			authorName = "Unknown";
		}
		
		elements = doc.getElementsByClass("workshopItemPreviewImageEnlargeable");
		if(!elements.isEmpty()) {
			String src = elements.first().parent().attr("onclick");
			int start = src.indexOf('\'');
			int end = src.lastIndexOf('\'');
			src = src.substring(start + 1, end);
			
			downloadImage(src);
		}
	}
	
	public static class MapDiscovery extends Task {
		private static MapDiscovery task = null;
		private final RLMapManager rlMapManager;
		private final Runnable onFinish;
		
		private MapDiscovery(RLMapManager rlMapManager, Runnable onFinish) {
			super();
			this.rlMapManager = rlMapManager;
			this.onFinish = onFinish;
		}
		
		public synchronized static MapDiscovery get() {
			return task;
		}
		
		public synchronized static MapDiscovery start(RLMapManager rlMapManager) {
			return start(rlMapManager, null);
		}
		
		public synchronized static MapDiscovery start(RLMapManager rlMapManager, Runnable onFinish) {
			if(task != null && task.isRunning()) {
				throw new IllegalStateException("Already running");
			}
			if(rlMapManager.getConfig().getPlatform() != Config.Platform.STEAM || rlMapManager.getConfig().getWorkshopFolder() == null) {
				throw new IllegalStateException("Steam workshop map discovery is only available for Steam installations");
			}
			task = new MapDiscovery(rlMapManager, onFinish);
			task.start();
			return task;
		}
		
		@Override
		protected void run() throws Exception {
			statusMessage = "Scanning for maps...";
			final HashSet<Long> knownMaps = getKnownMaps();
			final LinkedList<Pair<Long, File>> installedMaps = getInstalledMaps();
			final LinkedList<Pair<Long, File>> newMaps = new LinkedList<>();
			
			for(Pair<Long, File> pair : installedMaps) {
				long id = pair.getLeft();
				
				if(knownMaps.contains(id)) {
					knownMaps.remove(id);
					continue;
				}
				
				// map is new
				newMaps.add(pair);
			}
			
			statusMessage = "Downloading map metadata from workshop...";
			showProgress = true;
			showPercentage = true;
			progressTarget = newMaps.size();
			
			LinkedList<SteamWorkshopMap> registrableMaps = new LinkedList<>();
			
			for(Pair<Long, File> pair : newMaps) {
				long id = pair.getLeft();
				File udkFile = pair.getRight();
				
				SteamWorkshopMap map = SteamWorkshopMap.create(id, udkFile, udkFile.getName(), false);
				map.fetchDataFromWorkshop();
				registrableMaps.add(map);
				
				progress++;
				
				if(isCancelled()) {
					for(SteamWorkshopMap map2 : registrableMaps) {
						map2.delete();
					}
					throw new InterruptedException();
				}
			}
			
			resetProgress();
			statusMessage = "Finishing up...";
			
			// register all new maps
			for(SteamWorkshopMap map : registrableMaps) {
				rlMapManager.getConfig().registerMap(map);
			}
			
			// at this point, knownMaps contains only maps that are not installed anymore
			// -> delete them, if they are not manually downloaded
			for(Long id : knownMaps) {
				String key = MapType.STEAM_WORKSHOP.getAbbreviation() + "-" + id;
				SteamWorkshopMap map = (SteamWorkshopMap) rlMapManager.getConfig().getMaps().get(key);
				if(map != null && !map.isManuallyDownloaded()) {
					rlMapManager.getConfig().deleteMap(map);
				}
			}
			
			rlMapManager.getConfig().save();
			
			if(onFinish != null) {
				onFinish.run();
			}
			
			String s = "Done. Added " + registrableMaps.size() + " map";
			if(registrableMaps.size() != 1) {
				s += "s";
			}
			s += ", removed " + knownMaps.size() + " map";
			if(knownMaps.size() != 1) {
				s += "s";
			}
			s += ".";
			statusMessage = s;
		}
		
		private HashSet<Long> getKnownMaps() {
			HashSet<Long> ids = new HashSet<>();
			for(RLMap map : rlMapManager.getConfig().getMaps().values()) {
				if(map.getType() != MapType.STEAM_WORKSHOP) {
					continue;
				}
				ids.add(((SteamWorkshopMap) map).getWorkshopID());
			}
			return ids;
		}
		
		private LinkedList<Pair<Long, File>> getInstalledMaps() {
			File workshopFolder = rlMapManager.getConfig().getWorkshopFolder();
			if(workshopFolder == null) {
				throw new IllegalStateException("Workshop folder doesn't exist");
			}
			
			LinkedList<Pair<Long, File>> maps = new LinkedList<>();
			
			File[] mapFolders = workshopFolder.listFiles();
			if(mapFolders == null) {
				return maps;
			}
			for(File mapFolder : mapFolders) {
				long id;
				try {
					id = Long.parseLong(mapFolder.getName());
				} catch(Exception e) {
					continue;
				}
				
				String[] files = mapFolder.list((dir, name) -> name.endsWith("udk"));
				if(files == null || files.length == 0) {
					continue;
				}
				
				File udkFile = new File(mapFolder, files[0]);
				maps.add(Pair.of(id, udkFile));
			}
			
			return maps;
		}
	}
	
	public static class MapDownload extends Task {
		private static MapDownload task = null;
		private final RLMapManager rlMapManager;
		private final Runnable onFinish;
		private final String url;
		private File tempFile;
		private SteamWorkshopDownloader steamWorkshopDownloader;
		
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
			if(rlMapManager.getConfig().getPlatform() == Config.Platform.STEAM) {
				throw new IllegalStateException("Direct Steam workshop map download is not supported for Steam installations");
			}
			task = new MapDownload(url, rlMapManager, onFinish);
			task.start();
			return task;
		}
		
		public static ZipEntry findUdkFile(ZipFile zipFile) throws Exception {
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			
			while(zipEntries.hasMoreElements()) {
				ZipEntry entry = zipEntries.nextElement();
				final String name = entry.getName();
				if(name.endsWith(".udk")) {
					return entry;
				}
			}
			
			throw new Exception("UDK file not found in downloaded zip.");
		}
		
		@Override
		protected void run() throws Exception {
			statusMessage = "Checking URL...";
			long workshopID = checkURL();
			String mapID = MapType.STEAM_WORKSHOP.getAbbreviation() + "-" + workshopID;
			
			if(rlMapManager.getConfig().getMaps().containsKey(mapID)) {
				throw new Exception("Map is already downloaded.");
			}
			
			Consumer<SteamWorkshopDownloader.State> stateChangeConsumer = state -> {
				switch(state) {
					case REQUESTING:
						statusMessage = "Preparing download...";
						break;
					case PREPARING:
						showPercentage = true;
						progressTarget = 100;
						break;
					case DOWNLOADING:
						statusMessage = "Downloading...";
						resetProgress();
						break;
				}
			};
			
			tempFile = File.createTempFile("RLMM-download-steam", mapID);
			steamWorkshopDownloader = new SteamWorkshopDownloader(workshopID, tempFile, stateChangeConsumer);
			steamWorkshopDownloader.download();
			steamWorkshopDownloader = null;
			
			resetProgress();
			statusMessage = "Unzipping...";
			
			ZipFile zipFile = new ZipFile(tempFile);
			ZipEntry zipEntry = findUdkFile(zipFile);
			String udkFilename = zipEntry.getName();
			File targetFile = new File(RLMapManager.FILE_MAPS, mapID + ".udk");
			
			if(udkFilename.contains("/")) {
				udkFilename = udkFilename.substring(udkFilename.lastIndexOf('/') + 1);
			}
			
			FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), targetFile);
			
			statusMessage = "Downloading map metadata from workshop...";
			
			SteamWorkshopMap map = create(workshopID, targetFile, udkFilename, true);
			map.fetchDataFromWorkshop();
			
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
			if(steamWorkshopDownloader == null || steamWorkshopDownloader.getState() == null) {
				return;
			}
			
			switch(steamWorkshopDownloader.getState()) {
				case PREPARING:
					JsonObject json = steamWorkshopDownloader.getPreparingStatusJson();
					if(json != null) {
						progress = json.get("progress").getAsLong();
					}
					break;
				case DOWNLOADING:
					statusMessage = "Downloading... " + steamWorkshopDownloader.getDownloadProgress();
					break;
			}
		}
		
		private long checkURL() throws Exception {
			if(url == null || url.isEmpty()) {
				throw new Exception("Please provide a valid URL to a Steam workshop item");
			}
			
			Pattern pattern = Pattern.compile("https?://steamcommunity\\.com/sharedfiles/filedetails/\\?id=(\\d+)");
			Matcher matcher = pattern.matcher(url);
			if(!matcher.find()) {
				throw new Exception("Please provide a valid URL to a Steam workshop item");
			}
			return Long.parseLong(matcher.group(1));
		}
	}
}
