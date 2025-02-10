package com.services.synchroteam;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entities.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utils.Utils;

public class Equipment {
    private final static Logger log = LoggerFactory.getLogger(Equipment.class);

    public static boolean equipmentExists(String id, String name, Tenant tenant) {
        try {
            String uri = "https://ws.synchroteam.com/api/v3/equipment/details?myId=" + id + "&name=" + name;
            String response = SynchroRequests.doGet(tenant, uri);

            if (response != null) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void insertEquipement(String myId, String custMyId, String name, Tenant tenant) {

        Map<String, Object> map = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("myId", custMyId);

        if (Utils.isEmpty(name)) {
            name = "Need to be updated - " + myId;
        }

        map.put("name", name);
        map.put("myId", myId);
        map.put("customer", customer);

        try {
            String json = new ObjectMapper().writeValueAsString(map);

            String uri = "https://ws.synchroteam.com/api/v3/equipment/send";
            String response = SynchroRequests.doPost(tenant, uri, json);

            if (response != null) {
                log.info("Equipment " + name + " successfully added.");
            }
        } catch (Exception e) {
            log.error("Failed to insert Equipement. Skipping. " + e);
            // e.printStackTrace();
        }
    }

}
