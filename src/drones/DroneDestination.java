package drones;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DroneDestination {

    private final int droneId;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime time;

    public DroneDestination(int droneId, double latitude, double longitude, LocalDateTime time) {
        this.droneId = droneId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public int getDroneId() {return droneId;}

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalTime getTime() {
        return time.toLocalTime();
    }
}
