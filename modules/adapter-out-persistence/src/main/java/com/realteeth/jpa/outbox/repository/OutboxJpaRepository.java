package com.realteeth.jpa.outbox.repository;

import com.realteeth.jpa.outbox.entity.OutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, Long>, OutboxJpaQueryRepository {
}
