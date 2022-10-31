package loadbalancer.providerregistry;

import provider.Provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRegistryImpl implements ProviderRegistry {

    private Map<String, Provider> registeredProviders;

    private int capacity;

    private ProviderRegistryObserverSubject observerSubject;

    public ProviderRegistryImpl(int capacity, ProviderRegistryObserverSubject observerSubject) {
        this.registeredProviders = new HashMap<>();
        this.capacity = capacity;
        this.observerSubject = observerSubject;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized int getRegisteredCount() {
        return registeredProviders.size();
    }

    @Override
    public void register(Provider provider) {
        registerProvider(provider);
    }

    @Override
    public void register(Iterable<Provider> providers) {
        List<Provider> providerList = new ArrayList<>();
        providers.forEach(providerList::add);
        registerProviders(providerList);
    }

    @Override
    public void unregister(Provider provider) {
        unregisterProvider(provider);
    }

    @Override
    public boolean isRegistered(Provider provider) {
        return isRegisteredProvider(provider);
    }

    @Override
    public synchronized Iterable<Provider> getProviders() {
        return registeredProviders.values().stream().toList();
    }

    private synchronized void registerProvider(Provider provider) {
        if (isRegisteredProvider(provider))
            throw new IllegalArgumentException(String.format("Provider is already registered. Id: %s", provider.getId()));

        if (!isAvailableToRegister())
            throw new IllegalStateException(String.format("Reached to capacity. No provider is acceptable"));

        System.out.println("Registered provider id: " + provider.getId());

        registeredProviders.put(provider.getId(), provider);
        observerSubject.notifyProviderRegistered(provider);
    }

    private synchronized void registerProviders(List<Provider> providers) {
        var freeSlotCount = getFreeSlotCount();
        if (freeSlotCount < providers.size())
            throw new IllegalStateException(String.format("Reached to capacity. No provider is acceptable"));
        
        // Check for all providers to register only once
        for (var provider : providers) {
            if (isRegisteredProvider(provider))
                throw new IllegalArgumentException(String.format("Provider is already registered. Id: %s", provider.getId()));
        }

        for (var provider : providers) {
          registerProvider(provider);
        }
    }

    private synchronized void unregisterProvider(Provider provider) {
        if (!isRegisteredProvider(provider))
            throw new IllegalArgumentException(String.format("Provider is not registered. Id: %s", provider.getId()));

        registeredProviders.remove(provider.getId());
        observerSubject.notifyProviderUnregistered(provider);
    }

    private synchronized boolean isRegisteredProvider(Provider provider) {
        return registeredProviders.containsKey(provider.getId());
    }

    private synchronized int getFreeSlotCount() {
        return capacity - registeredProviders.size();
    }

    private synchronized boolean isAvailableToRegister() {
        return getFreeSlotCount() > 0;
    }
}
