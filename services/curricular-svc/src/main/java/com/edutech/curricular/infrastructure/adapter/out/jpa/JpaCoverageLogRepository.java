package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.CoverageLog;
import com.edutech.curricular.domain.port.out.CoverageLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaCoverageLogRepository extends JpaRepository<CoverageLog, UUID>, CoverageLogRepository {

    @Override
    List<CoverageLog> findByTeacherId(UUID teacherId);

    @Override
    List<CoverageLog> findByCenterIdAndTopicId(UUID centerId, UUID topicId);

    @Override
    List<CoverageLog> findByCenterId(UUID centerId);

    @Override
    @Query("SELECT cl FROM CoverageLog cl WHERE cl.topicId IN :topicIds")
    List<CoverageLog> findByTopicIdIn(@Param("topicIds") List<UUID> topicIds);
}
