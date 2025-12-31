package org.oyyj.mycommonbase.common.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ase")
@Data
public class AESProperties {
    private String key;
    private String ivPrefix;
    private Integer ivPrefixLength;
}
