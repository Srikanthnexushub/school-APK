package com.edutech.aimentor.domain.port.out;

import com.edutech.aimentor.domain.model.DoubtTicket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoubtTicketRepository {

    DoubtTicket save(DoubtTicket doubtTicket);

    Optional<DoubtTicket> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<DoubtTicket> findById(UUID id);

    List<DoubtTicket> findAllByStudentId(UUID studentId);
}
