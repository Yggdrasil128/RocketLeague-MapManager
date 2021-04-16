package de.yggdrasil128.rocketleague.mapmanager.maps;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.net.URL;
import java.util.Objects;

public abstract class RLMap {
	public static final File IMAGES_FOLDER = new File(RLMapManager.FILE_ROOT, "mapImages");
	protected final long addedTimestamp = System.currentTimeMillis();
	protected String title, description, authorName, imageFileMimeType, udkFilename;
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
	
	protected abstract Logger getLogger();
	
	public String getID() {
		return getType().getAbbreviation() + "-" + getDiscriminator();
	}
	
	public abstract String getURL();
	
	@Nullable
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		if(title == null) {
			throw new NullPointerException("Title must be non-null");
		}
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		if(description == null) {
			throw new NullPointerException("Description must be non-null");
		}
		this.description = description;
	}
	
	public String getAuthorName() {
		return authorName != null ? authorName : "Unknown";
	}
	
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}
	
	public File getUdkFile() {
		return udkFile;
	}
	
	public String getUdkFilename() {
		return udkFilename;
	}
	
	public File getImageFile() {
		return imageFile;
	}
	
	public String getImageFileMimeType() {
		return imageFileMimeType;
	}
	
	public void setImage(File imageFile, String imageFileMimeType) {
		if(imageFile == null) {
			clearImageFile();
			return;
		}
		if(!imageFile.equals(this.imageFile)) {
			FileUtils.deleteQuietly(this.imageFile);
		}
		this.imageFile = imageFile;
		this.imageFileMimeType = imageFileMimeType;
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
	
	public abstract boolean canBeDeleted();
	
	public void delete() {
		if(!canBeDeleted()) {
			throw new UnsupportedOperationException("This map cannot be deleted.");
		}
		FileUtils.deleteQuietly(udkFile);
		if(imageFile != null) {
			FileUtils.deleteQuietly(imageFile);
		}
	}
	
	public boolean refreshMetadata() {
		return false;
	}
	
	@NotNull
	public String getDisplayName() {
		String s = getTitle();
		if(s != null) {
			return s;
		}
		s = getUdkFilename();
		s = s.substring(0, s.length() - 4);
		return s;
	}
	
	public void clearImageFile() {
		if(imageFile != null) {
			FileUtils.deleteQuietly(imageFile);
			imageFile = null;
			imageFileMimeType = null;
		}
	}
	
	protected void downloadImage(String src) {
		try {
			URL url = new URL(src);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
			if(con.getResponseCode() != 200) {
				return;
			}
			
			clearImageFile();
			
			String mimeType = con.getHeaderField("Content-Type");
			imageFile = new File(IMAGES_FOLDER, getID() + getImageFileExtension(mimeType));
			imageFileMimeType = mimeType;
			
			FileUtils.copyInputStreamToFile(con.getInputStream(), imageFile);
		} catch(Exception e) {
			getLogger().warn("Couldn't load image url '" + src + "' for map " + getID());
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
