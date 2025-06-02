package io.strategiz.api.dashboard.mapper;

import io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-01T22:55:44-0400",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.7 (Oracle Corporation)"
)
@Component
public class MarketSentimentResponseBuilderImpl implements MarketSentimentResponseBuilder {

    @Override
    public io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse buildMarketSentimentResponse(MarketSentimentResponse serviceResponse) {
        if ( serviceResponse == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse marketSentimentResponse = new io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse();

        marketSentimentResponse.setOverallSentiment( sentimentIndicatorToSentimentIndicator( serviceResponse.getOverallSentiment() ) );
        marketSentimentResponse.setAssetSentiments( assetSentimentListToAssetSentimentList( serviceResponse.getAssetSentiments() ) );
        marketSentimentResponse.setMarketTrends( marketTrendsToMarketTrends( serviceResponse.getMarketTrends() ) );

        return marketSentimentResponse;
    }

    protected io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.SentimentIndicator sentimentIndicatorToSentimentIndicator(MarketSentimentResponse.SentimentIndicator sentimentIndicator) {
        if ( sentimentIndicator == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.SentimentIndicator sentimentIndicator1 = new io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.SentimentIndicator();

        sentimentIndicator1.setScore( sentimentIndicator.getScore() );
        sentimentIndicator1.setCategory( sentimentIndicator.getCategory() );
        sentimentIndicator1.setTimestamp( sentimentIndicator.getTimestamp() );

        return sentimentIndicator1;
    }

    protected io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment assetSentimentToAssetSentiment(MarketSentimentResponse.AssetSentiment assetSentiment) {
        if ( assetSentiment == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment assetSentiment1 = new io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment();

        assetSentiment1.setSymbol( assetSentiment.getSymbol() );
        assetSentiment1.setName( assetSentiment.getName() );
        assetSentiment1.setSentimentScore( assetSentiment.getSentimentScore() );
        assetSentiment1.setSentimentCategory( assetSentiment.getSentimentCategory() );
        assetSentiment1.setColor( assetSentiment.getColor() );

        return assetSentiment1;
    }

    protected List<io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment> assetSentimentListToAssetSentimentList(List<MarketSentimentResponse.AssetSentiment> list) {
        if ( list == null ) {
            return null;
        }

        List<io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment> list1 = new ArrayList<io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.AssetSentiment>( list.size() );
        for ( MarketSentimentResponse.AssetSentiment assetSentiment : list ) {
            list1.add( assetSentimentToAssetSentiment( assetSentiment ) );
        }

        return list1;
    }

    protected io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.MarketTrends marketTrendsToMarketTrends(MarketSentimentResponse.MarketTrends marketTrends) {
        if ( marketTrends == null ) {
            return null;
        }

        io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.MarketTrends marketTrends1 = new io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse.MarketTrends();

        marketTrends1.setFearGreedIndex( marketTrends.getFearGreedIndex() );
        marketTrends1.setFearGreedCategory( marketTrends.getFearGreedCategory() );
        marketTrends1.setUptrendPercentage( marketTrends.getUptrendPercentage() );
        marketTrends1.setDowntrendPercentage( marketTrends.getDowntrendPercentage() );
        marketTrends1.setNeutralTrendPercentage( marketTrends.getNeutralTrendPercentage() );

        return marketTrends1;
    }
}
