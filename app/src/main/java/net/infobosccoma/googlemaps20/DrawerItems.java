package net.infobosccoma.googlemaps20;

/**
 * Created by Arnau on 09/03/2015.
 */

public class DrawerItems {

	String ItemName;
	int imgResID;
	String title;

	public DrawerItems(String itemName, int imgResID) {
		ItemName = itemName;
		this.imgResID = imgResID;
	}

	public DrawerItems(String title) {
		this(null, 0);
		this.title = title;
	}

	public String getItemName() {
		return ItemName;
	}

	public void setItemName(String itemName) {
		ItemName = itemName;
	}

	public int getImgResID() {
		return imgResID;
	}

	public void setImgResID(int imgResID) {
		this.imgResID = imgResID;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
