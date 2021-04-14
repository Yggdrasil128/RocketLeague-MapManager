package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@SuppressWarnings("SameReturnValue")
public abstract class AbstractApiHttpHandler implements HttpHandler {
	private final String context;
	private final Logger logger;
	private final HashMap<String, ApiHandler> handlers = new HashMap<>();
	
	public AbstractApiHttpHandler(String context, Logger logger) {
		this.context = context;
		this.logger = logger;
	}
	
	protected void registerHandler(String name, ApiHandler apiHandler) {
		handlers.put(name, apiHandler);
	}
	
	protected void registerFunctionRaw(String name, ApiFunctionRaw function) {
		registerHandler(name, ApiHandler.of(function));
	}
	
	protected void registerFunction(String name, ApiFunction function) {
		registerFunctionRaw(name, ApiFunctionRaw.of(function));
	}
	
	public static Pair<String, HashMap<String, String>> parseRequestURI(String uri) throws IOException {
		HashMap<String, String> parameters = new HashMap<>();
		String functionName;
		int index = uri.indexOf('?');
		if(index != -1) {
			String s = uri.substring(index + 1);
			functionName = uri.substring(0, index);
			String[] parts = s.split("&");
			for(String part : parts) {
				part = part.trim();
				if(part.isEmpty()) {
					continue;
				}
				index = part.indexOf('=');
				if(index == -1) {
					String parameter = java.net.URLDecoder.decode(part, StandardCharsets.UTF_8.name());
					parameters.put(parameter, null);
				} else {
					String parameter = java.net.URLDecoder.decode(part.substring(0, index), StandardCharsets.UTF_8.name());
					String value = java.net.URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8.name());
					parameters.put(parameter, value);
				}
			}
		} else {
			functionName = uri;
		}
		if(functionName.endsWith("/")) {
			functionName = functionName.substring(0, functionName.length() - 1);
		}
		return Pair.of(functionName, parameters);
	}
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		String uri = httpExchange.getRequestURI().toString().substring(context.length());
		final Pair<String, HashMap<String, String>> pair = parseRequestURI(uri);
		String functionName = pair.getLeft();
		HashMap<String, String> parameters = pair.getRight();
		
		// POST body
		if(httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
			String postBody = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
			parameters.put("postBody", postBody);
		}
		
		OutputStream outputStream = httpExchange.getResponseBody();
		
		ApiHandler handler = handlers.get(functionName);
		if(handler == null) {
			httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NOT_FOUND, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		handler.handle(parameters, httpExchange, outputStream, logger, functionName);
	}
	
	public interface ApiHandler {
		static ApiHandler of(ApiFunctionRaw apiFunctionRaw) {
			return (parameters, httpExchange, outputStream, logger, functionName) -> {
				byte[] output;
				
				try {
					output = apiFunctionRaw.apply(parameters, httpExchange, outputStream);
				} catch(AssertionError | IllegalArgumentException e) {
					httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_BAD_REQUEST, -1);
					outputStream.flush();
					outputStream.close();
					return;
				} catch(NoSuchElementException e) {
					httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NOT_FOUND, -1);
					outputStream.flush();
					outputStream.close();
					return;
				} catch(Exception e) {
					logger.error("Uncaught exception on ApiHttpHandler command '" + functionName + "'", e);
					
					httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_INTERNAL_ERROR, -1);
					outputStream.flush();
					outputStream.close();
					return;
				}
				
				if(output.length == 0) {
					httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_NO_CONTENT, -1);
				} else {
					httpExchange.sendResponseHeaders(HttpsURLConnection.HTTP_OK, output.length);
					outputStream.write(output);
				}
				outputStream.flush();
				outputStream.close();
			};
		}
		
		void handle(Map<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream, Logger logger, String functionName) throws IOException;
	}
	
	@FunctionalInterface
	public interface ApiFunction {
		String apply(Map<String, String> parameters) throws Exception;
	}
	
	@FunctionalInterface
	public interface ApiFunctionRaw {
		static ApiFunctionRaw of(ApiFunction apiFunction) {
			return (parameters, httpExchange, outputStream) -> {
				String result = apiFunction.apply(parameters);
				if(result == null || result.isEmpty()) {
					return new byte[0];
				}
				return result.getBytes(StandardCharsets.UTF_8);
			};
		}
		
		byte[] apply(Map<String, String> parameters, HttpExchange httpExchange, OutputStream outputStream) throws Exception;
	}
	
}
