package com.sungjiduk.backend.spot.repository;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.content.entity.Content;
import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PilgrimageSpotRepository extends JpaRepository<PilgrimageSpot, Long> {

    Optional<PilgrimageSpot> findByContentAndName(Content content, String name);

    List<PilgrimageSpot> findByContentOrderByIdAsc(Content content);

    default PilgrimageSpot findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
    }
}
