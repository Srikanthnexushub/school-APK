package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.DoubtTicket;
import com.edutech.aimentor.domain.port.out.DoubtTicketRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DoubtTicketPersistenceAdapter implements DoubtTicketRepository {

    private final SpringDataDoubtTicketRepository doubtTicketRepo;

    public DoubtTicketPersistenceAdapter(SpringDataDoubtTicketRepository doubtTicketRepo) {
        this.doubtTicketRepo = doubtTicketRepo;
    }

    @Override
    public DoubtTicket save(DoubtTicket doubtTicket) {
        return doubtTicketRepo.save(doubtTicket);
    }

    @Override
    public Optional<DoubtTicket> findByIdAndStudentId(UUID id, UUID studentId) {
        return doubtTicketRepo.findByIdAndStudentIdAndDeletedAtIsNull(id, studentId);
    }

    @Override
    public Optional<DoubtTicket> findById(UUID id) {
        return doubtTicketRepo.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<DoubtTicket> findAllByStudentId(UUID studentId) {
        return doubtTicketRepo.findAllByStudentIdAndDeletedAtIsNull(studentId);
    }
}
