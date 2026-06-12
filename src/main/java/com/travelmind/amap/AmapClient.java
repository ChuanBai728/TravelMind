package com.travelmind.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmind.amap.dto.AmapPoi;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 高德地图 API 客户端
 */
@Component
public class AmapClient {

    private static final Logger log = LoggerFactory.getLogger(AmapClient.class);

    private final AmapProperties amapProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public AmapClient(AmapProperties amapProperties, ObjectMapper objectMapper) {
        this.amapProperties = amapProperties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(amapProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(amapProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 搜索 POI
     *
     * @param city     城市名称
     * @param keyword  关键词
     * @param pageSize 每页数量
     * @param page     页码
     * @return POI 列表
     */
    public List<AmapPoi> searchPois(String city, String keyword, int pageSize, int page) {
        try {
            HttpUrl url = HttpUrl.parse(amapProperties.getBaseUrl() + "/v3/place/text").newBuilder()
                    .addQueryParameter("key", amapProperties.getApiKey())
                    .addQueryParameter("keywords", keyword)
                    .addQueryParameter("city", city)
                    .addQueryParameter("offset", String.valueOf(pageSize))
                    .addQueryParameter("page", String.valueOf(page))
                    .addQueryParameter("extensions", "all")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            log.debug("Searching POIs: city={}, keyword={}", city, keyword);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Amap POI search failed with status: {}", response.code());
                    return new ArrayList<>();
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                String status = root.path("status").asText();
                if (!"1".equals(status)) {
                    log.warn("Amap POI search returned error: {}", root.path("info").asText());
                    return new ArrayList<>();
                }

                JsonNode pois = root.path("pois");
                List<AmapPoi> result = new ArrayList<>();
                if (pois.isArray()) {
                    for (JsonNode poiNode : pois) {
                        AmapPoi poi = objectMapper.treeToValue(poiNode, AmapPoi.class);
                        result.add(poi);
                    }
                }
                return result;
            }
        } catch (IOException e) {
            log.error("Amap POI search failed", e);
            return new ArrayList<>();
        }
    }

    /**
     * 地理编码
     *
     * @param address 地址
     * @param city    城市（可选）
     * @return 经纬度，格式为 "经度,纬度"
     */
    public String geocode(String address, String city) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(amapProperties.getBaseUrl() + "/v3/geocode/geo").newBuilder()
                    .addQueryParameter("key", amapProperties.getApiKey())
                    .addQueryParameter("address", address);

            if (city != null && !city.isEmpty()) {
                urlBuilder.addQueryParameter("city", city);
            }

            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            log.debug("Geocoding address: {}", address);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Amap geocode failed with status: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);

                String status = root.path("status").asText();
                if (!"1".equals(status)) {
                    log.warn("Amap geocode returned error: {}", root.path("info").asText());
                    return null;
                }

                JsonNode geocodes = root.path("geocodes");
                if (geocodes.isArray() && geocodes.size() > 0) {
                    return geocodes.get(0).path("location").asText();
                }
                return null;
            }
        } catch (IOException e) {
            log.error("Amap geocode failed", e);
            return null;
        }
    }

}
