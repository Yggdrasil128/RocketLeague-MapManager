package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.maps.CustomMap;
import de.yggdrasil128.rocketleague.mapmanager.maps.MapType;
import de.yggdrasil128.rocketleague.mapmanager.maps.SteamWorkshopMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApiUploadHttpHandler extends AbstractApiUploadHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(ApiUploadHttpHandler.class.getName());
	private final RLMapManager rlMapManager;
	
	public ApiUploadHttpHandler(RLMapManager rlMapManager, String context) {
		super(context);
		this.rlMapManager = rlMapManager;
		
		registerHandler("map", this::handleMapUpload);
		registerHandler("test", this::handleTestUpload);
	}
	
	private void handleMapUpload(File file, HashMap<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception {
		String outputString;
		int responseCode = HttpsURLConnection.HTTP_OK;
		
		try {
			String filename = parameters.get("filename");
			int id = rlMapManager.getConfig().getNextCustomMapID();
			String mapID = MapType.CUSTOM.getAbbreviation() + "-" + id;
			String udkFilename;
			
			File targetFile = new File(RLMapManager.FILE_MAPS, mapID + ".udk");
			
			if(filename.endsWith(".udk") || filename.endsWith(".upk")) {
				FileUtils.copyFile(file, targetFile);
				udkFilename = filename;
			} else if(filename.endsWith(".zip")) {
				ZipFile zipFile = new ZipFile(file);
				ZipEntry zipEntry = SteamWorkshopMap.MapDownload.findUdkFile(zipFile);
				udkFilename = zipEntry.getName();
				
				if(udkFilename.contains("/")) {
					udkFilename = udkFilename.substring(udkFilename.lastIndexOf('/') + 1);
				}
				
				FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), targetFile);
			} else {
				throw new IllegalArgumentException("Unrecognized file type");
			}
			
			CustomMap map = CustomMap.create(id, targetFile, udkFilename);
			rlMapManager.getConfig().registerMap(map);
			rlMapManager.getConfig().save();
			
			outputString = "Successfully imported '" + udkFilename + "'.";
		} catch(AssertionError | IllegalArgumentException e) {
			responseCode = HttpsURLConnection.HTTP_BAD_REQUEST;
			outputString = "Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
		} catch(Exception e) {
			logger.error("Uncaught exception on map upload", e);
			
			responseCode = HttpsURLConnection.HTTP_INTERNAL_ERROR;
			outputString = "Error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
		}
		
		byte[] bytes = outputString.getBytes(StandardCharsets.UTF_8);
		httpExchange.sendResponseHeaders(responseCode, bytes.length);
		outputStream.write(bytes);
		outputStream.flush();
		outputStream.close();
	}
	
	private void handleTestUpload(File file, HashMap<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception {
		File targetFile = new File("C:\\Users\\Yggdrasil128\\temp\\testUpload.bin");
		FileUtils.copyFile(file, targetFile);
		
		httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NO_CONTENT, -1);
		outputStream.flush();
		outputStream.close();
	}
}
