package com.services.fortnox;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.entities.Tenant;

public class FortnoxRequests {

    private static final String BASE_URL = "https://api.fortnox.se";

    public static HttpHeaders getHTTPHeaders(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            return headers;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String doPost(Tenant tenant, String uri, String data) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpEntity<String> entity = new HttpEntity<>(data, getHTTPHeaders(tenant.getFortnoxToken()));
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + uri, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    public static String doGet(Tenant tenant, String uri) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpEntity<String> entity = new HttpEntity<>(getHTTPHeaders(tenant.getFortnoxToken()));
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + uri, HttpMethod.GET, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    public static String doPut(Tenant tenant, String uri, String data) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpEntity<String> entity = new HttpEntity<>(data, getHTTPHeaders(tenant.getFortnoxToken()));
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + uri, HttpMethod.PUT, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    public static String doDelete(Tenant tenant, String uri) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpEntity<String> entity = new HttpEntity<>(getHTTPHeaders(tenant.getFortnoxToken()));
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + uri, HttpMethod.DELETE, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }
}
