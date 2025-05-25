@echo off
echo Cleaning up old Coinbase files that have been migrated to the new structure...

REM Delete old Coinbase client files
echo Deleting old Coinbase client files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\client\CoinbaseClient.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\client\exception\CoinbaseApiException.java"

REM Delete old Coinbase model files
echo Deleting old Coinbase model files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\model\CoinbaseResponse.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\model\Balance.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\model\TickerPrice.java"

REM Delete old Coinbase service files
echo Deleting old Coinbase service files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\service\CoinbaseCloudService.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\service\firestore\FirestoreService.java"

REM Delete old Coinbase admin dashboard files
echo Deleting old Coinbase admin dashboard files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\admindashboard\CoinbaseAdminDashboardService.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\admindashboard\CoinbaseAdminDashboardClient.java"

REM Delete old Coinbase advanced trade files
echo Deleting old Coinbase advanced trade files...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\advanced\CoinbaseAdvancedTradeService.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\coinbase\advanced\CoinbaseAdvancedTradeClient.java"

REM Delete old trading agent files that have been migrated
echo Deleting old trading agent files that have been migrated...
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\gemini\CoinbaseDataFetcher.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\gemini\GeminiTradingAgent.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\gemini\model\GeminiTradingSignal.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\gemini\model\TradingAgentPrompt.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\gemini\controller\GeminiTradingController.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\service\BitcoinTradingAgent.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\controller\BitcoinTradingAgentController.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\model\HistoricalPriceData.java"
del /s /q "C:\Users\cuzto\Documents\GitHub\strategiz-core\src\main\java\io\strategiz\trading\agent\model\TradingSignal.java"

echo Cleanup of old Coinbase files complete!
