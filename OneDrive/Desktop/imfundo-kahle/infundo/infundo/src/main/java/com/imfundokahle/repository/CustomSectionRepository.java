package com.imfundokahle.repository;

import com.imfundokahle.model.CustomSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomSectionRepository extends JpaRepository<CustomSection, Long> {
    List<CustomSection> findAllByOrderByOrdenAsc();
    List<CustomSection> findByVisibleTrueOrderByOrdenAsc();
}
