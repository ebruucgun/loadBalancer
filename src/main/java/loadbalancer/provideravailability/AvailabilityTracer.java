package loadbalancer.provideravailability;

import loadbalancer.provideravailability.heartbeat.HeartbeatTraceable;

public interface AvailabilityTracer {

    void updateAliveness(HeartbeatTraceable traceable, boolean isAlive);

    void updateExclusion(Traceable traceable, boolean isExcluded);

    boolean isAlive(HeartbeatTraceable traceable);

    boolean isExcluded(Traceable traceable);
}
