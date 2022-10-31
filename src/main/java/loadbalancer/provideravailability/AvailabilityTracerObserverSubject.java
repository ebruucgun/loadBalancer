package loadbalancer.provideravailability;

public interface AvailabilityTracerObserverSubject {

    void attach(AvailabilityTracerObserver observer);

    void detach(AvailabilityTracerObserver observer);

    void notifyAvailabilityChanged(Traceable traceable, boolean isAvailable);
}
