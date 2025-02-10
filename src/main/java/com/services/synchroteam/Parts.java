package com.services.synchroteam;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entities.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Parts {

    private final static Logger log = LoggerFactory.getLogger(Parts.class);

    public static void insertPart(String reference, String name, String price, String vat, String note, String type, boolean tracked, boolean active,
            Tenant tenant) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> category = new HashMap<>();
        Map<String, Object> tax = new HashMap<>();
        category.put("name", "MainParts");
        tax.put("id", "1");
        tax.put("name", "VAT");
        tax.put("tax", vat);
        map.put("name", name);
        map.put("reference", reference);
        map.put("price", price);
        map.put("isTracked", tracked);
        map.put("description", note);
        map.put("category", category);
        map.put("status", active ? "active" : "inactive");
        String aType = "part";
        if (type.equalsIgnoreCase("SERVICE")) {
            aType = "service";
        }
        map.put("type", aType);

        try {
            String json = new ObjectMapper().writeValueAsString(map);

            String uri = "https://ws.synchroteam.com/api/v3/part/send";
            String response = SynchroRequests.doPost(tenant, uri, json);

            if (response != null) {
                log.info("Part " + name + " successfully added/updated.");
            }
        } catch (Exception e) {
            log.error("Failed to insert Part. Skipping." + e);
            // e.printStackTrace();
        }
    }

}