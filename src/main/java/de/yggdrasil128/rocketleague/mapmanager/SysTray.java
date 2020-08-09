package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.config.RLMap;
import de.yggdrasil128.rocketleague.mapmanager.config.RLMapMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class SysTray {
	private final RLMapManager rlMapManager;
	private final Logger logger = LoggerFactory.getLogger(SysTray.class.getName());
	private Menu loadFavoriteMapMenu;
	
	public SysTray(RLMapManager rlMapManager) throws Exception {
		if(!SystemTray.isSupported()) {
			throw new UnsupportedOperationException("SystemTray is not supported on this machine.");
		}
		this.rlMapManager = rlMapManager;
		
		createIcon();
	}
	
	public static void main(String[] args) throws Exception {
		new SysTray(null);
	}
	
	@SuppressWarnings("unused")
	private Image getImage() throws IOException {
		byte[] imageData = rlMapManager
				.getWebInterface()
				.getStaticFilesHttpHandler()
				.getFileData()
				.get("img/favicon16.png");
		
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);
		return ImageIO.read(byteArrayInputStream);
	}
	
	private Image getImageFromFile() throws IOException {
		return ImageIO.read(new File("E:\\Dropbox\\Eclipse Workspace\\RocketLeague-MapManager\\src\\main\\resources\\webui\\img\\favicon16.png"));
	}
	
	private void createIcon() throws Exception {
		MenuItem menuItem1 = new MenuItem("Open in browser");
		loadFavoriteMapMenu = new Menu("Load favorite map");
		MenuItem menuItem3 = new MenuItem("Start/Stop Rocket League");
		MenuItem menuItem4 = new MenuItem("Exit");
		
		menuItem1.addActionListener(event -> rlMapManager.getWebInterface().openInBrowser());
		menuItem3.addActionListener(event -> {
			if(menuItem3.getLabel().startsWith("Start")) {
				rlMapManager.startRocketLeague();
				menuItem3.setLabel("Stop Rocket League");
			} else {
				rlMapManager.stopRocketLeague();
				menuItem3.setLabel("Start Rocket League");
			}
		});
		menuItem4.addActionListener(event -> System.exit(0));
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(rlMapManager.isRocketLeagueRunning()) {
					if(!menuItem3.getLabel().equals("Stop Rocket League")) {
						menuItem3.setLabel("Stop Rocket League");
					}
				} else {
					if(!menuItem3.getLabel().equals("Start Rocket League")) {
						menuItem3.setLabel("Start Rocket League");
					}
				}
			}
		}, 0, 5000);
		
		PopupMenu popupMenu = new PopupMenu();
		popupMenu.add(menuItem1);
		popupMenu.addSeparator();
		popupMenu.add(loadFavoriteMapMenu);
		popupMenu.add(menuItem3);
		popupMenu.addSeparator();
		popupMenu.add(menuItem4);
		
		TrayIcon trayIcon = new TrayIcon(getImageFromFile(), "RL Map Manager", popupMenu);
		SystemTray.getSystemTray().add(trayIcon);
		
		updateLoadFavoriteMapMenu();
	}
	
	public void updateLoadFavoriteMapMenu() {
		loadFavoriteMapMenu.removeAll();
		
		// load favorite maps and sort them by title
		TreeMap<String, Long> maps = new TreeMap<>();
		for(RLMap rlMap : rlMapManager.getMaps().values()) {
			final RLMapMetadata mapMetadata = rlMapManager.getConfig().getMapMetadata(rlMap.getID());
			if(mapMetadata.isFavorite()) {
				maps.put(mapMetadata.getTitle(), rlMap.getID());
			}
		}
		
		if(maps.isEmpty()) {
			final MenuItem menuItem = new MenuItem("No favorite maps");
			menuItem.setEnabled(false);
			loadFavoriteMapMenu.add(menuItem);
			return;
		}
		
		long loadedMapID = rlMapManager.getConfig().getLoadedMapID();
		
		for(Map.Entry<String, Long> entry : maps.entrySet()) {
			String mapName = entry.getKey();
			long mapID = entry.getValue();
			
			final CheckboxMenuItem menuItem = new CheckboxMenuItem(mapName);
			if(loadedMapID == mapID) {
				menuItem.setState(true);
				menuItem.addItemListener(event -> {
					rlMapManager.unloadMap();
					rlMapManager.getWebInterface().getApiHttpHandler().getLastUpdatedMaps().now(null);
					updateLoadFavoriteMapMenu();
					menuItem.setState(false);
				});
			} else {
				menuItem.setState(false);
				menuItem.addItemListener(event -> {
					try {
						final RLMap rlMap = rlMapManager.getMaps().get(mapID);
						rlMapManager.loadMap(rlMap);
						rlMapManager.getWebInterface().getApiHttpHandler().getLastUpdatedMaps().now(null);
						// set all other to unchecked
						for(int i = 0; i < loadFavoriteMapMenu.getItemCount(); i++) {
							CheckboxMenuItem menuItem1 = (CheckboxMenuItem) loadFavoriteMapMenu.getItem(i);
							menuItem1.setState(false);
						}
						menuItem.setState(true);
					} catch(IOException e) {
						logger.error("Couldn't load map", e);
					}
				});
			}
			
			loadFavoriteMapMenu.add(menuItem);
		}
	}
}
