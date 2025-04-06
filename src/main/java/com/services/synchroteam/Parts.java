package com.services.synchroteam;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dto.PartDTO;
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
        return getParts(tenant, 100, 1);
    }

    public static String getParts(Tenant tenant, int pageSize, int page) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/part/list" + "?pageSize=" + pageSize + "&page=" + page;
            String response = SynchroRequests.doGet(tenant, uri);

            if (response != null) {
                log.info("Parts list successfully retrieved for page {} with size {}", page, pageSize);
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve parts list: " + e);
        }
        return null;
    }

    public static String getPart(Tenant tenant, String idOrReference, boolean isReference) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/part/details?" + (isReference ? "reference=" : "id=") + idOrReference;
            String response = SynchroRequests.doGet(tenant, uri);

            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve part details for {} {}: {}", isReference ? "reference" : "id", idOrReference, e);
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

    public static Activity updateParts(List<PartDTO> parts, Tenant tenant, Activity activity) {
        try {
            StringBuilder results = new StringBuilder();
            boolean allSuccess = true;
            ObjectMapper mapper = new ObjectMapper();

            for (PartDTO part : parts) {
                try {
                    Map<String, Object> partMap = new HashMap<>();

                    if (part.getName() == null && part.getReference() != null) {
                        String existingPartJson = getPart(tenant, part.getReference(), true);
                        if (existingPartJson != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> existingPart = mapper.readValue(existingPartJson, Map.class);
                            if (existingPart.containsKey("name")) {
                                part.setName((String) existingPart.get("name"));
                            }
                        }
                    }

                    if (part.getReference() != null)
                        partMap.put("reference", part.getReference());
                    if (part.getName() != null)
                        partMap.put("name", part.getName());
                    if (part.getDescription() != null)
                        partMap.put("description", part.getDescription());
                    if (part.getPrice() != null)
                        partMap.put("price", part.getPrice());
                    if (part.getMinQuantity() != null)
                        partMap.put("minQuantity", part.getMinQuantity());
                    if (part.getIsTracked() != null)
                        partMap.put("isTracked", part.getIsTracked());
                    if (part.getIsSerializable() != null)
                        partMap.put("isSerializable", part.getIsSerializable());
                    if (part.getStatus() != null)
                        partMap.put("status", part.getStatus());
                    if (part.getType() != null)
                        partMap.put("type", part.getType());

                    // Handle nested objects
                    if (part.getCategory() != null) {
                        Map<String, Object> category = new HashMap<>();
                        if (part.getCategory().getId() != null) {
                            category.put("id", part.getCategory().getId());
                        }
                        if (part.getCategory().getName() != null) {
                            category.put("name", part.getCategory().getName());
                        }
                        if (!category.isEmpty()) {
                            partMap.put("category", category);
                        }
                    }

                    if (part.getTax() != null && part.getTax().getId() != null) {
                        Map<String, Object> tax = new HashMap<>();
                        tax.put("id", part.getTax().getId());
                        partMap.put("tax", tax);
                    }

                    String json = mapper.writeValueAsString(partMap);
                    String uri = "https://ws.synchroteam.com/api/v3/part/send";
                    String response = SynchroRequests.doPost(tenant, uri, json);

                    if (response != null) {
                        results.append("Part ").append(part.getReference()).append(" successfully processed.\n");
                    } else {
                        allSuccess = false;
                        results.append("Failed to process part ").append(part.getReference()).append(".\n");
                    }
                } catch (Exception e) {
                    allSuccess = false;
                    results.append("Error processing part ").append(part.getReference()).append(": ").append(e.getMessage()).append("\n");
                }
            }

            activity.setSuccessful(allSuccess);
            activity.setActivity2(results.toString());
        } catch (Exception e) {
            log.error("Failed to update parts: " + e);
            activity.setSuccessful(false);
            activity.setActivity2("Failed to process parts: " + e.getMessage());
        }
        return activity;
    }
}