package com.imfundokahle.repository;

import com.imfundokahle.model.Language;
import com.imfundokahle.model.Material;
import com.imfundokahle.model.MaterialCategory;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio de materiales. */
public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByTeacherOrderByCreatedAtDesc(User teacher);
    List<Material> findByLanguage(Language language);
    List<Material> findByCategory(MaterialCategory category);
    List<Material> findAllByOrderByCreatedAtDesc();
    long countByTeacher(User teacher);

    @org.springframework.data.jpa.repository.Query(
        "SELECT m FROM Material m " +
        "WHERE (m.schedule IS NULL AND m.visibility != 'TEACHERS_ONLY') " +
        "   OR m.schedule IN (SELECT ce.schedule FROM ClassEnrollment ce WHERE ce.student = :student AND ce.status = 'ACTIVE') " +
        "ORDER BY m.createdAt DESC"
    )
    List<Material> findVisibleToStudent(@org.springframework.data.repository.query.Param("student") User student);
}
