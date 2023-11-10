package com.fortnox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Insert {
	
	private final static Logger log = LoggerFactory.getLogger(Insert.class);
	
	public static void insertCustomer(String myId, String name, String address2, String address3,
			String address4, String postcode, String domain, String apiKey) {

		String address = address2.replace("\"", "").trim() + (!isEmpty(address3.replace("\"", "").trim())?", ":"") 
				+ address3.replace("\"", "").trim() + (!isEmpty(address4.replace("\"", "").trim())?", ":"") 
				+ address4.replace("\"", "").trim() +(!isEmpty(postcode.replace("\"", "").trim())?", ":"") 
				+ postcode.replace("\"", "").trim();
		
		Map<String,Object> map = new HashMap<>();
		map.put("myId", myId.replace("\"", "").trim());
		map.put("name", name.replace("\"", "").trim());
		map.put("address", address.replace("\"", "").trim());
		
		try {
			String json = new ObjectMapper().writeValueAsString(map);
			
			String uri = "https://ws.synchroteam.com/api/v3/customer/send";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
				log.info("Customer "+myId+"-"+name+" successfully added.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Customer. Skipping. "+e);
			//e.printStackTrace();
		}
	}
	
	public static void insertPart(String reference, String name, String price, String vat, String note, String type, boolean tracked, boolean active, String domain, String apiKey) {	
		Map<String,Object> map = new HashMap<>();
		Map<String,Object> category = new HashMap<>();
		Map<String,Object> tax = new HashMap<>();
		category.put("name", "MainParts");
		tax.put("id", "1");
		tax.put("name", "VAT");
		tax.put("tax", vat.replace("\"", "").trim());
		map.put("name", name.replace("\"", "").trim());
		map.put("reference", reference.replace("\"", "").trim());
		map.put("price", price.replace("\"", "").trim());
		map.put("isTracked", tracked);
		map.put("description", note.replace("\"", "").trim());
		map.put("category", category);
		map.put("status", active ? "active" : "inactive");
		String aType = "part";
		if (type.equalsIgnoreCase("SERVICE")) {
			aType = "service";
		}
		map.put("type", aType);
		
		
		try {
			String json = new ObjectMapper().writeValueAsString(map);
			
			String uri = "https://ws.synchroteam.com/api/v3/part/send";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
//				try {
//				    Files.write(Paths.get("myfile.txt"), name.getBytes(), StandardOpenOption.APPEND);
//				}catch (IOException e) {
//				    //exception handling left as an exercise for the reader
//				}
				log.info("Part "+name+" successfully added/updated.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Part. Skipping." + e);
			//e.printStackTrace();
		}
	}
	
	
	public static void insertToDepot(String depotName, String productCode, Integer quantity, String domain, String apiKey) {
		List<Map<String,Object>> ls = new ArrayList<>();
		List<Map<String,Object>> partsLs = new ArrayList<>();
		
		Map<String,Object> map = new HashMap<>();
		Map<String,Object> parts = new HashMap<>();
		
		parts.put("productCode", productCode.replace("\"", "").trim());
		parts.put("quantity", quantity);
		partsLs.add(parts);
		
		Map<String,Object> depot = new HashMap<>();
		depot.put("name", depotName);
		
		map.put("depot", depot);
		map.put("parts", partsLs);
		ls.add(map);
		
		try {
			String json = new ObjectMapper().writeValueAsString(ls);
			
			String uri = "https://ws.synchroteam.com/api/v3/depot/quantities";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
				log.info("Part " + productCode + "successfully added/updated to depot.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Part. Skipping." + e);
			//e.printStackTrace();
		}
	}
	
	public static void insertEquipement(String myId, String custMyId, String name, String domain, String apiKey) {

		Map<String,Object> map = new HashMap<>();
		Map<String,Object> customer = new HashMap<>();
		customer.put("myId", custMyId.replace("\"", "").trim());
		
		name = name.replace("\"", "").trim();
		
		if(isEmpty(name)) {
			name = "Need to be updated - " + myId.replace("\"", "").trim();
		}

		map.put("name", name);
		map.put("myId", myId.replace("\"", "").trim());
		map.put("customer", customer);
		
		try {
			String json = new ObjectMapper().writeValueAsString(map);
			
			String uri = "https://ws.synchroteam.com/api/v3/equipment/send";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
				log.info("Equipment "+name+" successfully added.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Equipement. Skipping. " +e);
			//e.printStackTrace();
		}
	}
	
	public static void insertJob(String myId, String description, String custMyId, String equipmentId, String address1, String address2, String address3,
			String address4, String postcode, String custPO, String noRepItems, String serviceProdType, 
			String serviceProdTypeDesc, String serviceWorkType, String serviceWorkTypeDesc, String domain, String apiKey) {
		
		log.info("Posting job - "+ myId);

		Map<String,Object> map = new HashMap<>();
		Map<String,Object> customer = new HashMap<>();
		customer.put("myId", custMyId.replace("\"", "").trim());
		
		Map<String,Object> site = new HashMap<>();
		site.put("address", "");
		
		Map<String,Object> type = new HashMap<>();
		type.put("name", "Field Service");
		
		Map<String,Object> equipment = new HashMap<>();
		equipment.put("myId", equipmentId.replace("\"", "").trim());
		
		String address = address1.replace("\"", "").trim() + (!isEmpty(address2.replace("\"", "").trim())?", ":"") + address2.replace("\"", "").trim() 
				+ (!isEmpty(address3.replace("\"", "").trim())?", ":"") 
				+ address3.replace("\"", "").trim() + (!isEmpty(address4.replace("\"", "").trim())?", ":"") 
				+ address4.replace("\"", "").trim() +(!isEmpty(postcode.replace("\"", "").trim())?", ":"") 
				+ postcode.replace("\"", "").trim();
		
		List<Map<String,Object>> customFieldValuesLs =  new ArrayList<Map<String,Object>>();
		Map<String,Object> customFieldValues = new HashMap<>();
		
		customFieldValues.put("label", "Customer PO Number:");
		customFieldValues.put("value", custPO.replace("\"", "").trim());
		
		customFieldValuesLs.add(customFieldValues);
		
	
		if(noRepItems !=null && !isEmpty(noRepItems.replace("\"", "").trim())) {
			Map<String,Object> customFieldValues1 = new HashMap<>();
			customFieldValues1.put("label", "NoRepItems");
			customFieldValues1.put("value", noRepItems.replace("\"", "").trim());
			customFieldValuesLs.add(customFieldValues1);
		}
		
		
		
		if(serviceProdType !=null && !isEmpty(serviceProdType.replace("\"", "").trim())) {
			Map<String,Object> customFieldValues2 = new HashMap<>();
			customFieldValues2.put("label", "ServiceProdType");
			customFieldValues2.put("value", serviceProdType.replace("\"", "").trim());
			customFieldValuesLs.add(customFieldValues2);
		}
		
		
		
		if(serviceProdTypeDesc !=null && !isEmpty(serviceProdTypeDesc.replace("\"", "").trim())) {
			Map<String,Object> customFieldValues3 = new HashMap<>();
			customFieldValues3.put("label", "ServiceProdTypeDesc");
			customFieldValues3.put("value", serviceProdTypeDesc.replace("\"", "").trim());	
			customFieldValuesLs.add(customFieldValues3);
		}

	
		
		if(serviceWorkType !=null && !isEmpty(serviceWorkType.replace("\"", "").trim())) {
			Map<String,Object> customFieldValues4 = new HashMap<>();
			customFieldValues4.put("label", "ServiceWorkType");
			customFieldValues4.put("value", serviceWorkType.replace("\"", "").trim());
			customFieldValuesLs.add(customFieldValues4);
		}

		
	
		if(serviceWorkTypeDesc !=null && !isEmpty(serviceWorkTypeDesc.replace("\"", "").trim())) {
			Map<String,Object> customFieldValues5 = new HashMap<>();
			customFieldValues5.put("label", "ServiceWorkTypeDesc");
			customFieldValues5.put("value", serviceWorkTypeDesc.replace("\"", "").trim());
			customFieldValuesLs.add(customFieldValues5);
		}

		map.put("myId", myId.replace("\"", "").trim());
		map.put("description", description.replace("\"", "").trim());
		map.put("address", address);
		map.put("customer", customer);
		map.put("site", site);
		map.put("equipment", equipment);
		map.put("type", type);
		map.put("customFieldValues", customFieldValuesLs);

		
		try {
			String json = new ObjectMapper().writeValueAsString(map);
		
			String uri = "https://ws.synchroteam.com/api/v3/job/send";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
				log.info("Job "+myId+" successfully added.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void sendPartToJob(String myId, String reference, String quantity, String domain, String apiKey) {
		log.info("Sending part for job - "+ myId);
		
		Map<String,Object> map = new HashMap<>();
		Map<String,Object> part = new HashMap<>();
		List<Map<String,Object>> parts = new ArrayList<>();
		
		part.put("quantity", quantity.replace("\"", "").trim());
		part.put("reference", reference.replace("\"", "").trim());
		
		parts.add(part);
		
		map.put("myId", myId.replace("\"", "").trim());
		map.put("parts", parts);
		
		try {
			String json = new ObjectMapper().writeValueAsString(map);

			String uri = "https://ws.synchroteam.com/api/v3/job/parts/send";
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
			HttpEntity<String> entity = new HttpEntity<String>(json, Headers.getHTTPHeaders( domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
			
			if(response.getStatusCodeValue()==200) {
				log.info("Parts for job-"+reference+" successfully added.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Job. Skipping. " +e);
			//e.printStackTrace();
		}
		
	}
	
	private static boolean isEmpty(String s) {
		if(s==null || s.length()==0)
			return true;
		else
			return false;
	}



}
