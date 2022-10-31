package loadbalancer.providerregistry;

import provider.Provider;

public interface ProviderRegistryObserverSubject {

    void attach(ProviderRegistryObserver observer);

    void detach(ProviderRegistryObserver observer);

    void notifyProviderRegistered(Provider provider);

    void notifyProviderUnregistered(Provider provider);
}
