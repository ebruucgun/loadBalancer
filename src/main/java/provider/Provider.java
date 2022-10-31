package provider;

import loadbalancer.provideravailability.heartbeat.HeartbeatTraceable;

public interface Provider extends HeartbeatTraceable {

    String getId();

    String get();
}
