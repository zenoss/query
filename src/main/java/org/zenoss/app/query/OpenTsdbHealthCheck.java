package org.zenoss.app.query;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.dropwizardspring.annotations.HealthCheck;

@Configuration
@HealthCheck
public class OpenTsdbHealthCheck extends com.yammer.metrics.core.HealthCheck {
    @Autowired
    QueryAppConfiguration config;

    protected OpenTsdbHealthCheck() {
        super("OpenTSDB");
    }

    @Override
    protected Result check() throws Exception {
        try {
            URL url = new URL(config.getPerformanceMetricQueryConfig()
                    .getOpenTsdbUrl() + "/version?json");
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection
                    .setConnectTimeout(config.getPerformanceMetricQueryConfig()
                            .getConnectionTimeoutMs());
            connection.setReadTimeout(config.getPerformanceMetricQueryConfig()
                    .getConnectionTimeoutMs());
            if (Math.floor(connection.getResponseCode() / 100) != 2) {
                return Result
                        .unhealthy("Unexpected result code from OpenTSDB Server: "
                                + connection.getResponseCode());
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(connection.getInputStream(), baos);
            new JSONObject(baos.toString());
            return Result.healthy();
        } catch (Throwable t) {
            return Result.unhealthy(t);
        }
    }
}
