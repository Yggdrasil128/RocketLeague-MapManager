package de.yggdrasil128.rocketleague.mapmanager.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SteamWorkshopDownloader {
	private static final String FILE_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/details/file";
	private static final String REQUEST_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/request";
	private static final String STATUS_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/status";
	private static final String TRANSMIT_ENDPOINT = "https://backend-01-prd.steamworkshopdownloader.io/api/download/transmit";
	
	private final Gson gson;
	private final long mapID;
	private final Consumer<State> stateChangeConsumer;
	private final File file;
	private State state = null;
	private JsonObject mapInfoJson, preparingStatusJson;
	private long downloadSize;
	private ProgressInputStream progressInputStream;
	
	public SteamWorkshopDownloader(long mapID, File file, Consumer<State> stateChangeConsumer) {
		this.mapID = mapID;
		this.stateChangeConsumer = stateChangeConsumer;
		this.file = file == null ? new File("C:\\Users\\Yggdrasil128\\temp\\" + mapID + ".zip") : file;
		gson = new GsonBuilder().serializeNulls().create();
	}
	
	public State getState() {
		return state;
	}
	
	public String getDownloadProgress() {
		if(progressInputStream == null) {
			return "";
		}
		return progressInputStream.getStatusString();
	}
	
	public JsonObject getMapInfoJson() {
		return mapInfoJson;
	}
	
	public JsonObject getPreparingStatusJson() {
		return preparingStatusJson;
	}
	
	public void download() throws Exception {
		if(state != null) {
			throw new IllegalStateException("Already started");
		}
		state = State.REQUESTING;
		if(stateChangeConsumer != null) {
			stateChangeConsumer.accept(state);
		}
		
		long startTime = System.currentTimeMillis();
		
		String result = postRequest(FILE_ENDPOINT, "[" + mapID + "]");
		if(result.trim().equalsIgnoreCase("null")) {
			throw new Exception("Map not found");
		}
		
		JsonElement ele = gson.fromJson(result, JsonElement.class);
		mapInfoJson = ele.getAsJsonArray().get(0).getAsJsonObject();
		downloadSize = Long.parseLong(mapInfoJson.get("file_size").getAsString());
		
		if(!"Rocket League".equals(mapInfoJson.get("app_name").getAsString())) {
			throw new Exception("Not a Rocket League map");
		}
		
		JsonObject json = new JsonObject();
		json.addProperty("publishedFileId", mapID);
		json.add("collectionId", null);
		json.addProperty("extract", false);
		json.addProperty("hidden", true);
		json.addProperty("direct", false);
		json.addProperty("autodownload", false);
		String data = gson.toJson(json);
		
		result = postRequest(REQUEST_ENDPOINT, data);
		json = gson.fromJson(result, JsonObject.class);
		String uuid = json.get("uuid").getAsString();
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] UUID is " + uuid);
		
		data = "{\"uuids\":[\"" + uuid + "\"]}";
		String status;
		state = State.PREPARING;
		if(stateChangeConsumer != null) {
			stateChangeConsumer.accept(state);
		}
		do {
			//noinspection BusyWait
			Thread.sleep(1000);
			
			result = postRequest(STATUS_ENDPOINT, data);
			System.out.println(result);
			json = gson.fromJson(result, JsonObject.class);
			json = json.get(uuid).getAsJsonObject();
			preparingStatusJson = json;
			status = json.get("status").getAsString();
			
			System.out.println("[" + (System.currentTimeMillis() - startTime) + "] Status: " + status);
		} while(status.equals("retrieving") || status.equals("preparing"));
		
		if(!status.equals("prepared")) {
			throw new IOException("Unexpected status: " + status);
		}
		
		System.out.println("[" + (System.currentTimeMillis() - startTime) + "] Downloading...");
		
		state = State.DOWNLOADING;
		if(stateChangeConsumer != null) {
			stateChangeConsumer.accept(state);
		}
		download(TRANSMIT_ENDPOINT + "?uuid=" + uuid, file);
		
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
	
	private void download(@NotNull String url, File file) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		con.setRequestMethod("GET");
		con.setDoInput(true);
		
		final int responseCode = con.getResponseCode();
		if(responseCode != HttpsURLConnection.HTTP_OK) {
			throw new IOException("Unexpected response code " + responseCode);
		}
		
		for(Map.Entry<String, List<String>> stringListEntry : con.getHeaderFields().entrySet()) {
			System.out.println(stringListEntry.getKey() + ": " + StringUtils.join(stringListEntry.getValue(), " || "));
		}
		
		progressInputStream = new ProgressInputStream(con.getInputStream(), downloadSize);
		FileUtils.copyInputStreamToFile(progressInputStream, file);
	}
	
	public enum State {
		REQUESTING,
		PREPARING,
		DOWNLOADING;
	}
}
