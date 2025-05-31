package io.strategiz.api.dashboard.mapper;

import java.util.List;

import io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryDTO;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for converting between service layer PortfolioSummaryResponse
 * and API layer PortfolioSummaryDTO.
 */
@Mapper(componentModel = "spring")
public interface PortfolioSummaryMapper {

    /**
     * Convert from service model to API DTO
     *
     * @param serviceModel The service layer model
     * @return The API layer DTO
     */
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "hasExchangeConnections", expression = "java(serviceModel.getAssets() != null && !serviceModel.getAssets().isEmpty())")
    @Mapping(target = "statusMessage", expression = "java(determineStatusMessage(serviceModel))")
    @Mapping(target = "needsApiKeyConfiguration", expression = "java(determineNeedsApiKeyConfig(serviceModel))")
    PortfolioSummaryDTO toDto(PortfolioSummaryResponse serviceModel);
    
    /**
     * Map service model Asset to DTO AssetDTO
     */
    PortfolioSummaryDTO.AssetDTO mapAsset(PortfolioSummaryResponse.Asset asset);
    
    /**
     * Helper method to determine status message
     */
    @Named("determineStatusMessage")
    default String determineStatusMessage(PortfolioSummaryResponse model) {
        if (model.getAssets() == null || model.getAssets().isEmpty()) {
            return "No exchange connections found. Please connect your accounts.";
        }
        return null;
    }
    
    /**
     * Helper method to determine if API key configuration is needed
     */
    @Named("determineNeedsApiKeyConfig")
    default boolean determineNeedsApiKeyConfig(PortfolioSummaryResponse model) {
        return model.getAssets() == null || model.getAssets().isEmpty();
    }
}
