//package org.whu.backend.service.admin;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.whu.backend.dto.recommendation.RecommendationItemDto;
//import org.whu.backend.entity.FeaturedItem;
//import org.whu.backend.repository.FeaturedItemRepository;
//import org.whu.backend.repository.post.TravelPostRepository;
//import org.whu.backend.repository.travelRepo.TravelPackageRepository;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
///**
// * 【新增】管理员推流的服务
// */
//@Service
//@Slf4j
//public class AdminPushService {
//    @Autowired
//    private FeaturedItemRepository featuredItemRepository;
//    @Autowired
//    private TravelPackageRepository packageRepository;
//    @Autowired
//    private TravelPostRepository postRepository;
//
//    // 在这里可以为管理员实现对 FeaturedItem 的增删改查
//    // ...
//
//    /**
//     * 获取所有官方推荐内容，并转换为可展示的DTO
//     * @return 推荐项目DTO列表
//     */
//    @Transactional(readOnly = true)
//    public List<RecommendationItemDto> getFeaturedItemsForDisplay() {
//        List<FeaturedItem> featuredItems = featuredItemRepository.findAllByOrderByPriorityDesc();
//
//        return featuredItems.stream().map(item -> {
//            if (item.getItemType() == FeaturedItem.ItemType.PACKAGE) {
//                return packageRepository.findById(item.getItemId())
//                        .map(pkg -> new RecommendationItemDto(pkg.getId(), pkg.getTitle(), pkg.getCoverImageUrl(), "PACKAGE", "官方推荐"))
//                        .orElse(null);
//            } else if (item.getItemType() == FeaturedItem.ItemType.POST) {
//                return postRepository.findById(item.getItemId())
//                        .map(post -> new RecommendationItemDto(post.getId(), post.getTitle(), post.getCoverImageUrl(), "POST", "官方推荐"))
//                        .orElse(null);
//            }
//            return null;
//        }).filter(Objects::nonNull).collect(Collectors.toList());
//    }
//}