package loadbalancer.provideravailability.heartbeat;

import loadbalancer.provideravailability.Traceable;

public interface HeartbeatTraceable extends Traceable {

    boolean check();
}
