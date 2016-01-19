package drones;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Dispatcher extends Thread {

    private final Map<Integer, Drone> drones = new HashMap<>();
    private final Map<Integer, List<DroneDestination>> droneLocations;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private LocalTime time = LocalTime.of(7, 45, 0); // arbitrary time value to start simulation

    /**
     * The Dispatcher constructor
     *
     * @param droneLocations a list of drone locations
     * @param tubes          the list of tubes
     */
    public Dispatcher(Map<Integer, List<DroneDestination>> droneLocations, List<Tube> tubes) {
        this.setName("Dispatcher");

        this.droneLocations = droneLocations;

        // for simplicity inject dispatcher in drones (unwanted high-coupling)
        drones.put(5937, new Drone(5937, tubes, this.droneLocations.get(5937).get(0), this));
        drones.put(6043, new Drone(6043, tubes, this.droneLocations.get(6043).get(0), this));
    }

    /**
     * Start the thread (called by Thread.start())
     */
    public void run() {
        // start the drones
        for (Drone drone : drones.values()) {

            System.out.println(String.format("Sending initial destinations to drone %d", drone.getDroneId()));

            while (drone.addDestination(droneLocations.get(drone.getDroneId()).get(0))) {
                droneLocations.get(drone.getDroneId()).remove(0);
            }
            drone.setName(String.valueOf(drone.getDroneId()));
            drone.start();
        }

        long t0 = System.nanoTime();
        long t1 = t0;
        while (time.isBefore(LocalTime.of(8, 10))) {  // end simulation at 8:10
            if (t1 - t0 >= TrafficDrones.SampleInterval) {

                readWriteLock.writeLock().lock();
                time = time.plusSeconds((t1 - t0) / TrafficDrones.SampleInterval);
                readWriteLock.writeLock().unlock();

                for (Drone d : drones.values()) {
                    if (!droneLocations.get(d.getDroneId()).isEmpty()) {
                        if (d.addDestination(droneLocations.get(d.getDroneId()).get(0))) {
                            droneLocations.get(d.getDroneId()).remove(0);
                        }
                    } else {
                        System.out.println(String.format("Dispatcher have no more destinations for drone %d", d.getDroneId()));
                    }
                }
                t0 = System.nanoTime();
            }
            t1 = System.nanoTime();
        }

        System.out.println(String.format("Shutting down at %s", time));

        // terminate drones
        for (Drone drone : drones.values()) {
            drone.terminate("SHUTDOWN");
        }
    }

    /**
     * For simplicity the Dispatcher is keeping track of the actual time
     *
     * @return a local time
     */
    public LocalTime getTime() {
        LocalTime time;
        readWriteLock.readLock().lock();
        time = this.time;
        readWriteLock.readLock().unlock();
        return time;
    }

    /**
     * Report a traffic report
     *
     * @param droneId   the drone id
     * @param condition the condition
     */
    public void reportTraffic(int droneId, String condition) {
        System.out.println(String.format("Dispatcher is reporting traffic: %s, %s, %f, %s",
                droneId, time, drones.get(droneId).getSpeed(), condition));
    }
}
