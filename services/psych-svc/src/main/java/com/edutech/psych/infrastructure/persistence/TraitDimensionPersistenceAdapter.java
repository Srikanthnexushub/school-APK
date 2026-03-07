package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.TraitDimension;
import com.edutech.psych.domain.port.out.TraitDimensionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TraitDimensionPersistenceAdapter implements TraitDimensionRepository {

    private final SpringDataTraitDimensionRepository springData;

    public TraitDimensionPersistenceAdapter(SpringDataTraitDimensionRepository springData) {
        this.springData = springData;
    }

    @Override
    public List<TraitDimension> findAll() {
        return springData.findAll();
    }

    @Override
    public Optional<TraitDimension> findByCode(String code) {
        return springData.findByCode(code);
    }

}
