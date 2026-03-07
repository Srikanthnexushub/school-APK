package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.TraitDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTraitDimensionRepository extends JpaRepository<TraitDimension, UUID> {

    Optional<TraitDimension> findByCode(String code);
}
