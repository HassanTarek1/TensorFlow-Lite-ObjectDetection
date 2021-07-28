package org.tensorflow.lite.examples.detection;

import java.util.ArrayList;

public class DataHolder {
    private static final DataHolder instance = new DataHolder();

    public static DataHolder getInstance() {
        return instance;
    }

    private ArrayList<String> longitudes;
    private ArrayList<String> latitudes;
    private ArrayList<String> types;

    public void setTypes(ArrayList<String> type) {
        this.types = type;
    }

    public void setLongitudes(ArrayList<String> longitude) {
        this.longitudes = longitude;
    }

    public void setLatitudes(ArrayList<String> latitude) {
        this.latitudes = latitude;
    }

    public ArrayList<String> getLatitudes() {
        return latitudes;
    }

    public ArrayList<String> getLongitudes() {
        return longitudes;
    }

    public ArrayList<String> getTypes() {
        return types;
    }
}
