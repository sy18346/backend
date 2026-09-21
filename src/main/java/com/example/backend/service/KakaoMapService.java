package com.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoMapService {

    private final WebClient webClient;

    @Value("${kakao.map.rest-api-key:}")
    private String restApiKey;

    @SuppressWarnings("unchecked")
    public Map<String, Object> convertCoordinates(double longitude, double latitude) {
        if (restApiKey == null || restApiKey.isEmpty()) {
            throw new IllegalStateException("카카오맵 REST API 키가 설정되지 않았습니다. 환경 변수 KAKAO_MAP_REST_API_KEY를 설정해주세요.");
        }
        
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/geo/transcoord.json")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .queryParam("input_coord", "WGS84")
                        .queryParam("output_coord", "WCONGNAMUL")  
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> getKakaoCoordinates(double longitude, double latitude) {
        try {
            Map<String, Object> response = convertCoordinates(longitude, latitude);

            if (response == null || !response.containsKey("documents")) {
                return Map.of("x", longitude, "y", latitude);
            }

            java.util.List<Map<String, Object>> documents =
                    (java.util.List<Map<String, Object>>) response.get("documents");

            if (documents == null || documents.isEmpty()) {
                return Map.of("x", longitude, "y", latitude);
            }

            Map<String, Object> document = documents.get(0);

            double x = Double.parseDouble(document.get("x").toString());
            double y = Double.parseDouble(document.get("y").toString());

            return Map.of("x", x, "y", y);
        } catch (IllegalStateException e) {
            // API 키가 없을 때는 원본 좌표 반환
            return Map.of("x", longitude, "y", latitude);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> searchAddress(String address) {
        if (restApiKey == null || restApiKey.isEmpty()) {
            throw new IllegalStateException("카카오맵 REST API 키가 설정되지 않았습니다. 환경 변수 KAKAO_MAP_REST_API_KEY를 설정해주세요.");
        }
        
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response;
    }
}

