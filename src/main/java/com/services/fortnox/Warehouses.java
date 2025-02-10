package com.services.fortnox;

import org.springframework.stereotype.Service;

import com.entities.Tenant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Warehouses {

    public static JsonNode doGetDepot(Tenant tenant, String stockPointCode) throws JsonMappingException, JsonProcessingException {
        String uri = "/api/warehouse/stockpoints-v1/" + stockPointCode;
        String response = FortnoxRequests.doGet(tenant, uri);
        if (response != null) {
            ObjectMapper responseMapper = new ObjectMapper();
            return responseMapper.readTree(response);
        }
        return null;
    }

    public static JsonNode doStockLevel(Tenant tenant, String id) throws JsonMappingException, JsonProcessingException {
        String uri = "/api/warehouse/status-v1/stockbalance?itemIds=" + id;
        String response = FortnoxRequests.doGet(tenant, uri);
        if (response != null) {
            ObjectMapper responseMapper = new ObjectMapper();
            return responseMapper.readTree(response);
        }
        return null;
    }

}