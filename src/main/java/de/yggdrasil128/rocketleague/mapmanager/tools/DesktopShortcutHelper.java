package de.yggdrasil128.rocketleague.mapmanager.tools;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DesktopShortcutHelper {
	public static void createShortcut(File jarFile) {
		createShortcut(new File(System.getProperty("user.home") + "\\Desktop\\RL Map Manager.lnk"), jarFile);
	}
	
	public static void createShortcut(File shortcutFile, File jarFile) {
		try {
			ShellLink shellLink = new ShellLink();
			shellLink.setTarget(System.getProperty("java.home") + "\\bin\\javaw.exe");
			shellLink.setCMDArgs("-jar \"" + jarFile.getAbsolutePath() + "\"");
			shellLink.setWorkingDir(RLMapManager.FILE_ROOT.getAbsolutePath());
			shellLink.setIconLocation(RLMapManager.FILE_ROOT.getAbsolutePath() + "\\app.ico");
			shellLink.saveTo(shortcutFile.getAbsolutePath());
		} catch(IOException e) {
			LoggerFactory.getLogger(DesktopShortcutHelper.class.getName()).warn("createShortcut", e);
		}
	}
	
	public static File findShortcut() {
		try {
			File desktop = new File(System.getProperty("user.home"), "Desktop");
			final File[] files = desktop.listFiles();
			if(files == null) {
				throw new IOException("listFiles returned null");
			}
			
			for(File file : files) {
				if(!file.getName().toLowerCase().endsWith(".lnk")) {
					continue;
				}
				
				byte[] dataRaw = FileUtils.readFileToByteArray(file);
				
				// remove null bytes
				byte[] data = new byte[dataRaw.length];
				int i = 0;
				for(byte b : dataRaw) {
					if(b == 0) {
						continue;
					}
					data[i++] = b;
				}
				String s = new String(data, 0, i, StandardCharsets.US_ASCII);
				
				if(s.contains("RocketLeague-MapManager")) {
					return file;
				}
			}
		} catch(IOException e) {
			LoggerFactory.getLogger(DesktopShortcutHelper.class.getName()).warn("findShortcut", e);
		}
		return null;
	}
	
	public static void createOrUpdateShortcut(File jarFile) {
		File shortcutFile = findShortcut();
		if(shortcutFile != null) {
			createShortcut(shortcutFile, jarFile);
		} else {
			createShortcut(jarFile);
		}
	}
	
	public static void deleteIfExists() {
		File shortcutFile = findShortcut();
		if(shortcutFile != null) {
			//noinspection ResultOfMethodCallIgnored
			shortcutFile.delete();
		}
	}
	
	public static void saveAppIcon(RLMapManager rlMapManager) {
		final byte[] bytes = rlMapManager.getWebInterface().getStaticFilesHttpHandler().getFileData().get("img/icon48.ico");
		File file = new File(RLMapManager.FILE_ROOT, "app.ico");
		try {
			FileUtils.writeByteArrayToFile(file, bytes);
		} catch(IOException e) {
			LoggerFactory.getLogger(DesktopShortcutHelper.class.getName()).warn("saveAppIcon", e);
		}
	}
}
