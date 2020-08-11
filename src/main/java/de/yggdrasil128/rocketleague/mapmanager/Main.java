package de.yggdrasil128.rocketleague.mapmanager;

import de.yggdrasil128.rocketleague.mapmanager.webui.WebInterface;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		boolean autostart = Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("--autostart"));
		
		File thisJarFile = getThisJarFile();
		if(thisJarFile == null || thisJarFile.getParentFile().equals(RLMapManager.FILE_ROOT)) {
			// is launching from IDE or within app folder
			RLMapManager rlMapManager = new RLMapManager();
			
			int port = rlMapManager.getConfig().getWebInterfacePort();
			if(WebInterface.isPortBound(port)) {
				int result = JOptionPane.showConfirmDialog(
						null,
						"RL Map Manager is already running. Would you like to open it in a new browser tab?",
						"Rocket League Map Manager",
						JOptionPane.YES_NO_OPTION);
				if(result == 0) {
					WebInterface.openInBrowser(port, Throwable::printStackTrace);
				}
				System.exit(0);
				return;
			}
			
			rlMapManager.start();
			if(!autostart || rlMapManager.getConfig().getAutostartOpenBrowser()) {
				rlMapManager.getWebInterface().openInBrowser();
			}
//			rlMapManager.getWebInterface().getIpWhitelist().setWhitelist(Collections.singleton(InetAddress.getByName("192.168.178.22")));
			return;
		}
		
		// this is a jar file running outside RLMapManager.FILE_ROOT
		// check if RLMM is installed in RLMapManager.FILE_ROOT
		File installedJarFile = findInstalledJarFile();
		if(installedJarFile != null) {
			int result = JOptionPane.showOptionDialog(
					null,
					"RL Map Manager is already installed. What would you like to do?",
					"Rocket League Map Manager",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					new String[]{"Start the app", "Reinstall", "Exit"},
					null);
			switch(result) {
				case 0:
					String command = "\"" + System.getProperty("java.home") + "\\bin\\java.exe\" -jar \"" + installedJarFile.getAbsolutePath() + "\"";
					Runtime.getRuntime().exec(command);
				case 2:
					return;
			}
		}
		
		// launch setup
		
		RLMapManager rlMapManager = new RLMapManager(true);
		int port = rlMapManager.getConfig().getWebInterfacePort();
		if(WebInterface.isPortBound(port)) {
			// try to tell the running server to shut down
			try {
				HttpURLConnection con = (HttpURLConnection) new URL("http://localhost:" + port + "/api/exitApp").openConnection();
				con.getResponseCode();
				con.disconnect();
			} catch(IOException ignored) {
				// if an exception occurred, then that means that the server is down now, so we can continue
			}
			
			// wait max 5 seconds
			int i;
			for(i = 0; i < 5; i++) {
				Thread.sleep(1000);
				if(!WebInterface.isPortBound(port)) {
					break;
				}
			}
			if(i == 5) {
				JOptionPane.showMessageDialog(
						null,
						"Cannot launch RL Map Manager installer because it is already running.",
						"Rocket League Map Manager",
						JOptionPane.ERROR_MESSAGE);
				System.exit(0);
				return;
			}
		}
		
		rlMapManager.start();
		rlMapManager.getWebInterface().openInBrowser();
	}
	
	public static File getThisJarFile() {
		try {
			File file = new File(RLMapManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(file.isFile()) {
				return file;
			}
		} catch(URISyntaxException ignored) {
		}
		return null;
	}
	
	public static File findInstalledJarFile() {
		final List<File> jarFiles = findInstalledJarFiles();
		if(jarFiles.isEmpty()) {
			return null;
		}
		return jarFiles.get(0);
	}
	
	public static List<File> findInstalledJarFiles() {
		ArrayList<File> jarFiles = new ArrayList<>();
		
		File[] files = RLMapManager.FILE_ROOT.listFiles();
		if(files == null) {
			return jarFiles;
		}
		
		for(File file : files) {
			if(!file.isFile()) {
				continue;
			}
			String name = file.getName();
			if(name.startsWith("RocketLeague-MapManager") && name.toLowerCase().endsWith(".jar")) {
				jarFiles.add(file);
			}
		}
		
		return jarFiles;
	}
	
	public static void deleteInstalledJarFiles() {
		//noinspection ResultOfMethodCallIgnored
		findInstalledJarFiles().forEach(File::delete);
	}
}
