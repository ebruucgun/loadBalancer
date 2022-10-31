package loadbalancer.provideravailability;

import loadbalancer.provideravailability.heartbeat.HeartbeatTraceable;
import loadbalancer.provideravailability.heartbeat.HeartbeatTracer;
import loadbalancer.provideravailability.heartbeat.HeartbeatTracerImpl;
import loadbalancer.providerregistry.ProviderRegistryObserver;
import provider.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AvailabilityTracerImpl implements AvailabilityTracer, ProviderRegistryObserver {

    protected class TraceInfo {

        private final Traceable traceable;
        private boolean isAlive;
        private boolean isExcluded;

        public TraceInfo(Traceable traceable) {
            this(traceable, false, false);
        }

        public TraceInfo(Traceable traceable, boolean isAlive, boolean isExcluded) {
            if (traceable == null)
                throw new IllegalArgumentException("Traceable can not be null");

            this.traceable = traceable;
            this.isAlive = isAlive;
            this.isExcluded = isExcluded;
        }

        public String getId() {
            return traceable.getTraceableId();
        }

        public Traceable getTraceable() {
            return traceable;
        }

        public boolean isAlive() {
            return isAlive;
        }

        public void setAlive(boolean alive) {
            isAlive = alive;
        }

        public boolean isExcluded() {
            return isExcluded;
        }

        public void setExcluded(boolean excluded) {
            isExcluded = excluded;
        }

        public boolean isAvailable() { return isAlive() && !isExcluded(); }
    }

    protected class ReInclusionPolicyInfo {
        private String traceableId;

        private volatile int requiredHeartbeatCount;

        public ReInclusionPolicyInfo(String traceableId, int requiredHeartbeatCount) {
            this.traceableId = traceableId;
            this.requiredHeartbeatCount = requiredHeartbeatCount;
        }

        public String getTraceableId() {
            return traceableId;
        }

        public int getRequiredHeartbeatCount() {
            return requiredHeartbeatCount;
        }

        public void decreaseCounter() {
            requiredHeartbeatCount--;
        }
    }

    private final String providerRegistryObserverId = UUID.randomUUID().toString();

    private final Map<String, TraceInfo> tracked;
    private final Map<String, HeartbeatTracer> heartbeatTracers;

    private final Map<String, ReInclusionPolicyInfo> reInclusionPolicyElements;

    private final Object lock = new Object();

    private final AvailabilityTracerObserverSubject availabilityTracerObserverSubject;

    private final boolean isApplyReInclusionPolicy;

    private static final int HEARTBEAT_TRACE_FREQUENCY_IN_MS = 2 * 1000;


    public AvailabilityTracerImpl(AvailabilityTracerObserverSubject availabilityTracerObserverSubject,
                                  boolean isApplyReInclusionPolicy) {
        tracked = new HashMap<>();
        heartbeatTracers = new HashMap<>();
        reInclusionPolicyElements = new HashMap<>();

        this.availabilityTracerObserverSubject = availabilityTracerObserverSubject;
        this.isApplyReInclusionPolicy = isApplyReInclusionPolicy;
    }

    @Override
    public String getObserverId() {
        return providerRegistryObserverId;
    }

    @Override
    public void providerRegistered(Provider provider) {
        handleProviderRegister(provider);
    }

    @Override
    public void providerUnregistered(Provider provider) {
        handleProviderUnregister(provider);
    }

    @Override
    public void updateAliveness(HeartbeatTraceable traceable, boolean isAlive) {
        updateAlivenessTraceable(traceable.getTraceableId(), isAlive);
    }

    @Override
    public void updateExclusion(Traceable traceable, boolean isExcluded) {
        updateExclusionTraceable(traceable.getTraceableId(), isExcluded);
    }

    @Override
    public boolean isAlive(HeartbeatTraceable traceable) {
        return isHeartbeatTraceableAlive(traceable.getTraceableId());
    }

    @Override
    public boolean isExcluded(Traceable traceable) {
        return isTraceableExcluded(traceable.getTraceableId());
    }

    private void handleProviderRegister(Provider provider) {
        synchronized (lock) {
            if (hasTraceInfo(provider)) {
                throw new IllegalStateException("Provider is already registered in AvailabilityTracer");
            }

            TraceInfo traceInfo = createTraceInfo(provider);
            addUpdateTraceInfo(traceInfo);

            HeartbeatTracer heartbeatTracer = createHeartbeatTracer(provider);
            saveHeartbeatTracer(heartbeatTracer);
            startHeartbeatTracer(heartbeatTracer);
        }
    }
    
    private void handleProviderUnregister(Provider provider) {
        synchronized (lock) {
            String traceableId = provider.getTraceableId();
            if (!hasTraceInfo(provider)) {
                throw new IllegalArgumentException("Provider is already unregistered in AvailabilityTracer");
            }

            TraceInfo traceInfo = getTraceInfo(traceableId);
            deleteTraceInfo(traceInfo);

            HeartbeatTracer heartbeatTracer = getHeartbeatTracer(traceableId);
            stopHeartbeatTracer(heartbeatTracer);
            deleteHeartbeatTracer(heartbeatTracer.getHeartbeatTraceable().getTraceableId());
        }
    }

    private void updateAlivenessTraceable(String id, boolean isAlive) {
        synchronized (lock) {
            TraceInfo traceInfo = getTraceInfo(id);
            if (traceInfo == null)
                throw new IllegalStateException("Trace info could not be found to update aliveness for: " + id);

            boolean shouldApplyReInclusionPolicy = shouldApplyReInclusionPolicy(traceInfo.isExcluded());
            if (shouldApplyReInclusionPolicy) {
                ReInclusionPolicyInfo reInclusionPolicyInfo = decreaseCounterReInclusionPolicyInfo(id);
                System.out.println(
                        String.format("Reinclusion policy triggered: %s. Required heartbeat count: %d",
                                id, reInclusionPolicyInfo.getRequiredHeartbeatCount()));
                if (shouldReInclusionApplied(reInclusionPolicyInfo)) {
                    System.out.println(
                            String.format("Re-included Traceable %s", id));
                    updateExclusion(traceInfo.getTraceable(), false);
                    deleteReInclusionPolicyInfo(id);
                }
            }

            boolean willBeNotified = shouldAvailabilityNotifyTriggeredOnAlivenessChange(traceInfo, isAlive);

            traceInfo.setAlive(isAlive);
            addUpdateTraceInfo(traceInfo);

            if (willBeNotified) {
                availabilityTracerObserverSubject.notifyAvailabilityChanged(traceInfo.getTraceable(), getAvailability(traceInfo));
            }
        }
    }

    private void updateExclusionTraceable(String id, boolean isExcluded) {
        synchronized (lock) {
            TraceInfo traceInfo = getTraceInfo(id);
            if (traceInfo == null) {
                throw new IllegalStateException("Trace info could not be found to update exclusion for: " + id);
            }

            boolean shouldApplyReInclusionPolicy = shouldApplyReInclusionPolicy(isExcluded);
            if (shouldApplyReInclusionPolicy) {
                ReInclusionPolicyInfo reInclusionPolicyInfo = getCreateReInclusionPolicyInfo(id);
                System.out.println(
                        String.format("Reinclusion policy enabled: %s. Required heartbeat count: %d",
                                reInclusionPolicyInfo.getTraceableId(),
                                reInclusionPolicyInfo.getRequiredHeartbeatCount()));
            }

            boolean willBeNotified = shouldAvailabilityNotifyTriggeredOnExclusionChange(traceInfo, isExcluded);
            traceInfo.setExcluded(isExcluded);
            addUpdateTraceInfo(traceInfo);

            if (willBeNotified) {
                availabilityTracerObserverSubject.notifyAvailabilityChanged(traceInfo.getTraceable(), getAvailability(traceInfo));
            }
        }
    }

    private boolean shouldAvailabilityNotifyTriggeredOnExclusionChange(TraceInfo traceInfo, boolean newExclusionState) {
        synchronized (lock) {
            if (traceInfo.isExcluded() == newExclusionState)
                return false;

            return traceInfo.isAlive();
        }
    }

    private boolean shouldAvailabilityNotifyTriggeredOnAlivenessChange(TraceInfo traceInfo, boolean newAlivenessState) {
        synchronized (lock) {
            if (traceInfo.isAlive() == newAlivenessState) {
                return false;
            }

            return !traceInfo.isExcluded();
        }
    }

    private boolean shouldApplyReInclusionPolicy(boolean isExcluded) {
        return isApplyReInclusionPolicy && isExcluded;
    }

    private ReInclusionPolicyInfo getCreateReInclusionPolicyInfo(String traceableId) {
        synchronized (lock) {
            ReInclusionPolicyInfo reInclusionPolicyInfo = null;
            if (reInclusionPolicyElements.containsKey(traceableId)) {
                reInclusionPolicyInfo = reInclusionPolicyElements.get(traceableId);
            }
            else {
                reInclusionPolicyInfo = new ReInclusionPolicyInfo(traceableId, 2);
            }

            return reInclusionPolicyInfo;
        }
    }

    private ReInclusionPolicyInfo decreaseCounterReInclusionPolicyInfo(String traceableId) {
        synchronized (lock) {
            ReInclusionPolicyInfo reInclusionPolicyInfo = getCreateReInclusionPolicyInfo(traceableId);
            reInclusionPolicyInfo.decreaseCounter();
            updateReInclusionPolicyInfo(reInclusionPolicyInfo);
            return reInclusionPolicyInfo;
        }
    }

    private void updateReInclusionPolicyInfo(ReInclusionPolicyInfo reInclusionPolicyInfo) {
        synchronized (lock) {
            reInclusionPolicyElements.put(reInclusionPolicyInfo.getTraceableId(), reInclusionPolicyInfo);
        }
    }

    private boolean shouldReInclusionApplied(ReInclusionPolicyInfo reInclusionPolicyInfo) {
        return reInclusionPolicyInfo.getRequiredHeartbeatCount() <= 0;
    }

    private void deleteReInclusionPolicyInfo(String traceableId) {
        synchronized (lock) {
            reInclusionPolicyElements.remove(traceableId);
        }
    }

    private boolean getAvailability(TraceInfo traceInfo) {
        return traceInfo.isAvailable();
    }

    private boolean isHeartbeatTraceableAlive(String id) {
        synchronized (lock) {
            TraceInfo traceInfo = getTraceInfo(id);
            if (traceInfo == null) {
                throw new IllegalStateException("Trace info could not be found to query aliveness for: " + id);
            }

            return traceInfo.isAlive();
        }
    }

    private boolean isTraceableExcluded(String id) {
        synchronized (lock) {
            TraceInfo traceInfo = getTraceInfo(id);
            if (traceInfo == null) {
                throw new IllegalStateException("Trace info could not be found to query exclusion for: " + id);
            }

            return traceInfo.isExcluded();
        }
    }

    private boolean isTraceableAvailable(String id) {
        synchronized (lock) {
            TraceInfo traceInfo = getTraceInfo(id);
            if (traceInfo == null) {
                throw new IllegalStateException("Trace info could not be found to query availability:" + id);
            }

            return traceInfo.isAlive() && !traceInfo.isExcluded();
        }
    }

    private TraceInfo getTraceInfo(String id) {
        synchronized (lock) {
            if (!hasTraceInfo(id))
                return null;

            return tracked.get(id);
        }
    }

    private TraceInfo createTraceInfo(Traceable traceable) {
        return new TraceInfo(traceable);
    }

    private boolean hasTraceInfo(Traceable traceable) {
        synchronized (lock) {
            String traceableId = traceable.getTraceableId();
            return hasTraceInfo(traceableId);
        }
    }

    private boolean hasTraceInfo(String id) {
        synchronized (lock) {
            return tracked.containsKey(id);
        }
    }

    private void addUpdateTraceInfo(TraceInfo traceInfo) {
        synchronized (lock) {
            tracked.put(traceInfo.getId(), traceInfo);
        }
    }

    private void deleteTraceInfo(TraceInfo traceInfo) {
        synchronized (lock) {
            tracked.remove(traceInfo.getId());
        }
    }

    private boolean hasHeartbeatTracer(String heartbeatTraceableId) {
        synchronized (lock) {
            return heartbeatTracers.containsKey(heartbeatTraceableId);
        }
    }

    private HeartbeatTracer getHeartbeatTracer(String heartbeatTraceableId) {
        synchronized (lock) {
            return heartbeatTracers.get(heartbeatTraceableId);
        }
    }

    private void deleteHeartbeatTracer(String heartbeatTraceableId) {
        synchronized (lock) {
            heartbeatTracers.remove(heartbeatTraceableId);
        }
    }

    private HeartbeatTracer createHeartbeatTracer(HeartbeatTraceable heartbeatTraceable) {
        return new HeartbeatTracerImpl(heartbeatTraceable, this, HEARTBEAT_TRACE_FREQUENCY_IN_MS);
    }

    private void saveHeartbeatTracer(HeartbeatTracer tracer) {
        synchronized (lock) {
            if (tracer == null) {
                throw new IllegalArgumentException("HeartbeatTracer can not be null while saving tracer");
            }

            heartbeatTracers.put(tracer.getHeartbeatTraceable().getTraceableId(), tracer);
        }
    }

    private void startHeartbeatTracer(HeartbeatTracer tracer) {
        synchronized (lock) {
            tracer.start();
        }
    }

    private void stopHeartbeatTracer(HeartbeatTracer tracer) {
        synchronized (lock) {
            tracer.stop();
        }
    }
}
