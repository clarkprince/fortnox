package com.services.synchroteam;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dto.PartPriceUpdateDTO;
import com.entities.Activity;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utils.Utils;

public class Parts {

    private final static Logger log = LoggerFactory.getLogger(Parts.class);

    public static Activity insertPart(String reference, String name, String price, String vat, String note, String type, boolean tracked,
            boolean active, Tenant tenant, Activity activity) {
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
                activity.setSuccessful(true);
                activity.setActivity2(Utils.prettyPrint(response));
            }
        } catch (Exception e) {
            log.error("Failed to insert Part. Skipping." + e);
            activity.setSuccessful(false);
        }
        return activity;
    }

    public static String getParts(Tenant tenant) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/part/list";
            String response = SynchroRequests.doGet(tenant, uri);

            if (response != null) {
                log.info("Parts list successfully retrieved.");
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve parts list: " + e);
        }
        return null;
    }

    public static String getPart(Tenant tenant, String id) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/part/details?id=" + id;
            String response = SynchroRequests.doGet(tenant, uri);

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve part details for ID " + id + ": " + e);
        }
        return null;
    }

    public static Activity updatePartPrices(List<PartPriceUpdateDTO> prices, Tenant tenant, Activity activity) {
        try {
            String json = new ObjectMapper().writeValueAsString(prices);
            String uri = "https://ws.synchroteam.com/api/v3/part/prices";
            String response = SynchroRequests.doPost(tenant, uri, json);

            if (response != null) {
                log.info("Part prices successfully updated.");
                activity.setSuccessful(true);
                activity.setActivity2(Utils.prettyPrint(response));
            }
        } catch (Exception e) {
            log.error("Failed to update part prices: " + e);
            activity.setSuccessful(false);
        }
        return activity;
    }
}