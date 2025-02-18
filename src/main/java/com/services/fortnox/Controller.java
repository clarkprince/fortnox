package com.services.fortnox;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ProcessManager;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
public class Controller {

	@Autowired
	private ProcessManager processManager;

	@Autowired
	private Invoices invoices;

	@RequestMapping(value = "/fortnox/post", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> postToFortnox(@RequestParam("domain") String domain, @RequestBody JsonNode data) {
		try {
			Tenant tenant = processManager.authorisedTenantByDomain(domain);
			return invoices.doInvoiceCreate(data, false, null, tenant);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
