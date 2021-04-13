package de.yggdrasil128.rocketleague.mapmanager.tools;

import org.apache.commons.io.FileUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class GoogleDriveDownloader {
	private final String id;
	private final File file;
	private boolean started = false;
	private ProgressInputStream progressInputStream;
	
	public GoogleDriveDownloader(String id, File file) {
		this.id = id;
		this.file = file == null ? new File("C:\\Users\\Yggdrasil128\\temp\\" + id + ".zip") : file;
	}
	
	public ProgressInputStream getProgressInputStream() {
		return progressInputStream;
	}
	
	public void download() throws IOException {
		if(started) {
			throw new IllegalStateException("Already started");
		}
		started = true;
		
		String url = "https://drive.google.com/uc?export=download&id=" + id;
		while(true) {
			System.out.println("GET " + url);
			HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
			final int responseCode = con.getResponseCode();
			if(responseCode == 302) {
				url = con.getHeaderField("Location");
				continue;
			}
			if(responseCode != 200) {
				throw new IOException("Google drive API returned unexpected response code " + responseCode);
			}
			System.out.println("Downloading...");
			
			progressInputStream = new ProgressInputStream(con.getInputStream(), 0);
			FileUtils.copyInputStreamToFile(progressInputStream, file);
			System.out.println("Done.");
			break;
		}
	}
}
