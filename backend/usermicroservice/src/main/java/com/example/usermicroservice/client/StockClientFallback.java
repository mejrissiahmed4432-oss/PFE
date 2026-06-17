package com.example.usermicroservice.client;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class StockClientFallback implements StockClient {
    @Override
    public List<Map<String, Object>> getAvailableEquipment() {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getAvailableSoftware() {
        return Collections.emptyList();
    }
}
