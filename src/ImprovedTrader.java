import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;

import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.history.Candle;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PriceHistReq;
import com.studerw.tda.model.history.PriceHistReq.Builder;
import com.studerw.tda.model.history.PriceHistory;
import com.studerw.tda.model.quote.EtfQuote;

public class ImprovedTrader {
	
	private static final String CONSUMER_KEY = "OYFW1QGFSQZQ113LCAGLFKBLVG5CDFAE";
	private static final String REFRESH_TOKEN = "CcQrIvDSbn4SAix5IVLUQMqcAIVfDfnISOJx4DJtN+lU3xfyJkTk8Ea5Sg5U07BVEMuQpQbc0n4SMdnmLTasZCC+0GIywdhodsZTPTaG5WenmuHUCo1mJzHodupfz8VAzOFCAY6hjezgLRc5qFRgqpP0D56RKLMNmuSJERXNoxM1d7DLGjzbpyh4zpDhOfTUL6TgUynKPKz61nJ+LZuNGzo30GeQg+O7A3cBHlPUsM6YwIQBLPvotlQJ9ztH4aoBde0SAXTpF1rcYK2gjvtEvrNfA5+it0TWTzLp9c6qy6/ClmzCChUBCYbc3hAoTfxQWXVxzE6Oad6vv93SU9IwncPpIBitgbezDS4S0toYAENPpyGtMRGa2ZdmNjLedq0uighyEB7tuhGVfifqKC+We7z6vU3dKKik/piCFTfhnRKYCJYXFS4t+EQ5EnQ100MQuG4LYrgoVi/JHHvlPrqk73KEqrnJ9imxMRG0XwfGCQ0agLodciNlS6DEvcYKUp/6sUK6kawgWKaxMAGXbwK2ZXYwNNVErCc/zl3vHeoQgjJgI1THx3Zb9nLfAK3s66CUzOepPLRKyyIMzMSb+OS8fxTmoEpHtNkISiNsWg5R1vRKfimbzENce1h8tBFpoJoD/FM9R28h5wxweX6D3pRWUqdsmQwYHQmvSm5UsLpqUZQcU9/ycYX9trPp1uwGbEIcUsiPl+s7a8Nak49gleaRLXJ+vGCzAdZ2rKI1cnKDJ/Yjy/4qMqEy/3cTLUXp0jlRTl2xB/zkj5JM+EKuyZQAUgjT8o/JkICSg2HazqeDG+0NqBQGV5z5RYu8DVRc/DfDwqdCeU9X4jT4V2f0tH4Hk0zgYNUvKGJJrN7gBNpMipnblOUOIAp5Ark+tNDK92Ub8AoR0b5CVB0=212FD3x19z9sWBHDJACbC00B75E";
	
	static TdaClient tdaClient;
	
	//specify security to be traded here
	private static String ticker;
	
	//indicators to be initialized when building strategy
	private static SMAIndicator sma20, sma50;
	private static RSIIndicator rsi;
	
	private static int sharesOwned = 0;
	private static int cash;
	
