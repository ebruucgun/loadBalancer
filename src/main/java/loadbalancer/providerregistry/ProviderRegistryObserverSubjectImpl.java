package loadbalancer.providerregistry;

import provider.Provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRegistryObserverSubjectImpl implements ProviderRegistryObserverSubject{

        private Map<String, ProviderRegistryObserver> observers;
        private Object lock = new Object();

    public ProviderRegistryObserverSubjectImpl() {
        observers = new HashMap<>();
    }

    @Override
    public void attach(ProviderRegistryObserver observer) {
        synchronized (lock) {
            observers.put(observer.getObserverId(), observer);
        }
    }

    @Override
    public void detach(ProviderRegistryObserver observer) {
        synchronized (lock) {
            observers.remove(observer.getObserverId());
        }
    }

    @Override
    public void notifyProviderRegistered(Provider provider) {
        List<ProviderRegistryObserver> currentObservers;
        synchronized (lock) {
            currentObservers = observers.values().stream().toList();
        }

        currentObservers.forEach(observer -> observer.providerRegistered(provider));
    }

    @Override
    public void notifyProviderUnregistered(Provider provider) {
        List<ProviderRegistryObserver> currentObservers;
        synchronized (lock) {
            currentObservers = observers.values().stream().toList();
        }

        currentObservers.forEach(observer -> observer.providerUnregistered(provider));
    }
}
