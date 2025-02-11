package com.services.fortnox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.entities.Activity;
import com.entities.Part;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.PartRepository;
import com.utils.Utils;

@Service
public class Invoices {
	private final Logger log = LoggerFactory.getLogger(Invoices.class);

	@Autowired
	private FnCustomers fnCustomersService;

	@Autowired
	private PartRepository partRepository;

	public ResponseEntity<String> doInvoiceCreate(JsonNode map, boolean isInvoice, JsonNode invoiceMap, Tenant tenant) throws IOException {
		try {
			String response = invoiceCreate(map, isInvoice, invoiceMap, tenant, null);
			if (response != null) {
				log.info("Successfully created an invoice on Fortnox");
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Error creating invoice: {}", e.getMessage());
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	public Activity invoiceOrOrderCreate(JsonNode map, boolean isInvoice, JsonNode invoiceMap, Tenant tenant, Activity activity) throws IOException {
		String response = invoiceCreate(map, isInvoice, invoiceMap, tenant, activity);
		if (response != null) {
			activity.setSuccessful(true);
			activity.setActivity2(Utils.prettyPrint(response));
		}
		return activity;
	}

	public String invoiceCreate(JsonNode map, boolean isInvoice, JsonNode invoiceMap, Tenant tenant, Activity activity) throws IOException {
		String uri = isInvoice ? "/3/invoices" : "/3/orders";

		Map<String, Object> newMap = new HashMap<>();
		Map<String, Object> invoice = new HashMap<>();
		String custNum = map.get("customer").get("myId").asText();
		if (custNum == null || custNum.equalsIgnoreCase("null") || custNum.equalsIgnoreCase("")) {
			custNum = map.get("customer").get("id").asText();
		}

		if (custNum != null) {
			newMap.put("CustomerNumber", custNum);
			JsonNode fcust = fnCustomersService.doGetSingleCustomer(tenant, custNum);
			if (fcust != null) {
				newMap.put("CustomerName", fcust.get("Name").asText());
				newMap.put("Address1", fcust.get("Address1").asText());
				newMap.put("Address2", fcust.get("Address2").asText());
				newMap.put("City", fcust.get("City").asText());
				newMap.put("ZipCode", fcust.get("ZipCode").asText());
			} else {
				newMap.put("CustomerName", map.get("customer").get("name").asText());
				newMap.put("Address1", map.get("addressStreet").asText());
				newMap.put("Address2", map.get("addressProvince").asText());
				newMap.put("City", map.get("addressCity").asText());
				newMap.put("ZipCode", map.get("addressZIP").asText());
			}
			// newMap.put("Country", map.get("addressCountry").asText());
			newMap.put("@url", map.get("publicLink").asText());

			String description = map.get("description").asText();
			if (isInvoice) {
				description = invoiceMap.get("description").asText() == null || invoiceMap.get("description").asText() == "null" ? ""
						: invoiceMap.get("description").asText();
				if (description != null && description.trim() != "") {
					String myId = invoiceMap.get("myId") != null ? invoiceMap.get("myId").asText() : "";
					String num = invoiceMap.get("num") != null ? String.valueOf(invoiceMap.get("num").asInt()) : "";
					String technician = map.get("technician").get("name") != null ? map.get("technician").get("name").asText() : "";
					String comment = myId + " - " + num + " - " + technician + " - " + description;
					newMap.put("Comments", comment);
					newMap.put("Remarks", myId + " - " + num + " - " + technician);
				}
			} else {
				String myId = map.get("myId") != null ? map.get("myId").asText() : "";
				String num = map.get("num") != null ? String.valueOf(map.get("num").asInt()) : "";
				String technician = map.get("technician").get("name") != null ? map.get("technician").get("name").asText() : "";
				String comment = myId + " - " + num + " - " + technician + " - " + description;
				newMap.put("Comments", comment);
				newMap.put("Remarks", myId + " - " + num + " - " + technician);
			}

			String site = map.get("site").asText();
			newMap.put("DeliveryName",
					site != null && !site.equalsIgnoreCase("null")
							? (map.get("site").get("myId").asText() + " " + map.get("site").get("name").asText())
							: "");
			newMap.put("DeliveryAddress1", map.get("addressStreet").asText());
			newMap.put("DeliveryAddress2", map.get("addressProvince").asText());
			newMap.put("DeliveryCity", map.get("addressCity").asText());
			newMap.put("DeliveryZipCode", map.get("addressZIP").asText());

			List<Map<String, Object>> rows = new ArrayList<>();
			boolean havePartsBeenRun = false;
			Map<String, JsonNode> partsMap = new HashMap<>();
			try {
				partsMap = getPartsMap(tenant.getSynchroteamDomain());
			} catch (Exception e1) {
				log.error("Error getting parts map: {}", e1.getMessage());
			}

			JsonNode lines = isInvoice ? invoiceMap.get("lines") : null;
			if (isInvoice && lines != null && lines.size() > 0) {
				JsonNode parts = lines;
				if (parts.size() > 0) {
					for (JsonNode part : parts) {
						Map<String, Object> d = new HashMap<>();
						JsonNode article = partsMap.get(part.get("partProductCode").asText());
						if (article == null && !havePartsBeenRun) {
							// ProcessManager.parts();
							havePartsBeenRun = true;
							partsMap = getPartsMap(tenant.getSynchroteamDomain());
							article = partsMap.get(part.get("partProductCode").asText());
						}

						if (article != null) {
							String q = part.get("quantity").asText();
							if (q != null && q.length() > 3 && q.contains(",")) {
								q = q.substring(0, q.length() - 3);
							}
							d.put("ArticleNumber", part.get("partProductCode").asText());
							d.put("DeliveredQuantity", q);
							d.put("Description", part.get("description").asText());
						}
						rows.add(d);
					}
				}
			} else {
				JsonNode parts = map.get("parts");
				if (parts.size() > 0) {
					for (JsonNode part : parts) {
						try {
							Map<String, Object> d = new HashMap<>();
							JsonNode article = partsMap.get(part.get("reference").asText());
							if (article == null && !havePartsBeenRun) {
								// getParts();
								havePartsBeenRun = true;
								partsMap = getPartsMap(tenant.getSynchroteamDomain());
								article = partsMap.get(part.get("reference").asText());
							}

							if (article != null) {
								String q = part.get("quantity").asText();
								if (q != null && q.length() > 3 && q.contains(",")) {
									q = q.substring(0, q.length() - 3);
								}
								d.put("ArticleNumber", part.get("reference").asText());
								d.put("DeliveredQuantity", q);
								d.put("Description", article.get("Description").asText());
							}
							rows.add(d);
						} catch (Exception e2) {
							log.error(e2.getMessage());
						}
					}
				}
			}

			if (isInvoice) {
				newMap.put("InvoiceRows", rows);
				invoice.put("Invoice", newMap);
			} else {
				newMap.put("OrderRows", rows);
				invoice.put("Order", newMap);
			}

			ObjectMapper mapper = new ObjectMapper();
			String newjson = mapper.writeValueAsString(invoice);
			return FortnoxRequests.doPost(tenant, uri, newjson);
		}

		return null;
	}

	public Map<String, JsonNode> getPartsMap(String domain) throws IOException {
		ObjectMapper partMapper = new ObjectMapper();
		Map<String, JsonNode> partsMap = new HashMap<>();

		List<Part> parts = partRepository.findBySourceAndTenantDomain("fortnox", domain);
		for (Part part : parts) {
			JsonNode partDetails = partMapper.readTree(part.getJsonData());
			partsMap.put(partDetails.get("ArticleNumber").asText(), partDetails);
		}
		return partsMap;
	}
}