	public static void main(String[] args) {
		
		ticker = args[0];
		cash = Integer.parseInt(args[1]);
		
		//instantiate client
		Properties tdaProps = new Properties();
		tdaProps.setProperty("tda.client_id", CONSUMER_KEY);	
		tdaProps.setProperty("tda.token.refresh", REFRESH_TOKEN);
		
		tdaClient = new HttpTdaClient(tdaProps);
		
		BarSeries series = initBarSeries(400);
		Strategy strategy = buildStrategy(series);
		
		TradingRecord tradingRecord = new BaseTradingRecord();
		
		Num profitability;
		
		while(true) {
			
			try {
				Thread.sleep(45000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			Bar newBar = getLatestBar();
			
			try {
				
				series.addBar(newBar);
				System.out.println(newBar.getDateName() + " ---- New Bar Added: Closed at " + newBar.getClosePrice());
				
				int seriesEndIndex = series.getEndIndex();
				
				System.out.println("20 SMA now at " + sma20.getValue(seriesEndIndex));
				System.out.println("50 SMA now at " + sma50.getValue(seriesEndIndex));
				System.out.println("RSI now at " + rsi.getValue(seriesEndIndex));
				
				if(strategy.shouldEnter(seriesEndIndex)) {
					
					if(cash > (newBar.getClosePrice().intValue() * 10)) {
						
						System.out.println("Entering at " + newBar.getClosePrice());
						tradingRecord.enter(seriesEndIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
						
						cash -= newBar.getClosePrice().intValue();
						
						sharesOwned += 10;
						
					} else {
						
						System.out.println("Insufficient Funds to enter position");
						
					}
					
				} else if(strategy.shouldExit(seriesEndIndex)) {
					
					if(sharesOwned >= 10) {
						
						System.out.println("Exiting at " + newBar.getClosePrice());
						tradingRecord.exit(seriesEndIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
						
						cash += newBar.getClosePrice().intValue();
						
						sharesOwned -= 10;
						
					} else {
						
						System.out.println("Insufficient shares to exit position");
						
					}
					
				}
				
				//calculate profitability
				AnalysisCriterion profitabilityCriterion = new TotalProfitCriterion();
				profitability = profitabilityCriterion.calculate(series, tradingRecord);
				
				System.out.println("Strategy Profitability is now at " + profitability);
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(newBar.getDateName() + " ---- Failed to add Bar. Bar with this timestamp already exists!");
			}
			
		}
		
	}
	
	public static String getQuote(String ticker) {
		
		EtfQuote quote = (EtfQuote) tdaClient.fetchQuote(ticker);
		
		return quote.getAskPrice().toString();
		
	}
	
	private static Strategy buildStrategy(BarSeries series) {
		
        if (series == null) {
        	
            throw new IllegalArgumentException("Series cannot be null");
            
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        sma20 = new SMAIndicator(closePrice, 20);
        sma50 = new SMAIndicator(closePrice, 50);
        
        rsi = new RSIIndicator(closePrice, 2);

        Rule entryRule = new CrossedUpIndicatorRule(closePrice, sma20);
        
        Rule exitRule = new CrossedDownIndicatorRule(closePrice, sma20);
        
        return new BaseStrategy(entryRule, exitRule);
        
    }
	
	public static BarSeries initBarSeries(int maxBarCount) {
		
		System.out.println("Initiating Bar Series");
		System.out.println("------------------------------------------------------------------");
		
		PriceHistReq request = Builder.priceHistReq()
		        .withSymbol(ticker)
		        .withStartDate(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 3))
		        .withEndDate(System.currentTimeMillis())
		        .withFrequencyType(FrequencyType.minute)
		        .withFrequency(1)
		        .build();

		PriceHistory priceHistory = tdaClient.priceHistory(request);
		
		List<Candle> candleArray = priceHistory.getCandles();
		
		BarSeries series = new BaseBarSeriesBuilder().withName(ticker + "-Series").build();
		series.setMaximumBarCount(maxBarCount);

		for(int i = 0; i < candleArray.size(); i++) {
			
			series.addBar(Instant.ofEpochMilli(candleArray.get(i).getDatetime()).atZone(ZoneId.systemDefault()), 
					candleArray.get(i).getOpen(),
					candleArray.get(i).getHigh(),
					candleArray.get(i).getClose(),
					candleArray.get(i).getClose(),
					candleArray.get(i).getVolume());
			
		}
		
		return series;

	}
	
	public static Bar getLatestBar(){
		
		PriceHistReq request = Builder.priceHistReq()
		        .withSymbol(ticker)
		        .withStartDate(System.currentTimeMillis() - (1000 * 60 * 2))
		        .withEndDate(System.currentTimeMillis())
		        .withFrequencyType(FrequencyType.minute)
		        .withFrequency(1)
		        .build();

		PriceHistory priceHistory = tdaClient.priceHistory(request);
		
		List<Candle> candleArray = priceHistory.getCandles();
		
		Candle lastCandle = candleArray.get(candleArray.size() - 1);
		
		return new BaseBar(Duration.ofMinutes(1),
				Instant.ofEpochMilli(lastCandle.getDatetime()).atZone(ZoneId.systemDefault()),
				lastCandle.getOpen(),
				lastCandle.getHigh(),
				lastCandle.getLow(),
				lastCandle.getClose(),
				new BigDecimal(lastCandle.getVolume()));
		
	}

}
