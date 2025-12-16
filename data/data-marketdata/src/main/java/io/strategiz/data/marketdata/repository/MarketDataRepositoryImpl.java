package io.strategiz.data.marketdata.repository;

import com.google.cloud.firestore.*;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of MarketDataRepository extending BaseRepository
 * Market data is system-wide (not user-scoped), so we use "SYSTEM" as the userId
 */
@Repository
public class MarketDataRepositoryImpl extends BaseRepository<MarketDataEntity> implements MarketDataRepository {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRepositoryImpl.class);
    private static final String SYSTEM_USER = "SYSTEM";
    private static final int BATCH_SIZE = 500; // Firestore batch write limit

    @Autowired
    public MarketDataRepositoryImpl(Firestore firestore) {
        super(firestore, MarketDataEntity.class);
    }

    @Override
    public MarketDataEntity save(MarketDataEntity entity) {
        if (entity.getId() == null && entity.getSymbol() != null && entity.getTimestamp() != null && entity.getTimeframe() != null) {
            entity.setId(MarketDataEntity.createId(entity.getSymbol(), entity.getTimestamp(), entity.getTimeframe()));
        }
        return super.save(entity, SYSTEM_USER);
    }

    @Override
    public List<MarketDataEntity> saveAll(List<MarketDataEntity> entities) {
        if (entities.isEmpty()) {
            return entities;
        }

        try {
            List<MarketDataEntity> savedEntities = new ArrayList<>();

            // Process in batches (Firestore limit is 500 per batch)
            for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, entities.size());
                List<MarketDataEntity> batch = entities.subList(i, endIndex);

                WriteBatch writeBatch = firestore.batch();

                for (MarketDataEntity entity : batch) {
                    if (entity.getId() == null && entity.getSymbol() != null && entity.getTimestamp() != null && entity.getTimeframe() != null) {
                        entity.setId(MarketDataEntity.createId(entity.getSymbol(), entity.getTimestamp(), entity.getTimeframe()));
                    }

                    // Initialize audit fields if needed
                    if (!entity._hasAudit()) {
                        entity._initAudit(SYSTEM_USER);
                    } else {
                        entity._updateAudit(SYSTEM_USER);
                    }

                    DocumentReference docRef = getCollection().document(entity.getId());
                    writeBatch.set(docRef, entity);
                }

                // Commit the batch
                writeBatch.commit().get();

                savedEntities.addAll(batch);
                log.info("Saved batch of {} market data entities", batch.size());
            }

            return savedEntities;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error saving batch of market data", e);
            throw new RuntimeException("Failed to save market data batch", e);
        }
    }

    @Override
    public List<MarketDataEntity> findBySymbol(String symbol) {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    MarketDataEntity entity = doc.toObject(MarketDataEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding market data for symbol: {}", symbol, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date) {
        try {
            // Convert LocalDate to epoch milliseconds range (start and end of day)
            Long startOfDay = convertLocalDateToTimestamp(date);
            Long endOfDay = convertLocalDateToTimestamp(date.plusDays(1));

            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    MarketDataEntity entity = doc.toObject(MarketDataEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding market data for {} on {}", symbol, date, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        try {
            Long startTimestamp = convertLocalDateToTimestamp(startDate);
            Long endTimestamp = convertLocalDateToTimestamp(endDate.plusDays(1));

            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThan("timestamp", endTimestamp)
                .orderBy("timestamp", Query.Direction.ASCENDING);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    MarketDataEntity entity = doc.toObject(MarketDataEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding market data for {} between {} and {}", symbol, startDate, endDate, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe) {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereEqualTo("timeframe", timeframe)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(500);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    MarketDataEntity entity = doc.toObject(MarketDataEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding market data for {} with timeframe {}", symbol, timeframe, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date) {
        try {
            List<MarketDataEntity> results = new ArrayList<>();

            Long startOfDay = convertLocalDateToTimestamp(date);
            Long endOfDay = convertLocalDateToTimestamp(date.plusDays(1));

            // Firestore doesn't support 'IN' queries with more than 10 items
            for (int i = 0; i < symbols.size(); i += 10) {
                int endIndex = Math.min(i + 10, symbols.size());
                List<String> batch = symbols.subList(i, endIndex).stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

                // Note: No isActive filter - market data is immutable and never soft-deleted
                Query query = getCollection()
                    .whereIn("symbol", batch)
                    .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                    .whereLessThan("timestamp", endOfDay);

                List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

                results.addAll(docs.stream()
                    .map(doc -> {
                        MarketDataEntity entity = doc.toObject(MarketDataEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList()));
            }

            return results;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding market data for multiple symbols on {}", date, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (!docs.isEmpty()) {
                MarketDataEntity entity = docs.get(0).toObject(MarketDataEntity.class);
                entity.setId(docs.get(0).getId());
                return Optional.of(entity);
            }
            return Optional.empty();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding latest market data for {}", symbol, e);
            return Optional.empty();
        }
    }

    @Override
    public List<MarketDataEntity> findLatestBySymbols(List<String> symbols) {
        List<MarketDataEntity> results = new ArrayList<>();

        for (String symbol : symbols) {
            findLatestBySymbol(symbol).ifPresent(results::add);
        }

        return results;
    }

    @Override
    public int deleteOlderThan(LocalDate cutoffDate) {
        try {
            Long cutoffTimestamp = convertLocalDateToTimestamp(cutoffDate);

            Query query = getCollection()
                .whereLessThan("timestamp", cutoffTimestamp);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            WriteBatch batch = firestore.batch();
            int count = 0;

            for (QueryDocumentSnapshot doc : docs) {
                batch.delete(doc.getReference());
                count++;

                if (count % BATCH_SIZE == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }

            if (count % BATCH_SIZE != 0) {
                batch.commit().get();
            }

            log.info("Deleted {} market data records older than {}", count, cutoffDate);
            return count;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error deleting old market data", e);
            return 0;
        }
    }

    @Override
    public long countBySymbol(String symbol) {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase());

            return query.get().get().size();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error counting market data for {}", symbol, e);
            return 0;
        }
    }

    @Override
    public boolean existsBySymbolAndDate(String symbol, LocalDate date) {
        try {
            Long startOfDay = convertLocalDateToTimestamp(date);
            Long endOfDay = convertLocalDateToTimestamp(date.plusDays(1));

            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay)
                .limit(1);

            return !query.get().get().isEmpty();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error checking existence for {} on {}", symbol, date, e);
            return false;
        }
    }

    @Override
    public List<String> findDistinctSymbols() {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            Query query = getCollection()
                .select("symbol");

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> doc.getString("symbol"))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error finding distinct symbols", e);
            return Collections.emptyList();
        }
    }

    @Override
    public DateRange getDateRangeForSymbol(String symbol) {
        try {
            // Note: No isActive filter - market data is immutable and never soft-deleted
            // Get earliest
            Query earliestQuery = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(1);

            List<QueryDocumentSnapshot> earliestDocs = earliestQuery.get().get().getDocuments();

            if (earliestDocs.isEmpty()) {
                return null;
            }

            // Get latest
            Query latestQuery = getCollection()
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1);

            List<QueryDocumentSnapshot> latestDocs = latestQuery.get().get().getDocuments();

            if (!latestDocs.isEmpty()) {
                MarketDataEntity earliest = earliestDocs.get(0).toObject(MarketDataEntity.class);
                MarketDataEntity latest = latestDocs.get(0).toObject(MarketDataEntity.class);

                return new DateRange(
                    earliest.getTimestampAsLocalDateTime().toLocalDate(),
                    latest.getTimestampAsLocalDateTime().toLocalDate()
                );
            }

            return null;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Error getting date range for {}", symbol, e);
            return null;
        }
    }

    // Helper method to convert LocalDate to epoch milliseconds (Long)
    private Long convertLocalDateToTimestamp(LocalDate date) {
        Instant instant = date.atStartOfDay(ZoneId.of("UTC")).toInstant();
        return instant.toEpochMilli();
    }
}
