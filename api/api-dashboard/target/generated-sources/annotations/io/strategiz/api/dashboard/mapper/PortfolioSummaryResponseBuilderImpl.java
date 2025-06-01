package io.strategiz.api.dashboard.mapper;

import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-31T23:07:07-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class PortfolioSummaryResponseBuilderImpl implements PortfolioSummaryResponseBuilder {

    @Override
    public io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse buildPortfolioSummaryResponse(PortfolioSummaryResponse serviceData) {
        if ( serviceData == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse portfolioSummaryResponse = new io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse();

        portfolioSummaryResponse.setTotalValue( serviceData.getTotalValue() );
        portfolioSummaryResponse.setDailyChange( serviceData.getDailyChange() );
        portfolioSummaryResponse.setDailyChangePercent( serviceData.getDailyChangePercent() );
        portfolioSummaryResponse.setExchanges( stringExchangeDataMapToStringExchangeDataMap( serviceData.getExchanges() ) );
        portfolioSummaryResponse.setHasExchangeConnections( serviceData.isHasExchangeConnections() );
        portfolioSummaryResponse.setStatusMessage( serviceData.getStatusMessage() );
        portfolioSummaryResponse.setNeedsApiKeyConfiguration( serviceData.isNeedsApiKeyConfiguration() );

        return portfolioSummaryResponse;
    }

    protected io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData assetDataToAssetData(PortfolioSummaryResponse.AssetData assetData) {
        if ( assetData == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData assetData1 = new io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData();

        assetData1.setSymbol( assetData.getSymbol() );
        assetData1.setName( assetData.getName() );
        assetData1.setQuantity( assetData.getQuantity() );
        assetData1.setPrice( assetData.getPrice() );
        assetData1.setValue( assetData.getValue() );
        assetData1.setAllocationPercent( assetData.getAllocationPercent() );

        return assetData1;
    }

    protected Map<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData> stringAssetDataMapToStringAssetDataMap(Map<String, PortfolioSummaryResponse.AssetData> map) {
        if ( map == null ) {
            return null;
        }

        Map<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData> map1 = new LinkedHashMap<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData>( Math.max( (int) ( map.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, PortfolioSummaryResponse.AssetData> entry : map.entrySet() ) {
            String key = entry.getKey();
            io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.AssetData value = assetDataToAssetData( entry.getValue() );
            map1.put( key, value );
        }

        return map1;
    }

    protected io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData exchangeDataToExchangeData(PortfolioSummaryResponse.ExchangeData exchangeData) {
        if ( exchangeData == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData exchangeData1 = new io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData();

        exchangeData1.setName( exchangeData.getName() );
        exchangeData1.setValue( exchangeData.getValue() );
        exchangeData1.setAssets( stringAssetDataMapToStringAssetDataMap( exchangeData.getAssets() ) );

        return exchangeData1;
    }

    protected Map<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData> stringExchangeDataMapToStringExchangeDataMap(Map<String, PortfolioSummaryResponse.ExchangeData> map) {
        if ( map == null ) {
            return null;
        }

        Map<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData> map1 = new LinkedHashMap<String, io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData>( Math.max( (int) ( map.size() / .75f ) + 1, 16 ) );

        for ( java.util.Map.Entry<String, PortfolioSummaryResponse.ExchangeData> entry : map.entrySet() ) {
            String key = entry.getKey();
            io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse.ExchangeData value = exchangeDataToExchangeData( entry.getValue() );
            map1.put( key, value );
        }

        return map1;
    }
}
