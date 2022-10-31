package loadbalancer.providerregistry;

import provider.Provider;

public interface ProviderRegistryObserver {

    String getObserverId();

    void providerRegistered(Provider provider);

    void providerUnregistered(Provider provider);
}
