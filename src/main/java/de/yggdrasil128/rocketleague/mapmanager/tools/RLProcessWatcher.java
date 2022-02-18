package de.yggdrasil128.rocketleague.mapmanager.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;

public class RLProcessWatcher {
	private static final Logger logger = LoggerFactory.getLogger(RLProcessWatcher.class.getName());
	private static final long CACHE_TTL = Duration.ofMillis(4800).toNanos();
	private static final boolean PRINT_UPDATE_TIME_TO_SYSOUT = false;
	
	private long lastUpdate;
	private Integer pid;
	
	public RLProcessWatcher() {
	}
	
	public synchronized Integer getPID() {
		long now = System.nanoTime();
		if(now - lastUpdate > CACHE_TTL) {
			update();
		}
		return pid;
	}
	
	public boolean isRunning() {
		return getPID() != null;
	}
	
	private void update() {
		lastUpdate = System.nanoTime();
		try {
			Process process = Runtime.getRuntime().exec("tasklist /FI \"ImageName eq RocketLeague.exe\"");
			try(BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while((line = input.readLine()) != null) {
					if(!line.startsWith("RocketLeague.exe")) {
						continue;
					}
					line = line.substring("RocketLeague.exe".length()).trim();
					int index = line.indexOf(' ');
					line = line.substring(0, index);
					pid = Integer.parseInt(line);
				}
			}
		} catch(Exception e) {
			logger.warn("Unable to fetch RocketLeague PID", e);
		}
		if(PRINT_UPDATE_TIME_TO_SYSOUT) {
			System.out.println("RocketLeagueProcessWatcher::update took " + (System.nanoTime() - lastUpdate) + " ns.");
		}
	}
}
