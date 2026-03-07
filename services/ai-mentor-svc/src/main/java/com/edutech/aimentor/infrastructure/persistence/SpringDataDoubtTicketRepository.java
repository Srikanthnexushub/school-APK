package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.DoubtTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataDoubtTicketRepository extends JpaRepository<DoubtTicket, UUID> {

    Optional<DoubtTicket> findByIdAndStudentIdAndDeletedAtIsNull(UUID id, UUID studentId);

    Optional<DoubtTicket> findByIdAndDeletedAtIsNull(UUID id);

    List<DoubtTicket> findAllByStudentIdAndDeletedAtIsNull(UUID studentId);
}
