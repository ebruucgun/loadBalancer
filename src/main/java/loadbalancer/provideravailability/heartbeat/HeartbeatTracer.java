package loadbalancer.provideravailability.heartbeat;

public interface HeartbeatTracer {

    HeartbeatTraceable getHeartbeatTraceable();

    void start();

    void stop();
}
