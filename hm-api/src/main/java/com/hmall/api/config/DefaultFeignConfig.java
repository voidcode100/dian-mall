package com.hmall.api.config;

import com.hmall.api.fallback.ItemClientFallBackFactory;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
    @Bean
    public RequestInterceptor userInfoRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long userInfo = UserContext.getUser();
                if(userInfo != null){
                    requestTemplate.header("user-info",userInfo.toString());
                }

                String xid = RootContext.getXID();
                requestTemplate.header(RootContext.KEY_XID,xid);

            }
        };
    }

    @Bean
    public ItemClientFallBackFactory itemClientFallBackFactory(){
        return new ItemClientFallBackFactory();
    }

}
