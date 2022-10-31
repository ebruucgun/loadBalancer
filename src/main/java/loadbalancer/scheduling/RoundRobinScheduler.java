package loadbalancer.scheduling;

import provider.Provider;

import java.util.*;

public class RoundRobinScheduler extends AbstractScheduler {

    private Map<String, ProviderSchedulingNode> providers;
    private Queue<String> queue;

    public RoundRobinScheduler() {
        providers = new HashMap<>();
        queue = new LinkedList<>();
    }

    @Override
    public void providerRegistered(Provider provider) {
        addRegisteredNode(provider);
    }

    @Override
    public void providerUnregistered(Provider provider) {
        removeRegisteredNode(provider);
    }

    @Override
    public String getNext() {
        ProviderSchedulingNode next = findNext();
        if (next != null)
            return next.getProvider().get();

        return null;
    }

    @Override
    protected ProviderSchedulingNode findProviderNode(Provider provider) {
        if (provider == null)
            return null;

        return findProviderNode(provider.getId());
    }

    @Override
    protected ProviderSchedulingNode findProviderNode(String id) {
        synchronized (lock) {
            if (providers.containsKey(id)) {
                return providers.get(id);
            }
        }
        return null;
    }

    private void addRegisteredNode(Provider provider) {
        synchronized (lock) {
            var node = new ProviderSchedulingNode(provider, false);
            String providerId = provider.getId();
            providers.put(providerId, node);
            enqueue(provider.getId());
        }
    }

    private void removeRegisteredNode(Provider provider) {
        synchronized (lock) {
            providers.remove(provider.getId());
        }
    }

    private void enqueue(String providerId) {
        synchronized (lock) {
            queue.offer(providerId);
        }
    }

    private String dequeue() {
        synchronized (lock) {
            return queue.poll();
        }
    }

    private ProviderSchedulingNode findNext() {
        synchronized (lock) {
            ProviderSchedulingNode nextNode = null;

            boolean lookNext = !queue.isEmpty();
            while (lookNext) {
                String currentId = dequeue();
                if (currentId != null) {
                    if (providers.containsKey(currentId)) {
                        nextNode = providers.get(currentId);
                        if (isNodeAvailable(nextNode)) {
                            lookNext = false;
                            enqueue(currentId);
                        }
                    }
                }
                else {
                    // There is no element in the queue
                    lookNext = false;
                }
            }

            return nextNode;
        }
    }
}
