package drones;

public class Tube {

    private final String station;
    private final double latitude;
    private final double longitude;

    public Tube(String station, double latitude, double longitude) {
        this.station = station;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getStation() {
        return station;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
