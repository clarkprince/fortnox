package com.services.synchroteam;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.entities.Tenant;

public class SynchroRequests {

	public static HttpHeaders getHTTPHeaders(String domain, String apiKey) {
		try {
			String key = domain + ":" + apiKey;
			String base64Key = Base64.getEncoder().encodeToString(key.getBytes());
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("authorization", "Basic " + base64Key);
			headers.set("cache-control", "no-cache");

			return headers;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static String doPost(Tenant tenant, String uri, String data) {
		String domain = tenant.getSynchroteamDomain();
		String apiKey = tenant.getSynchroteamAPIKey();

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

		HttpEntity<String> entity = new HttpEntity<String>(data, SynchroRequests.getHTTPHeaders(domain, apiKey));
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);

		if (response.getStatusCode().is2xxSuccessful()) {
			String quotaRemaining = response.getHeaders().getFirst("X-Quota-Remaining");
			if (quotaRemaining != null) {
				tenant.setSynchroteamQuotaRemaining(Integer.parseInt(quotaRemaining));
			}
			return response.getBody();
		}
		return null;
	}

	public static String doGet(Tenant tenant, String uri) {
		String domain = tenant.getSynchroteamDomain();
		String apiKey = tenant.getSynchroteamAPIKey();

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

		HttpEntity<String> entity = new HttpEntity<String>(SynchroRequests.getHTTPHeaders(domain, apiKey));
		ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
		if (response.getStatusCode().is2xxSuccessful()) {
			return response.getBody();
		}
		return null;
	}
}
