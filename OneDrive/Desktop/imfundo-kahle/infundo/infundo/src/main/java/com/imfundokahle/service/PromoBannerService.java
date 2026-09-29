package com.imfundokahle.service;

import com.imfundokahle.model.PromoBannerConfig;
import com.imfundokahle.model.PromoTema;
import com.imfundokahle.repository.PromoBannerConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromoBannerService {

    private final PromoBannerConfigRepository repository;

    public PromoBannerService(PromoBannerConfigRepository repository) {
        this.repository = repository;
    }

    public PromoTema getTema() {
        return repository.findAll().stream()
                .findFirst()
                .map(PromoBannerConfig::getTema)
                .orElse(PromoTema.NORMAL);
    }

    @Transactional
    public void setTema(PromoTema tema) {
        PromoBannerConfig config = repository.findAll().stream().findFirst()
                .orElseGet(PromoBannerConfig::new);
        config.setTema(tema);
        repository.save(config);
    }
}
