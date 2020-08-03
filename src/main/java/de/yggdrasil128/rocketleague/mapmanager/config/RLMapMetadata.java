package de.yggdrasil128.rocketleague.mapmanager.config;

import de.yggdrasil128.rocketleague.mapmanager.RLMapManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class RLMapMetadata {
	private static final File IMAGES_FOLDER = new File(RLMapManager.FILE_ROOT, "mapImages");
	
	static {
		if(!IMAGES_FOLDER.exists()) {
			//noinspection ResultOfMethodCallIgnored
			IMAGES_FOLDER.mkdirs();
		}
	}
	
	private static final transient Logger logger = LoggerFactory.getLogger(RLMapMetadata.class);
	
	private final long id;
	private String title, description, authorName, imageFileMIMEType;
	private boolean isFavorite = false;
	private transient File imageFile;
	
	private long lastLoadedTimestamp = 0;
	
	public RLMapMetadata(long id) {
		this.id = id;
	}
	
	private static String getExtension(String mimeType) {
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
	
	@SuppressWarnings("unused")
	public long getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getAuthorName() {
		return authorName;
	}
	
	public File getImageFile() {
		if(imageFileMIMEType == null) {
			return null;
		}
		if(imageFile == null) {
			imageFile = new File(IMAGES_FOLDER, id + getExtension(imageFileMIMEType));
		}
		return imageFile;
	}
	
	public String getImageFileMIMEType() {
		return imageFileMIMEType;
	}
	
	public boolean isFavorite() {
		return isFavorite;
	}
	
	public void setFavorite(boolean favorite) {
		isFavorite = favorite;
	}
	
	public long getLastLoadedTimestamp() {
		return lastLoadedTimestamp;
	}
	
	public void setLastLoadedTimestamp(long lastLoadedTimestamp) {
		this.lastLoadedTimestamp = lastLoadedTimestamp;
	}
	
	public void fetchFromWorkshop() throws IOException {
		Document doc = Jsoup.connect("https://steamcommunity.com/sharedfiles/filedetails/?id=" + id).timeout(5000).get();
		
		Elements elements;
		
		elements = doc.getElementsByClass("workshopItemTitle");
		if(!elements.isEmpty()) {
			title = elements.get(0).text();
		} else {
			logger.warn("Couldn't find title for workshop item " + id);
			title = null;
		}
		
		elements = doc.getElementsByClass("workshopItemDescription");
		if(!elements.isEmpty()) {
			description = elements.get(0).text();
		} else {
			logger.warn("Couldn't find description for workshop item " + id);
			description = "Error: Couldn't fetch information about this map from the Steam workshop " +
						  "because the associated workshop page no longer exists.";
		}
		
		elements = doc.getElementsByClass("friendBlockContent");
		if(!elements.isEmpty()) {
			authorName = elements.get(0).ownText();
		} else {
			logger.warn("Couldn't find authorName for workshop item " + id);
			authorName = "Unknown";
		}
		
		elements = doc.getElementsByClass("workshopItemPreviewImageEnlargeable");
		for(Element element : elements) {
			String src = element.parent().attr("onclick");
			int start = src.indexOf('\'');
			int end = src.lastIndexOf('\'');
			src = src.substring(start + 1, end);
			
			try {
				downloadImage(src);
			} catch(RuntimeException e) {
				logger.warn("Couldn't load image url '" + src + "' for workshop item " + id);
				continue;
			}
			break;
		}
	}
	
	private void downloadImage(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		if(con.getResponseCode() != 200) {
			return;
		}
		
		String mimeType = con.getHeaderField("Content-Type");
//		if(getExtension(mimeType) == null) {
//			throw new RuntimeException("Unknown MIME type '" + mimeType + "' for URL: " + urlString);
//		}
		InputStream inputStream = con.getInputStream();
		byte[] bytes = IOUtils.toByteArray(inputStream);
		inputStream.close();
		
		File imageFile = getImageFile();
		if(imageFile != null) {
			//noinspection ResultOfMethodCallIgnored
			imageFile.delete();
		}
		
		this.imageFile = null;
		this.imageFileMIMEType = mimeType;
		
		imageFile = getImageFile();
		FileUtils.writeByteArrayToFile(imageFile, bytes);
		this.imageFile = imageFile;
	}
}
