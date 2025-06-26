package org.whu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.PostImage;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}