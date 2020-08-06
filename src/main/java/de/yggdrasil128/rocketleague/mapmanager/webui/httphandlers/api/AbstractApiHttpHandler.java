package de.yggdrasil128.rocketleague.mapmanager.webui.httphandlers.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@SuppressWarnings("SameReturnValue")
public abstract class AbstractApiHttpHandler implements HttpHandler {
	private final HashMap<String, ApiFunctionRaw> functions = new HashMap<>();
	private final Logger logger;
	
	public AbstractApiHttpHandler(Logger logger) {
		this.logger = logger;
	}
	
	protected void registerFunction(String name, ApiFunction function) {
		registerFunction(name, ApiFunctionRaw.of(function));
	}
	
	protected void registerFunction(String name, ApiFunctionRaw function) {
		functions.put(name, function);
	}
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		OutputStream outputStream = httpExchange.getResponseBody();
		
		String functionName = httpExchange.getRequestURI().toString().substring(5);
		HashMap<String, String> parameters = new HashMap<>();
		int index = functionName.indexOf('?');
		if(index != -1) {
			String s = functionName.substring(index + 1);
			functionName = functionName.substring(0, index);
			String[] parts = s.split("&");
			for(String part : parts) {
				part = part.trim();
				if(part.isEmpty()) {
					continue;
				}
				index = part.indexOf('=');
				if(index == -1) {
					parameters.put(part, null);
				} else {
					parameters.put(part.substring(0, index), part.substring(index + 1));
				}
			}
		}
		if(functionName.endsWith("/")) {
			functionName = functionName.substring(0, functionName.length() - 1);
		}
		// POST body
		if(httpExchange.getRequestMethod().equalsIgnoreCase("POST")) {
			String postBody = IOUtils.toString(httpExchange.getRequestBody(), StandardCharsets.UTF_8);
			parameters.put("postBody", postBody);
		}
		
		ApiFunctionRaw function = functions.get(functionName);
		if(function == null) {
			httpExchange.sendResponseHeaders(404, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		byte[] output;
		
		try {
			output = function.apply(parameters, httpExchange, outputStream);
		} catch(AssertionError | IllegalArgumentException e) {
			httpExchange.sendResponseHeaders(400, -1);
			outputStream.flush();
			outputStream.close();
			return;
		} catch(NoSuchElementException e) {
			httpExchange.sendResponseHeaders(404, -1);
			outputStream.flush();
			outputStream.close();
			return;
		} catch(Exception e) {
			logger.error("Uncaught exception on ApiHttpHandler command '" + function + "'", e);
			
			httpExchange.sendResponseHeaders(500, -1);
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		if(output.length == 0) {
			httpExchange.sendResponseHeaders(204, -1);
		} else {
			httpExchange.sendResponseHeaders(200, output.length);
			outputStream.write(output);
		}
		outputStream.flush();
		outputStream.close();
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
