package org.whu.backend.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;
import org.whu.backend.service.DtoConverter;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐服务
 * 负责根据用户画像，计算并生成个性化推荐列表。
 */
@Service
@Slf4j
public class RecommendationService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TravelPackageRepository packageRepository;
    @Autowired
    private TravelPostRepository postRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DtoConverter dtoConverter; // 注入通用的DTO转换器

    public static final int MAX_SIZE = 10;

    /**
     * 【新增】为指定用户推荐【旅游团】
     * @param userId 当前登录用户的ID
     * @return 推荐的旅游团摘要DTO列表
     */
    @Transactional(readOnly = true)
    public List<PackageSummaryDto> getRecommendedPackages(String userId) {
        log.info("服务层：开始为用户ID '{}' 生成个性化【旅游团】推荐...", userId);
        Map<String, Integer> userProfile = getUserProfile(userId);

        if (userProfile.isEmpty()) {
            log.info("服务层：用户ID '{}' 画像为空，返回通用热门旅游团。", userId);
            return getGenericPackageRecommendations();
        }

        List<TravelPackage> candidates = packageRepository.findAll();
        Map<TravelPackage, Double> itemScores = new HashMap<>();
        calculateScoresForItems(candidates, userProfile, itemScores, TravelPackage::getTags);

        return itemScores.entrySet().stream()
                .sorted(Map.Entry.<TravelPackage, Double>comparingByValue().reversed())
                .limit(MAX_SIZE) // 推荐旅游团数量
                .map(entry -> dtoConverter.convertPackageToSummaryDto(entry.getKey())) // 使用DtoConverter转换
                .collect(Collectors.toList());
    }

    /**
     * 【新增】为指定用户推荐【游记】
     * @param userId 当前登录用户的ID
     * @return 推荐的游记摘要DTO列表
     */
    @Transactional(readOnly = true)
    public List<PostSummaryDto> getRecommendedPosts(String userId) {
        log.info("服务层：开始为用户ID '{}' 生成个性化【游记】推荐...", userId);
        Map<String, Integer> userProfile = getUserProfile(userId);

        if (userProfile.isEmpty()) {
            log.info("服务层：用户ID '{}' 画像为空，返回通用热门游记。", userId);
            return getGenericPostRecommendations();
        }

        List<TravelPost> candidates = postRepository.findAll();
        Map<TravelPost, Double> itemScores = new HashMap<>();
        calculateScoresForItems(candidates, userProfile, itemScores, TravelPost::getTags);

        return itemScores.entrySet().stream()
                .sorted(Map.Entry.<TravelPost, Double>comparingByValue().reversed())
                .limit(10) // 假设推荐10篇游记
                .map(entry -> dtoConverter.convertPostToSummaryDto(entry.getKey())) // 使用DtoConverter转换
                .collect(Collectors.toList());
    }

    /**
     * 私有辅助方法：获取并解析用户画像
     */
    private Map<String, Integer> getUserProfile(String userId) {
        return userRepository.findById(userId)
                .map(User::getInterestProfile)
                .filter(profileJson -> !profileJson.isBlank())
                .map(profileJson -> {
                    try {
                        return objectMapper.readValue(profileJson, new TypeReference<Map<String, Integer>>() {});
                    } catch (IOException e) {
                        log.error("服务层：解析用户ID '{}' 的兴趣画像JSON失败。", userId, e);
                        return Collections.<String, Integer>emptyMap();
                    }
                })
                .orElse(Collections.emptyMap());
    }

    /**
     * 私有辅助方法：计算分数的核心逻辑 (保持通用性)
     */
    private <T> void calculateScoresForItems(List<T> items, Map<String, Integer> userProfile, Map<T, Double> itemScores, Function<T, Set<Tag>> tagExtractor) {
        for (T item : items) {
            double score = 0.0;
            Set<Tag> tags = tagExtractor.apply(item);
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            for (Tag tag : tags) {
                score += userProfile.getOrDefault(tag.getName(), 0);
            }
            if (score > 0) {
                itemScores.put(item, score);
            }
        }
    }

    /**
     * 降级策略：返回通用热门【旅游团】
     */
    public List<PackageSummaryDto> getGenericPackageRecommendations() {
        return packageRepository.findTop10ByOrderByCreatedTimeDesc().stream()
                .map(dtoConverter::convertPackageToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 降级策略：返回通用热门【游记】
     */
    public List<PostSummaryDto> getGenericPostRecommendations() {
        return postRepository.findTop10ByOrderByCreatedTimeDesc().stream()
                .map(dtoConverter::convertPostToSummaryDto)
                .collect(Collectors.toList());
    }
}