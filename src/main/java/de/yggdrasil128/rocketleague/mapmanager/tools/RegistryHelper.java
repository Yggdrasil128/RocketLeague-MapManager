package de.yggdrasil128.rocketleague.mapmanager.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RegistryHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistryHelper.class.getName());
	
	public static String query(String key, String value) {
		try {
			String command = "REG QUERY \"" + key + "\" /v \"" + value + "\"";
			Process process = Runtime.getRuntime().exec(command);
			try(BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while((line = input.readLine()) != null) {
					if(!line.contains(value)) {
						continue;
					}
					int index = line.indexOf("REG_SZ");
					if(index < 0) {
						continue;
					}
					line = line.substring(index + 6).trim();
					return line;
				}
			}
			return null;
		} catch(IOException e) {
			LOGGER.warn("query failed", e);
			return null;
		}
	}
	
	public static void add(String key, String value, String data) {
		data = data.replace("\"", "\\\"");
		try {
			String command = "REG ADD \"" + key + "\" /v \"" + value + "\" /t REG_SZ /d \"" + data + "\" /f";
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch(IOException | InterruptedException e) {
			LOGGER.warn("add failed", e);
		}
	}
	
	public static void addKey(String key) {
		try {
			String command = "REG ADD \"" + key + "\" /f";
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch(IOException | InterruptedException e) {
			LOGGER.warn("addKey failed", e);
		}
	}
	
	public static void delete(String key, String value) {
		try {
			String command = "REG DELETE \"" + key + "\" /v \"" + value + "\" /f";
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch(IOException | InterruptedException e) {
			LOGGER.warn("delete failed", e);
		}
	}
}
