package com.services.fortnox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.services.synchroteam.Customers;

@Service
public class FnCustomers {
    private final Logger log = LoggerFactory.getLogger(FnCustomers.class);
    private int totalPages = 1;

    public void insertToFortnox(JsonNode data, Tenant tenant) throws Exception {
        log.info("inserting customer to fortnox");
        try {
            String uri = "/3/customers";
            String vatNumber = data.get("vatNumber").asText();
            if (vatNumber == null || vatNumber.equalsIgnoreCase("null") || vatNumber.equalsIgnoreCase("")) {
                log.error("No vat number for customer: " + data.get("name").asText());
                return;
            }

            Map<String, Object> customerParent = new HashMap<>();
            Map<String, Object> customer = new HashMap<>();
            if (data.get("myId") == null || data.get("myId").asText() == "") {
                customer.put("CustomerNumber", data.get("id").asText());
            } else {
                customer.put("CustomerNumber", data.get("myId").asText());
            }

            customer.put("Name", data.get("name").asText());
            customer.put("Address1", data.get("addressStreet").asText());
            customer.put("Address2", data.get("addressProvince").asText());
            customer.put("City", data.get("addressCity").asText());
            customer.put("ZipCode", data.get("addressZIP").asText());

            customerParent.put("Customer", customer);

            ObjectMapper mapper = new ObjectMapper();
            String newjson = mapper.writeValueAsString(customerParent);

            try {
                String response = FortnoxRequests.doPost(tenant, uri, newjson);
                if (response != null) {
                    log.info("Successfully created a customer on Fortnox");
                }
            } catch (Exception e) {
                String response = FortnoxRequests.doPut(tenant, uri + "/" + customer.get("CustomerNumber").toString(), newjson);
                if (response != null) {
                    log.info("Successfully updated a customer on Fortnox");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public ProcessMonitor getCustomers(Tenant tenant, String fromTime, int size, ProcessMonitor processMonitor) throws IOException {
        int offset = 0;
        try {
            for (int i = 0; i < totalPages; i++) {
                offset = i * size;
                JsonNode customerLs = doGetCustomers(tenant, fromTime, size, offset);
                doCustomerInsert(customerLs, tenant);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        totalPages = 1;
        offset = 0;
        return processMonitor;
    }

    public JsonNode doGetCustomers(Tenant tenant, String fromTime, int size, int offset) {
        try {
            String response = requestCustomers(tenant, fromTime, size, offset);
            if (response != null) {
                ObjectMapper responseMapper = new ObjectMapper();
                JsonNode resposeTree = responseMapper.readTree(response);
                JsonNode meta = resposeTree.get("MetaInformation");
                totalPages = meta.get("@TotalPages").asInt();
                return resposeTree.get("Customers");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public String requestCustomers(Tenant tenant, String fromTime, int size, int offset) {
        String uri = "/3/customers?limit=" + size + "&lastmodified=" + fromTime + "&offset=" + offset;
        return FortnoxRequests.doGet(tenant, uri);
    }

    public void doCustomerInsert(JsonNode customerLs, Tenant tenant) {
        for (JsonNode customer : customerLs) {
            Customers.insertCustomer(customer.get("CustomerNumber").asText(), customer.get("Name").asText(), customer.get("Address1").asText(),
                    customer.get("Address1").asText(), customer.get("Address2").asText() + " " + customer.get("City").asText(),
                    customer.get("ZipCode").asText(), tenant);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void doSynchroteamCustomersToFortnox(String token, Tenant tenant) {
        try {
            ArrayNode customers = Customers.getCustomers(tenant);
            for (JsonNode custNode : customers) {
                insertToFortnox(custNode, tenant);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public JsonNode doGetSingleCustomer(Tenant tenant, String customerNumber) {
        try {
            String uri = "/3/customers/" + customerNumber;
            String response = FortnoxRequests.doGet(tenant, uri);
            if (response != null) {
                ObjectMapper responseMapper = new ObjectMapper();
                JsonNode resposeTree = responseMapper.readTree(response);
                return resposeTree.get("Customer");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
