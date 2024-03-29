package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class SysTray {
	private final RLMapManager rlMapManager;
	//	private final Logger logger = LoggerFactory.getLogger(SysTray.class.getName());
	private Menu loadFavoriteMapMenu;
	private MenuItem startStopRLMenuItem;
	
	public SysTray(RLMapManager rlMapManager) throws Exception {
		if(!SystemTray.isSupported()) {
			throw new UnsupportedOperationException("SystemTray is not supported on this machine.");
		}
		this.rlMapManager = rlMapManager;
		
		createIcon();
	}
	
	private Image getImage() throws IOException {
		byte[] imageData = rlMapManager
				.getWebInterface()
				.getStaticFilesHttpHandler()
				.getFileData()
				.get("img/icon16.png");
		
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);
		return ImageIO.read(byteArrayInputStream);
	}
	
	private void createIcon() throws Exception {
		MenuItem menuItem1 = new MenuItem("Open in browser");
		loadFavoriteMapMenu = new Menu("Load favorite map");
		startStopRLMenuItem = new MenuItem("Start/Stop Rocket League");
		MenuItem menuItem4 = new MenuItem("Exit");
		
		menuItem1.addActionListener(event -> rlMapManager.getWebInterface().openInBrowser());
		startStopRLMenuItem.addActionListener(event -> {
			if(startStopRLMenuItem.getLabel().startsWith("Start")) {
				rlMapManager.startRocketLeague();
			} else {
				rlMapManager.stopRocketLeague();
			}
		});
		menuItem4.addActionListener(event -> System.exit(0));
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				
			}
		}, 0, 5000);
		
		PopupMenu popupMenu = new PopupMenu();
		popupMenu.add(menuItem1);
		popupMenu.addSeparator();
		popupMenu.add(loadFavoriteMapMenu);
		popupMenu.add(startStopRLMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(menuItem4);
		
		TrayIcon trayIcon = new TrayIcon(getImage(), "RL Map Manager", popupMenu);
		trayIcon.addMouseListener(new MouseListenerImpl());
		
		SystemTray.getSystemTray().add(trayIcon);
		
		updateLoadFavoriteMapMenu();
	}
	
	public void updateLoadFavoriteMapMenu() {
		loadFavoriteMapMenu.removeAll();
		
		// load favorite maps and sort them by title
		TreeMap<String, String> maps = new TreeMap<>();
		for(RLMap map : rlMapManager.getConfig().getMaps().values()) {
			if(map.isFavorite()) {
				maps.put(map.getDisplayName(), map.getID());
			}
		}
		
		if(maps.isEmpty()) {
			final MenuItem menuItem = new MenuItem("No favorite maps");
			menuItem.setEnabled(false);
			loadFavoriteMapMenu.add(menuItem);
			return;
		}
		
		String loadedMapID = rlMapManager.getConfig().getLoadedMapID();
		
		for(Map.Entry<String, String> entry : maps.entrySet()) {
			String mapName = entry.getKey();
			String mapID = entry.getValue();
			
			final CheckboxMenuItem menuItem = new CheckboxMenuItem(mapName);
			if(mapID.equals(loadedMapID)) {
				menuItem.setState(true);
				menuItem.addItemListener(event -> {
					rlMapManager.unloadMap();
					rlMapManager.getWebInterface().getApiHttpHandler().getLastUpdatedMaps().now(null);
				});
			} else {
				menuItem.setState(false);
				menuItem.addItemListener(event -> {
					final RLMap rlMap = rlMapManager.getConfig().getMaps().get(mapID);
					rlMapManager.loadMap(rlMap);
					rlMapManager.getWebInterface().getApiHttpHandler().getLastUpdatedMaps().now(null);
				});
			}
			
			loadFavoriteMapMenu.add(menuItem);
		}
	}
	
	private void onPopupMenuOpened() {
		if(rlMapManager.getRLProcessWatcher().isRunning()) {
			if(!startStopRLMenuItem.getLabel().equals("Stop Rocket League")) {
				startStopRLMenuItem.setLabel("Stop Rocket League");
			}
		} else {
			if(!startStopRLMenuItem.getLabel().equals("Start Rocket League")) {
				startStopRLMenuItem.setLabel("Start Rocket League");
			}
		}
	}
	
	private class MouseListenerImpl implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}
		
		@Override
		public void mousePressed(MouseEvent e) {
			if(e.isPopupTrigger()) {
				onPopupMenuOpened();
			}
			if(e.getClickCount() == 2) {
				rlMapManager.getWebInterface().openInBrowser();
			}
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			if(e.isPopupTrigger()) {
				onPopupMenuOpened();
			}
		}
		
		@Override
		public void mouseEntered(MouseEvent e) {
			
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			
		}
	}
}
