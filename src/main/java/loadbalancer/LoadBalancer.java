package loadbalancer;

import provider.Provider;

public interface LoadBalancer
{
    String get();

    void register(Provider provider);

    void register(Iterable<Provider> providers);

    void unregister(Provider provider);

    boolean isRegistered(Provider provider);

    void exclude(Provider provider);

    void include(Provider provider);

    boolean isExcluded(Provider provider);
}
