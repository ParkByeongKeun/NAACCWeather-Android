package net.ijoon.ijoonweatherstation;

/**
 * Created by himanshusingh on 12/10/17.
 */

public class DataProgrssBar {
      String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    int progress;

    public DataProgrssBar(int progress, String date) {

        this.progress = progress;
        this.date = date;

    }
}
