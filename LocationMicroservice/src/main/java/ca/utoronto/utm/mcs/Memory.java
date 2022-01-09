package ca.utoronto.utm.mcs;

import java.util.Map;
import java.util.ArrayList;

public final class Memory {
    private static double userLongitude;
    private static double userLatitude;
    private static String userStreetAt;
    private static ArrayList<Map<String, Object>> nearbyDrivers;
    private static ArrayList<Map<String, Object>> userNavigation;
    private static int navigationTotalTime;

    public Memory() {
    }

    public void setLongitude(double longitude) {
        userLongitude = longitude;
    }

    public double getLongitude() {
        return userLongitude;
    }

    public void setLatitude(double latitude) {
        userLatitude = latitude;
    }

    public double getLatitude() {
        return userLatitude;
    }

    public void setStreetAt(String streetAt) {
        userStreetAt = streetAt;
    }

    public String getStreetAt() {
        return userStreetAt;
    }

    public void setNearbyDrivers(ArrayList<Map<String, Object>> nearbyDriver) {
        nearbyDrivers = nearbyDriver;
    }

    public ArrayList<Map<String, Object>> getNearbyDrivers() {
        return nearbyDrivers;
    }

    public void setNavigation(ArrayList<Map<String, Object>> navigation) {
        userNavigation = navigation;
    }

    public ArrayList<Map<String, Object>> getNavigation() {
        return userNavigation;
    }

    public void setTotalNavigationTime(int totalTime) {
        navigationTotalTime = totalTime;
    }

    public int getTotalNavigationTime() {
        return navigationTotalTime;
    }
}
