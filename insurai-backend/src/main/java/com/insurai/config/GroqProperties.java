package com.insurai.config;

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "groq.api")
public class GroqProperties {
    private String key;
    private String model = "llama-3.3-70b-versatile";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
