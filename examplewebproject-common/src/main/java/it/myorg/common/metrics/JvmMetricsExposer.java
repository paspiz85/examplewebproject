package it.myorg.common.metrics;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

/**
 * This bean reports basic JVM parameters.
 *
 * @author paspiz85
 */
public class JvmMetricsExposer implements InitializingBean {

    /**
     * Metrics to track JVM uptime.
     */
    private static class JvmUptimeGaugeSet implements MetricSet {

        JvmUptimeGaugeSet() {
        }

        public Map<String, Metric> getMetrics() {
            HashMap<String, Metric> gauges = new HashMap<>();
            gauges.put("jvm.uptime", (Gauge) () -> ManagementFactory.getRuntimeMXBean().getUptime());
            return Collections.unmodifiableMap(gauges);
        }
    }

    private MetricRegistry metricRegistry;
    private boolean jvmMetricsEnabled;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (jvmMetricsEnabled) {
            metricRegistry.registerAll(new MemoryUsageGaugeSet());
            metricRegistry.registerAll(new ThreadStatesGaugeSet());
            metricRegistry.registerAll(new GarbageCollectorMetricSet());
            metricRegistry.registerAll(new JvmUptimeGaugeSet());
            metricRegistry.registerAll(new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
            metricRegistry.registerAll(new ClassLoadingGaugeSet());
            metricRegistry.register("jvm.fileDescriptorRatio", new FileDescriptorRatioGauge());
        }
    }

    public void setJvmMetricsEnabled(boolean jvmMetricsEnabled) {
        this.jvmMetricsEnabled = jvmMetricsEnabled;
    }

    public void setMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

}
