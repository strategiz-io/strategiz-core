/**
 * Centralized audit system for Strategiz platform Firestore entities.
 * 
 * This package provides a comprehensive audit system that enforces consistent
 * audit fields across ALL Firestore entities in the platform. The system ensures
 * every entity has proper user tracking, versioning, timestamps, and soft delete capabilities.
 * 
 * <h2>Core Components:</h2>
 * <ul>
 *   <li>{@link io.strategiz.data.base.audit.AuditFields} - Standardized audit fields data structure</li>
 *   <li>{@link io.strategiz.data.base.audit.AuditableEntity} - Base class for all Firestore entities</li>
 *   <li>{@link io.strategiz.data.base.audit.AuditEnforcementService} - Service for audit field management</li>
 * </ul>
 * 
 * <h2>Usage Requirements:</h2>
 * <ol>
 *   <li><strong>All Firestore entities MUST extend {@link io.strategiz.data.base.audit.AuditableEntity}</strong></li>
 *   <li><strong>All repositories MUST extend {@link io.strategiz.data.base.repository.BaseFirestoreRepository}</strong></li>
 *   <li><strong>All service operations MUST provide user ID for audit tracking</strong></li>
 * </ol>
 * 
 * <h2>Example Entity Implementation:</h2>
 * <pre>{@code
 * @Document
 * @Collection("watchlist_items")
 * public class WatchlistItem extends AuditableEntity {
 *     @Id
 *     private String id;
 *     
 *     private String symbol;
 *     // ... other fields
 *     
 *     public WatchlistItem() {}
 *     
 *     public WatchlistItem(String symbol, String createdBy) {
 *         super(createdBy); // Initializes audit fields
 *         this.symbol = symbol;
 *     }
 *     
 *     @Override
 *     public String getCollectionName() {
 *         return "watchlist_items";
 *     }
 *     
 *     @Override
 *     public String getId() { return id; }
 *     
 *     @Override
 *     public void setId(String id) { this.id = id; }
 *     
 *     @Override
 *     protected AuditableEntity copy() {
 *         // Implementation for copying
 *     }
 * }
 * }</pre>
 * 
 * <h2>Example Repository Implementation:</h2>
 * <pre>{@code
 * @Repository
 * public class WatchlistRepository extends BaseFirestoreRepository<WatchlistItem> {
 *     
 *     public WatchlistRepository(Firestore firestore, AuditEnforcementService auditService) {
 *         super(firestore, auditService, WatchlistItem.class);
 *     }
 *     
 *     // Custom query methods
 *     public CompletableFuture<List<WatchlistItem>> findByUserAndSymbol(String userId, String symbol) {
 *         // Custom implementation
 *     }
 * }
 * }</pre>
 * 
 * <h2>Example Service Usage:</h2>
 * <pre>{@code
 * @Service
 * public class WatchlistService {
 *     
 *     private final WatchlistRepository repository;
 *     
 *     public CompletableFuture<WatchlistItem> addToWatchlist(String userId, String symbol) {
 *         WatchlistItem item = new WatchlistItem(symbol, userId);
 *         return repository.create(item, userId);
 *     }
 *     
 *     public CompletableFuture<WatchlistItem> updateWatchlistItem(String userId, String itemId, String newSymbol) {
 *         return repository.findActiveById(itemId)
 *             .thenCompose(optional -> {
 *                 if (optional.isPresent()) {
 *                     WatchlistItem item = optional.get();
 *                     item.setSymbol(newSymbol);
 *                     return repository.update(item, userId);
 *                 }
 *                 throw new EntityNotFoundException("Watchlist item not found");
 *             });
 *     }
 * }
 * }</pre>
 * 
 * <h2>Audit Fields Structure in Firestore:</h2>
 * <pre>{@code
 * {
 *   "id": "item123",
 *   "symbol": "AAPL",
 *   "auditFields": {
 *     "createdBy": "user456",
 *     "modifiedBy": "user789", 
 *     "createdDate": "2024-01-15T10:30:00.000Z",
 *     "modifiedDate": "2024-01-16T14:45:30.000Z",
 *     "version": 3,
 *     "isActive": true
 *   }
 * }
 * }</pre>
 * 
 * <h2>Benefits:</h2>
 * <ul>
 *   <li><strong>Consistency:</strong> All entities have identical audit fields</li>
 *   <li><strong>User Tracking:</strong> Know who created/modified every record</li>
 *   <li><strong>Versioning:</strong> Optimistic locking prevents concurrent modification issues</li>
 *   <li><strong>Soft Delete:</strong> Safe deletion with recovery capabilities</li>
 *   <li><strong>Timestamp Precision:</strong> Accurate audit trail with Firestore timestamps</li>
 *   <li><strong>Validation:</strong> Automatic validation prevents invalid audit states</li>
 *   <li><strong>Query Support:</strong> Structured for efficient Firestore queries</li>
 * </ul>
 * 
 * <h2>Migration Guide:</h2>
 * For existing entities that don't use this audit system:
 * <ol>
 *   <li>Extend {@link io.strategiz.data.base.audit.AuditableEntity} instead of plain class</li>
 *   <li>Remove individual audit fields (createdBy, createdAt, etc.)</li>
 *   <li>Update constructors to call {@code super(userId)} or use {@code initializeAuditFields()}</li>
 *   <li>Update repository to extend {@link io.strategiz.data.base.repository.BaseFirestoreRepository}</li>
 *   <li>Update service methods to pass user ID to repository operations</li>
 *   <li>Run data migration script to convert existing documents</li>
 * </ol>
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
package io.strategiz.data.base.audit;