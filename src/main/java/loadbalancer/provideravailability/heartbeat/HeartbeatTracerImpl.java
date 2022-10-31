package loadbalancer.provideravailability.heartbeat;

import loadbalancer.provideravailability.AvailabilityTracer;

import java.util.Timer;
import java.util.TimerTask;

public class HeartbeatTracerImpl implements HeartbeatTracer {

  private final HeartbeatTraceable traceable;

  private final AvailabilityTracer availabilityTracer;

  private final int traceFrequencyInMs;

  private final Timer timer;
  private final TimerTask timerTask;

  private static final int FIRST_RUN_DELAY_IN_MS = 2 * 1000;

  public HeartbeatTracerImpl(
      HeartbeatTraceable traceable, AvailabilityTracer availabilityTracer, int traceFrequencyInMs) {
    if (traceable == null) throw new IllegalArgumentException("HeartbeatTraceable can not be null");

    if (availabilityTracer == null)
      throw new IllegalArgumentException("AvailabilityTracer can not be null");

    if (traceFrequencyInMs <= 0) throw new IllegalArgumentException("Frequency must be positive");

    this.traceable = traceable;
    this.availabilityTracer = availabilityTracer;
    this.traceFrequencyInMs = traceFrequencyInMs;

    this.timer = new Timer(traceable.getTraceableId(), true);
    this.timerTask =
        new TimerTask() {
          @Override
          public void run() {
            boolean traceResult = trace();
            System.out.println(String.format("Heartbeat checked for %s: %s", traceable.getTraceableId(), traceResult));
            availabilityTracer.updateAliveness(traceable, traceResult);
          }
        };
  }

  @Override
  public HeartbeatTraceable getHeartbeatTraceable() {
    return traceable;
  }

  @Override
  public void start() {
    timer.scheduleAtFixedRate(timerTask, FIRST_RUN_DELAY_IN_MS, traceFrequencyInMs);
  }

  @Override
  public void stop() {
    timer.cancel();
  }

  private boolean trace() {
    return traceable.check();
  }
}
