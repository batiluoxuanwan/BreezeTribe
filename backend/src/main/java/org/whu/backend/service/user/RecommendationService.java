package org.whu.backend.service.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.common.exception.BizException;
import org.whu.backend.dto.post.PostSummaryDto;
import org.whu.backend.dto.recommend.RecommendRequestDto;
import org.whu.backend.dto.travelpack.PackageSummaryDto;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.travelpost.TravelPost;
import org.whu.backend.repository.authRepo.UserRepository;
import org.whu.backend.repository.post.TravelPostRepository;
import org.whu.backend.repository.travelRepo.TravelOrderRepository;
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
    private TravelOrderRepository travelOrderRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DtoConverter dtoConverter; // 注入通用的DTO转换器

//    public static final int MAX_SIZE = 3;
//    public static final int CANDIDATES_NUM = 8;

    /**
     * 【新增】为指定用户推荐【旅游团】
     *
     * @param userId 当前登录用户的ID
     * @return 推荐的旅游团摘要DTO列表
     */
    @Transactional(readOnly = true)
    public List<PackageSummaryDto> getRecommendedPackages(String userId, RecommendRequestDto requestDto) {
        log.info("服务层：开始为用户ID '{}' 生成个性化【旅游团】推荐...", userId);
        if (requestDto.getTotalNum() < requestDto.getRecommendNum()) {
            throw new BizException("推荐数量不能小于总数");
        }
        Map<String, Integer> userProfile = getUserProfile(userId);

        if (userProfile.isEmpty()) {
            log.info("服务层：用户ID '{}' 画像为空，返回通用热门旅游团。", userId);
            return getGenericPackageRecommendations(requestDto);
        }

        // 1. 获取用户已购买过的产品ID，用于后续排除
        Set<String> purchasedPackageIds = travelOrderRepository.findPackageIdsByUserId(userId);
        log.info("服务层：用户 '{}' 已购买过 {} 个产品，将在推荐中排除。", userId, purchasedPackageIds.size());

        // 2. 获取候选内容，并排除已购买的
        List<TravelPackage> candidates = purchasedPackageIds.isEmpty()
                ? packageRepository.findByStatus(TravelPackage.PackageStatus.PUBLISHED)
                : packageRepository.findByStatusAndIdNotIn(TravelPackage.PackageStatus.PUBLISHED, purchasedPackageIds);

        // 3. 计算分数
        Map<TravelPackage, Double> itemScores = new HashMap<>();
        calculateScoresForItems(candidates, userProfile, itemScores, TravelPackage::getTags);

        // 4. 【新增】多样性与随机性处理
        List<TravelPackage> topCandidates = itemScores.entrySet().stream()
                .sorted(Map.Entry.<TravelPackage, Double>comparingByValue().reversed())
                .limit(requestDto.getTotalNum()) // 先取出排名靠前的若干个作为“优质候选池”
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Collections.shuffle(topCandidates); // 将优质候选池打乱，增加随机性

        // 5. 取最终结果并转换
        return topCandidates.stream()
                .limit(requestDto.getRecommendNum()) // 从打乱的池中取最终的10个
                .map(dtoConverter::convertPackageToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 【新增】为指定用户推荐【游记】
     *
     * @param userId 当前登录用户的ID
     * @return 推荐的游记摘要DTO列表
     */
    @Transactional(readOnly = true)
    public List<PostSummaryDto> getRecommendedPosts(String userId, RecommendRequestDto requestDto) {
        log.info("服务层：开始为用户ID '{}' 生成个性化【游记】推荐...", userId);
        Map<String, Integer> userProfile = getUserProfile(userId);

        if (userProfile.isEmpty()) {
            log.info("服务层：用户ID '{}' 画像为空，返回通用热门游记。", userId);
            return getGenericPostRecommendations(requestDto);
        }

        // TODO: 游记推荐也可以增加排除逻辑，例如排除用户自己发布的、或已点赞/收藏的
        List<TravelPost> candidates = postRepository.findAll();
        Map<TravelPost, Double> itemScores = new HashMap<>();
        calculateScoresForItems(candidates, userProfile, itemScores, TravelPost::getTags);

        // 增加多样性处理
        List<TravelPost> topCandidates = itemScores.entrySet().stream()
                .sorted(Map.Entry.<TravelPost, Double>comparingByValue().reversed())
                .limit(requestDto.getTotalNum())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Collections.shuffle(topCandidates);

        return topCandidates.stream()
                .limit(requestDto.getRecommendNum())
                .map(dtoConverter::convertPostToSummaryDto)
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
                        return objectMapper.readValue(profileJson, new TypeReference<>() {
                        });
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
     * 【核心重构】降级策略：返回基于隐藏分和随机性的【旅游团】
     */
    public List<PackageSummaryDto> getGenericPackageRecommendations(RecommendRequestDto requestDto) {
        // 1. 获取所有状态为“已发布”的旅游团，并按隐藏分从高到低排序
        List<TravelPackage> allPublishedPackages = packageRepository.findAllByStatusOrderByHiddenScoreDesc(TravelPackage.PackageStatus.PUBLISHED);

        // 2. 从排序后的列表中，取出前N个作为“优质候选池”
        int poolSize = Math.min(allPublishedPackages.size(), requestDto.getTotalNum());
        List<TravelPackage> hotPool = new ArrayList<>(allPublishedPackages.subList(0, poolSize));

        // 3. 将这个优质候选池的顺序完全打乱
        Collections.shuffle(hotPool);

        // 4. 从打乱后的池中，取出最终推荐的个数
        int finalSize = Math.min(hotPool.size(), requestDto.getRecommendNum());
        return hotPool.stream()
                .limit(finalSize)
                .map(dtoConverter::convertPackageToSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * 【已重构】降级策略：返回通用热门【游记】
     */
    public List<PostSummaryDto> getGenericPostRecommendations(RecommendRequestDto requestDto) {
        // 游记的通用推荐也可以采用类似的随机策略，例如按浏览量排序
        List<TravelPost> allPosts = postRepository.findTop100ByOrderByViewCountDesc();

        int poolSize = Math.min(allPosts.size(), requestDto.getTotalNum());
        List<TravelPost> hotPool = new ArrayList<>(allPosts.subList(0, poolSize));

        Collections.shuffle(hotPool);

        int finalSize = Math.min(hotPool.size(), requestDto.getRecommendNum());
        return hotPool.stream()
                .limit(finalSize)
                .map(dtoConverter::convertPostToSummaryDto)
                .collect(Collectors.toList());
    }
}