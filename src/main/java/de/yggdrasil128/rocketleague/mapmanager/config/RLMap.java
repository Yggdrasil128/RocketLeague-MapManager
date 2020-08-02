package de.yggdrasil128.rocketleague.mapmanager.config;

import java.io.File;

public class RLMap {
	private final long id;
	private final File udkFile;
	private final long mapSize;
	
	public RLMap(long id, File udkFile) {
		this.id = id;
		this.udkFile = udkFile;
		this.mapSize = udkFile.length();
	}
	
	public String getUDKFilename() {
		return udkFile.getName();
	}
	
	public long getID() {
		return id;
	}
	
	public File getUdkFile() {
		return udkFile;
	}
	
	public long getMapSize() {
		return mapSize;
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		RLMap map = (RLMap) o;
		return id == map.id;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}
}
