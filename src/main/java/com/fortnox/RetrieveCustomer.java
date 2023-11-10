package com.fortnox;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RetrieveCustomer {
	
	private static final Logger log = LoggerFactory.getLogger(RetrieveCustomer.class);
	
	public static void doSynchroteamCustomersToFortnox(String token, String domain, String apiKey) {

		log.info("Retrieving customers from sychroteam");
		ObjectMapper mapper = new ObjectMapper();

		try {
			int totalPages = 1;
			int page = 1;
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd+HH:mm:ss");
			String d = simpleDateFormat.format(new Date());
			
			do{
				String uri = "https://ws.synchroteam.com/api/v3/customer/list?changedFrom="+d+"&page="+page;
				RestTemplate restTemplate = new RestTemplate();
				restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
				
				HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

				if (response.getStatusCodeValue() == 200) {
					JsonNode root = mapper.readTree(response.getBody());
					totalPages = (int) Math.ceil( root.get("recordsTotal").asInt() / 25.0);
					JsonNode jobNode = root.get("data");
					if (jobNode.size() > 0) {
						for (JsonNode j : jobNode) {
							insertToFortnox(j, token);
						}
					}
				}
	
				page++;
			}while(page <= totalPages);
			log.info("Successfully ran job retrieval process");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

	}
	
	private static void insertToFortnox(JsonNode data, String token) throws Exception {	
		log.info("inserting customer to fortnox");
		try {
	    	String uri = "https://api.fortnox.se/3/customers";
	    	String vatNumber = data.get("vatNumber").asText();
	    	if (vatNumber == null || vatNumber.equalsIgnoreCase("null") || vatNumber.equalsIgnoreCase("")) {
	    		log.error("No vat number for customer: " + data.get("name").asText());
	    		return;
	    	}
	    	
	    	RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
			headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
			headers.set("Authorization", "Bearer "+ token);
			
			Map<String, Object> customerParent = new HashMap<>();
			Map<String, Object> customer = new HashMap<>();
			if(data.get("myId") == null || data.get("myId").asText() == "") {
				customer.put("CustomerNumber", data.get("id").asText());
			}else {
				customer.put("CustomerNumber", data.get("myId").asText());
			}
	
			customer.put("Name", data.get("name").asText());
			customer.put("Address1", data.get("addressStreet").asText());
			customer.put("Address2",  data.get("addressProvince").asText());
			customer.put("City",  data.get("addressCity").asText());
			customer.put("ZipCode",  data.get("addressZIP").asText());
			
			customerParent.put("Customer", customer);
			
			ObjectMapper mapper = new ObjectMapper();
			String newjson = mapper.writeValueAsString(customerParent);
			HttpEntity<String> entity = new HttpEntity<String>(newjson, headers);
			
			try {
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
				if (response.getStatusCodeValue() == 201) {
					log.info("Successfully created an customer on Fortnox");
				}
			}catch (Exception e) {
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT, entity, String.class);
				if (response.getStatusCodeValue() == 201) {
					log.info("Successfully updated an customer on Fortnox");
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
