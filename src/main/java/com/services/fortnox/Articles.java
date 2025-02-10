package com.services.fortnox;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.Part;
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

    public void getParts(Tenant tenant, String fromTime, int size) throws IOException {
        int i = 0;
        while (i < totalPages) {
            offset = i * size;
            try {
                JsonNode partLs = doGetArticles(tenant, fromTime, size);
                for (JsonNode part : partLs) {
                    try {
                        String articleNo = URLEncoder.encode(part.get("ArticleNumber").asText(), "UTF-8");
                        JsonNode partDetails = doGetPartDetails(articleNo, tenant);

                        doPartsInsert(partDetails, tenant);

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
                log.error(e.getMessage());
            }
            i++;
        }
        totalPages = 1;
        offset = 0;
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

    public JsonNode doGetPartDetails(String id, Tenant tenant) throws JsonMappingException, JsonProcessingException {
        String uri = "/3/articles/" + id;
        String response = FortnoxRequests.doGet(tenant, uri);
        if (response != null) {
            ObjectMapper responseMapper = new ObjectMapper();
            JsonNode resposeTree = responseMapper.readTree(response);
            return resposeTree.get("Article");
        }
        return null;
    }

    public void doPartsInsert(JsonNode part, Tenant tenant) {
        Parts.insertPart(part.get("ArticleNumber").asText(), part.get("Description").asText(), part.get("SalesPrice").asText(),
                part.get("VAT").asText(), part.get("ManufacturerArticleNumber") != null ? part.get("ManufacturerArticleNumber").asText() : "",
                part.get("Type").asText(), part.get("StockGoods").asBoolean(true), part.get("Active").asBoolean(true), tenant);
    }

    public JsonNode doGetArticles(Tenant tenant, String fromTime, int size) throws JsonMappingException, JsonProcessingException {
        String uri = "/3/articles?limit=" + size + "&lastmodified=" + fromTime + "&offset=" + offset;
        String response = FortnoxRequests.doGet(tenant, uri);
        if (response != null) {
            ObjectMapper responseMapper = new ObjectMapper();
            JsonNode resposeTree = responseMapper.readTree(response);
            JsonNode meta = resposeTree.get("MetaInformation");
            totalPages = meta.get("@TotalPages").asInt();
            return resposeTree.get("Articles");
        }
        return null;
    }
}
