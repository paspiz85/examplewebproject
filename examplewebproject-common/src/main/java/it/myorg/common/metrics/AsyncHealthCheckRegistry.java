package it.myorg.common.metrics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * {@link HealthCheckRegistry} that can run cheks asynchnously.
 *
 * @author paspiz85
 */
public class AsyncHealthCheckRegistry extends HealthCheckRegistry implements InitializingBean, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHealthCheckRegistry.class);
    private static final SimpleDateFormat TIMESTAMP_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final String TIMESTAMP_NAME = "healthCheckTimestamp";

    private Runnable healthCheckBackgroundTask;
    private ExecutorService healthCheckExecutor;
    private int healthCheckInterval;
    private ScheduledExecutorService healthSummaryPollingExecutor;
    private Timer healthCheckExecutionTimer;
    private SortedMap<String, HealthCheck.Result> results = new TreeMap<>();
    private volatile boolean running;
    private boolean startOnInit;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.healthCheckBackgroundTask = new Runnable() {
            @Override
            public void run() {
                Timer.Context timer = null;
                if (healthCheckExecutionTimer != null) {
                    timer = healthCheckExecutionTimer.time();
                }
                try {
                    if (healthCheckExecutor == null) {
                        results = AsyncHealthCheckRegistry.super.runHealthChecks();
                    } else {
                        results = AsyncHealthCheckRegistry.super.runHealthChecks(healthCheckExecutor);
                    }
                } finally {
                    if (timer != null) {
                        timer.stop();
                    }
                }
            }
        };
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (startOnInit) {
            start();
        }
    }

    @Override
    public HealthCheck.Result runHealthCheck(String name) throws NoSuchElementException {
        if (running) {
            return results.get(name);
        } else {
            return super.runHealthCheck(name);
        }
    }

    @Override
    public SortedMap<String, HealthCheck.Result> runHealthChecks() {
        if (running) {
            return results;
        } else {
            return super.runHealthChecks();
        }
    }

    @Override
    public SortedMap<String, HealthCheck.Result> runHealthChecks(ExecutorService executor) {
        if (running) {
            return results;
        } else {
            return super.runHealthChecks(executor);
        }
    }

    public void setHealthCheckExecutor(ExecutorService healthCheckExecutor) {
        this.healthCheckExecutor = healthCheckExecutor;
    }

    public void setHealthCheckInterval(int healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public void setHealthSummaryPollingExecutor(ScheduledExecutorService healthSummaryPollingExecutor) {
        this.healthSummaryPollingExecutor = healthSummaryPollingExecutor;
    }

    /**
     * Optional {@link MetricRegistry} to use a timer on healthcheks execution.
     *
     * @param metricRegistry metricRegistry.
     */
    public void setMetricRegistry(MetricRegistry metricRegistry) {
        if (metricRegistry != null) {
            healthCheckExecutionTimer = metricRegistry.timer("healthCheckExecutionTimer");
        }
    }

    public void setStartOnInit(boolean startOnInit) {
        this.startOnInit = startOnInit;
    }

    /**
     * Start {@link AsyncHealthCheckRegistry}.
     */
    public synchronized void start() {
        if (!running && healthCheckInterval > 0) {
            running = true;
            register(TIMESTAMP_NAME, new HealthCheck() {
                @Override
                protected HealthCheck.Result check() throws Exception {
                    return HealthCheck.Result.healthy(TIMESTAMP_DATE_FORMAT.format(new Date()));
                }
            });
            healthCheckBackgroundTask.run();
            healthSummaryPollingExecutor.scheduleAtFixedRate(healthCheckBackgroundTask, healthCheckInterval, healthCheckInterval, TimeUnit.SECONDS);
            LOGGER.info("First health check execution completed and scheduled execution triggered.");
        }
    }

    /**
     * Stop {@link AsyncHealthCheckRegistry}.
     */
    public synchronized void stop() {
        if (running) {
            running = false;
            healthSummaryPollingExecutor.shutdownNow();
            healthCheckExecutor.shutdownNow();
        }
    }

    @Override
    public void destroy() {
        stop();
    }

}
