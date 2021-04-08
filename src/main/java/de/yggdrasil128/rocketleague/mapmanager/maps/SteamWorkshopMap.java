package de.yggdrasil128.rocketleague.mapmanager.maps;

import com.google.gson.JsonObject;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.HashSet;
import java.util.LinkedList;

import static de.yggdrasil128.rocketleague.mapmanager.config.Config.GSON;

public class SteamWorkshopMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(SteamWorkshopMap.class.getName());
	
	private long workshopID;
	
	public static SteamWorkshopMap create(long workshopID, File udkFile) {
		SteamWorkshopMap map = new SteamWorkshopMap();
		map.workshopID = workshopID;
		map.udkFile = udkFile;
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
			title = elements.get(0).text();
		} else {
			logger.warn("Couldn't find title for workshop item " + workshopID);
			title = null;
		}
		
		elements = doc.getElementsByClass("workshopItemDescription");
		if(!elements.isEmpty()) {
			description = elements.get(0).text();
		} else {
			logger.warn("Couldn't find description for workshop item " + workshopID);
			description = "Error: Couldn't fetch information about this map from the Steam workshop " +
						  "because the associated workshop page no longer exists.";
		}
		
		elements = doc.getElementsByClass("friendBlockContent");
		if(!elements.isEmpty()) {
			authorName = elements.get(0).ownText();
		} else {
			logger.warn("Couldn't find authorName for workshop item " + workshopID);
			authorName = "Unknown";
		}
		
		elements = doc.getElementsByClass("workshopItemPreviewImageEnlargeable");
		for(Element element : elements) {
			String src = element.parent().attr("onclick");
			int start = src.indexOf('\'');
			int end = src.lastIndexOf('\'');
			src = src.substring(start + 1, end);
			
			try {
				URL url = new URL(src);
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				if(con.getResponseCode() != 200) {
					return;
				}
				
				clearImageFile();
				
				String mimeType = con.getHeaderField("Content-Type");
				imageFile = new File(IMAGES_FOLDER, getID() + getImageFileExtension(mimeType));
				imageFileMimeType = mimeType;
				
				FileUtils.copyInputStreamToFile(con.getInputStream(), imageFile);
			} catch(RuntimeException e) {
				logger.warn("Couldn't load image url '" + src + "' for workshop item " + workshopID);
				continue;
			}
			break;
		}
	}
	
	public static class MapDiscovery {
		private static MapDiscovery task = null;
		private static Logger logger;
		private final RLMapManager rlMapManager;
		private final Thread thread;
		private int progress, progressTarget;
		private Throwable throwable = null;
		
		private MapDiscovery(RLMapManager rlMapManager) {
			if(logger == null) {
				logger = LoggerFactory.getLogger(MapDiscovery.class.getName());
			}
			this.rlMapManager = rlMapManager;
			thread = new Thread(this::run);
			progress = 0;
			progressTarget = 0;
			thread.start();
		}
		
		public synchronized static MapDiscovery get() {
			return task;
		}
		
		public synchronized static void start(RLMapManager rlMapManager) {
			if(task != null && !task.isDone()) {
				throw new IllegalStateException("Already running");
			}
			if(rlMapManager.getConfig().getPlatform() != Config.Platform.STEAM || rlMapManager.getConfig().getWorkshopFolder() == null) {
				throw new IllegalStateException("Steam workshop map discovery is only available for Steam installations");
			}
			task = new MapDiscovery(rlMapManager);
		}
		
		public static String getStatusJson() {
			JsonObject json = new JsonObject();
			if(task == null) {
				json.addProperty("progressFloat", 0);
				json.addProperty("isDone", true);
				json.addProperty("message", "Not started");
				return GSON.toJson(json);
			}
			
			boolean isDone = task.isDone();
			json.addProperty("isDone", isDone);
			
			if(isDone) {
				Throwable throwable = task.getThrowable();
				if(throwable == null) {
					int mapCount = task.rlMapManager.getConfig().getMaps().size();
					String s = "Successfully discovered " + mapCount + (mapCount == 1 ? " map." : " maps.");
					json.addProperty("message", s);
					return GSON.toJson(json);
				}
				
				json.addProperty("message", "Error:<br />" + throwable);
				return GSON.toJson(json);
			}
			
			json.addProperty("message", "Discovering maps, please wait...");
			json.addProperty("progress", task.getProgress());
			json.addProperty("progressTarget", task.getProgressTarget());
			return GSON.toJson(json);
		}
		
		public boolean isDone() {
			return !thread.isAlive();
		}
		
		public int getProgress() {
			return progress;
		}
		
		public int getProgressTarget() {
			return progressTarget;
		}
		
		public Throwable getThrowable() {
			return throwable;
		}
		
		private void run() {
			logger.info("Starting Map Discovery");
			try {
				final HashSet<Long> knownMaps = getKnownMaps();
				final LinkedList<Pair<Long, File>> installedMaps = getInstalledMaps();
				progressTarget = installedMaps.size();
				
				for(Pair<Long, File> installedMap : installedMaps) {
					progress++;
					long id = installedMap.getLeft();
					File udkFile = installedMap.getRight();
					
					if(knownMaps.contains(id)) {
						knownMaps.remove(id);
						continue;
					}
					
					// map is new
					SteamWorkshopMap map = SteamWorkshopMap.create(id, udkFile);
					map.fetchDataFromWorkshop();
					rlMapManager.getConfig().registerMap(map);
				}
				
				// at this point, knownMaps contains only maps that are not installed anymore
				// -> delete them
				for(Long id : knownMaps) {
					String key = MapType.STEAM_WORKSHOP.getAbbreviation() + "-" + id;
					RLMap map = rlMapManager.getConfig().getMaps().get(key);
					if(map != null) {
						rlMapManager.getConfig().deleteMap(map);
					}
				}
			} catch(Exception e) {
				throwable = e;
				logger.error("Fatal error", e);
			}
			logger.info("Finished Map Discovery");
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
}
