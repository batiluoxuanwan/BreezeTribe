package org.whu.backend.service.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.whu.backend.dto.post.PostSearchRequestDto;
import org.whu.backend.dto.travelpack.PackageSearchRequestDto;
import org.whu.backend.entity.travelpac.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.accounts.User;
import org.whu.backend.entity.association.PackageRoute;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.travelpost.TravelPost;

import java.util.ArrayList;
import java.util.List;

public class SearchSpecification {

    /**
     * 【核心重构】从搜索DTO动态构建Specification
     */
    public static Specification<TravelPackage> from(PackageSearchRequestDto searchDto) {
        return (Root<TravelPackage> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // --- 条件1: 必须是已发布的旅行团 ---
            predicates.add(cb.equal(root.get("status"), TravelPackage.PackageStatus.PUBLISHED));

            // --- 条件2: 关键词模糊搜索 ---
            if (StringUtils.hasText(searchDto.getKeyword())) {
                // 【正确路径】一次性定义好所有需要的JOIN
                Join<TravelPackage, PackageRoute> packageRouteJoin = root.join("routes", JoinType.LEFT);
                Join<PackageRoute, Route> routeJoin = packageRouteJoin.join("route", JoinType.LEFT);
                Join<Route, RouteSpot> routeSpotJoin = routeJoin.join("spots", JoinType.LEFT);
                Join<RouteSpot, Spot> spotJoin = routeSpotJoin.join("spot", JoinType.LEFT);

                String likePattern = "%" + searchDto.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), likePattern),
                        cb.like(root.get("detailedDescription"), likePattern),
                        cb.like(routeJoin.get("name"), likePattern),      // 直接从routeJoin获取name
                        cb.like(spotJoin.get("name"), likePattern)        // 直接从spotJoin获取name
                ));
            }

            // --- 条件3: 【重大改变】价格区间搜索，使用子查询 ---
            if (searchDto.getMinPrice() != null || searchDto.getMaxPrice() != null) {
                // 创建一个子查询，查询TravelDeparture
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<TravelDeparture> departureRoot = subquery.from(TravelDeparture.class);
                subquery.select(departureRoot.get("id"));

                List<Predicate> subqueryPredicates = new ArrayList<>();
                // 关联条件：子查询的团期必须属于主查询的产品
                subqueryPredicates.add(cb.equal(departureRoot.get("travelPackage"), root));
                // 状态条件：团期必须是可报名的
                subqueryPredicates.add(cb.equal(departureRoot.get("status"), TravelDeparture.DepartureStatus.OPEN));

                // 价格条件
                if (searchDto.getMinPrice() != null) {
                    subqueryPredicates.add(cb.greaterThanOrEqualTo(departureRoot.get("price"), searchDto.getMinPrice()));
                }
                if (searchDto.getMaxPrice() != null) {
                    subqueryPredicates.add(cb.lessThanOrEqualTo(departureRoot.get("price"), searchDto.getMaxPrice()));
                }

                subquery.where(subqueryPredicates.toArray(new Predicate[0]));

                // 主查询条件：必须存在(EXISTS)满足子查询条件的团期
                predicates.add(cb.exists(subquery));
            }

            // --- 条件4: 行程天数搜索 (逻辑不变) ---
            if (searchDto.getMinDuration() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("durationInDays"), searchDto.getMinDuration()));
            }
            if (searchDto.getMaxDuration() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("durationInDays"), searchDto.getMaxDuration()));
            }

            // --- 条件5: 城市搜索 ---
            if (StringUtils.hasText(searchDto.getCity())) {
                // 【正确路径】
                Join<TravelPackage, PackageRoute> packageRouteJoin = root.join("routes", JoinType.LEFT);
                Join<PackageRoute, Route> routeJoin = packageRouteJoin.join("route", JoinType.LEFT);
                Join<Route, RouteSpot> routeSpotJoin = routeJoin.join("spots", JoinType.LEFT);
                Join<RouteSpot, Spot> spotJoin = routeSpotJoin.join("spot", JoinType.LEFT);

                predicates.add(cb.like(spotJoin.get("city"), "%" + searchDto.getCity() + "%"));
            }

            // 使用 distinct 避免因JOIN产生重复结果
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