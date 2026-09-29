package com.imfundokahle.repository;

import com.imfundokahle.model.ContentText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentTextRepository extends JpaRepository<ContentText, Long> {
    Optional<ContentText> findByMessageKey(String messageKey);
    List<ContentText> findAllByMessageKeyIn(List<String> messageKeys);
}
