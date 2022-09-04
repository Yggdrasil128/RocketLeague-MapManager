package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import de.yggdrasil128.rocketleague.mapmanager.tools.JavaXSwingTools;
import de.yggdrasil128.rocketleague.mapmanager.tools.ZipTools;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CustomMap extends RLMap {
	private static final Logger logger = LoggerFactory.getLogger(CustomMap.class.getName());
	
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
	
	public static String importMap(RLMapManager rlMapManager) throws Exception {
		JFrame jFrame = JavaXSwingTools.makeModalFrame();
		
		final JFileChooser chooser = new JFileChooser(System.getenv("SystemDrive"));
		chooser.setDialogTitle("Select map file");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		final int result = chooser.showOpenDialog(jFrame);
		jFrame.dispose();
		
		if(result != JFileChooser.APPROVE_OPTION) {
			return "";
		}
		
		File selectedFile = chooser.getSelectedFile();
		
		int id = rlMapManager.getConfig().getNextCustomMapID();
		String mapID = MapType.CUSTOM.getAbbreviation() + "-" + id;
		File targetFile = new File(RLMapManager.FILE_MAPS, mapID + ".udk");
		String udkFilename, imageFileMimeType = null;
		File imageFile = null;
		
		if(selectedFile.getName().endsWith(".udk") || selectedFile.getName().endsWith(".upk")) {
			FileUtils.copyFile(selectedFile, targetFile);
			udkFilename = selectedFile.getName();
		} else if(selectedFile.getName().endsWith(".zip")) {
			ZipFile zipFile = new ZipFile(selectedFile);
			ZipEntry zipEntry = ZipTools.findZipEntry(zipFile, ZipTools.MAP_EXTENSIONS);
			if(zipEntry == null) {
				return "Error: Zip doesn't contain a map file.";
			}
			
			udkFilename = ZipTools.getZipEntryFilename(zipEntry);
			
			FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), targetFile);
			
			// check if there is also a picture in the zip file
			zipEntry = ZipTools.findZipEntry(zipFile, ZipTools.IMAGE_EXTENSIONS);
			if(zipEntry != null) {
				String extension = zipEntry.getName().substring(zipEntry.getName().lastIndexOf('.')); // includes '.'
				imageFileMimeType = RLMap.getImageFileMimeType(extension);
				imageFile = new File(RLMapManager.FILE_MAP_IMAGES, mapID + extension);
				
				FileUtils.copyInputStreamToFile(zipFile.getInputStream(zipEntry), imageFile);
			}
			
			zipFile.close();
		} else {
			return "Error: Not a map/zip file.";
		}
		
		CustomMap map = CustomMap.create(id, targetFile, udkFilename);
		if(imageFile != null) {
			map.imageFile = imageFile;
			map.imageFileMimeType = imageFileMimeType;
		}
		
		rlMapManager.getConfig().registerMap(map);
		rlMapManager.getConfig().save();
		
		return "Done. Successfully imported '" + udkFilename + "'.";
	}
}
