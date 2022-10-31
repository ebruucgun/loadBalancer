package loadbalancer.provideravailability;

import loadbalancer.providerregistry.ProviderRegistryObserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvailabilityTracerObserverSubjectImpl implements AvailabilityTracerObserverSubject {

    private Map<String, AvailabilityTracerObserver> observers;
    private Object lock = new Object();

    public AvailabilityTracerObserverSubjectImpl() {
        observers = new HashMap<>();
    }

    @Override
    public void attach(AvailabilityTracerObserver observer) {
        synchronized (lock) {
            observers.put(observer.getObserverId(), observer);
        }
    }

    @Override
    public void detach(AvailabilityTracerObserver observer) {
        synchronized (lock) {
            observers.remove(observer.getObserverId());
        }
    }

    @Override
    public void notifyAvailabilityChanged(Traceable traceable, boolean isAvailable) {
        List<AvailabilityTracerObserver> currentObservers;
        synchronized (lock) {
            currentObservers = observers.values().stream().toList();
        }

        currentObservers.forEach(observer -> observer.traceableAvailabilityChanged(traceable, isAvailable));
    }
}
