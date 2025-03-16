package io.strategiz.marketplace.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Controller for the Strategy Marketplace
 * Handles operations related to creating, listing, purchasing, and applying strategies
 */
@RestController
@RequestMapping("/api/marketplace")
public class StrategyMarketplaceController {

    private static final Logger log = LoggerFactory.getLogger(StrategyMarketplaceController.class);

    /**
     * List all public strategies in the marketplace
     */
    @GetMapping("/strategies")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> listPublicStrategies(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            List<Map<String, Object>> strategies = new ArrayList<>();
            
            // Query for public strategies
            QuerySnapshot querySnapshot = firestore.collection("strategies")
                .whereEqualTo("isPublic", true)
                .limit(limit)
                .get()
                .get();
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Map<String, Object> strategy = document.getData();
                if (strategy != null) {
                    strategy.put("id", document.getId());
                    strategies.add(strategy);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "strategies", strategies
            ));
            
        } catch (Exception e) {
            log.error("Error listing public strategies: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to list strategies: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get details of a specific strategy
     */
    @GetMapping("/strategies/{strategyId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getStrategyDetails(@PathVariable String strategyId) {
        try {
            Firestore firestore = FirestoreClient.getFirestore();
            
            DocumentSnapshot strategyDoc = firestore.collection("strategies")
                .document(strategyId)
                .get()
                .get();
            
            if (!strategyDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy not found"
                ));
            }
            
            Map<String, Object> strategy = strategyDoc.getData();
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy data is null"
                ));
            }
            
            strategy.put("id", strategyDoc.getId());
            
            // Get reviews for this strategy
            List<Map<String, Object>> reviews = new ArrayList<>();
            QuerySnapshot reviewsSnapshot = firestore.collection("strategies")
                .document(strategyId)
                .collection("reviews")
                .limit(10)
                .get()
                .get();
            
            for (DocumentSnapshot reviewDoc : reviewsSnapshot.getDocuments()) {
                Map<String, Object> review = reviewDoc.getData();
                if (review != null) {
                    review.put("id", reviewDoc.getId());
                    reviews.add(review);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "strategy", strategy,
                "reviews", reviews
            ));
            
        } catch (Exception e) {
            log.error("Error getting strategy details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to get strategy details: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Create a new strategy
     */
    @PostMapping("/strategies")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Object> createStrategy(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            Double price = Double.parseDouble(request.get("price").toString());
            
            if (userId == null || name == null || description == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Missing required fields: userId, name, description"
                ));
            }
            
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Get user details
            DocumentSnapshot userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .get();
            
            if (!userDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User not found"
                ));
            }
            
            Map<String, Object> userData = userDoc.getData();
            if (userData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User data is null"
                ));
            }
            
            String creatorName = (String) userData.get("displayName");
            String creatorEmail = (String) userData.get("email");
            
            // Create strategy document
            Map<String, Object> strategyData = new HashMap<>();
            strategyData.put("name", name);
            strategyData.put("description", description);
            strategyData.put("creatorId", userId);
            strategyData.put("creatorName", creatorName);
            strategyData.put("creatorEmail", creatorEmail);
            strategyData.put("price", price);
            strategyData.put("currency", "USD");
            strategyData.put("isPublic", request.getOrDefault("isPublic", false));
            strategyData.put("createdAt", FieldValue.serverTimestamp());
            strategyData.put("updatedAt", FieldValue.serverTimestamp());
            strategyData.put("version", "1.0.0");
            strategyData.put("purchaseCount", 0);
            strategyData.put("averageRating", 0.0);
            
            // Add optional fields if provided
            if (request.containsKey("tags")) {
                strategyData.put("tags", request.get("tags"));
            }
            
            if (request.containsKey("configuration")) {
                strategyData.put("configuration", request.get("configuration"));
            }
            
            if (request.containsKey("supportedExchanges")) {
                strategyData.put("supportedExchanges", request.get("supportedExchanges"));
            }
            
            if (request.containsKey("metadata")) {
                strategyData.put("metadata", request.get("metadata"));
            }
            
            // Save strategy to Firestore
            DocumentReference strategyRef = firestore.collection("strategies").document();
            strategyRef.set(strategyData).get();
            
            String strategyId = strategyRef.getId();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Strategy created successfully",
                "strategyId", strategyId
            ));
            
        } catch (Exception e) {
            log.error("Error creating strategy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to create strategy: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Purchase a strategy
     */
    @PostMapping("/strategies/{strategyId}/purchase")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Object> purchaseStrategy(
            @PathVariable String strategyId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String buyerId = (String) request.get("userId");
            
            if (buyerId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Missing required field: userId"
                ));
            }
            
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Get strategy details
            DocumentSnapshot strategyDoc = firestore.collection("strategies")
                .document(strategyId)
                .get()
                .get();
            
            if (!strategyDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy not found"
                ));
            }
            
            Map<String, Object> strategyData = strategyDoc.getData();
            if (strategyData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy data is null"
                ));
            }
            
            String strategyName = (String) strategyData.get("name");
            String sellerId = (String) strategyData.get("creatorId");
            String sellerEmail = (String) strategyData.get("creatorEmail");
            double price = Double.parseDouble(strategyData.get("price").toString());
            String currency = (String) strategyData.get("currency");
            
            // Get buyer details
            DocumentSnapshot buyerDoc = firestore.collection("users")
                .document(buyerId)
                .get()
                .get();
            
            if (!buyerDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Buyer not found"
                ));
            }
            
            Map<String, Object> buyerData = buyerDoc.getData();
            if (buyerData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Buyer data is null"
                ));
            }
            
            String buyerEmail = (String) buyerData.get("email");
            
            // Check if user already purchased this strategy
            QuerySnapshot existingPurchaseQuery = firestore.collection("users")
                .document(buyerId)
                .collection("purchases")
                .whereEqualTo("strategyId", strategyId)
                .whereEqualTo("status", "active")
                .get()
                .get();
            
            if (!existingPurchaseQuery.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "You have already purchased this strategy"
                ));
            }
            
            // Create transaction
            String transactionId = UUID.randomUUID().toString();
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", transactionId);
            transactionData.put("strategyId", strategyId);
            transactionData.put("strategyName", strategyName);
            transactionData.put("buyerId", buyerId);
            transactionData.put("buyerEmail", buyerEmail);
            transactionData.put("sellerId", sellerId);
            transactionData.put("sellerEmail", sellerEmail);
            transactionData.put("amount", price);
            transactionData.put("currency", currency);
            transactionData.put("status", "completed"); // Simplified for now
            transactionData.put("createdAt", FieldValue.serverTimestamp());
            transactionData.put("completedAt", FieldValue.serverTimestamp());
            transactionData.put("paymentMethod", "credit_card"); // Simplified for now
            
            // Create purchase record
            Map<String, Object> purchaseData = new HashMap<>();
            purchaseData.put("strategyId", strategyId);
            purchaseData.put("strategyName", strategyName);
            purchaseData.put("transactionId", transactionId);
            purchaseData.put("purchasedAt", FieldValue.serverTimestamp());
            purchaseData.put("status", "active");
            purchaseData.put("price", price);
            purchaseData.put("currency", currency);
            
            // Update strategy purchase count
            Map<String, Object> strategyUpdates = new HashMap<>();
            strategyUpdates.put("purchaseCount", ((Number) strategyData.getOrDefault("purchaseCount", 0)).longValue() + 1);
            
            // Batch write all changes
            WriteBatch batch = firestore.batch();
            
            // Add transaction
            batch.set(firestore.collection("transactions").document(transactionId), transactionData);
            
            // Add purchase to user's purchases subcollection
            batch.set(
                firestore.collection("users")
                    .document(buyerId)
                    .collection("purchases")
                    .document(transactionId),
                purchaseData
            );
            
            // Update strategy purchase count
            batch.update(firestore.collection("strategies").document(strategyId), strategyUpdates);
            
            // Commit the batch
            batch.commit().get();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Strategy purchased successfully",
                "transactionId", transactionId
            ));
            
        } catch (Exception e) {
            log.error("Error purchasing strategy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to purchase strategy: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Apply a purchased strategy to an exchange
     */
    @PostMapping("/strategies/{strategyId}/apply")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Object> applyStrategy(
            @PathVariable String strategyId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String userId = (String) request.get("userId");
            String exchangeId = (String) request.get("exchangeId");
            @SuppressWarnings("unchecked")
            Map<String, Object> configuration = (Map<String, Object>) request.get("configuration");
            
            if (userId == null || exchangeId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Missing required fields: userId, exchangeId"
                ));
            }
            
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Verify the user has purchased this strategy
            QuerySnapshot purchaseQuery = firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .whereEqualTo("strategyId", strategyId)
                .whereEqualTo("status", "active")
                .get()
                .get();
            
            if (purchaseQuery.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "You must purchase this strategy before applying it"
                ));
            }
            
            // Get strategy details
            DocumentSnapshot strategyDoc = firestore.collection("strategies")
                .document(strategyId)
                .get()
                .get();
            
            if (!strategyDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy not found"
                ));
            }
            
            Map<String, Object> strategyData = strategyDoc.getData();
            if (strategyData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Strategy data is null"
                ));
            }
            
            String strategyName = (String) strategyData.get("name");
            String strategyVersion = (String) strategyData.get("version");
            
            // Check if the exchange is supported by this strategy
            @SuppressWarnings("unchecked")
            List<String> supportedExchanges = (List<String>) strategyData.get("supportedExchanges");
            if (supportedExchanges != null && !supportedExchanges.contains(exchangeId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "This strategy does not support the selected exchange"
                ));
            }
            
            // Create application record
            String applicationId = UUID.randomUUID().toString();
            Map<String, Object> applicationData = new HashMap<>();
            applicationData.put("id", applicationId);
            applicationData.put("strategyId", strategyId);
            applicationData.put("strategyName", strategyName);
            applicationData.put("strategyVersion", strategyVersion);
            applicationData.put("userId", userId);
            applicationData.put("exchangeId", exchangeId);
            applicationData.put("isActive", true);
            applicationData.put("configuration", configuration);
            applicationData.put("appliedAt", FieldValue.serverTimestamp());
            applicationData.put("status", "active");
            
            // Save application to user's strategy_applications subcollection
            firestore.collection("users")
                .document(userId)
                .collection("strategy_applications")
                .document(applicationId)
                .set(applicationData)
                .get();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Strategy applied successfully",
                "applicationId", applicationId
            ));
            
        } catch (Exception e) {
            log.error("Error applying strategy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to apply strategy: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Submit a review for a strategy
     */
    @PostMapping("/strategies/{strategyId}/review")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Object> submitReview(
            @PathVariable String strategyId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String userId = (String) request.get("userId");
            Integer rating = (Integer) request.get("rating");
            String comment = (String) request.get("comment");
            
            if (userId == null || rating == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Missing required fields: userId, rating"
                ));
            }
            
            if (rating < 1 || rating > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Rating must be between 1 and 5"
                ));
            }
            
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Verify the user has purchased this strategy
            QuerySnapshot purchaseQuery = firestore.collection("users")
                .document(userId)
                .collection("purchases")
                .whereEqualTo("strategyId", strategyId)
                .whereEqualTo("status", "active")
                .get()
                .get();
            
            boolean isVerifiedPurchase = !purchaseQuery.isEmpty();
            
            // Get user details
            DocumentSnapshot userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .get();
            
            if (!userDoc.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User not found"
                ));
            }
            
            Map<String, Object> userData = userDoc.getData();
            if (userData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User data is null"
                ));
            }
            
            String userName = (String) userData.getOrDefault("displayName", "Anonymous");
            
            // Check if user already submitted a review
            QuerySnapshot existingReviewQuery = firestore.collection("strategies")
                .document(strategyId)
                .collection("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            String reviewId;
            boolean isUpdate = false;
            
            if (!existingReviewQuery.isEmpty()) {
                // Update existing review
                DocumentSnapshot existingReview = existingReviewQuery.getDocuments().get(0);
                reviewId = existingReview.getId();
                isUpdate = true;
            } else {
                // Create new review ID
                reviewId = UUID.randomUUID().toString();
            }
            
            // Create review data
            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("userId", userId);
            reviewData.put("userName", userName);
            reviewData.put("rating", rating);
            reviewData.put("comment", comment);
            reviewData.put("isVerifiedPurchase", isVerifiedPurchase);
            
            if (isUpdate) {
                reviewData.put("updatedAt", FieldValue.serverTimestamp());
            } else {
                reviewData.put("createdAt", FieldValue.serverTimestamp());
                reviewData.put("updatedAt", FieldValue.serverTimestamp());
            }
            
            // Save review
            firestore.collection("strategies")
                .document(strategyId)
                .collection("reviews")
                .document(reviewId)
                .set(reviewData, SetOptions.merge())
                .get();
            
            // Update strategy's average rating
            updateStrategyAverageRating(strategyId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", isUpdate ? "Review updated successfully" : "Review submitted successfully",
                "reviewId", reviewId
            ));
            
        } catch (Exception e) {
            log.error("Error submitting review: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Failed to submit review: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Helper method to update a strategy's average rating
     */
    private void updateStrategyAverageRating(String strategyId) throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        
        // Get all reviews for this strategy
        QuerySnapshot reviewsSnapshot = firestore.collection("strategies")
            .document(strategyId)
            .collection("reviews")
            .get()
            .get();
        
        if (reviewsSnapshot.isEmpty()) {
            return;
        }
        
        // Calculate average rating
        double totalRating = 0;
        int reviewCount = 0;
        
        for (DocumentSnapshot reviewDoc : reviewsSnapshot.getDocuments()) {
            Map<String, Object> reviewData = reviewDoc.getData();
            if (reviewData != null && reviewData.containsKey("rating")) {
                totalRating += ((Number) reviewData.get("rating")).doubleValue();
                reviewCount++;
            }
        }
        
        double averageRating = totalRating / reviewCount;
        
        // Update strategy document
        firestore.collection("strategies")
            .document(strategyId)
            .update("averageRating", averageRating)
            .get();
    }
}
