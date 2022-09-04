package de.yggdrasil128.rocketleague.mapmanager.tools;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class ZipTools {
	public static final String[] MAP_EXTENSIONS = new String[]{".udk", ".upk"};
	public static final String[] IMAGE_EXTENSIONS = new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp"};
	
	public static ZipEntry findZipEntry(ZipFile zipFile, String[] fileExtensions) {
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		
		while(zipEntries.hasMoreElements()) {
			ZipEntry entry = zipEntries.nextElement();
			final String name = entry.getName();
			for(String fileExtension : fileExtensions) {
				if(name.endsWith(fileExtension)) {
					return entry;
				}
			}
		}
		
		return null;
	}
	
	public static String getZipEntryFilename(ZipEntry zipEntry) {
		String name = zipEntry.getName();
		
		if(name.contains("/")) {
			name = name.substring(name.lastIndexOf('/') + 1);
		}
		
		return name;
	}
}
