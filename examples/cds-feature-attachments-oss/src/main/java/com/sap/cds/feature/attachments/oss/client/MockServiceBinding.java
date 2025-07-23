package com.sap.cds.feature.attachments.oss.client;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sap.cloud.environment.servicebinding.api.ServiceBinding;

public class MockServiceBinding implements ServiceBinding {

    private final Map<String, Object> credentials = Map.of(
            "clientid", "stub-client-id",
            "clientsecret", "stub-secret",
            "url", "https://example.authentication.sap.hana.ondemand.com",
            "xsappname", "my-xsapp"
    );

    private final Map<String, Object> metadata = Map.of(
            "meta1", "value1",
            "meta2", 1234
    );

    @Override
    public Optional<String> getName() {
        return Optional.of("stub-xsuaa");
    }

    @Override
    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();
        return keys;
    }

    @Override
    public Optional<String> getServiceName() {
        return Optional.of("xsuaa");
    }

    @Override
    public Optional<String> getServicePlan() {
        return Optional.of("application");
    }

    @Override
    public List<String> getTags() {
        return List.of("authentication", "uaa");
    }

    @Override
    public Map<String, Object> getCredentials() {
        return credentials;
    }

    @Override
    public Optional<Object> get(String key) {
        if (credentials.containsKey(key)) {
            return Optional.ofNullable(credentials.get(key));
        } else if (metadata.containsKey(key)) {
            return Optional.ofNullable(metadata.get(key));
        }
        return Optional.empty();
    }

    @Override
    public boolean containsKey(String key) {
        return credentials.containsKey(key) || metadata.containsKey(key);
    }

}