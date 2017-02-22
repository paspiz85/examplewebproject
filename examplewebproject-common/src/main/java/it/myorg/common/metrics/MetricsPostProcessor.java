package it.myorg.common.metrics;

import static java.text.MessageFormat.format;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

/**
 * A post processor to detect and register {@link Metric} beans into a
 * {@link MetricRegistry}.
 *
 * @author paspiz85
 */
public class MetricsPostProcessor implements BeanPostProcessor {

    private final MetricRegistry metricRegistry;

    /**
     * Constructor.
     *
     * @param metricRegistry metricRegistry
     */
    public MetricsPostProcessor(final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public final Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public final Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Metric && !(bean instanceof MetricRegistry)) {
            Metric metric = (Metric) bean;
            try {
                metricRegistry.register(beanName, metric);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(format("A metric named ''{0}'' has been registered already!", beanName), e);
            }
        }
        return bean;
    }

}
