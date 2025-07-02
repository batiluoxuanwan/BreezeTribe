package org.whu.backend.service.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.whu.backend.dto.post.PostSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.entity.travelpac.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.association.PackageRoute;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.travelpost.TravelPost;

import java.util.ArrayList;
import java.util.List;

public class SearchSpecification {

    // 模糊查找旅游团
    public static Specification<TravelPackage> from(PackageSearchRequestDto searchDto) {
        return (Root<TravelPackage> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // --- [优化] 预先定义好所有可能用到的JOIN ---
            // 这样做可以避免在多个if块中重复创建JOIN，让SQL更高效
            Join<TravelPackage, PackageRoute> packageRouteJoin = root.join("routes", JoinType.LEFT);
            Join<PackageRoute, Route> routeJoin = packageRouteJoin.join("route", JoinType.LEFT);
            Join<Route, RouteSpot> routeSpotJoin = routeJoin.join("spots", JoinType.LEFT);
            Join<RouteSpot, Spot> spotJoin = routeSpotJoin.join("spot", JoinType.LEFT);


            // 1. 关键词模糊搜索 (现在复用预定义的JOIN)
            if (StringUtils.hasText(searchDto.getKeyword())) {
                String likePattern = "%" + searchDto.getKeyword() + "%";
                Predicate titleLike = cb.like(root.get("title"), likePattern);
                Predicate descriptionLike = cb.like(root.get("detailedDescription"), likePattern);
                Predicate routeNameLike = cb.like(routeJoin.get("name"), likePattern);
                Predicate routeDescriptionLike = cb.like(routeJoin.get("description"), likePattern);
                Predicate spotNameLike = cb.like(spotJoin.get("name"), likePattern);

                predicates.add(cb.or(titleLike, descriptionLike, routeNameLike, routeDescriptionLike, spotNameLike));
            }

            // 2. 价格区间搜索
            if (searchDto.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), searchDto.getMinPrice()));
            }
            if (searchDto.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), searchDto.getMaxPrice()));
            }

            // 3. 行程天数搜索
            if (searchDto.getMinDuration() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("durationInDays"), searchDto.getMinDuration()));
            }
            if (searchDto.getMaxDuration() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("durationInDays"), searchDto.getMaxDuration()));
            }

            // 4. 城市搜索 (复用预定义的JOIN)
            if (StringUtils.hasText(searchDto.getCity())) {
                String cityLikePattern = "%" + searchDto.getCity() + "%";
                predicates.add(cb.like(spotJoin.get("city"), cityLikePattern));
            }

            // 将所有条件用 AND 连接起来，并去重
            // 眼不见为净 :)
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 模糊查找游记
    public static Specification<TravelPost> from(PostSearchRequestDto searchDto) {
        return (Root<TravelPost> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // --- 预先定义好所有可能用到的JOIN ---
            Join<TravelPost, User> authorJoin = root.join("author", JoinType.LEFT);
            Join<TravelPost, Spot> spotJoin = root.join("spot", JoinType.LEFT);
            Join<TravelPost, Tag> tagJoin = root.join("tags", JoinType.LEFT);

            // 1. 关键词模糊搜索
            if (StringUtils.hasText(searchDto.getKeyword())) {
                String likePattern = "%" + searchDto.getKeyword() + "%";

                // 匹配游记标题
                Predicate titleLike = cb.like(root.get("title"), likePattern);
                // 匹配游记内容
                Predicate contentLike = cb.like(root.get("content"), likePattern);
                // 匹配作者用户名
                Predicate authorNameLike = cb.like(authorJoin.get("username"), likePattern);
                // 匹配景点名
                Predicate spotNameLike = cb.like(spotJoin.get("name"), likePattern);
                // 匹配城市名
                Predicate cityNameLike = cb.like(spotJoin.get("city"), likePattern);

                // 将所有模糊搜索条件用 OR 连接起来
                predicates.add(cb.or(titleLike, contentLike, authorNameLike, spotNameLike, cityNameLike));
            }

            // 2. 按标签名精确搜索
            if (StringUtils.hasText(searchDto.getTagName())) {
                predicates.add(cb.equal(tagJoin.get("name"), searchDto.getTagName()));
            }

            // 将所有条件用 AND 连接起来，并去重
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}