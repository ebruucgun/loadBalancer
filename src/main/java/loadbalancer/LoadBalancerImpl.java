package loadbalancer;

import loadbalancer.provideravailability.AvailabilityTracer;
import loadbalancer.provideravailability.AvailabilityTracerImpl;
import loadbalancer.provideravailability.AvailabilityTracerObserverSubject;
import loadbalancer.provideravailability.AvailabilityTracerObserverSubjectImpl;
import loadbalancer.providerregistry.ProviderRegistry;
import loadbalancer.providerregistry.ProviderRegistryImpl;
import loadbalancer.providerregistry.ProviderRegistryObserverSubject;
import loadbalancer.providerregistry.ProviderRegistryObserverSubjectImpl;
import loadbalancer.scheduling.*;
import provider.Provider;

public class LoadBalancerImpl implements LoadBalancer {

    private final ProviderRegistryObserverSubject providerRegistryObserverSubject;

    private final AvailabilityTracerObserverSubject availabilityTracerObserverSubject;
    private ProviderRegistry providerRegistry;
    private AbstractScheduler scheduler;

    private AvailabilityTracer availabilityTracer;

    public LoadBalancerImpl(int providerCapacity, boolean isApplyReInclusionPolicy, SchedulerType schedulerType) {
        scheduler = createScheduler(schedulerType);

        providerRegistryObserverSubject = new ProviderRegistryObserverSubjectImpl();
        availabilityTracerObserverSubject = new AvailabilityTracerObserverSubjectImpl();

        AvailabilityTracerImpl availabilityTracerImpl = createAvailabilityTracerImpl(availabilityTracerObserverSubject, isApplyReInclusionPolicy);
        availabilityTracer = availabilityTracerImpl;

        providerRegistryObserverSubject.attach(scheduler);
        providerRegistryObserverSubject.attach(availabilityTracerImpl);

        availabilityTracerObserverSubject.attach(scheduler);

        providerRegistry = new ProviderRegistryImpl(providerCapacity, providerRegistryObserverSubject);
    }

    @Override
    public String get() {
        return scheduler.getNext();
    }

    @Override
    public void register(Provider provider) {
        providerRegistry.register(provider);
    }

    @Override
    public void register(Iterable<Provider> providers) {
        providerRegistry.register(providers);
    }

    @Override
    public void unregister(Provider provider) {
        providerRegistry.unregister(provider);
    }

    @Override
    public boolean isRegistered(Provider provider) {
        return providerRegistry.isRegistered(provider);
    }

    @Override
    public void exclude(Provider provider) {
        excludeProvider(provider);
    }

    @Override
    public void include(Provider provider) {
        includeProvider(provider);
    }

    @Override
    public boolean isExcluded(Provider provider) {
        return isExcludedProvider(provider);
    }

    private AbstractScheduler createScheduler(SchedulerType schedulerType) {
        switch (schedulerType) {
            case RANDOM -> {
                return new RandomScheduler();
            }
            case ROUND_ROBIN -> {
                return new RoundRobinScheduler();
            }
        }

        throw new IllegalArgumentException("Unknown scheduling type: " + schedulerType);
    }

    private AvailabilityTracerImpl createAvailabilityTracerImpl(AvailabilityTracerObserverSubject availabilityTracerObserverSubject,
                                                                boolean isApplyReInclusionPolicy) {
        return new AvailabilityTracerImpl(availabilityTracerObserverSubject, isApplyReInclusionPolicy);
    }

    private void excludeProvider(Provider provider) {
        if (provider == null)
            throw new IllegalArgumentException("Provider can not be invalid");

        System.out.println("Manually excluded provider id: " + provider.getId());

        availabilityTracer.updateExclusion(provider, true);
    }

    private void includeProvider(Provider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider can not be invalid");
        }

        System.out.println("Manually included provider id: " + provider.getId());

        availabilityTracer.updateExclusion(provider, false);
    }

    private boolean isExcludedProvider(Provider provider) {
        if (provider == null)
            throw new IllegalArgumentException("Provider can not be invalid");

        return availabilityTracer.isExcluded(provider);
    }
}
