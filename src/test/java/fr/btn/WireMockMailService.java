package fr.btn;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class WireMockMailService implements QuarkusTestResourceLifecycleManager {
    private WireMockServer wireMockServer;

    private static final String API_KEY = ConfigProvider.getConfig().getValue("app-config.api-key", String.class);
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor(8089);


        stubFor(
                post(urlEqualTo("/mail"))
                        .withHeader("x-api-key", equalTo(API_KEY))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                        )
        );

        return Collections.singletonMap("%test.quarkus.rest-client.mail-service.url", wireMockServer.baseUrl());
    }

    @Override
    public void stop() {
        if(wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}
