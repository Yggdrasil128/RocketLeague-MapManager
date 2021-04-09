package de.yggdrasil128.rocketleague.mapmanager.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SteamWorkshopDownloader {
	private static final String REQUEST_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/request";
	private static final String STATUS_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/status";
	private static final String TRANSMIT_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/transmit";
	
	private final Gson gson;
	
	public SteamWorkshopDownloader() {
		gson = new GsonBuilder().serializeNulls().create();
	}
	
	public void download(long mapID) throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		
		JsonObject json = new JsonObject();
		json.addProperty("publishedFileId", mapID);
		json.add("collectionId", null);
		json.addProperty("extract", false);
		json.addProperty("hidden", true);
		json.addProperty("direct", false);
		json.addProperty("autodownload", false);
		String data = gson.toJson(json);
		
		String result = postRequest(REQUEST_ENDPOINT, data);
		json = gson.fromJson(result, JsonObject.class);
		String uuid = json.get("uuid").getAsString();
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] UUID is " + uuid);
		
		data = "{\"uuids\":[\"" + uuid + "\"]}";
		String status;
		do {
			//noinspection BusyWait
			Thread.sleep(1000);
			
			result = postRequest(STATUS_ENDPOINT, data);
			json = gson.fromJson(result, JsonObject.class);
			json = json.get(uuid).getAsJsonObject();
			status = json.get("status").getAsString();
			
			System.out.println("[" + (System.currentTimeMillis() - startTime) + "] Status: " + status);
		} while(status.equals("retrieving") || status.equals("preparing"));
		
		if(!status.equals("prepared")) {
			throw new IOException("Unexpected status: " + status);
		}
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] Downloading...");
		byte[] bytes = getRequest(TRANSMIT_ENDPOINT + "?uuid=" + uuid);
		FileUtils.writeByteArrayToFile(new File("C:\\Users\\Yggdrasil128\\temp\\" + mapID + ".zip"), bytes);
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] Done!");
	}
	
	private String postRequest(@NotNull String url, @NotNull String data) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestProperty("Content-Type", "application/json");
		
		OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
		wr.write(data);
		wr.flush();
		
		final int responseCode = con.getResponseCode();
		if(responseCode != HttpsURLConnection.HTTP_OK) {
			throw new IOException("Unexpected response code " + responseCode);
		}
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
		String line;
		while((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		
		return sb.toString();
	}
	
	private byte[] getRequest(@NotNull String url) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		con.setRequestMethod("GET");
		con.setDoInput(true);
		
		final int responseCode = con.getResponseCode();
		if(responseCode != HttpsURLConnection.HTTP_OK) {
			throw new IOException("Unexpected response code " + responseCode);
		}
		
		InputStream inputStream = con.getInputStream();
		byte[] bytes = IOUtils.toByteArray(inputStream);
		inputStream.close();
		
		return bytes;
	}
}
