package com.imfundokahle.repository;

import com.imfundokahle.model.Proposal;
import com.imfundokahle.model.ProposalStatus;
import com.imfundokahle.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Repositorio de propuestas. */
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByTeacherOrderByCreatedAtDesc(User teacher);
    List<Proposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);
    List<Proposal> findAllByOrderByCreatedAtDesc();
    List<Proposal> findTop5ByStatusOrderByCreatedAtDesc(ProposalStatus status);
    long countByStatus(ProposalStatus status);
    long countByTeacherAndStatus(User teacher, ProposalStatus status);
}
