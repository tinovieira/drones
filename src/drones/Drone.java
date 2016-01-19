package drones;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class Drone extends Thread {

    private final int id;

    private boolean isTerminated;

    private final List<Tube> tubes;

    private final Queue<DroneDestination> destinations = new LinkedList<>();

    public String[] conditions = new String[]{"HEAVY", "LIGHT", "MODERATE"};

    private final Random random = new Random();

    private final Dispatcher dispatcher;

    // the current position of the drone
    private double lat;
    private double lon;
    private double bearing;

    private double speed; // current speed (m/s)
    private double distanceToDestination; // distance to next destination
    private double distanceTravelled; // distance since last speed/bearing adjustment

    public Drone(int id, List<Tube> tubes, DroneDestination initialLocation, Dispatcher dispatcher) {

        this.id = id;
        this.tubes = tubes;

        lat = initialLocation.getLatitude();
        lon = initialLocation.getLongitude();

        this.dispatcher = dispatcher;

    }


    /**
     * Start the thread (called by Thread.start())
     */
    public void run() {

        while (!isTerminated) {
            if (!destinations.isEmpty() && dispatcher.getTime().isAfter(destinations.peek().getTime())) {
                DroneDestination destination = destinations.poll();
                lat = destination.getLatitude();
                lon = destination.getLongitude();
                checkNearbyLocations(350);
            }
        }
    }

    /**
     * Start the thread (called by Thread.start())
     */
    public void runTimer() {

        LocalTime t0 = dispatcher.getTime();
        LocalTime t1 = t0;

        setDestination();

        while (!isTerminated) {

            long dt = t0.until(t1, ChronoUnit.SECONDS);
            if (dt >= 1) {
                checkNearbyLocations(350); // find stations within distance of 350m
                move(dt);
                t0 = dispatcher.getTime();
            }
            t1 = dispatcher.getTime();
        }
    }

    /**
     * Get the drone id
     *
     * @return the drone id
     */
    public int getDroneId() {
        return id;
    }

    /**
     * Get the current speed of the Drone
     *
     * @return the drone speed
     */
    public double getSpeed() {
        return speed;
    }


    /**
     * Terminate the drone with a "secret" code
     *
     * @param code the code
     */
    public void terminate(String code) {
        if (code.equals("SHUTDOWN"))
            isTerminated = true;
    }

    /**
     * Add a destination to the drones internal destinations. Since the drone has limited memory
     * this call may return false
     *
     * @param location to store
     * @return true if the drone stored the destination in its destinations
     */
    public boolean addDestination(DroneDestination location) {

        return destinations.size() < 10 && destinations.offer(location);
    }

    /**
     * Set the speed required to reach next destination in time
     */
    private void setDestination() {

        DroneDestination destination = destinations.peek(); // assuming drone is travelling to head of list

        if (destination != null) {

            distanceToDestination = calculateDistance(lat, destination.getLatitude(), lon, destination.getLongitude());
            long seconds = dispatcher.getTime().until(destination.getTime(), ChronoUnit.SECONDS);

            speed = distanceToDestination / seconds;
            bearing = calculateBearing(lat, destination.getLatitude(), lon, destination.getLongitude());

        } else {
            System.out.println(String.format("Drone %d have no more destinations...", id));
            isTerminated = true;
        }
    }

    /**
     * Find nearby stations and report traffic conditions to dispatcher
     */
    private void checkNearbyLocations(double radius) {

        for (Tube tube : tubes) {
            if (radius >= calculateDistance(lat, tube.getLatitude(), lon, tube.getLongitude())) {
                System.out.println(String.format("Drone %d is sending report from %s to Dispatcher", id, tube.getStation()));

                dispatcher.reportTraffic(id, conditions[random.nextInt(3)]);
            }
        }
    }

    /**
     * Move the drone to its next location.
     * During the move there is a small chance the drone crashes
     */
    private void move(long elapsedTime) {

        if (random.nextInt(1000000) == 1) {
            System.out.println(String.format("Drone %d crashed into a building", id));
            isTerminated = true;
        } else {
            double d = speed * elapsedTime;

            double delta_latitude = d * Math.sin(bearing) / 110540;
            double delta_longitude = d * Math.cos(bearing) / (111320 * Math.cos(lat));

            lat += delta_latitude;
            lon += delta_longitude;
            distanceTravelled += d;

            if (distanceTravelled >= distanceToDestination) {
                destinations.poll();
                setDestination();
            }
        }
    }

    /**
     * Calculate distance in meters between two points in latitude and longitude
     * Copied from: http://stackoverflow.com/a/16794680/470864
     */
    public static double calculateDistance(double lat1, double lat2, double lon1, double lon2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }

    /**
     * Calculate bearing between two pairs of latitude and longitude
     * Copied from: http://stackoverflow.com/a/9462757/470864
     */
    public static double calculateBearing(double lat1, double lat2, double lon1, double lon2) {

        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(lon2 - lon1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }
}
