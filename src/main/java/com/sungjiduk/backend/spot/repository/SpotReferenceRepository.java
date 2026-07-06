package com.sungjiduk.backend.spot.repository;

import com.sungjiduk.backend.spot.entity.PilgrimageSpot;
import com.sungjiduk.backend.spot.entity.SpotReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpotReferenceRepository extends JpaRepository<SpotReference, Long> {

    Optional<SpotReference> findBySpotAndSourceName(PilgrimageSpot spot, String sourceName);

    List<SpotReference> findBySpotInAndSourceName(Collection<PilgrimageSpot> spots, String sourceName);
}
