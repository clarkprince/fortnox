package com.fortnox;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
public class FortnoxController {
	
	@RequestMapping(value = "/fortnox/post", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> postToFortnox(@RequestParam("domain") String domain, @RequestBody JsonNode data) {
		try {
			return Fortnox.doInvoiceCreate(data, domain, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
