package de.yggdrasil128.rocketleague.mapmanager.maps;

public class CustomMap extends RLMap {
	private int id;
	
	@Override
	protected String getDiscriminator() {
		return String.valueOf(id);
	}
	
	@Override
	public MapType getType() {
		return MapType.CUSTOM;
	}
}
