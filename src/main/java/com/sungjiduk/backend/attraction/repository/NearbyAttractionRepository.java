package com.sungjiduk.backend.attraction.repository;

import com.sungjiduk.backend.attraction.entity.NearbyAttraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NearbyAttractionRepository extends JpaRepository<NearbyAttraction, Long> {

    Optional<NearbyAttraction> findByMapsUrl(String mapsUrl);
}
