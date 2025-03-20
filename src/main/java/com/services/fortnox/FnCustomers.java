package com.services.fortnox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.entities.Activity;
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
                doCustomerInsert(customerLs, tenant, processMonitor);
            }
        } catch (Exception e) {
            processMonitor.setSuccessful(false);
            log.error(e.getMessage());
            e.printStackTrace();
        }
        totalPages = 1;
        offset = 0;
        processMonitor.setSuccessful(true);
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

    public ProcessMonitor doCustomerInsert(JsonNode customerLs, Tenant tenant, ProcessMonitor processMonitor) {
        for (JsonNode customer : customerLs) {
            Activity activity = new Activity();
            activity.setActivity1(customer.toPrettyString());
            activity = createSynchroteamCustomer(customer, tenant, activity);
            processMonitor.getActivities().add(activity);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return processMonitor;
    }

    private Activity createSynchroteamCustomer(JsonNode customer, Tenant tenant, Activity activity) {
        String customerNumber = customer.get("CustomerNumber").asText();
        String name = customer.get("Name").asText();
        String address1 = customer.get("Address1").asText();
        String address2 = customer.get("Address2").asText();
        String city = customer.get("City").asText();
        String zipCode = customer.get("ZipCode").asText();

        String formattedAddress2 = address2 + " " + city;

        return Customers.insertCustomer(customerNumber, name, address1, address1, formattedAddress2, zipCode, tenant, activity);
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

    public Activity reprocessCustomer(String customerNumber, Tenant tenant, Activity activity) {
        try {
            JsonNode customer = doGetSingleCustomer(tenant, customerNumber);
            if (customer != null) {
                activity.setActivity1(customer.toPrettyString());
                activity = createSynchroteamCustomer(customer, tenant, activity);
            }
        } catch (Exception e) {
            activity.setSuccessful(false);
            activity.setMessage("Failed to reprocess customer: " + e.getMessage());
            log.error("Failed to reprocess customer {}: {}", customerNumber, e.getMessage());
        }
        return activity;
    }
}
