package com.bnpl.user.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectTimeout(3000)
                .setRetryAttempts(2);
        try {
            log.info("Configuring RedissonClient bean for Redis at {}:{}", redisHost, redisPort);
            return Redisson.create(config);
        } catch (Exception e) {
            log.warn("⚠️ Redis is not running on {}:{}. Distributed locking will be disabled.", redisHost, redisPort);
            return null;
        }
    }
}
