package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;

    private final String dataId = "gateway-route.json";
    private final String group = "DEFAULT_GROUP";
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final Set<String> routeIds = new HashSet<>();
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        //项目启动先拉取一次配置，并配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        //监听到配置变更时，重新加载路由配置
                        updateRouteConfig(configInfo);
                    }
                });
        //第一次读取到配置，更新路由表
        updateRouteConfig(configInfo);
    }
    public void updateRouteConfig(String config){
        log.info("监听路由信息: {}",config);
        //解析路由配置
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(config, RouteDefinition.class);
        //如果routeIds不为空
        //删除旧的路由
        if(!routeIds.isEmpty()){
            for (String routeId : routeIds) {
                routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
            }
        }

        //更新路由
        for (RouteDefinition routeDefinition : routeDefinitions) {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        }

    }
}
