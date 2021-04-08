package de.yggdrasil128.rocketleague.mapmanager.maps;

public class LethamyrMap extends RLMap {
	private String id;
	
	@Override
	protected String getDiscriminator() {
		return id;
	}
	
	@Override
	public MapType getType() {
		return MapType.LETHAMYR;
	}
}
