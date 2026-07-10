package com.sungjiduk.backend.trip.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sungjiduk.backend.trip.entity.TripStop;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {
    @Query(
        """
        SELECT s.pilgrimageSpotId
        FROM TripStop s
        WHERE s.createdAt BETWEEN :start AND :end
        GROUP BY s.pilgrimageSpotId
        ORDER BY COUNT(s.pilgrimageSpotId) DESC""")
    List<Long> findMostFrequentSpotToday(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    long countAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByPilgrimageSpotIdAndCreatedAtBetween(Long pilgrimageSpotId, LocalDateTime start, LocalDateTime end);
}
