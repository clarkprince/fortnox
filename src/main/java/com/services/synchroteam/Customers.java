package com.services.synchroteam;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.utils.Utils;

public class Customers {

    private static final Logger log = LoggerFactory.getLogger(Customers.class);

    public static ArrayNode getCustomers(Tenant tenant) {
        try {
            log.info("Retrieving customers from sychroteam");

            int totalPages = 1;
            int page = 1;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd+HH:mm:ss");
            String d = simpleDateFormat.format(new Date());
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode data = mapper.createArrayNode();

            do {
                String uri = "https://ws.synchroteam.com/api/v3/customer/list?pageSize=100&changedFrom=" + d + "&page=" + page;
                String body = SynchroRequests.doGet(tenant, uri);
                if (body != null) {
                    JsonNode root = mapper.readTree(body);
                    data.addAll((ArrayNode) root.get("data"));
                    totalPages = (int) Math.ceil(root.get("recordsTotal").asInt() / 100.0);
                }
                page++;
            } while (page <= totalPages);

            return data;
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static void insertCustomer(String myId, String name, String address2, String address3, String address4, String postcode, Tenant tenant) {

        String address = address2 + (!Utils.isEmpty(address3) ? ", " : "") + address3 + (!Utils.isEmpty(address4) ? ", " : "") + address4
                + (!Utils.isEmpty(postcode) ? ", " : "") + postcode;

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        map.put("name", name);
        map.put("address", address);

        try {
            String json = new ObjectMapper().writeValueAsString(map);

            String uri = "https://ws.synchroteam.com/api/v3/customer/send";
            String response = SynchroRequests.doPost(tenant, uri, json);

            if (response != null) {
                log.info("Customer " + myId + "-" + name + " successfully added.");
            }
        } catch (Exception e) {
            log.error("Failed to insert Customer. Skipping. " + e);
            // e.printStackTrace();
        }
    }

    public static boolean customerExists(String id, Tenant tenant) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/customer/details?myId=" + id;
            String response = SynchroRequests.doGet(tenant, uri);
            if (response != null) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }

    }
}
