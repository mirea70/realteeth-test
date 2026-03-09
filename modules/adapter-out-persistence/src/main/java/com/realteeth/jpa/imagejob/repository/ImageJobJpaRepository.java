package com.realteeth.jpa.imagejob.repository;

import com.realteeth.jpa.imagejob.entity.ImageJobJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageJobJpaRepository extends JpaRepository<ImageJobJpaEntity, Long> {
}
