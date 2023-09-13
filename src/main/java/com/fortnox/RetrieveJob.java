package com.fortnox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableScheduling
public class RetrieveJob {

	private static final Logger log = LoggerFactory.getLogger(RetrieveJob.class);
	private static String jobsListFilePath;
	private static String invoiceListFilePath;
	//private static String jobsOutPath;

	@Scheduled(cron = "${out.cron.expression}")
	public void getTenants() {
		try {
			String tenantJson = new String(Files.readAllBytes(Paths.get("tenants.json")));
			ObjectMapper tenantMapper = new ObjectMapper();
			JsonNode tenantList = tenantMapper.readTree(tenantJson);
			
			for(JsonNode tenant: tenantList){
				Map<String, Object> m = tenantMapper.convertValue(tenant, new TypeReference<Map<String, Object>>(){});
				for (Entry<String, Object> p : m.entrySet()) {
					@SuppressWarnings("unchecked")
					LinkedHashMap<String, Object> v =  (LinkedHashMap<String, Object>) p.getValue(); 
					checkingValidatedJobs(v.get("synchroteamDomain").toString(), v.get("synchroteamAPIKey").toString());
					invoiceList(v.get("synchroteamDomain").toString(), v.get("synchroteamAPIKey").toString());
				}
				
			}
		}catch(Exception e) {
			log.error("Error getting tenant settings. "+ e);
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	public static void invoiceList(String domain, String apiKey) {

		log.info("Retrieving validated jobs");
		ObjectMapper mapper = new ObjectMapper();
		List<String> myIds = new ArrayList<>();

		try {
			String settingsJson = new String(Files.readAllBytes(Paths.get("settings.json")));
			ObjectMapper settingsMapper = new ObjectMapper();
			Map<String, String> map = settingsMapper.readValue(settingsJson, Map.class);

			invoiceListFilePath = map.get("invoiceListFilePath");

			BufferedReader br = new BufferedReader(new FileReader(invoiceListFilePath));
			String line;
			while ((line = br.readLine()) != null) {
				myIds.add(line);
			}
			log.info("Successfully retrieved previously sent invoices");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			log.info("Retrieving list of invoices");
			int totalPages = 1;
			int page = 1;
			
			do{
				String uri = "https://ws.synchroteam.com/api/v3/invoice/list?status=sent&page="+page;
				RestTemplate restTemplate = new RestTemplate();
				
				HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

				if (response.getStatusCodeValue() == 200) {
					JsonNode root = mapper.readTree(response.getBody());
					totalPages = (int) Math.ceil( root.get("recordsTotal").asInt() / 25.0);
					JsonNode jobNode = root.get("data");
					if (jobNode.size() > 0) {
						for (JsonNode j : jobNode) {
							String myId = j.get("id").toString().replace("\"", "").trim();
							String jobId = j.get("job").get("id").asText();
							if (!isEmpty(myId)) {
								if (!myIds.contains(myId)) {
									retrieveAndSaveInvoice(myId, jobId, domain, apiKey);
								}
							}
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
	
	
	public static void checkingValidatedJobs(String domain, String apiKey) {

		log.info("Retrieving validated jobs");
		ObjectMapper mapper = new ObjectMapper();
		List<String> myIds = new ArrayList<>();

		try {
			String settingsJson = new String(Files.readAllBytes(Paths.get("settings.json")));
			ObjectMapper settingsMapper = new ObjectMapper();
			Map<String, String> map = settingsMapper.readValue(settingsJson, Map.class);

			jobsListFilePath = map.get("jobsListFilePath");
			//jobsOutPath = map.get("jobsOutPath");

			BufferedReader br = new BufferedReader(new FileReader(jobsListFilePath));
			String line;
			while ((line = br.readLine()) != null) {
				myIds.add(line);
			}
			log.info("Successfully retrieved previously sent jobs");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			log.info("Retrieving list of validated jobs");
			int totalPages = 1;
			int page = 1;
			
			do{
				String uri = "https://ws.synchroteam.com/api/v3/job/list?status=validated&page="+page;
				RestTemplate restTemplate = new RestTemplate();
				
				HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
				ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

				if (response.getStatusCodeValue() == 200) {
					JsonNode root = mapper.readTree(response.getBody());
					totalPages = (int) Math.ceil( root.get("recordsTotal").asInt() / 25.0);
					JsonNode jobNode = root.get("data");
					if (jobNode.size() > 0) {
						for (JsonNode j : jobNode) {
							String myId = j.get("id").toString().replace("\"", "").trim();
							if (!isEmpty(myId)) {
								if (!myIds.contains(myId)) {
									retrieveAndSaveJob(myId, domain, apiKey);
								}
							}
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

	private static void retrieveAndSaveJob(String myId, String domain, String apiKey) {
		log.info("Retrieving job-" + myId);

		try {
			String uri = "https://ws.synchroteam.com/api/v3/job/details?id=" + myId;
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			ObjectMapper mapper = new ObjectMapper();

			if (response.getStatusCodeValue() == 200) {
				String body = response.getBody();
				JsonNode map = mapper.readTree(body);
				
				
				Fortnox.doInvoiceCreate(map, domain, false, null);
				log.info("Saving job-" + myId + " to a file");
				//Files.write(Paths.get(jobsOutPath + "/Job-" + myId + ".txt"), newjson.getBytes());
				saveIdToFile(myId, jobsListFilePath);
			}
			log.info("Job-" + myId + " has been successfully written out.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void retrieveAndSaveInvoice(String myId, String jobId, String domain, String apiKey) {
		log.info("Retrieving invoice-" + myId);

		try {
			String uri = "https://ws.synchroteam.com/api/v3/job/details?id=" + jobId;
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			ObjectMapper mapper = new ObjectMapper();

			if (response.getStatusCodeValue() == 200) {
				String body = response.getBody();
				JsonNode map = mapper.readTree(body);
				
				
				String aUri = "https://ws.synchroteam.com/api/v3/invoice/details?id=" + myId;
				RestTemplate aRestTemplate = new RestTemplate();
				aRestTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

				HttpEntity<String> aEntity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
				ResponseEntity<String> aResponse = aRestTemplate.exchange(aUri, HttpMethod.GET, aEntity, String.class);
				ObjectMapper aMapper = new ObjectMapper();
				
				if (aResponse.getStatusCodeValue() == 200) {
					String aBody = aResponse.getBody();
					JsonNode invoiceMap = aMapper.readTree(aBody);
					
					Fortnox.doInvoiceCreate(map, domain, true, invoiceMap);
					log.info("Saving job-" + myId + " to a file");
					saveIdToFile(myId, invoiceListFilePath);
				}
			}
			log.info("Invoice-" + myId + " has been successfully written out.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getFieldFromUser(String id, String domain, String apiKey){
		log.info("Retrieving user-" + id);

		try {
			String uri = "https://ws.synchroteam.com/api/v3/user/details?id=" + id;
			RestTemplate restTemplate = new RestTemplate();

			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

			ObjectMapper mapper = new ObjectMapper();
			if (response.getStatusCodeValue() == 200) {

				JsonNode root = mapper.readTree(response.getBody());
				JsonNode userNode = root.get("CustomFieldValues");
				if (userNode.size() > 0) {
					for (JsonNode j : userNode) {
						String label = j.get("label").toString().replace("\"", "").trim();
						if (!isEmpty(label) && label.equalsIgnoreCase("EGEEID")) {
							return j.get("value").textValue();
						}
					}
				}		
			}
			log.info("User-" + id + " has been successfully retrieved.");
		} catch (Exception e) {
			log.info("User not found.");
		}
		return "";
	}

	private static void saveIdToFile(String myId, String path) {
		try {
			File file = new File(path);
			FileWriter fr = new FileWriter(file, true);
			BufferedWriter br = new BufferedWriter(fr);
			br.write(myId + "\n");
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

		}
	}

	private static boolean isEmpty(String s) {
		if (s == null || s.length() == 0)
			return true;
		else
			return false;
	}
}
