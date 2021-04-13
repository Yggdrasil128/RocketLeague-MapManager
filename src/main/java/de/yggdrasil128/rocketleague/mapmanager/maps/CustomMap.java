package de.yggdrasil128.rocketleague.mapmanager.maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(CustomMap.class.getName());
	
	private int id;
	
	@Override
	protected String getDiscriminator() {
		return String.valueOf(id);
	}
	
	@Override
	public MapType getType() {
		return MapType.CUSTOM;
	}
	
	@Override
	protected Logger getLogger() {
		return logger;
	}
}
