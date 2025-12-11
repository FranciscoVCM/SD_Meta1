package com.googol.webserver;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpAndHttpsConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server -> server.addAdditionalTomcatConnectors(httpConnector());
    }

    private Connector httpConnector() {
        Connector connector =
                new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);       // Porta HTTP
        connector.setSecure(false);
        return connector;
    }
}
