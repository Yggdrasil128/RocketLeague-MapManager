package de.yggdrasil128.rocketleague.mapmanager.tools;

import org.apache.commons.io.FileUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDriveDownloader {
	private final String id;
	private final File file;
	private boolean cancelled = false;
	private HttpsURLConnection httpsURLConnection;
	private long downloadSize = 0;
	private ProgressInputStream progressInputStream;
	private Map<String, List<String>> headerFields;
	
	public GoogleDriveDownloader(String id, File file) {
		this.id = id;
		this.file = file;
	}
	
	public void cancel() {
		cancelled = true;
		if(httpsURLConnection != null) {
			httpsURLConnection.disconnect();
		}
	}
	
	public String getStatus() {
		if(progressInputStream == null) {
			return "Downloading...";
		} else {
			return "Downloading... " + progressInputStream.getStatusString();
		}
	}
	
	public void download() throws Exception {
		String googleDriveURL = "https://drive.google.com/uc?export=download&id=" + id;
		download(googleDriveURL, null);
		
		if(headerFields.get("Content-Type") == null || !headerFields.get("Content-Type").get(0).startsWith("text/html")) {
			// download was successful
			return;
		}
		
		// parse downloaded HTML doc to get the download size
		String html = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		Pattern pattern = Pattern.compile(" \\((\\d+(\\.\\d+)?)([M|G])\\)");
		Matcher matcher = pattern.matcher(html);
		if(matcher.find()) {
			double downloadSize = Double.parseDouble(matcher.group(1));
			downloadSize *= ProgressInputStream.MEBIBYTE_SIZE;
			if(matcher.group(3).equals("G")) {
				downloadSize *= 1024;
			}
			this.downloadSize = Math.round(downloadSize);
		}
		
		String virusWarningCookie = getVirusWarningCookie();
		if(virusWarningCookie == null) {
			googleDriveURL += "&confirm=t";
			download(googleDriveURL, null);
		} else {
			// parse cookie
			pattern = Pattern.compile("(download_warning_\\d+_" + id + "=(.{4}));");
			matcher = pattern.matcher(virusWarningCookie);
			if(!matcher.find()) {
				throw new Exception("Couldn't parse Google Drive download warning cookie");
			}
			String cookie = matcher.group(1);
			String cookieValue = matcher.group(2);
			
			googleDriveURL += "&confirm=" + cookieValue;
			download(googleDriveURL, cookie);
		}
	}
	
	private void download(String url, String cookie) throws Exception {
		try {
			httpsURLConnection = (HttpsURLConnection) new URL(url).openConnection();
			
			if(cookie != null) {
				httpsURLConnection.setRequestProperty("Cookie", cookie);
			}
			
			final int responseCode = httpsURLConnection.getResponseCode();
			if(responseCode != 200) {
				throw new IOException("Google drive API returned unexpected response code " + responseCode);
			}
			
			headerFields = httpsURLConnection.getHeaderFields();
			
			progressInputStream = new ProgressInputStream(httpsURLConnection.getInputStream(), downloadSize);
			FileUtils.copyInputStreamToFile(progressInputStream, file);
			progressInputStream = null;
			httpsURLConnection = null;
		} catch(Exception e) {
			if(cancelled) {
				throw new InterruptedException();
			} else {
				throw e;
			}
		}
	}
	
	private String getVirusWarningCookie() {
		if(headerFields.get("Set-Cookie") == null) {
			return null;
		}
		for(String cookie : headerFields.get("Set-Cookie")) {
			if(cookie.startsWith("download_warning")) {
				return cookie;
			}
		}
		return null;
	}
}
