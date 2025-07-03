package org.whu.backend.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.repository.authRepo.UserRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户画像服务
 * 负责根据用户的行为，动态更新其兴趣标签和分数。
 * 为简单起见，我们将用户画像（一个JSON字符串）直接存储在User表的扩展字段中。
 */
@Service
@Slf4j
public class UserProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper; // Spring Boot自带的JSON工具

    /**
     * 更新用户画像的核心方法
     * @param userId 用户ID
     * @param tags   用户本次行为关联的标签集合
     * @param weight 本次行为的权重（如：下单+10分，收藏+5分）
     */
    @Transactional
    public void updateUserProfile(String userId, Set<Tag> tags, int weight) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        // 1. 获取用户现有的画像（JSON字符串）
        String profileJson = user.getInterestProfile();
        Map<String, Integer> profile;
        try {
            if (profileJson == null || profileJson.isBlank()) {
                profile = new HashMap<>();
            } else {
                profile = objectMapper.readValue(profileJson, new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.error("解析用户ID '{}' 的兴趣画像失败，将创建新画像。", userId, e);
            profile = new HashMap<>();
        }

        // 2. 更新分数
        for (Tag tag : tags) {
            profile.merge(tag.getName(), weight, Integer::sum);
        }

        // 3. 将更新后的画像写回数据库
        try {
            String updatedProfileJson = objectMapper.writeValueAsString(profile);
            user.setInterestProfile(updatedProfileJson);
            userRepository.save(user);
            log.info("成功更新用户ID '{}' 的兴趣画像，新增/更新了 {} 个标签的权重。", userId, tags.size());
        } catch (IOException e) {
            log.error("序列化用户ID '{}' 的新兴趣画像失败。", userId, e);
        }
    }
}