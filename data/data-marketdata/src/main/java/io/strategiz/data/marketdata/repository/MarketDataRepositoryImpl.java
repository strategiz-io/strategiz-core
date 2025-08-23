package io.strategiz.data.marketdata.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of MarketDataRepository
 * Stores and retrieves historical market data from Firestore
 */
@Repository
public class MarketDataRepositoryImpl implements MarketDataRepository {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataRepositoryImpl.class);
    private static final String COLLECTION_NAME = "marketdata";
    private static final int BATCH_SIZE = 500; // Firestore batch write limit
    
    private final Firestore firestore;
    
    @Autowired
    public MarketDataRepositoryImpl(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    public MarketDataEntity save(MarketDataEntity entity) {
        try {
            if (entity.getId() == null) {
                entity.setId(MarketDataEntity.createId(
                    entity.getSymbol(), 
                    entity.getDate(), 
                    entity.getTimeframe()
                ));
            }
            
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(entity.getId());
            ApiFuture<WriteResult> future = docRef.set(entity);
            future.get(); // Wait for write to complete
            
            log.debug("Saved market data: {}", entity.getId());
            return entity;
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving market data for {}", entity.getSymbol(), e);
            throw new RuntimeException("Failed to save market data", e);
        }
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
                    if (entity.getId() == null) {
                        entity.setId(MarketDataEntity.createId(
                            entity.getSymbol(), 
                            entity.getDate(), 
                            entity.getTimeframe()
                        ));
                    }
                    
                    DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(entity.getId());
                    writeBatch.set(docRef, entity);
                }
                
                // Commit the batch
                ApiFuture<List<WriteResult>> future = writeBatch.commit();
                future.get(); // Wait for batch write to complete
                
                savedEntities.addAll(batch);
                log.info("Saved batch of {} market data entities", batch.size());
            }
            
            return savedEntities;
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving batch of market data", e);
            throw new RuntimeException("Failed to save market data batch", e);
        }
    }
    
    @Override
    public Optional<MarketDataEntity> findById(String id) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return Optional.ofNullable(document.toObject(MarketDataEntity.class));
            }
            return Optional.empty();
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data by ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<MarketDataEntity> findBySymbol(String symbol) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1000); // Limit to prevent huge queries
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(MarketDataEntity.class))
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data for symbol: {}", symbol, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<MarketDataEntity> findBySymbolAndDate(String symbol, LocalDate date) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereEqualTo("date", date.toString());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(MarketDataEntity.class))
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data for {} on {}", symbol, date, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<MarketDataEntity> findBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .orderBy("date", Query.Direction.ASCENDING);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(MarketDataEntity.class))
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data for {} between {} and {}", symbol, startDate, endDate, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<MarketDataEntity> findBySymbolAndTimeframe(String symbol, String timeframe) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereEqualTo("timeframe", timeframe)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(500);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(MarketDataEntity.class))
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data for {} with timeframe {}", symbol, timeframe, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<MarketDataEntity> findBySymbolsAndDate(List<String> symbols, LocalDate date) {
        try {
            List<MarketDataEntity> results = new ArrayList<>();
            
            // Firestore doesn't support 'IN' queries with more than 10 items
            // So we need to batch the queries
            for (int i = 0; i < symbols.size(); i += 10) {
                int endIndex = Math.min(i + 10, symbols.size());
                List<String> batch = symbols.subList(i, endIndex).stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
                
                Query query = firestore.collection(COLLECTION_NAME)
                    .whereIn("symbol", batch)
                    .whereEqualTo("date", date.toString());
                
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot querySnapshot = future.get();
                
                results.addAll(querySnapshot.getDocuments().stream()
                    .map(doc -> doc.toObject(MarketDataEntity.class))
                    .collect(Collectors.toList()));
            }
            
            return results;
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding market data for multiple symbols on {}", date, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Optional<MarketDataEntity> findLatestBySymbol(String symbol) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            if (!querySnapshot.isEmpty()) {
                return Optional.ofNullable(
                    querySnapshot.getDocuments().get(0).toObject(MarketDataEntity.class)
                );
            }
            return Optional.empty();
            
        } catch (InterruptedException | ExecutionException e) {
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
            Query query = firestore.collection(COLLECTION_NAME)
                .whereLessThan("date", cutoffDate.toString());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            WriteBatch batch = firestore.batch();
            int count = 0;
            
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                batch.delete(doc.getReference());
                count++;
                
                // Commit batch at 500 (Firestore limit)
                if (count % BATCH_SIZE == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
            
            // Commit remaining
            if (count % BATCH_SIZE != 0) {
                batch.commit().get();
            }
            
            log.info("Deleted {} market data records older than {}", count, cutoffDate);
            return count;
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting old market data", e);
            return 0;
        }
    }
    
    @Override
    public long countBySymbol(String symbol) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase());
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.size();
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error counting market data for {}", symbol, e);
            return 0;
        }
    }
    
    @Override
    public boolean existsBySymbolAndDate(String symbol, LocalDate date) {
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .whereEqualTo("date", date.toString())
                .limit(1);
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return !querySnapshot.isEmpty();
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error checking existence for {} on {}", symbol, date, e);
            return false;
        }
    }
    
    @Override
    public List<String> findDistinctSymbols() {
        try {
            // This is inefficient for large collections
            // In production, consider maintaining a separate symbols collection
            Query query = firestore.collection(COLLECTION_NAME)
                .select("symbol");
            
            ApiFuture<QuerySnapshot> future = query.get();
            QuerySnapshot querySnapshot = future.get();
            
            return querySnapshot.getDocuments().stream()
                .map(doc -> doc.getString("symbol"))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
                
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding distinct symbols", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public DateRange getDateRangeForSymbol(String symbol) {
        try {
            // Get earliest date
            Query earliestQuery = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(1);
            
            ApiFuture<QuerySnapshot> earliestFuture = earliestQuery.get();
            QuerySnapshot earliestSnapshot = earliestFuture.get();
            
            if (earliestSnapshot.isEmpty()) {
                return null;
            }
            
            // Get latest date
            Query latestQuery = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("symbol", symbol.toUpperCase())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1);
            
            ApiFuture<QuerySnapshot> latestFuture = latestQuery.get();
            QuerySnapshot latestSnapshot = latestFuture.get();
            
            if (!latestSnapshot.isEmpty()) {
                MarketDataEntity earliest = earliestSnapshot.getDocuments().get(0).toObject(MarketDataEntity.class);
                MarketDataEntity latest = latestSnapshot.getDocuments().get(0).toObject(MarketDataEntity.class);
                
                return new DateRange(earliest.getDate(), latest.getDate());
            }
            
            return null;
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting date range for {}", symbol, e);
            return null;
        }
    }
}