package com.services.fortnox;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.Activity;
import com.entities.Part;
import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.PartRepository;
import com.services.synchroteam.Depots;
import com.services.synchroteam.Parts;

@Service
public class Articles {
    private final Logger log = LoggerFactory.getLogger(Articles.class);
    private int offset = 0;
    private int totalPages = 1;

    @Autowired
    private PartRepository partRepository;

    public ProcessMonitor getParts(Tenant tenant, String fromTime, int size, ProcessMonitor processMonitor) throws IOException {
        int i = 0;
        while (i < totalPages) {
            offset = i * size;
            try {
                JsonNode partLs = doGetArticles(tenant, fromTime, size);
                for (JsonNode part : partLs) {
                    try {
                        String articleNo = URLEncoder.encode(part.get("ArticleNumber").asText(), "UTF-8");
                        JsonNode partDetails = doGetPartDetails(articleNo, tenant);
                        Activity activity = new Activity();
                        activity.setActivity1(partDetails.toPrettyString());

                        activity = doPartsInsert(partDetails, tenant, activity);
                        processMonitor.getActivities().add(activity);

                        JsonNode stockDepots = Warehouses.doStockLevel(tenant, articleNo);
                        for (JsonNode depot : stockDepots) {
                            if (depot.get("stockPointCode") != null && partDetails.get("StockGoods").asBoolean(false)) {
                                Depots.insertToDepot(depot.get("stockPointCode").asText(), part.get("ArticleNumber").asText(),
                                        depot.get("availableStock").asInt(0), tenant);
                            }
                        }
                        savePartToDatabase(partDetails, tenant.getSynchroteamDomain());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            } catch (Exception e) {
                processMonitor.setSuccessful(false);
                log.error(e.getMessage());
            }
            i++;
        }
        totalPages = 1;
        offset = 0;
        processMonitor.setSuccessful(true);
        return processMonitor;
    }

    public void savePartToDatabase(JsonNode partNode, String domain) {
        Part part = new Part();
        part.setArticleNumber(partNode.get("ArticleNumber").asText());
        part.setTenantDomain(domain);
        part.setSource("fortnox");

        Optional<Part> existingPart = partRepository.findByArticleNumberAndTenantDomain(part.getArticleNumber(), domain);
        if (existingPart.isPresent()) {
            part.setId(existingPart.get().getId());
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            part.setJsonData(mapper.writeValueAsString(partNode));
            partRepository.save(part);
        } catch (JsonProcessingException e) {
            log.error("Error saving part: " + e.getMessage());
        }
    }

    public JsonNode doGetPartDetails(String id, Tenant tenant) {
        try {
            String uri = "/3/articles/" + id;
            String response = FortnoxRequests.doGet(tenant, uri);
            if (response != null) {
                ObjectMapper responseMapper = new ObjectMapper();
                JsonNode resposeTree = responseMapper.readTree(response);
                return resposeTree.get("Article");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Activity doPartsInsert(JsonNode part, Tenant tenant, Activity activity) {
        activity = Parts.insertPart(part.get("ArticleNumber").asText(), part.get("Description").asText(), part.get("SalesPrice").asText(),
                part.get("VAT").asText(), part.get("ManufacturerArticleNumber") != null ? part.get("ManufacturerArticleNumber").asText() : "",
                part.get("Type").asText(), part.get("StockGoods").asBoolean(true), part.get("Active").asBoolean(true), tenant, activity);
        return activity;
    }

    public JsonNode doGetArticles(Tenant tenant, String fromTime, int size) throws JsonMappingException, JsonProcessingException {
        String response = requestArticles(tenant, fromTime, size, offset);
        if (response != null) {
            ObjectMapper responseMapper = new ObjectMapper();
            JsonNode resposeTree = responseMapper.readTree(response);
            JsonNode meta = resposeTree.get("MetaInformation");
            totalPages = meta.get("@TotalPages").asInt();
            return resposeTree.get("Articles");
        }
        return null;
    }

    public String requestArticles(Tenant tenant, String fromTime, int size, int offset) {
        String uri = "/3/articles?limit=" + size + "&lastmodified=" + fromTime + "&offset=" + offset;
        return FortnoxRequests.doGet(tenant, uri);
    }

    public Activity reprocessArticle(String articleNumber, Tenant tenant, Activity activity) {
        try {
            JsonNode article = doGetPartDetails(articleNumber, tenant);
            if (article != null) {
                activity.setActivity1(article.toPrettyString());
                activity = doPartsInsert(article, tenant, activity);

                // Update stock levels if necessary
                if (article.get("StockGoods").asBoolean(false)) {
                    JsonNode stockDepots = Warehouses.doStockLevel(tenant, articleNumber);
                    for (JsonNode depot : stockDepots) {
                        if (depot.get("stockPointCode") != null) {
                            Depots.insertToDepot(depot.get("stockPointCode").asText(), articleNumber, depot.get("availableStock").asInt(0), tenant);
                        }
                    }
                }

                // Save to database
                savePartToDatabase(article, tenant.getSynchroteamDomain());
            }
        } catch (Exception e) {
            activity.setSuccessful(false);
            activity.setMessage("Failed to reprocess article: " + e.getMessage());
            log.error("Failed to reprocess article {}: {}", articleNumber, e.getMessage());
        }
        return activity;
    }
}
