package de.yggdrasil128.rocketleague.mapmanager.maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CustomMap extends RLMap {
	private static final transient Logger logger = LoggerFactory.getLogger(CustomMap.class.getName());
	
	private int id;
	
	public static CustomMap create(int id, File udkFile, String udkFilename) {
		CustomMap map = new CustomMap();
		map.id = id;
		map.udkFile = udkFile;
		map.udkFilename = udkFilename;
		return map;
	}
	
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
	
	@Override
	public String getURL() {
		return null;
	}
	
	@Override
	public boolean canBeDeleted() {
		return true;
	}
}
