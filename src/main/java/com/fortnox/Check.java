package com.fortnox;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class Check {

	public static boolean customerExists(String id, String domain, String apiKey) {
		try {
			id = id.replace("\"", "").trim();
			String uri = "https://ws.synchroteam.com/api/v3/customer/details?myId=" + id;
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			if (response.getStatusCodeValue() == 200) {
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}

	}

	public static boolean partExists(String id, String domain, String apiKey) {
		try {
			id = id.replace("\"", "").trim();
			String uri = "https://ws.synchroteam.com/api/v3/part/details?reference=" + id;
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			if (response.getStatusCodeValue() == 200) {
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean equipmentExists(String id, String name, String domain, String apiKey) {
		try {
			id = id.replace("\"", "").trim();
			name = name.replace("\"", "").trim();
			String uri = "https://ws.synchroteam.com/api/v3/equipment/details?myId="+id+"&name="+name;
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			if (response.getStatusCodeValue() == 200) {
				
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean jobExists(String id, String domain, String apiKey) {
		try {
			id = id.replace("\"", "").trim();
			String uri = "https://ws.synchroteam.com/api/v3/job/details?myId=" + id;
			RestTemplate restTemplate = new RestTemplate();
			HttpEntity<String> entity = new HttpEntity<String>(Headers.getHTTPHeaders(domain, apiKey));
			ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
			if (response.getStatusCodeValue() == 200) {
				return true;
			}

			return false;
		} catch (Exception e) {
			return false;
		}
	}
}
