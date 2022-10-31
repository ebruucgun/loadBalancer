
import loadbalancer.LoadBalancer;
import loadbalancer.LoadBalancerImpl;
import loadbalancer.scheduling.SchedulerType;
import provider.Provider;
import provider.ProviderImpl;

import java.util.*;

public class LoadBalancerApplication {

    public static final int LOAD_BALANCER_CAPACITY = 10;

    public static final boolean APPLY_REINCLUSION_POLICY = false;

    private static int numberOfProviders = 5;
    private static int numberOfExclusion = 2;

    public static void main(String[] args) {
//        driveRandomScheduledLoadBalancer();
        driveRoundRobinScheduledLoadBalancer();
    }

    private static void driveRandomScheduledLoadBalancer() {
        System.out.println("RANDOM SCHEDULED LOAD BALANCER");
        var loadBalancer = createLoadBalancerRandom();
        var providers = createProviders();
        var exclusions = createExclusions(providers, numberOfExclusion);

        System.out.println("Providers to register: " + Arrays.toString(providers.stream().map(provider -> provider.getId()).toArray()));
        System.out.println("Providers to exclude: " + Arrays.toString(exclusions.stream().map(provider -> provider.getId()).toArray()));

        loadBalancer.register(providers);
        exclusions.forEach(exclusion -> loadBalancer.exclude(exclusion));

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 10; i++) {
            String providerId = loadBalancer.get();
            System.out.println(String.format("%d: %s",  i, providerId));
        }
    }

    private static void driveRoundRobinScheduledLoadBalancer() {
        System.out.println("ROUND ROBIN SCHEDULED LOAD BALANCER");
        var loadBalancer = createLoadBalancerRoundRobin();
        var providers = createProviders();
        var exclusions = createExclusions(providers, numberOfExclusion);

        System.out.println("Providers to register: " + Arrays.toString(providers.stream().map(provider -> provider.getId()).toArray()));
        System.out.println("Providers to exclude: " + Arrays.toString(exclusions.stream().map(provider -> provider.getId()).toArray()));

        loadBalancer.register(providers);
        exclusions.forEach(exclusion -> loadBalancer.exclude(exclusion));

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 10; i++) {
            String providerId = loadBalancer.get();
            System.out.println(String.format("%d: %s",  i, providerId));
        }
    }

    private static LoadBalancer createLoadBalancerRandom() {
        return new LoadBalancerImpl(LOAD_BALANCER_CAPACITY, APPLY_REINCLUSION_POLICY, SchedulerType.RANDOM);
    }

    private static LoadBalancer createLoadBalancerRoundRobin() {
        return new LoadBalancerImpl(LOAD_BALANCER_CAPACITY, APPLY_REINCLUSION_POLICY, SchedulerType.ROUND_ROBIN);
    }

    private static List<Provider> createProviders() {
        Provider[] providers = new Provider[numberOfProviders];
        for (int i = 0; i < numberOfProviders; i++) {
            providers[i] = new ProviderImpl();
        }

        return Arrays.stream(providers).toList();
    }

    private static List<Provider> createExclusions(List<Provider> providers, int numberOfExclusion) {
        if (numberOfExclusion <= 0)
            return new ArrayList<>();
        if (numberOfExclusion == providers.size())
            return new ArrayList(providers);

        var random = new Random();
        ArrayList<Provider> exclusions = new ArrayList<>();
        Set<Integer> alreadyExcluded = new HashSet<>();

        for (int i = 0; i < numberOfExclusion; i++) {
            boolean findNextIndex = true;
            while (findNextIndex) {
                int index = random.nextInt(providers.size());
                if (!alreadyExcluded.contains(index)){
                    alreadyExcluded.add(index);
                    findNextIndex = false;
                    exclusions.add(providers.get(index));
                }
            }
        }

        return exclusions;
    }
}
