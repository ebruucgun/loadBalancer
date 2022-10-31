package loadbalancer.scheduling;

import loadbalancer.provideravailability.AvailabilityTracerObserver;
import loadbalancer.provideravailability.Traceable;
import loadbalancer.providerregistry.ProviderRegistryObserver;
import provider.Provider;

import java.util.UUID;


public abstract class AbstractScheduler implements Scheduler, ProviderRegistryObserver, AvailabilityTracerObserver {

    protected class ProviderSchedulingNode {

        private Provider provider;
        private boolean isAvailable;

        public ProviderSchedulingNode(Provider provider) {
            this(provider, false);
        }

        public ProviderSchedulingNode(Provider provider, boolean isAvailable) {
            this.provider = provider;
            this.isAvailable = isAvailable;
        }

        public Provider getProvider() {
            return provider;
        }


        public boolean isAvailable() {
            return isAvailable;
        }

        public void setAvailable(boolean available) {
            isAvailable = available;
        }
    }

    protected String uuid = UUID.randomUUID().toString();

    protected Object lock = new Object();

    @Override
    public String getObserverId() {
        return uuid;
    }

    @Override
    public void traceableAvailabilityChanged(Traceable traceable, boolean isAvailable) {
        updateAvailable(traceable.getTraceableId(), isAvailable);
    }

    protected boolean isNodeAvailable(ProviderSchedulingNode node) {
        return node.isAvailable();
    }

    protected void updateAvailable(String key, boolean isAvailable) {
        synchronized (lock) {
            var node = findProviderNode(key);
            if (node != null) {
                node.setAvailable(isAvailable);
            }
        }
    }
    protected abstract ProviderSchedulingNode findProviderNode(Provider provider);

    protected abstract ProviderSchedulingNode findProviderNode(String id);
}
