package com.schemaguard.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "schema-guard")
public class SchemaGuardProperties {

    /**
     * Enable or disable the schema guard interceptor globally.
     */
    private boolean enabled = true;

    /**
     * The URL of the Micro-SaaS Schema Registry API.
     */
    private String registryUrl = "https://api.schemaguard.com/schemas";

    /**
     * Cache Time-To-Live in minutes.
     */
    private long cacheTtlMinutes = 60;
    
    /**
     * Maximum number of schemas to hold in the local cache.
     */
    private long cacheMaxSize = 1000;

    /**
     * Map of Kafka topic names to their corresponding Schema IDs.
     */
    private java.util.Map<String, String> topics = new java.util.HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public long getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public void setCacheTtlMinutes(long cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public java.util.Map<String, String> getTopics() {
        return topics;
    }

    public void setTopics(java.util.Map<String, String> topics) {
        this.topics = topics;
    }
}
