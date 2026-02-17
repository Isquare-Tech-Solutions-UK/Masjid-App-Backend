package com.masjidapp.dto.container;

import com.masjidapp.entity.AdminUser;
import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component("authRequestContainer")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "request")
@Data
public class AuthRequestContainer {

   private AdminUser adminUser;

}
