package org.whu.backend.repository.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.whu.backend.entity.travelpost.PostImage;

import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    boolean existsByMediaFileId(String fileId);

    Optional<PostImage> findTopByMediaFileId(String fileId);
}