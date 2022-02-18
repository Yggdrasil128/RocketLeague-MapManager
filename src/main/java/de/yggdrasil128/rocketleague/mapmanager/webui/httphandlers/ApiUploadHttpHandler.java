package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.maps.RLMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;

public class ApiUploadHttpHandler extends AbstractApiUploadHttpHandler {
	private static final Logger logger = LoggerFactory.getLogger(ApiUploadHttpHandler.class.getName());
	private final RLMapManager rlMapManager;
	
	public ApiUploadHttpHandler(RLMapManager rlMapManager, String context) {
		super(context);
		this.rlMapManager = rlMapManager;
		
		registerHandler("mapImage", this::handleMapImageUpload);
	}
	
	private void handleMapImageUpload(File file, HashMap<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception {
		int responseCode = HttpsURLConnection.HTTP_OK;
		
		try {
			String mapID = parameters.get("mapID");
			String filename = parameters.get("filename");
			String mimeType = parameters.get("mimeType");
			
			RLMap map = rlMapManager.getConfig().getMaps().get(mapID);
			
			int index = filename.lastIndexOf('.');
			String fileExtension = filename.substring(index);
			
			File targetFile = new File(RLMap.IMAGES_FOLDER, mapID + fileExtension);
			
			FileUtils.copyFile(file, targetFile);
			
			map.setImage(targetFile, mimeType);
			rlMapManager.getConfig().save();
		} catch(IllegalArgumentException | NullPointerException | IndexOutOfBoundsException e) {
			responseCode = HttpsURLConnection.HTTP_BAD_REQUEST;
		} catch(Exception e) {
			logger.error("Uncaught exception on map upload", e);
			
			responseCode = HttpsURLConnection.HTTP_INTERNAL_ERROR;
		}
		
		httpExchange.sendResponseHeaders(responseCode, -1);
		outputStream.flush();
		outputStream.close();
	}
}
