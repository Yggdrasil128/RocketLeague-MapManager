package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StaticFilesHttpHandler implements HttpHandler {
	private static final String DIRSEP = System.getProperty("file.separator");
	private final Logger logger;
	private HashMap<String, byte[]> fileData;
	private final boolean isSetupMode;
	
	public StaticFilesHttpHandler(boolean isSetupMode) {
		this.isSetupMode = isSetupMode;
		logger = LoggerFactory.getLogger(StaticFilesHttpHandler.class.getName());
		
		readStaticFiles();
	}
	
	public HashMap<String, byte[]> getFileData() {
		return fileData;
	}
	
	public static String getMimeType(String filename) {
		filename = filename.toLowerCase();
		if(filename.endsWith(".html")) {
			return "text/html";
		}
		if(filename.endsWith(".css")) {
			return "text/css";
		}
		if(filename.endsWith(".js")) {
			return "text/javascript";
		}
		if(filename.endsWith(".png")) {
			return "image/png";
		}
		if(filename.endsWith(".ico")) {
			return "image/vnd.microsoft.icon";
		}
		if(filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if(filename.endsWith(".bmp")) {
			return "image/bmp";
		}
		return null;
	}
	
	private void readStaticFiles() {
		fileData = new HashMap<>();
		try {
			File file = new File(RLMapManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(file.isDirectory()) {
				file = new File(file.getParentFile().getParentFile().getPath() + DIRSEP + "src" + DIRSEP + "main" + DIRSEP + "resources" + DIRSEP + "webui");
				readStaticFilesFromDirectory(file, "");
				return;
			}
			
			ZipFile zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> e = zipFile.entries();
			
			while(e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				if(entry.isDirectory()) {
					continue;
				}
				if(!entry.getName().startsWith("webui")) {
					continue;
				}
				
				InputStream inputStream = zipFile.getInputStream(entry);
				byte[] data = IOUtils.toByteArray(inputStream);
				inputStream.close();
				
				String name = entry.getName().substring(6).replace('\\', '/');
				if(!name.endsWith(".html")) {
					fileData.put(name, data);
					continue;
				}
				
				if(name.equals(isSetupMode ? "index.html" : "setup.html")) {
					continue;
				}
				fileData.put("index.html", data);
			}
		} catch(IOException | URISyntaxException e) {
			logger.error("Couldn't read webui files from jar.", e);
		}
	}
	
	private void readStaticFilesFromDirectory(File dir, String prefix) throws IOException {
		File[] files = dir.listFiles();
		assert files != null;
		for(File file : files) {
			if(file.isDirectory()) {
				readStaticFilesFromDirectory(file, prefix + file.getName() + "/");
			} else {
				byte[] data = FileUtils.readFileToByteArray(file);
				fileData.put(prefix + file.getName(), data);
			}
		}
	}
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		OutputStream outputStream = httpExchange.getResponseBody();
		
		if(!"GET".equals(httpExchange.getRequestMethod())) {
			httpExchange.sendResponseHeaders(405, -1);
			outputStream.close();
		}
		
		String name = httpExchange.getRequestURI().toString().substring(1);
		if(name.isEmpty()) {
			name = "index.html";
		}
		
		if(!fileData.containsKey(name)) {
			httpExchange.sendResponseHeaders(404, -1);
			outputStream.close();
		}
		
		String mimeType = getMimeType(name);
		if(mimeType != null) {
			if(mimeType.startsWith("text/")) {
				mimeType += "; charset=UTF-8";
			}
			httpExchange.getResponseHeaders().set("Content-Type", mimeType);
		}
		
		byte[] data = fileData.get(name);
		
		httpExchange.sendResponseHeaders(200, data.length);
		outputStream.write(data);
		outputStream.flush();
		outputStream.close();
	}
}
