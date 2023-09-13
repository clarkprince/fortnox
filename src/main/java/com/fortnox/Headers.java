package com.fortnox;

import java.util.Arrays;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class Headers {

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

}
