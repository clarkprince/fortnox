package com.fortnox;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class Fortnox {
	private static int offset = 0;
	private static int totalPages = 1;
	
	private final static Logger log = LoggerFactory.getLogger(Fortnox.class);
	
	
	public static List<Map<String, String>> authFortnox()  {
		List<Map<String, String>> res = new ArrayList<>();
		
		try {
			String tenantJson = new String(Files.readAllBytes(Paths.get("tenants.json")));
			ObjectMapper tenantMapper = new ObjectMapper();
			JsonNode tenantList = tenantMapper.readTree(tenantJson);
			
			for(JsonNode tenant: tenantList){
				Map<String, Object> m = tenantMapper.convertValue(tenant, new TypeReference<Map<String, Object>>(){});
				for (Entry<String, Object> p : m.entrySet()) {
					@SuppressWarnings("unchecked")
					LinkedHashMap<String, Object> v =  (LinkedHashMap<String, Object>) p.getValue();
					
					String token = Controller.getAuth(v.get("fortNoxRefreshToken").toString(), true, v.get("synchroteamDomain").toString(), v.get("synchroteamAPIKey").toString());
					Map<String, String> r = new HashMap<String, String>();
					r.put("token", token);
					r.put("domain", v.get("synchroteamDomain").toString());
					r.put("apiKey", v.get("synchroteamAPIKey").toString());
					res.add(r);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		return res;
	}

	@Scheduled(cron = "${customers.fortnox.to.synchroteam}")
	public static void getCustomers() throws IOException {
		String settingsJson = new String(Files.readAllBytes(Paths.get("settings.json")));
		ObjectMapper settingsMapper = new ObjectMapper();
		Map<String, String> settingsMap = settingsMapper.readValue(settingsJson, Map.class);

		String synchroteamToFortnox = settingsMap.get("synchroteamToFortnoxCustomers");
		
		List<Map<String, String>> authLs = authFortnox();
		
		for(Map<String, String> auth : authLs) {
			if(auth.get("token") == null) {
				log.error("Token not found");
				return;
			}
			if(synchroteamToFortnox != null && synchroteamToFortnox.contains(auth.get("domain"))) {
				RetrieveCustomer.doSynchroteamCustomersToFortnox(auth.get("token"), auth.get("domain"), auth.get("apiKey"));
				return;
			}
			
			for(int i=0; i<totalPages; i++) {
				offset = i*500;
				JsonNode customerLs = doGetCustomer(auth.get("token"));
				doCustomerInsert(customerLs, auth.get("domain"), auth.get("apiKey"));
			}
			totalPages = 1;
			offset = 0;
		}		
	}
	
	public static JsonNode doGetCustomer(String token) throws JsonMappingException, JsonProcessingException {	
		String uri = "https://api.fortnox.se/3/customers?limit=500&offset="+offset;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCodeValue() == 200) {
			ObjectMapper responseMapper = new ObjectMapper();
			 JsonNode resposeTree = responseMapper.readTree(response.getBody());
			 JsonNode meta = resposeTree.get("MetaInformation");
			 totalPages = meta.get("@TotalPages").asInt();
			 return resposeTree.get("Customers");
		}
		return null;
	}
	
	public static void doCustomerInsert(JsonNode customerLs, String domain, String apiKey) {
		for(JsonNode customer : customerLs) {
			Insert.insertCustomer(customer.get("CustomerNumber").toString(), customer.get("Name").toString(), customer.get("Address1").toString(), 
				customer.get("Address1").toString(), customer.get("Address2").toString() + " " + customer.get("City").toString() , customer.get("ZipCode").toString(),
				 domain, apiKey);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static JsonNode doGetDepot(String token, String stockPointCode) throws JsonMappingException, JsonProcessingException {
		String uri = "https://api.fortnox.se/api/warehouse/stockpoints-v1/"+stockPointCode;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCodeValue() == 200) {
			ObjectMapper responseMapper = new ObjectMapper();
			JsonNode resposeTree = responseMapper.readTree(response.getBody());
			return resposeTree;
		}
		return null;
	}
	
	
	@Scheduled(cron = "${parts.fortnox.to.synchroteam}")
	public static void getParts() throws IOException {
		List<Map<String, String>> authLs = authFortnox();
		
		for(Map<String, String> auth : authLs) {
			int i = 0;
			while(i < totalPages) {
				offset = i*500;
				try {
					JsonNode partLs = doGetArticles(auth.get("token"));
					for(JsonNode part : partLs) {
						try {
//							if(!part.get("ArticleNumber").asText().equalsIgnoreCase("20125")) {
//								continue;
//							}
							JsonNode partDetails = doGetPartDetails(part.get("ArticleNumber").asText(), auth.get("token"));
							doPartsInsert(partDetails, auth.get("domain"), auth.get("apiKey"));
							JsonNode stockDepots = doStockLevel(auth.get("token"), part.get("ArticleNumber").asText());
							for(JsonNode depot: stockDepots) {
								if(depot.get("stockPointCode") != null && partDetails.get("StockGoods").asBoolean(false)) {
									Insert.insertToDepot(depot.get("stockPointCode").asText(), part.get("ArticleNumber").toString(), depot.get("availableStock").asInt(0), auth.get("domain"), auth.get("apiKey"));
									saveDepotToFile(partDetails, depot, auth.get("domain"));
								}
							}
							savePartToFile(partDetails, auth.get("domain"));
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}catch (Exception e) {
							log.error(e.getMessage());
						}
					}
				}catch (Exception e) {
					log.error(e.getMessage());
				}
				i++;
			}
			totalPages = 1;
			offset = 0;		
		}
	}
	
	public static void savePartToFile(JsonNode part, String domain) throws IOException {
		String fname = "parts/"+domain+"/"+part.get("ArticleNumber").asText()+".json";
		if(!Files.exists(Paths.get(fname))) {
			Files.createDirectories(Paths.get("parts/"+domain+"/"));
		    Files.createFile(Paths.get(fname));
		}

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(part);
		try {
		    Files.write(Paths.get(fname), json.getBytes());
		}catch (IOException e) {
			log.error("Error writing parts");
		}
	}
	
	public static void saveDepotToFile(JsonNode part, JsonNode depot, String domain) throws IOException {
		String fname = "depots/"+domain+"/"+part.get("ArticleNumber").asText()+"_"+ depot.get("stockPointCode").asText() +".json";
		if(!Files.exists(Paths.get(fname))) {
			Files.createDirectories(Paths.get("depots/"+domain+"/"));
		    Files.createFile(Paths.get(fname));
		}

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(depot);
		try {
		    Files.write(Paths.get(fname), json.getBytes());
		}catch (IOException e) {
			log.error("Error writing parts");
		}
	}
	
	public static JsonNode doGetPartDetails(String id, String token) throws JsonMappingException, JsonProcessingException {
		String uri = "https://api.fortnox.se/3/articles/"+id;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCodeValue() == 200) {
			ObjectMapper responseMapper = new ObjectMapper();
			JsonNode resposeTree = responseMapper.readTree(response.getBody());
			return resposeTree.get("Article");
		}
		return null;
	}
	
	public static JsonNode doGetArticles(String token) throws JsonMappingException, JsonProcessingException {
		String uri = "https://api.fortnox.se/3/articles?limit=500&offset="+offset;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCodeValue() == 200) {
			ObjectMapper responseMapper = new ObjectMapper();
			JsonNode resposeTree = responseMapper.readTree(response.getBody());
			JsonNode meta = resposeTree.get("MetaInformation");
			totalPages = meta.get("@TotalPages").asInt();
			return resposeTree.get("Articles");
		}
		return null;
	}
	
	public static JsonNode doStockLevel(String token, String id) throws JsonMappingException, JsonProcessingException {
		String uri = "https://api.fortnox.se/api/warehouse/status-v1/stockbalance?itemIds="+id;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCodeValue() == 200) {
			ObjectMapper responseMapper = new ObjectMapper();
			JsonNode resposeTree = responseMapper.readTree(response.getBody());
			return resposeTree;
		}
		return null;
	}
	
	public static void doPartsInsert(JsonNode part, String domain, String apiKey) {
			Insert.insertPart(part.get("ArticleNumber").toString(), part.get("Description").toString(),
					part.get("SalesPrice").toString(), part.get("VAT").toString(), part.get("ManufacturerArticleNumber") !=null ? part.get("ManufacturerArticleNumber").toString() : "",
							part.get("Type").asText(), part.get("StockGoods").asBoolean(true), domain, apiKey);
	}
	
	public static void doInvoiceCreate(JsonNode map, String sDomain, boolean isInvoice, JsonNode invoiceMap) throws IOException {
		String settingsJson = new String(Files.readAllBytes(Paths.get("settings.json")));
		ObjectMapper settingsMapper = new ObjectMapper();
		Map settings = settingsMapper.readValue(settingsJson, Map.class);
		
		String tenantJson = new String(Files.readAllBytes(Paths.get("tenants.json")));
		ObjectMapper tenantMapper = new ObjectMapper();
		JsonNode tenantList = tenantMapper.readTree(tenantJson);

		ObjectMapper m = new ObjectMapper();
		
		String domain = null;
		String apikey = null;
		String refreshToken = null;
		for(JsonNode tenant: tenantList){
			Map<String, Object> tenantMap = m.convertValue(tenant, Map.class);
			for (String key : tenantMap.keySet()) {
			    if(key.equals(sDomain)) {
					Map<String, Object> tenantS = m.convertValue(tenantMap.get(sDomain), Map.class);
			    	domain = tenantS.get("synchroteamDomain").toString();
					apikey = tenantS.get("synchroteamAPIKey").toString();
					refreshToken = tenantS.get("fortNoxRefreshToken").toString();
					break;
			    }
			}
		}
		
		String token = Controller.getAuth(refreshToken, true, domain, apikey);
		
    	// loop through custom values
//    	boolean isInvoice = false;
//    	for(JsonNode vals : map.get("customFieldValues")) {
//    		if(vals.get("fortnox") != null && vals.get("fortnox").asText().equalsIgnoreCase("invoice")) {
//    			isInvoice = true;
//    		}
//    	}
		
    	String uri = "https://api.fortnox.se/3/orders";
    	if(isInvoice) {
        	uri = "https://api.fortnox.se/3/invoices";
    	}
    	
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Authorization", "Bearer "+ token);
		
		Map<String, Object> newMap = new HashMap<>();
		Map<String, Object> invoice = new HashMap<>();
		String custNum = map.get("customer").get("myId").asText();
		if(custNum != null && !custNum.equalsIgnoreCase("null")) {
			newMap.put("CustomerNumber", custNum);
			newMap.put("CustomerName", map.get("customer").get("name").asText());
			newMap.put("Address1", map.get("addressStreet").asText());
			newMap.put("Address2",  map.get("addressProvince").asText());
			newMap.put("City",  map.get("addressCity").asText());
			newMap.put("ZipCode",  map.get("addressZIP").asText());
			//newMap.put("Country",  map.get("addressCountry").asText());
			newMap.put("@url",  map.get("publicLink").asText());

			String description = map.get("description").asText();
	    	if(isInvoice) {
	    		description = invoiceMap.get("description").asText();
				if(description != null && description.trim() != "") {
					String myId = invoiceMap.get("myId") != null ? invoiceMap.get("myId").asText() : "";
					String num = invoiceMap.get("num") != null ? String.valueOf(invoiceMap.get("num").asInt()) : "";
					String technician = map.get("technician").get("name") != null ? map.get("technician").get("name").asText() : "";
					String comment = myId + " - " + num + " - " + technician + " - " + description;
					newMap.put("Comments", comment);
					newMap.put("YourReference", comment);
				}
	    	}else {
				String myId = map.get("myId") != null ? map.get("myId").asText() : "";
				String num = map.get("num") != null ? String.valueOf(map.get("num").asInt()) : "";
				String technician = map.get("technician").get("name") != null ? map.get("technician").get("name").asText() : "";
				String comment = myId + " - " + num + " - " + technician + " - " + description;
				newMap.put("Comments", comment);
				newMap.put("YourReference", comment);
	    	}

			
			newMap.put("DeliveryAddress1", map.get("addressStreet").asText());
			newMap.put("DeliveryAddress2",  map.get("addressProvince").asText());
			newMap.put("DeliveryCity",  map.get("addressCity").asText());
			newMap.put("DeliveryZipCode",  map.get("addressZIP").asText());
			
			List<Map<String, Object>> rows = new ArrayList<Map<String,Object>>();
			boolean havePartsBeenRun = false;
	    	Map<String, JsonNode> partsMap = getPartsMap(domain);
			
			JsonNode lines = isInvoice ? invoiceMap.get("lines") : null;
			if(isInvoice && lines != null && lines.size() > 0) {
				JsonNode parts = lines;
				if (parts.size() > 0) {
					for (JsonNode part : parts) {
						Map<String, Object> d = new HashMap<>();
						JsonNode article = partsMap.get(part.get("partReference").asText());
						if(article == null && !havePartsBeenRun) {
							getParts();
							havePartsBeenRun = true;
							partsMap = getPartsMap(domain);
							article = partsMap.get(part.get("partReference").asText());
						}
						
						if(article != null) {
							d.put("ArticleNumber", part.get("partReference").asText());
							d.put("DeliveredQuantity", part.get("quantity").asText());
							d.put("Description", part.get("description").asText());
						}
						rows.add(d);
					}
				}
			}else {
				JsonNode parts = map.get("parts");
				if (parts.size() > 0) {
					for (JsonNode part : parts) {
						Map<String, Object> d = new HashMap<>();
						JsonNode article = partsMap.get(part.get("reference").asText());
						if(article == null && !havePartsBeenRun) {
							getParts();
							havePartsBeenRun = true;
							partsMap = getPartsMap(domain);
							article = partsMap.get(part.get("reference").asText());
						}
						
						if(article != null) {
							d.put("ArticleNumber", part.get("reference").asText());
							d.put("DeliveredQuantity", part.get("quantity").asText());
							d.put("Description", article.get("Description").asText());
						}
						rows.add(d);
					}
				}
			}

			if(isInvoice) {
				newMap.put("InvoiceRows", rows);
				invoice.put("Invoice", newMap);
			}else {
				newMap.put("OrderRows", rows);	
				invoice.put("Order", newMap);
			}
			
			ObjectMapper mapper = new ObjectMapper();
			String newjson = mapper.writeValueAsString(invoice);
			HttpEntity<String> entity = new HttpEntity<String>(newjson, headers);
			try {
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				if (response.getStatusCodeValue() == 201) {
					log.info("Successfully created an invoice on Fortnox");
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}		
	}
	
	public static Map<String, JsonNode> getPartsMap(String domain) throws IOException {
		ObjectMapper partMapper = new ObjectMapper();
    	Map<String, JsonNode> partsMap = new HashMap<String, JsonNode>();
    	String path = "parts/"+domain+"/";
        File f = new File(path);

        String[] pl = f.list();
        for (String p : pl) {
			String partJson = new String(Files.readAllBytes(Paths.get(path + p)));
			JsonNode partDetails = partMapper.readTree(partJson);
          	partsMap.put(partDetails.get("ArticleNumber").asText(), partDetails);
        }
		return partsMap;
	}
}
