package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public abstract class RLMap {
	protected static final File IMAGES_FOLDER = new File(RLMapManager.FILE_ROOT, "mapImages");
	protected final long addedTimestamp = System.currentTimeMillis();
	protected String title, description, authorName, imageFileMimeType;
	protected File udkFile, imageFile;
	protected boolean isFavorite;
	protected long lastLoadedTimestamp = 0;
	
	protected static String getImageFileExtension(String mimeType) {
		switch(mimeType.toLowerCase()) {
			case "image/jpeg":
				return ".jpg";
			case "image/png":
				return ".png";
			case "image/bmp":
				return ".bmp";
			case "image/gif":
				return ".gif";
			default:
				return "";
		}
	}
	
	protected abstract String getDiscriminator();
	
	public abstract MapType getType();
	
	public String getID() {
		return getType().getAbbreviation() + "-" + getDiscriminator();
	}
	
	@Nullable
	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getAuthorName() {
		return authorName;
	}
	
	public File getUdkFile() {
		return udkFile;
	}
	
	public String getUdkFilename() {
		return getUdkFile().getName();
	}
	
	public File getImageFile() {
		return imageFile;
	}
	
	public String getImageFileMimeType() {
		return imageFileMimeType;
	}
	
	public long getAddedTimestamp() {
		return addedTimestamp;
	}
	
	public long getLastLoadedTimestamp() {
		return lastLoadedTimestamp;
	}
	
	public void setLastLoadedNow() {
		lastLoadedTimestamp = System.currentTimeMillis();
	}
	
	public boolean isFavorite() {
		return isFavorite;
	}
	
	public void setFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}
	
	public long getSize() {
		return getUdkFile().length();
	}
	
	public void delete() {
		clearImageFile();
	}
	
	public void refreshMetadata() {
		
	}
	
	@NotNull
	public String getDisplayName() {
		String s = getTitle();
		if(s == null) {
			s = getUdkFilename();
			s = s.substring(0, s.length() - 4);
		}
		return s;
	}
	
	public void clearImageFile() {
		if(imageFile != null) {
			//noinspection ResultOfMethodCallIgnored
			imageFile.delete();
			imageFile = null;
			imageFileMimeType = null;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		RLMap rlMap = (RLMap) o;
		return getID().equals(rlMap.getID());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getID());
	}
}
