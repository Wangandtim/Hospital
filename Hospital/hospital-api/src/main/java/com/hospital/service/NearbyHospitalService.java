package com.hospital.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.NearbyHospital;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class NearbyHospitalService {

    @Value("${amap.key}")
    private String amapKey;

    @Value("${crawler.base-url}")
    private String crawlerBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取周边医院（整合高德地图 + Python爬虫）
     */
    public List<NearbyHospital> getNearbyHospitals(Double latitude, Double longitude,
                                                   Integer radius, String keywords) {
        // 1. 先调用高德地图搜索周边医院
        List<NearbyHospital> hospitals = searchFromAmap(latitude, longitude, radius, keywords);

        // 2. 对每个医院调用Python服务获取详细信息（急诊电话、预约链接）
        for (NearbyHospital hospital : hospitals) {
            enrichHospitalInfo(hospital);
        }

        return hospitals;
    }

    /**
     * 高德地图周边搜索
     */
    private List<NearbyHospital> searchFromAmap(Double latitude, Double longitude,
                                                Integer radius, String keywords) {
        String url = UriComponentsBuilder
                .fromHttpUrl("https://restapi.amap.com/v3/place/around")
                .queryParam("key", amapKey)
                .queryParam("location", longitude + "," + latitude)  // 高德是 经度,纬度
                .queryParam("radius", radius != null ? radius : 5000)  // 默认5公里
                .queryParam("keywords", keywords != null ? keywords : "医院")
                .queryParam("types", "090100")  // 综合医院
                .queryParam("offset", 20)
                .queryParam("page", 1)
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        List<NearbyHospital> result = new ArrayList<>();

        try {
            JsonNode json = mapper.readTree(response.getBody());
            JsonNode pois = json.get("pois");

            if (pois != null && pois.isArray()) {
                for (JsonNode poi : pois) {
                    NearbyHospital hospital = new NearbyHospital();
                    hospital.setName(poi.get("name").asText());
                    hospital.setAddress(poi.get("address").asText());
                    hospital.setDistance(Double.parseDouble(poi.get("distance").asText()));

                    // 解析经纬度
                    String[] location = poi.get("location").asText().split(",");
                    hospital.setLongitude(Double.parseDouble(location[0]));
                    hospital.setLatitude(Double.parseDouble(location[1]));

                    // 从tel字段提取电话
                    String tel = poi.get("tel").asText();
                    hospital.setPhone(parseEmergencyPhone(tel));

                    result.add(hospital);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("高德地图接口调用失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 调用Python服务获取医院详细信息
     */
    private void enrichHospitalInfo(NearbyHospital hospital) {
        try {
            // 调用Python爬虫服务
            String url = UriComponentsBuilder
                    .fromHttpUrl(crawlerBaseUrl + "/api/hospital/info")
                    .queryParam("name", hospital.getName())
                    .queryParam("address", hospital.getAddress())
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = mapper.readTree(response.getBody());

            if (json.get("success").asBoolean()) {
                JsonNode data = json.get("data");
                // 如果高德没有电话，使用Python爬取的
                if (hospital.getPhone() == null || hospital.getPhone().isEmpty()) {
                    hospital.setPhone(data.get("emergency_phone").asText());
                }
                hospital.setEmergencyLink(data.get("appointment_link").asText());
                hospital.setType(data.get("type").asText());
                hospital.setIsOpen(data.get("is_24h").asBoolean());
            }
        } catch (Exception e) {
            // 爬虫失败不影响主流程，记录日志即可
            System.err.println("获取医院详情失败: " + hospital.getName() + ", " + e.getMessage());
        }
    }

    /**
     * 从tel字段解析急诊电话（优先选急诊相关）
     */
    private String parseEmergencyPhone(String tel) {
        if (tel == null || tel.isEmpty()) return null;

        // 多个电话用;分隔，优先返回第一个，或包含"急诊"的
        String[] phones = tel.split(";");
        for (String phone : phones) {
            if (phone.contains("急诊")) return phone.replace("急诊", "").trim();
        }
        return phones[0].trim();
    }
}