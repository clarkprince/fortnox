package com.services.fortnox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.Activity;
import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.repository.SettingsRepository;
import com.services.synchroteam.Customers;

@Service
public class FnCustomers {
    private final Logger log = LoggerFactory.getLogger(FnCustomers.class);
    private int totalPages = 1;

    @Autowired
    private SettingsRepository settingsRepository;

    private boolean shouldCheckCustomerDeliveryAddress(String tenant) {
        return settingsRepository.findBySettingAndTenant("checkCustomerDeliveryAddress", tenant)
                .map(setting -> "true".equalsIgnoreCase(setting.getValue())).orElse(false);
    }

    private String safeGetText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return "";
        }
        String value = field.asText();
        return "null".equalsIgnoreCase(value) ? "" : value;
    }

    public void insertToFortnox(JsonNode data, Tenant tenant) throws Exception {
        log.info("inserting customer to fortnox");
        try {
            String uri = "/3/customers";
            String vatNumber = safeGetText(data, "vatNumber");
            if (vatNumber == null || vatNumber.isEmpty()) {
                log.error("No vat number for customer: " + safeGetText(data, "name"));
                return;
            }

            Map<String, Object> customerParent = new HashMap<>();
            Map<String, Object> customer = new HashMap<>();
            String myId = safeGetText(data, "myId");
            if (myId.isEmpty()) {
                customer.put("CustomerNumber", safeGetText(data, "id"));
            } else {
                customer.put("CustomerNumber", myId);
            }

            customer.put("Name", safeGetText(data, "name"));
            customer.put("Address1", safeGetText(data, "addressStreet"));
            customer.put("Address2", safeGetText(data, "addressProvince"));
            customer.put("City", safeGetText(data, "addressCity"));
            customer.put("ZipCode", safeGetText(data, "addressZIP"));

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

            // Check if we should validate delivery address
            if (shouldCheckCustomerDeliveryAddress(tenant.getSynchroteamDomain())) {
                String deliveryAddress1 = safeGetText(customer, "DeliveryAddress1");
                if (deliveryAddress1.trim().isEmpty()) {
                    activity.setSuccessful(false);
                    activity.setMessage("Skipped because there is no delivery address for the customer");
                    log.info("Skipped customer {} - no delivery address", safeGetText(customer, "CustomerNumber"));
                    processMonitor.getActivities().add(activity);
                    continue;
                }
            }

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
        String customerNumber = safeGetText(customer, "CustomerNumber");
        String name = safeGetText(customer, "Name");

        // Determine which address to use based on setting
        String address1, address2, city, zipCode;

        if (shouldCheckCustomerDeliveryAddress(tenant.getSynchroteamDomain())) {
            // Use delivery address fields
            address1 = safeGetText(customer, "DeliveryAddress1");
            address2 = safeGetText(customer, "DeliveryAddress2");
            city = safeGetText(customer, "DeliveryCity");
            zipCode = safeGetText(customer, "DeliveryZipCode");
        } else {
            // Use regular address fields (existing behavior)
            address1 = safeGetText(customer, "Address1");
            address2 = safeGetText(customer, "Address2");
            city = safeGetText(customer, "City");
            zipCode = safeGetText(customer, "ZipCode");
        }

        String formattedCityAddress = (address2.trim().isEmpty() ? "" : address2 + " ") + city;

        return Customers.insertCustomer(customerNumber, name, address1, address2, formattedCityAddress, zipCode, tenant, activity);
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

                // Check if we should validate delivery address
                if (shouldCheckCustomerDeliveryAddress(tenant.getSynchroteamDomain())) {
                    String deliveryAddress1 = safeGetText(customer, "DeliveryAddress1");
                    if (deliveryAddress1.trim().isEmpty()) {
                        activity.setSuccessful(false);
                        activity.setMessage("Skipped because there is no delivery address for the customer");
                        log.info("Skipped customer {} - no delivery address", customerNumber);
                        return activity;
                    }
                }

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
