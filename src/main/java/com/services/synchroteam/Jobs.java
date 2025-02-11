package com.services.synchroteam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.JobsHistory;
import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.JobsHistoryRepository;
import com.services.fortnox.Invoices;
import com.utils.Utils;

@Service
public class Jobs {

	private static final Logger log = LoggerFactory.getLogger(Jobs.class);
	private static final String API_BASE_URL = "https://ws.synchroteam.com/api/v3";
	private static final String EGEEID_FIELD = "EGEEID";

	@Autowired
	private JobsHistoryRepository jobsHistoryRepository;

	@Autowired
	private Invoices fortnoxInvoices;

	public ProcessMonitor checkingValidatedJobs(Tenant tenant, String fromTime, int pageSize, ProcessMonitor processMonitor) {
		log.info("Retrieving validated jobs");
		ObjectMapper mapper = new ObjectMapper();

		try {
			log.info("Retrieving list of validated jobs");
			int totalPages = 1;
			int page = 1;

			do {
				String response = requestJobs(tenant, fromTime, pageSize, page);

				if (response != null) {
					JsonNode root = mapper.readTree(response);
					totalPages = (int) Math.ceil(root.get("recordsTotal").asInt() / pageSize);
					JsonNode jobNode = root.get("data");

					if (jobNode.size() > 0) {
						for (JsonNode j : jobNode) {
							String myId = j.get("id").asText();
							if (!Utils.isEmpty(myId)) {
								JobsHistory existingJob = jobsHistoryRepository.findById(myId).orElse(null);
								if (existingJob == null) {
									JsonNode map = retrieveJob(myId, tenant);
									sendAndSaveJob(myId, map, tenant);
								}
							}
						}
					}
				}

				page++;
			} while (page <= totalPages);
			log.info("Successfully ran job retrieval process");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return processMonitor;
	}

	public String requestJobs(Tenant tenant, String fromTime, int pageSize, int page) {
		String uri = API_BASE_URL + "/job/list?status=validated&pageSize=" + pageSize + "&validatedSince=" + fromTime + "&page=" + page;
		return SynchroRequests.doGet(tenant, uri);
	}

	public JsonNode retrieveJob(String jobId, Tenant tenant) {
		log.info("Retrieving job -" + jobId);
		JsonNode map = null;
		try {
			String uri = API_BASE_URL + "/job/details?id=" + jobId;
			String response = SynchroRequests.doGet(tenant, uri);

			if (response != null) {
				map = new ObjectMapper().readTree(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	private void sendAndSaveJob(String jobId, JsonNode map, Tenant tenant) throws IOException {
		try {
			fortnoxInvoices.doInvoiceCreate(map, false, null, tenant);

			JobsHistory jobHistory = new JobsHistory();
			jobHistory.setId(jobId);
			jobsHistoryRepository.save(jobHistory);
			log.info("Job-" + jobId + " has been successfully been saved.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getFieldFromUser(String id, Tenant tenant) {
		log.info("Retrieving user-" + id);

		try {
			String uri = API_BASE_URL + "/user/details?id=" + id;
			String response = SynchroRequests.doGet(tenant, uri);

			if (response != null) {
				JsonNode root = new ObjectMapper().readTree(response);
				JsonNode userNode = root.get("CustomFieldValues");
				if (userNode.size() > 0) {
					for (JsonNode j : userNode) {
						String label = j.get("label").textValue();
						if (!Utils.isEmpty(label) && label.equalsIgnoreCase(EGEEID_FIELD)) {
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

	public boolean jobExists(String id, Tenant tenant) {
		try {
			String uri = API_BASE_URL + "/job/details?myId=" + id;
			String response = SynchroRequests.doGet(tenant, uri);

			if (response != null) {
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public void insertJob(String myId, String description, String custMyId, String equipmentId, String address1, String address2, String address3,
			String address4, String postcode, String custPO, String noRepItems, String serviceProdType, String serviceProdTypeDesc,
			String serviceWorkType, String serviceWorkTypeDesc, Tenant tenant) {

		log.info("Posting job - {}", myId);

		try {
			Map<String, Object> map = new HashMap<>();
			Map<String, Object> customer = new HashMap<>();
			customer.put("myId", custMyId);
			Map<String, Object> site = new HashMap<>();
			site.put("address", "");

			Map<String, Object> type = new HashMap<>();
			type.put("name", "Field Service");

			Map<String, Object> equipment = new HashMap<>();
			equipment.put("myId", equipmentId);

			String address = buildAddress(address1, address2, address3, address4, postcode);

			List<Map<String, Object>> customFieldValuesLs = new ArrayList<Map<String, Object>>();
			Map<String, Object> customFieldValues = new HashMap<>();

			customFieldValues.put("label", "Customer PO Number:");
			customFieldValues.put("value", custPO);

			customFieldValuesLs.add(customFieldValues);

			if (noRepItems != null && !Utils.isEmpty(noRepItems)) {
				Map<String, Object> customFieldValues1 = new HashMap<>();
				customFieldValues1.put("label", "NoRepItems");
				customFieldValues1.put("value", noRepItems);
				customFieldValuesLs.add(customFieldValues1);
			}

			if (serviceProdType != null && !Utils.isEmpty(serviceProdType)) {
				Map<String, Object> customFieldValues2 = new HashMap<>();
				customFieldValues2.put("label", "ServiceProdType");
				customFieldValues2.put("value", serviceProdType);
				customFieldValuesLs.add(customFieldValues2);
			}

			if (serviceProdTypeDesc != null && !Utils.isEmpty(serviceProdTypeDesc)) {
				Map<String, Object> customFieldValues3 = new HashMap<>();
				customFieldValues3.put("label", "ServiceProdTypeDesc");
				customFieldValues3.put("value", serviceProdTypeDesc);
				customFieldValuesLs.add(customFieldValues3);
			}

			if (serviceWorkType != null && !Utils.isEmpty(serviceWorkType)) {
				Map<String, Object> customFieldValues4 = new HashMap<>();
				customFieldValues4.put("label", "ServiceWorkType");
				customFieldValues4.put("value", serviceWorkType);
				customFieldValuesLs.add(customFieldValues4);
			}

			if (serviceWorkTypeDesc != null && !Utils.isEmpty(serviceWorkTypeDesc)) {
				Map<String, Object> customFieldValues5 = new HashMap<>();
				customFieldValues5.put("label", "ServiceWorkTypeDesc");
				customFieldValues5.put("value", serviceWorkTypeDesc);
				customFieldValuesLs.add(customFieldValues5);
			}

			map.put("myId", myId);
			map.put("description", description);
			map.put("address", address);
			map.put("customer", customer);
			map.put("site", site);
			map.put("equipment", equipment);
			map.put("type", type);
			map.put("customFieldValues", customFieldValuesLs);

			String json = new ObjectMapper().writeValueAsString(map);

			String uri = API_BASE_URL + "/job/send";
			String response = SynchroRequests.doPost(tenant, uri, json);

			if (response != null) {
				log.info("Job {} successfully added.", myId);
			}
		} catch (Exception e) {
			log.error("Failed to insert job {}: {}", myId, e.getMessage());
			throw new RuntimeException("Failed to insert job", e);
		}
	}

	private String buildAddress(String... parts) {
		return String.join(", ", Arrays.stream(parts).filter(part -> !Utils.isEmpty(part)).collect(Collectors.toList()));
	}

	public void sendPartToJob(String myId, String reference, String quantity, Tenant tenant) {
		log.info("Sending part for job - " + myId);

		Map<String, Object> map = new HashMap<>();
		Map<String, Object> part = new HashMap<>();
		List<Map<String, Object>> parts = new ArrayList<>();

		part.put("quantity", quantity);
		part.put("reference", reference);

		parts.add(part);

		map.put("myId", myId);
		map.put("parts", parts);

		try {
			String json = new ObjectMapper().writeValueAsString(map);

			String uri = API_BASE_URL + "/job/parts/send";
			String response = SynchroRequests.doPost(tenant, uri, json);

			if (response != null) {
				log.info("Parts for job-" + reference + " successfully added.");
			}
		} catch (Exception e) {
			log.error("Failed to insert Job. Skipping. " + e);
			// e.printStackTrace();
		}

	}
}
