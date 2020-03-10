package net.ijoon.ijoonweatherstation.util;

public class ListViewItem {
    private int logoIcon;
    private int linkIcon;
    private String titleStr;
    private String titleSerial;
    private boolean isOn ;

    public void setLinkIconId(int id) {
        linkIcon = id;
    }
    public void setLogoIconId(int id) {
        logoIcon = id;
    }
    public void setTitle(String title) {
        titleStr = title ;
    }
    public void setIsOn(boolean isOn) {
        this.isOn = isOn;
    }
    public void setTitleSerial(String serial) {
        titleSerial = serial;
    }

    public int getLogoIconId() {
        return this.logoIcon;
    }

    public int getLinkIconId() {
        return this.linkIcon;
    }
    public String getSerial() {
        return this.titleSerial;
    }
    public String getTitle() {
        return this.titleStr;
    }
    public boolean getIsOn() {
        return this.isOn;
    }
}