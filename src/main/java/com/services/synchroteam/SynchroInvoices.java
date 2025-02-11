package com.services.synchroteam;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.Activity;
import com.entities.InvoiceHistory;
import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.InvoiceHistoryRepository;
import com.services.fortnox.Invoices;
import com.utils.Utils;

@Service
public class SynchroInvoices {

	private static final Logger log = LoggerFactory.getLogger(SynchroInvoices.class);
	private static final String API_BASE_URL = "https://ws.synchroteam.com/api/v3";

	@Autowired
	private InvoiceHistoryRepository invoiceHistoryRepository;

	@Autowired
	private Invoices fortnoxInvoices;

	@Autowired
	private Jobs jobsService;

	public ProcessMonitor invoiceList(Tenant tenant, String fromTime, int pageSize, ProcessMonitor processMonitor) {

		log.info("Retrieving validated jobs");
		ObjectMapper mapper = new ObjectMapper();

		try {
			log.info("Retrieving list of invoices");
			int totalPages = 1;
			int page = 1;

			do {
				String response = requestInvoices(tenant, fromTime, pageSize, page);

				if (response != null) {
					JsonNode root = mapper.readTree(response);
					totalPages = (int) Math.ceil(root.get("recordsTotal").asInt() / pageSize);
					JsonNode jobNode = root.get("data");
					if (jobNode.size() > 0) {
						for (JsonNode j : jobNode) {
							String invoiceId = j.get("id").asText();
							String jobId = j.get("job").get("id").asText();
							if (!Utils.isEmpty(invoiceId)) {
								InvoiceHistory existingInvoice = invoiceHistoryRepository.findById(invoiceId).orElse(null);
								if (existingInvoice == null) {
									Activity activity = new Activity();
									JsonNode job = jobsService.retrieveJob(jobId, tenant);
									JsonNode invoice = retrieveInvoice(invoiceId, tenant);
									activity.setActivity1(invoice.toPrettyString());
									activity = sendAndSaveInvoice(invoiceId, job, invoice, tenant, activity);
									processMonitor.getActivities().add(activity);
								}
							}
						}
					}
				}

				page++;
			} while (page <= totalPages);
			log.info("Successfully processed invoices");
		} catch (Exception e) {
			processMonitor.setSuccessful(false);
			log.error("Failed to retrieve invoice list: ", e);
		}
		processMonitor.setSuccessful(true);
		return processMonitor;
	}

	public String requestInvoices(Tenant tenant, String fromTime, int pageSize, int page) {
		String uri = API_BASE_URL + "/invoice/list?status=sent&pageSize=" + pageSize + "&changedSince=" + fromTime + "&page=" + page;
		return SynchroRequests.doGet(tenant, uri);
	}

	public JsonNode retrieveInvoice(String invoiceId, Tenant tenant) {
		log.info("Retrieving invoice-" + invoiceId);
		JsonNode invoiceMap = null;
		try {
			String aUri = API_BASE_URL + "/invoice/details?id=" + invoiceId;
			String aResponse = SynchroRequests.doGet(tenant, aUri);

			if (aResponse != null) {
				invoiceMap = new ObjectMapper().readTree(aResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return invoiceMap;
	}

	private Activity sendAndSaveInvoice(String invoiceId, JsonNode jobMap, JsonNode invoiceMap, Tenant tenant, Activity activity) throws IOException {
		try {
			activity = fortnoxInvoices.invoiceOrOrderCreate(jobMap, true, invoiceMap, tenant, activity);

			InvoiceHistory invoiceHistory = new InvoiceHistory();
			invoiceHistory.setId(invoiceId);
			invoiceHistoryRepository.save(invoiceHistory);
			log.info("Invoice-" + invoiceId + " has been successfully sent.");
		} catch (Exception e) {
			activity.setSuccessful(false);
			activity.setMessage(e.getMessage());
			e.printStackTrace();
		}
		return activity;
	}
}
