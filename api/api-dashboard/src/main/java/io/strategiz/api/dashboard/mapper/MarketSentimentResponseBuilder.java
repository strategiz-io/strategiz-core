package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse;
import io.strategiz.api.base.model.ResponseMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct builder interface for converting service-layer market sentiment DTOs to API-layer response objects.
 * 
 * Implementations are generated automatically by MapStruct at compile time.
 */
@Mapper(componentModel = "spring")
public interface MarketSentimentResponseBuilder {

    MarketSentimentResponseBuilder INSTANCE = Mappers.getMapper(MarketSentimentResponseBuilder.class);

    /**
     * Converts a service layer market sentiment response to an API layer market sentiment response
     */
    @Mapping(target = "metadata", ignore = true)
    MarketSentimentResponse buildMarketSentimentResponse(io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse serviceResponse);
    
    /**
     * Preserves transparency by showing original market data alongside any processed metrics,
     * giving users visibility into the raw market sentiment indicators.
     */
    
    /**
     * Convert ResponseMetadata to Map<String, Object> for API response
     * 
     * @param metadata The ResponseMetadata object
     * @return Map representation of the metadata
     */
    default Map<String, Object> convertMetadataToMap(ResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("status", metadata.getStatus());
        metadataMap.put("errorCode", metadata.getErrorCode());
        metadataMap.put("errorMessage", metadata.getErrorMessage());
        metadataMap.put("timestamp", metadata.getTimestamp());
        
        return metadataMap;
    }
}
