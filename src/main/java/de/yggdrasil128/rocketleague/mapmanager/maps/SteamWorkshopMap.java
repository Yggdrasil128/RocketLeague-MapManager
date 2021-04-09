package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import de.yggdrasil128.rocketleague.mapmanager.tools.Task;
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

public class SteamWorkshopMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(SteamWorkshopMap.class.getName());
	
	private long workshopID;
	private boolean isManuallyDownloaded;
	
	public static SteamWorkshopMap create(long workshopID, File udkFile, boolean isManuallyDownloaded) {
		SteamWorkshopMap map = new SteamWorkshopMap();
		map.workshopID = workshopID;
		map.udkFile = udkFile;
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
			if(task != null && !task.isRunning()) {
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
			
			statusMessage = "Downloading map data from workshop...";
			showProgress = true;
			showPercentage = true;
			progressTarget = newMaps.size();
			
			LinkedList<SteamWorkshopMap> registrableMaps = new LinkedList<>();
			
			for(Pair<Long, File> pair : newMaps) {
				long id = pair.getLeft();
				File udkFile = pair.getRight();
				
				SteamWorkshopMap map = SteamWorkshopMap.create(id, udkFile, false);
				map.fetchDataFromWorkshop();
				registrableMaps.add(map);
				
				progress++;
				
				if(Thread.interrupted()) {
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
