package com.hospital.controller;

import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.service.DepartmentService;
import com.hospital.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private com.hospital.service.CatalogService catalogService;

    /**
     * 统一搜索：医院名/别名、科室名、医生姓名/科室/擅长、公告标题。
     * type=all|hospital|department|doctor|article；分页沿用 page/size/total/hasMore。
     * 空关键词或空结果返回 200 + 空列表。
     */
    @GetMapping
    public Map<String, Object> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return catalogService.search(keyword, type, hospitalId, page, size);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
            result.put("departments", new ArrayList<>());
            result.put("doctors", new ArrayList<>());
            result.put("totalCount", 0);
            result.put("total", 0);
            result.put("hasMore", false);
            return result;
        }
    }

    @GetMapping("/smart")
    public Map<String, Object> smartSearch(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("智能搜索关键词: " + keyword);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请输入搜索关键词");
                result.put("results", new ArrayList<>());
                result.put("totalCount", 0);
                return result;
            }

            List<Map<String, Object>> deptResults = departmentService.searchWithScore(keyword);
            List<Map<String, Object>> doctorResults = doctorService.searchWithScore(keyword);
            
            List<Map<String, Object>> allResults = new ArrayList<>();
            allResults.addAll(deptResults);
            allResults.addAll(doctorResults);
            
            allResults.sort((a, b) -> (Integer)b.get("score") - (Integer)a.get("score"));

            result.put("success", true);
            result.put("results", allResults);
            result.put("totalCount", allResults.size());
            result.put("keyword", keyword);

        } catch (Exception e) {
            System.out.println("智能搜索异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("results", new ArrayList<>());
            result.put("totalCount", 0);
        }

        return result;
    }

    @GetMapping("/type/{type}")
    public Map<String, Object> searchByType(@PathVariable String type, @RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("分类搜索 - 类型: " + type + ", 关键词: " + keyword);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请输入搜索关键词");
                result.put("data", new ArrayList<>());
                return result;
            }

            switch (type) {
                case "department":
                    List<Department> departments = departmentService.search(keyword);
                    result.put("data", departments);
                    result.put("type", "department");
                    break;
                case "doctor":
                    List<Doctor> doctors = doctorService.search(keyword);
                    result.put("data", doctors);
                    result.put("type", "doctor");
                    break;
                case "expertise":
                    List<Doctor> doctorsByExpertise = doctorService.searchByExpertise(keyword);
                    result.put("data", doctorsByExpertise);
                    result.put("type", "expertise");
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "不支持的搜索类型: " + type);
                    return result;
            }

            result.put("success", true);

        } catch (Exception e) {
            System.out.println("分类搜索异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    @GetMapping("/suggestions")
    public Map<String, Object> getSuggestions(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("搜索建议 - 关键词: " + keyword);
            
            if (keyword == null || keyword.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请输入搜索关键词");
                result.put("suggestions", new ArrayList<>());
                return result;
            }

            List<Map<String, String>> deptSuggestions = departmentService.getSuggestions(keyword);
            List<Map<String, String>> doctorSuggestions = doctorService.getSuggestions(keyword);
            
            List<Map<String, String>> allSuggestions = new ArrayList<>();
            allSuggestions.addAll(deptSuggestions);
            allSuggestions.addAll(doctorSuggestions);

            result.put("success", true);
            result.put("suggestions", allSuggestions);
            result.put("keyword", keyword);

        } catch (Exception e) {
            System.out.println("搜索建议异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取建议失败: " + e.getMessage());
            result.put("suggestions", new ArrayList<>());
        }

        return result;
    }

    @GetMapping("/hot")
    public Map<String, Object> getHotSearches() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> hotSearches = new ArrayList<>();
            
            hotSearches.add(createHotSearch("内科", "最热门科室"));
            hotSearches.add(createHotSearch("外科", "热门科室"));
            hotSearches.add(createHotSearch("儿科", "热门科室"));
            hotSearches.add(createHotSearch("张建国", "知名医生"));
            hotSearches.add(createHotSearch("心血管", "热门疾病"));
            hotSearches.add(createHotSearch("感冒", "常见病"));
            hotSearches.add(createHotSearch("皮肤科", "热门科室"));
            hotSearches.add(createHotSearch("口腔科", "热门科室"));

            result.put("success", true);
            result.put("hotSearches", hotSearches);

        } catch (Exception e) {
            System.out.println("热门搜索异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "获取热门搜索失败: " + e.getMessage());
            result.put("hotSearches", new ArrayList<>());
        }

        return result;
    }

    @GetMapping("/departments")
    public Map<String, Object> searchDepartments(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Department> departments = departmentService.search(keyword);
            result.put("success", true);
            result.put("data", departments);
            result.put("total", departments.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    @GetMapping("/doctors")
    public Map<String, Object> searchDoctors(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Doctor> doctors = doctorService.search(keyword);
            result.put("success", true);
            result.put("data", doctors);
            result.put("total", doctors.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    @GetMapping("/doctors/expertise")
    public Map<String, Object> searchDoctorsByExpertise(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Doctor> doctors = doctorService.searchByExpertise(keyword);
            result.put("success", true);
            result.put("data", doctors);
            result.put("total", doctors.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }

        return result;
    }

    private Map<String, Object> createHotSearch(String keyword, String category) {
        Map<String, Object> item = new HashMap<>();
        item.put("keyword", keyword);
        item.put("category", category);
        return item;
    }
}
