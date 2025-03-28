package com.interview.runningevents.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.interview.runningevents.application.model.PaginatedResult;
import com.interview.runningevents.application.model.RunningEventQuery;
import com.interview.runningevents.application.model.SortDirection;
import com.interview.runningevents.domain.model.RunningEvent;

public class RunningEventRepositoryImplTest {

    private RunningEventJpaRepository jpaRepository;
    private RunningEventMapperImpl mapper;
    private RunningEventRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(RunningEventJpaRepository.class);
        mapper = mock(RunningEventMapperImpl.class);
        repository = new RunningEventRepositoryImpl(jpaRepository, mapper);
    }

    @Test
    void shouldSaveRunningEvent() {
        // Given
        Long futureTime = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli();

        RunningEvent domainEvent = RunningEvent.builder()
                .name("Test Event")
                .dateTime(futureTime)
                .location("Test Location")
                .build();

        RunningEventEntity entity = RunningEventEntity.builder()
                .name("Test Event")
                .dateTime(futureTime)
                .location("Test Location")
                .build();

        RunningEventEntity savedEntity = RunningEventEntity.builder()
                .id(1L)
                .name("Test Event")
                .dateTime(futureTime)
                .location("Test Location")
                .build();

        RunningEvent savedDomainEvent = RunningEvent.builder()
                .id(1L)
                .name("Test Event")
                .dateTime(futureTime)
                .location("Test Location")
                .build();

        when(mapper.toEntity(domainEvent)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomainEvent);

        // When
        RunningEvent result = repository.save(domainEvent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(mapper, times(1)).toEntity(domainEvent);
        verify(jpaRepository, times(1)).save(entity);
        verify(mapper, times(1)).toDomain(savedEntity);
    }

    @Test
    void shouldThrowExceptionWhenSavingNullEvent() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        verify(jpaRepository, never()).save(any());
    }

    @Test
    void shouldFindRunningEventById() {
        // Given
        Long eventId = 1L;
        RunningEventEntity entity = RunningEventEntity.builder()
                .id(eventId)
                .name("Test Event")
                .dateTime(Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli())
                .location("Test Location")
                .build();

        RunningEvent domainEvent = RunningEvent.builder()
                .id(eventId)
                .name("Test Event")
                .dateTime(Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli())
                .location("Test Location")
                .build();

        when(jpaRepository.findById(eventId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainEvent);

        // When
        Optional<RunningEvent> result = repository.findById(eventId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(eventId);
        assertThat(result.get().getName()).isEqualTo("Test Event");

        verify(jpaRepository, times(1)).findById(eventId);
        verify(mapper, times(1)).toDomain(entity);
    }

    @Test
    void shouldReturnEmptyOptionalWhenEventNotFound() {
        // Given
        when(jpaRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<RunningEvent> result = repository.findById(99L);

        // Then
        assertThat(result).isEmpty();
        verify(jpaRepository, times(1)).findById(99L);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void shouldThrowExceptionWhenFindingByNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));
        verify(jpaRepository, never()).findById(any());
    }

    @Test
    void shouldFindAllRunningEventsAscendingOrder() {
        // Given
        RunningEventQuery query = RunningEventQuery.builder()
                .page(0)
                .pageSize(10)
                .sortDirection(SortDirection.ASC)
                .build();

        RunningEventEntity entity1 = RunningEventEntity.builder()
                .id(1L)
                .name("Event 1")
                .dateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 1")
                .build();

        RunningEventEntity entity2 = RunningEventEntity.builder()
                .id(2L)
                .name("Event 2")
                .dateTime(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 2")
                .build();

        RunningEvent domainEvent1 = RunningEvent.builder()
                .id(1L)
                .name("Event 1")
                .dateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 1")
                .build();

        RunningEvent domainEvent2 = RunningEvent.builder()
                .id(2L)
                .name("Event 2")
                .dateTime(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 2")
                .build();

        List<RunningEventEntity> entities = List.of(entity1, entity2);
        Page<RunningEventEntity> page = new PageImpl<>(entities, PageRequest.of(0, 10), 2);

        when(jpaRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toDomain(entity1)).thenReturn(domainEvent1);
        when(mapper.toDomain(entity2)).thenReturn(domainEvent2);

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalItems()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);

        // Verify that the correct sort parameters were passed to the repository
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository, times(1)).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime").getDirection())
                .isEqualTo(Sort.Direction.ASC);

        verify(mapper, times(1)).toDomain(entity1);
        verify(mapper, times(1)).toDomain(entity2);
    }

    @Test
    void shouldFindAllRunningEventsDescendingOrder() {
        // Given
        RunningEventQuery query = RunningEventQuery.builder()
                .page(0)
                .pageSize(10)
                .sortDirection(SortDirection.DESC)
                .build();

        RunningEventEntity entity1 = RunningEventEntity.builder()
                .id(1L)
                .name("Event 1")
                .dateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 1")
                .build();

        RunningEventEntity entity2 = RunningEventEntity.builder()
                .id(2L)
                .name("Event 2")
                .dateTime(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 2")
                .build();

        RunningEvent domainEvent1 = RunningEvent.builder()
                .id(1L)
                .name("Event 1")
                .dateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 1")
                .build();

        RunningEvent domainEvent2 = RunningEvent.builder()
                .id(2L)
                .name("Event 2")
                .dateTime(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 2")
                .build();

        // For the descending order test, we'd typically reverse the order of entities
        // but since we're just mocking the repository call, we can use the same list
        List<RunningEventEntity> entities = List.of(entity1, entity2);
        Page<RunningEventEntity> page = new PageImpl<>(entities, PageRequest.of(0, 10), 2);

        when(jpaRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toDomain(entity1)).thenReturn(domainEvent1);
        when(mapper.toDomain(entity2)).thenReturn(domainEvent2);

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotalItems()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(10);

        // Verify that the correct sort parameters were passed to the repository
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository, times(1)).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime").getDirection())
                .isEqualTo(Sort.Direction.DESC);

        verify(mapper, times(1)).toDomain(entity1);
        verify(mapper, times(1)).toDomain(entity2);
    }

    @Test
    void shouldFindRunningEventsWithDateFilterAscendingOrder() {
        // Given
        Long fromDate = Instant.now().toEpochMilli();
        Long toDate = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli();

        RunningEventQuery query = RunningEventQuery.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .page(0)
                .pageSize(10)
                .sortDirection(SortDirection.ASC)
                .build();

        Page<RunningEventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(jpaRepository.findByDateTimeBetween(eq(fromDate), eq(toDate), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();

        ArgumentCaptor<Long> fromDateCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> toDateCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(jpaRepository, times(1))
                .findByDateTimeBetween(fromDateCaptor.capture(), toDateCaptor.capture(), pageableCaptor.capture());

        assertThat(fromDateCaptor.getValue()).isEqualTo(fromDate);
        assertThat(toDateCaptor.getValue()).isEqualTo(toDate);

        // Verify sort direction is ascending
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldFindRunningEventsWithDateFilterDescendingOrder() {
        // Given
        Long fromDate = Instant.now().toEpochMilli();
        Long toDate = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli();

        RunningEventQuery query = RunningEventQuery.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .page(0)
                .pageSize(10)
                .sortDirection(SortDirection.DESC)
                .build();

        Page<RunningEventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(jpaRepository.findByDateTimeBetween(eq(fromDate), eq(toDate), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();

        ArgumentCaptor<Long> fromDateCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> toDateCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(jpaRepository, times(1))
                .findByDateTimeBetween(fromDateCaptor.capture(), toDateCaptor.capture(), pageableCaptor.capture());

        assertThat(fromDateCaptor.getValue()).isEqualTo(fromDate);
        assertThat(toDateCaptor.getValue()).isEqualTo(toDate);

        // Verify sort direction is descending
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldSortBySpecifiedField() {
        // Given
        RunningEventQuery query = RunningEventQuery.builder()
                .page(0)
                .pageSize(10)
                .sortBy("name")
                .sortDirection(SortDirection.ASC)
                .build();

        RunningEventEntity entity1 = RunningEventEntity.builder()
                .id(1L)
                .name("Alpha Event")
                .dateTime(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 1")
                .build();

        RunningEventEntity entity2 = RunningEventEntity.builder()
                .id(2L)
                .name("Beta Event")
                .dateTime(Instant.now().plus(2, ChronoUnit.DAYS).toEpochMilli())
                .location("Location 2")
                .build();

        List<RunningEventEntity> entities = List.of(entity1, entity2);
        Page<RunningEventEntity> page = new PageImpl<>(entities, PageRequest.of(0, 10), 2);

        when(jpaRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(mapper.toDomain(any(RunningEventEntity.class))).thenAnswer(i -> {
            RunningEventEntity entity = i.getArgument(0);
            return RunningEvent.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .dateTime(entity.getDateTime())
                    .location(entity.getLocation())
                    .build();
        });

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(2);

        // Verify that the correct sort parameters were passed to the repository
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository, times(1)).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldSortBySpecifiedFieldWithDateFilter() {
        // Given
        Long fromDate = Instant.now().toEpochMilli();
        Long toDate = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli();

        RunningEventQuery query = RunningEventQuery.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .page(0)
                .pageSize(10)
                .sortBy("location")
                .sortDirection(SortDirection.DESC)
                .build();

        Page<RunningEventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(jpaRepository.findByDateTimeBetween(eq(fromDate), eq(toDate), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PaginatedResult<RunningEvent> result = repository.findAll(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository, times(1)).findByDateTimeBetween(eq(fromDate), eq(toDate), pageableCaptor.capture());

        // Verify sort is by location in descending order
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("location").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldDefaultToDateTimeWhenSortByIsNull() {
        // Given
        RunningEventQuery query = RunningEventQuery.builder()
                .page(0)
                .pageSize(10)
                .sortBy(null) // Explicitly set to null
                .sortDirection(SortDirection.ASC)
                .build();

        Page<RunningEventEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(jpaRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When
        repository.findAll(query);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository, times(1)).findAll(pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime")).isNotNull();
        assertThat(capturedPageable.getSort().getOrderFor("dateTime").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldThrowExceptionWhenFindingAllWithNullQuery() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> repository.findAll(null));
        verify(jpaRepository, never()).findAll(any(Pageable.class));
        verify(jpaRepository, never()).findByDateTimeBetween(any(), any(), any(Pageable.class));
    }

    @Test
    void shouldDeleteRunningEventById() {
        // Given
        when(jpaRepository.existsById(1L)).thenReturn(true);

        // When
        boolean result = repository.deleteById(1L);

        // Then
        assertThat(result).isTrue();
        verify(jpaRepository, times(1)).existsById(1L);
        verify(jpaRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentEvent() {
        // Given
        when(jpaRepository.existsById(99L)).thenReturn(false);

        // When
        boolean result = repository.deleteById(99L);

        // Then
        assertThat(result).isFalse();
        verify(jpaRepository, times(1)).existsById(99L);
        verify(jpaRepository, never()).deleteById(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingWithNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> repository.deleteById(null));
        verify(jpaRepository, never()).existsById(any());
        verify(jpaRepository, never()).deleteById(any());
    }

    @Test
    void shouldCheckIfRunningEventExists() {
        // Given
        when(jpaRepository.existsById(1L)).thenReturn(true);
        when(jpaRepository.existsById(99L)).thenReturn(false);

        // When/Then
        assertThat(repository.existsById(1L)).isTrue();
        assertThat(repository.existsById(99L)).isFalse();

        verify(jpaRepository, times(1)).existsById(1L);
        verify(jpaRepository, times(1)).existsById(99L);
    }

    @Test
    void shouldThrowExceptionWhenCheckingExistenceWithNullId() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> repository.existsById(null));
        verify(jpaRepository, never()).existsById(any());
    }
}
