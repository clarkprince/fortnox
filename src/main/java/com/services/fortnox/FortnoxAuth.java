package com.services.fortnox;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FortnoxAuth {

    public static Map<String, String> doAuth(String code, boolean isRefresh) throws IOException {
        Map<String, String> tok = new HashMap<String, String>();
        String clientSecret = "8j9LCS7tj8";
        String clientId = "KGtuCBfCxWhA";

        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        if (isRefresh) {
            map.add("grant_type", "refresh_token");
            map.add("refresh_token", code);
        } else { // activating a customer
            map.add("grant_type", "authorization_code");
            map.add("code", code);
            map.add("redirect_uri", "http://fortnox.workwit.net");
        }

        String uri = "https://apps.fortnox.se/oauth-v1/token";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        headers.set("Authorization", "Basic " + auth);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper responseMapper = new ObjectMapper();
            try {
                JsonNode resposeTree = responseMapper.readTree(response.getBody());

                tok.put("access_token", resposeTree.get("access_token").asText());
                tok.put("refresh_token", resposeTree.get("refresh_token").asText());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return tok;
    }

}
