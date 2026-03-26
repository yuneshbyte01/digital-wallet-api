package com.yunesh.digitalwallet.statement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    List<Statement> findByUserId(UUID userId);

    Optional<Statement> findByUserIdAndMonthAndYear(UUID userId,
                                                    int month,
                                                    int year);
}