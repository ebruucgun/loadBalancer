package loadbalancer.providerregistry;

import provider.Provider;

public interface ProviderRegistry {
    void register(Provider provider);

    void register(Iterable<Provider> providers);

    void unregister(Provider provider);

    boolean isRegistered(Provider provider);

    Iterable<Provider> getProviders();

}
