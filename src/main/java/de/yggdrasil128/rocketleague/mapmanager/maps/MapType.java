package de.yggdrasil128.rocketleague.mapmanager.maps;

public enum MapType {
	STEAM_WORKSHOP("S", SteamWorkshopMap.class),
	LETHAMYR("L", LethamyrMap.class),
	CUSTOM("C", CustomMap.class);
	
	private final String abbreviation;
	private final Class<? extends RLMap> rlMapClass;
	
	MapType(String abbreviation, Class<? extends RLMap> rlMapClass) {
		this.abbreviation = abbreviation;
		this.rlMapClass = rlMapClass;
	}
	
	public String getAbbreviation() {
		return abbreviation;
	}
	
	public Class<? extends RLMap> getRLMapClass() {
		return rlMapClass;
	}
}
