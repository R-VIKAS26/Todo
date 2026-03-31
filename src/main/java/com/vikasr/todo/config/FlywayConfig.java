package com.vikasr.todo.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource,
                         @Value("${spring.flyway.locations:classpath:db/migration}") String locations,
                         @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(parseLocations(locations))
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerDependsOnFlywayPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                addDependsOn(beanFactory, "entityManagerFactory", "flyway");
                addDependsOn(beanFactory, "transactionManager", "flyway");
            }

            private void addDependsOn(ConfigurableListableBeanFactory beanFactory, String beanName, String dependsOn) {
                if (!beanFactory.containsBeanDefinition(beanName)) {
                    return;
                }
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                Set<String> dependencies = new LinkedHashSet<>();
                if (beanDefinition.getDependsOn() != null) {
                    dependencies.addAll(Arrays.asList(beanDefinition.getDependsOn()));
                }
                dependencies.add(dependsOn);
                beanDefinition.setDependsOn(dependencies.toArray(String[]::new));
            }
        };
    }

    private String[] parseLocations(String configuredLocations) {
        return Arrays.stream(configuredLocations.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(location -> !location.isEmpty())
                .toArray(String[]::new);
    }
}
