package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public abstract class AbstractApiUploadHttpHandler implements HttpHandler {
	private final String context;
	private final HashMap<String, UploadHandler> handlers = new HashMap<>();
	
	public AbstractApiUploadHttpHandler(String context) {
		this.context = context;
	}
	
	protected void registerHandler(@SuppressWarnings("SameParameterValue") String name, UploadHandler handler) {
		handlers.put(name, handler);
	}
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		String uri = httpExchange.getRequestURI().toString().substring(context.length());
		final Pair<String, HashMap<String, String>> pair = AbstractApiHttpHandler.parseRequestURI(uri);
		String functionName = pair.getLeft();
		HashMap<String, String> parameters = pair.getRight();
		
		OutputStream outputStream = httpExchange.getResponseBody();
		
		if(!httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
			httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_BAD_METHOD, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		UploadHandler handler = handlers.get(functionName);
		if(handler == null) {
			httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NOT_FOUND, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		File file = File.createTempFile("RLMM-api-upload-" + functionName, null);
		FileUtils.copyInputStreamToFile(httpExchange.getRequestBody(), file);
		
		try {
			handler.handle(file, parameters, httpExchange, outputStream);
		} catch(Exception e) {
			httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, -1);
			outputStream.flush();
			outputStream.close();
		}
		
		FileUtils.deleteQuietly(file);
	}
	
	protected interface UploadHandler {
		void handle(File file, HashMap<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception;
	}
}
