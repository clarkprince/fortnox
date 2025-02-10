package com.services.synchroteam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entities.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Depots {
    private final static Logger log = LoggerFactory.getLogger(Depots.class);

    public static void insertToDepot(String depotName, String productCode, Integer quantity, Tenant tenant) {
        List<Map<String, Object>> ls = new ArrayList<>();
        List<Map<String, Object>> partsLs = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();

        parts.put("productCode", productCode);
        parts.put("quantity", quantity);
        partsLs.add(parts);

        Map<String, Object> depot = new HashMap<>();
        depot.put("name", depotName);

        map.put("depot", depot);
        map.put("parts", partsLs);
        ls.add(map);

        try {
            String json = new ObjectMapper().writeValueAsString(ls);

            String uri = "https://ws.synchroteam.com/api/v3/depot/quantities";
            String response = SynchroRequests.doPost(tenant, uri, json);

            if (response != null) {
                log.info("Part " + productCode + " successfully added/updated to depot.");
            }
        } catch (Exception e) {
            log.error("Failed to insert Part. Skipping." + e);
            // e.printStackTrace();
        }
    }
}
