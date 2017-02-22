package it.myorg.common.metrics;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * A post processor to detect and register {@link HealthCheck} beans into a
 * {@link HealthCheckRegistry}.
 *
 * @author paspiz85
 */
public class HealthCheckPostProcessor implements BeanPostProcessor {

    private final HealthCheckRegistry healthCheckRegistry;

    /**
     * Constructor.
     *
     * @param healthCheckRegistry healthCheckRegistry
     */
    public HealthCheckPostProcessor(HealthCheckRegistry healthCheckRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
    }

    @Override
    public final Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public final Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HealthCheck) {
            HealthCheck healthCheck = (HealthCheck) bean;
            healthCheckRegistry.register(beanName, healthCheck);
        }
        return bean;
    }

}
