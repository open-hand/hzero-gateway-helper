package org.hzero.autoconfigure.gateway.helper;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import org.hzero.core.redis.RedisHelper;
import org.hzero.gateway.helper.api.AuthenticationHelper;
import org.hzero.gateway.helper.api.HelperFilter;
import org.hzero.gateway.helper.api.reactive.ReactiveAuthenticationHelper;
import org.hzero.gateway.helper.config.GatewayHelperProperties;
import org.hzero.gateway.helper.impl.DefaultAuthenticationHelper;
import org.hzero.gateway.helper.impl.HelperChain;
import org.hzero.gateway.helper.impl.reactive.DefaultReactiveAuthenticationHelper;
import org.hzero.gateway.helper.service.CustomPermissionCheckService;
import org.hzero.gateway.helper.service.SignatureService;
import org.hzero.gateway.helper.service.impl.DefaultCustomPermissionCheckService;
import org.hzero.gateway.helper.service.impl.DefaultSignatureService;

@ComponentScan(value = {
    "org.hzero.gateway.helper"
})
@EnableCaching
@Configuration
@EnableAsync
@Order(SecurityProperties.BASIC_AUTH_ORDER)
public class GatewayHelperAutoConfiguration {

    @Bean
    public HelperChain helperChain(Optional<List<HelperFilter>> optionalHelperFilters){
        return new HelperChain(optionalHelperFilters);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(AuthenticationHelper.class)
    public DefaultAuthenticationHelper authenticationHelper(HelperChain helperChain) {
        return new DefaultAuthenticationHelper(helperChain);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean(ReactiveAuthenticationHelper.class)
    public DefaultReactiveAuthenticationHelper reactiveAuthenticationHelper(HelperChain helperChain) {
        return new DefaultReactiveAuthenticationHelper(helperChain);
    }

    @Bean(name = "helperRestTemplate")
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = GatewayHelperProperties.PREFIX, value = "signature.enabled", havingValue = "true")
    public SignatureService signatureService(GatewayHelperProperties properties, RedisHelper redisHelper) {
        return new DefaultSignatureService(properties, redisHelper);
    }

    @Bean
    @Qualifier("permissionCheckSaveExecutor")
    public AsyncTaskExecutor commonAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ps-check-save-");
        executor.setMaxPoolSize(200);
        executor.setCorePoolSize(50);
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(CustomPermissionCheckService.class)
    public CustomPermissionCheckService customPermissionCheckService() {
        return new DefaultCustomPermissionCheckService();
    }

}
