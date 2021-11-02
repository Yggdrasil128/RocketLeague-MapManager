package de.yggdrasil128.rocketleague.mapmanager.tools;

import de.yggdrasil128.rocketleague.mapmanager.config.Config;
import org.apache.commons.io.FileUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class WorkshopTextures {
	private static final String DOWNLOAD_URL = "https://www.dropbox.com/s/vzovkbygbdx3h05/RocketLeague-WorkshopTextures.zip?dl=1";
	private static final long DOWNLOAD_SIZE = 46_638_192;
	
	private static File getCookedFolder(Config config) {
		return config.getUpkFile().getParentFile().getParentFile();
	}
	
	public static boolean checkIfInstalled(Config config) {
		return checkIfInstalled(getCookedFolder(config));
	}
	
	public static boolean checkIfInstalled(File cookedFolder) {
		File testFile = new File(cookedFolder, "EditorLandscapeResources.upk");
		return testFile.exists();
	}
	
	public static class InstallTask extends Task {
		private static InstallTask task = null;
		private final File cookedFolder;
		private File tempFile;
		private ProgressInputStream progressInputStream = null;
		
		private InstallTask(File cookedFolder) {
			this.cookedFolder = cookedFolder;
			task = this;
		}
		
		public static InstallTask get() {
			return task;
		}
		
		public synchronized static InstallTask create(Config config) {
			return create(getCookedFolder(config));
		}
		
		public synchronized static InstallTask create(File cookedFolder) {
			if(task != null && task.isRunning()) {
				throw new IllegalStateException("Already running");
			}
			task = new InstallTask(cookedFolder);
			return task;
		}
		
		public static boolean isTaskRunning() {
			if(task == null) {
				return false;
			}
			return task.isRunning();
		}
		
		@Override
		protected void run() throws Exception {
			statusMessage = "Preparing...";
			tempFile = File.createTempFile("RLMM-download-workshopTextures", null);
			
			statusMessage = "Downloading...";
			download();
			
			statusMessage = "Unzipping...";
			unzip();
			
			statusMessage = null;
		}
		
		private void download() throws IOException {
			HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL(DOWNLOAD_URL).openConnection();
			final int responseCode = httpsURLConnection.getResponseCode();
			if(responseCode != 200) {
				throw new IOException("Got unexpected response code " + responseCode);
			}
			
			progressInputStream = new ProgressInputStream(httpsURLConnection.getInputStream(), DOWNLOAD_SIZE);
			FileUtils.copyInputStreamToFile(progressInputStream, tempFile);
			progressInputStream = null;
		}
		
		private void unzip() throws IOException {
			try(ZipFile zipFile = new ZipFile(tempFile)) {
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while(entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					File file = new File(cookedFolder, entry.getName());
					FileUtils.copyInputStreamToFile(zipFile.getInputStream(entry), file);
				}
			}
		}
		
		@Override
		protected void cleanup() {
			FileUtils.deleteQuietly(tempFile);
		}
		
		@Override
		protected void beforeStatusQuery() {
			ProgressInputStream progressInputStream = this.progressInputStream; // prevent TOC/TOU
			if(progressInputStream != null) {
				statusMessage = "Downloading... " + progressInputStream.getStatusString();
			}
		}
	}
}
