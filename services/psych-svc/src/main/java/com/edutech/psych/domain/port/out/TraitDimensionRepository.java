package com.edutech.psych.domain.port.out;

import com.edutech.psych.domain.model.TraitDimension;

import java.util.List;
import java.util.Optional;

public interface TraitDimensionRepository {

    List<TraitDimension> findAll();

    Optional<TraitDimension> findByCode(String code);
}
