package com.imfundokahle.service;

import com.imfundokahle.model.Proposal;
import com.imfundokahle.model.ProposalStatus;
import com.imfundokahle.model.User;
import com.imfundokahle.repository.ProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de propuestas.
 */
@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;

    public ProposalService(ProposalRepository proposalRepository) {
        this.proposalRepository = proposalRepository;
    }

    @Transactional
    public Proposal save(Proposal proposal) {
        return proposalRepository.save(proposal);
    }

    public Proposal findById(Long id) {
        return proposalRepository.findById(id).orElse(null);
    }

    public List<Proposal> findAll() {
        return proposalRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Proposal> findByTeacher(User teacher) {
        return proposalRepository.findByTeacherOrderByCreatedAtDesc(teacher);
    }

    public List<Proposal> findByStatus(ProposalStatus status) {
        return proposalRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Proposal> findRecentPending() {
        return proposalRepository.findTop5ByStatusOrderByCreatedAtDesc(ProposalStatus.PENDING);
    }

    public long countByStatus(ProposalStatus status) {
        return proposalRepository.countByStatus(status);
    }

    public long countByTeacherAndStatus(User teacher, ProposalStatus status) {
        return proposalRepository.countByTeacherAndStatus(teacher, status);
    }

    /** Aprueba o rechaza una propuesta agregando un comentario del administrador. */
    @Transactional
    public void review(Long proposalId, ProposalStatus newStatus, String adminComment) {
        Proposal p = findById(proposalId);
        if (p != null) {
            p.setStatus(newStatus);
            p.setAdminComment(adminComment);
            proposalRepository.save(p);
        }
    }

    @Transactional
    public void delete(Long id) {
        proposalRepository.deleteById(id);
    }
}
