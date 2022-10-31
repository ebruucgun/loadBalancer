package loadbalancer.provideravailability;

public interface AvailabilityTracerObserver {

    String getObserverId();

    void traceableAvailabilityChanged(Traceable traceable, boolean isAvailable);
}
