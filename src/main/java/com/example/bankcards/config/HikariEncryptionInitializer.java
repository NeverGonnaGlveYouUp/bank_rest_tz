package com.example.bankcards.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class HikariEncryptionInitializer implements BeanPostProcessor {

    @Value("${db.encryption.key}")
    private String encryptionKey;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HikariDataSource hikariDataSource) {
            String initSql = String.format("SELECT set_config('app.encryption_key', '%s', false);", encryptionKey);
            hikariDataSource.setConnectionInitSql(initSql);
        }
        return bean;
    }
}
