package com.teamwiki.service;

import com.teamwiki.entity.SystemConfig;
import com.teamwiki.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    public String getConfig(String key) {
        return configRepository.findByConfigKey(key).map(SystemConfig::getConfigValue).orElse(null);
    }

    public String getConfig(String key, String defaultValue) {
        String val = getConfig(key);
        return val != null ? val : defaultValue;
    }

    @Transactional
    public void setConfig(String key, String value) {
        setConfig(key, value, null);
    }

    @Transactional
    public void setConfig(String key, String value, String description) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(SystemConfig.builder().configKey(key).build());
        config.setConfigValue(value);
        if (description != null) config.setDescription(description);
        configRepository.save(config);
    }
}
