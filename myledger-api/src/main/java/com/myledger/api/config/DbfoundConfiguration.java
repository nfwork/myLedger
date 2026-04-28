package com.myledger.api.config;

import com.nfwork.dbfound.core.DBFoundConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DbfoundConfiguration {

    @PostConstruct
    public void init(){
        //添加的参数，日志不会打印相应的内容
        DBFoundConfig.getSensitiveParamSet().add("old_password");
        DBFoundConfig.getSensitiveParamSet().add("new_password");
    }
}
