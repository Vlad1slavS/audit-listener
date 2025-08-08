package io.github.auditlistener.repository;

import io.github.auditlistener.model.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCorrelationIdOrderByTimestamp(String correlationId);

}
