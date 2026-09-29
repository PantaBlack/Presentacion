package com.imfundokahle.repository;

import com.imfundokahle.model.ContentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentImageRepository extends JpaRepository<ContentImage, Long> {
    Optional<ContentImage> findByImageKey(String imageKey);
    boolean existsByImageKey(String imageKey);
}
