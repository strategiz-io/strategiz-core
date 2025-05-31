package io.strategiz.api.dashboard.mapper;

import io.strategiz.api.dashboard.model.watchlist.WatchlistResponse;
import io.strategiz.api.base.model.ResponseMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

/**
 * MapStruct mapper for converting watchlist service model to API model
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WatchlistResponseBuilder {
    
    WatchlistResponseBuilder INSTANCE = Mappers.getMapper(WatchlistResponseBuilder.class);
    
    /**
     * Maps from service model to API model
     * 
     * @param response Service model response
     * @return API model response
     */
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "watchlistItems", source = "assets")
    WatchlistResponse buildWatchlistResponse(io.strategiz.service.dashboard.model.watchlist.WatchlistResponse response);
    
    /**
     * Converts ResponseMetadata to a Map
     * 
     * @param metadata Response metadata to convert
     * @return Map representation of metadata
     */
    default Map<String, Object> convertMetadataToMap(ResponseMetadata metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("status", metadata.getStatus());
        metadataMap.put("errorCode", metadata.getErrorCode());
        metadataMap.put("errorMessage", metadata.getErrorMessage());
        metadataMap.put("timestamp", metadata.getTimestamp());
        
        return metadataMap;
    }
}
