package loadbalancer.scheduling;

import provider.Provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomScheduler extends AbstractScheduler {

    private final List<ProviderSchedulingNode> providers;

    private Random random;

    public RandomScheduler() {
        providers = new ArrayList<>();
        random = new Random();
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
        List<ProviderSchedulingNode> currentNodes = getAvailableNodes();
        ProviderSchedulingNode nextNode = findNext(currentNodes);
        if (nextNode != null)
            return nextNode.getProvider().get();
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
            for (var provNode : providers) {
                if(provNode.getProvider().getId() == id) {
                    return provNode;
                }
            }
        }
        return null;
    }

    private List<ProviderSchedulingNode> getAvailableNodes() {
        synchronized (lock) {
            return providers.stream()
                    .filter(providerSchedulingNode -> isNodeAvailable(providerSchedulingNode))
                    .collect(Collectors.toList());
        }
    }

    private  ProviderSchedulingNode findNext(List<ProviderSchedulingNode> nodes) {

        var numberOfNodes = nodes.size();
        var randomIndex = random.nextInt(numberOfNodes);
        return nodes.get(randomIndex);
    }

    private void addRegisteredNode(Provider provider) {
        synchronized (lock) {
            var node = new ProviderSchedulingNode(provider, false);
            providers.add(node);
        }
    }

    private void removeRegisteredNode(Provider provider) {
        synchronized (lock) {
                providers.removeIf(providerSchedulingNode -> providerSchedulingNode.getProvider().getId() == provider.getId());
        }
    }
}
