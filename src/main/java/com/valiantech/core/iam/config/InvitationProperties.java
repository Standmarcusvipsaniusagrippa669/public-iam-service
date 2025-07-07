package com.valiantech.core.iam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "valiantech.invitation")
public class InvitationProperties {
    private String registrationUrlBase;
    private Integer tokenExpiryDays = 7;
    private Boolean enableInvite = false;
}
