package com.valiantech.core.iam.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import redis.clients.jedis.*;

import java.util.List;
import java.util.Set;

@Configuration
@Log4j2
public class JedisConfig {
    @Value("${redis.clusters.nodes}")
    private List<String> redisClustersNodes;
    @Bean
    @Scope("prototype")
    public UnifiedJedis jedisClient(
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port,
            @Value("${redis.isCluster}") boolean isCluster,
            @Value("${redis.password:}") String password) {

        DefaultJedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
                .password(password == null || password.isEmpty() ? null : password)
                .connectionTimeoutMillis(5000)
                .socketTimeoutMillis(5000)
                .blockingSocketTimeoutMillis(5000)
                .build();

        try {
            if (isCluster) {
                Set<HostAndPort> nodes = redisClustersNodes.stream()
                        .map(node -> {
                            String[] parts = node.split(":");
                            String nodeHost = parts[0].trim();
                            int nodePort = (parts.length > 1) ? Integer.parseInt(parts[1].trim()) : port;
                            return new HostAndPort(nodeHost, nodePort);
                        })
                        .collect(java.util.stream.Collectors.toSet());

                if (nodes.isEmpty()) {
                    throw new RuntimeException("Cluster nodes are empty");
                }

                return new JedisCluster(nodes, jedisClientConfig);

            } else {
                HostAndPort hostAndPort = new HostAndPort(host, port);
                return new JedisPooled(hostAndPort, jedisClientConfig);
            }
        } catch (Exception e) {
            log.error("Error creating Redis client", e);
            throw new RuntimeException("Error creating Redis client");
        }
    }

}