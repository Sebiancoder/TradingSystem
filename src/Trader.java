import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.json.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class Trader {

	private static final int MS_IN_MIN = 60000;
	private static final int MS_IN_DAY = 86400000;
	private static final long MS_IN_200DAY = 17280000000l;
	
	private static final String REFRESH_TOKEN = 
			"2icPCnDjmwk5V9muXUgzo8VLzZiT5EUw76l3PuAY1TsTb9FXkcAA8OUU0TdJsDt6Ay4Ky4uIRAiPBs%2FGHtx8P%2F%2BdO1ZgMhFvI0hn9qLFBTtaLaGL63biX9AH%2FeoP"
			+ "ksdTI4MFe9vr1Mt2VW5q%2FpifqxbtlGQUV%2FQe53QSW40gmtFpvFwAPCEKH4ztg9r83lS%2FZqwYm6go9rXBmVupByTRr5F74dkF4xDP9B5llLtgSziBiNqPYIAEDJQz"
			+ "xclQB%2Fp78%2FGjozfwYErbo1%2FFuyXq7TsUJ5QqMuzBGMJHFvMkY1ZspjCPoHQwHXXmjCd9yQ9ij7070iWitiBFjjl9i604NasRdiEmGlDwYrpNYkHnnaHVBUJIICkD"
			+ "IrG3vVdkUUWaorOtqlk9f2RoiQt78BE%2FILZ0%2B4bbwAoaeFOKoTecqGii0vTUe6iUdRjm8as100MQuG4LYrgoVi%2FJHHvlDwPlN%2BprUjJiUfEa2Jd7NJ3Mb9BRV%"
			+ "2BzwYRRie6bJ%2BJVKBQ%2BN2kuRe%2B3ELJdHyBatWF2xkFsEVnh205bWWwRxCJfH%2B6eqOL83bIyTSH0GzDPAmHRpxwaJlqX3FS3o2AaexjUJCGm55rZbOaAAEjtfBm"
			+ "xdogH3ksofPhStbNFJUvRjUUY7OpISyMDocgCMdCHLQXEibklXlV7MgMru8yjwuy5HOvv%2FO968WtykZt3bfUyfQR5jpRQDx%2BycA8NLGm5BlNN0JHI5P%2FHTXtFKIW"
			+ "FD3%2FRJHncN5Whg0v2xYqtf9YrGDhCG%2FNUFy%2FL337rE6c33LbfshISpzPoKBsEhvGtZfvNMRVUvptdOnJPGsfZqf1xfWkiE%2FdA48I%2Fgwq5vs7rDCtprorze5v"
			+ "lb%2BwqjIVhVF2RJlZiAIQxklmY%2FoQvAKtHZuWb3Zcd22m%2FQd1g%3D212FD3x19z9sWBHDJACbC00B75E";
	
	private static final String REDIRECT_URI = "http%3A%2F%2Flocalhost%2Ftest";

	private static String apiKey = "OYFW1QGFSQZQ113LCAGLFKBLVG5CDFAE";
	private static String accessToken;

	/* public static void main(String[] args) {
		
		accessToken = getNewAuthKey();

		BarSeries minuteBarSeries = generateBarSeries("VYM", "minute");
		minuteBarSeries.setMaximumBarCount(300);

		while (true) {

			// trading loop
			minuteBarSeries = updateBarSeries(minuteBarSeries, "VYM", "minute");
			System.out.println(minuteBarSeries.getLastBar().getClosePrice() + " "
					+ minuteBarSeries.getLastBar().getSimpleDateName());
			System.out.println(minuteBarSeries.getBarCount());
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

//		ClosePriceIndicator closePrice = new ClosePriceIndicator(dailyBarSeries);
//		
//		SMAIndicator smaIndicator = new SMAIndicator(closePrice, 20);
//		
//		System.out.println(smaIndicator.getValue(253).doubleValue());

	} */

	public static double getCurrentPrice(String ticker) {

		String urlString = "https://api.tdameritrade.com/v1/marketdata/" + ticker + "/quotes?apikey=" + apiKey;

		try {

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Authorization", accessToken);
			InputStream is = conn.getInputStream();

			JSONTokener tokener = new JSONTokener(is);
			JSONObject dataAsJSON = new JSONObject(tokener);

			Double price = dataAsJSON.getJSONObject(ticker).getDouble("lastPrice");

			return price;

		} catch (Exception e) {

			e.printStackTrace();

			return 0;

		}

	}

	public static BarSeries generateBarSeries(String ticker, String barType) {
		
		System.out.println(getCurrentReadableDate() + " - Generating BarSeries");

		String urlString = "";

		if (barType == "daily") {

			urlString = "https://api.tdameritrade.com/v1/marketdata/" + ticker + "/pricehistory?apikey=" + apiKey
					+ "&frequencyType=daily" + "&frequency=1" + "&startDate="
					+ (System.currentTimeMillis() - (MS_IN_DAY * 365)) + "&endDate=" + System.currentTimeMillis()
					+ "&needExtendedHoursData=true";

		} else if (barType == "minute") {

			urlString = "https://api.tdameritrade.com/v1/marketdata/" + ticker + "/pricehistory?apikey=" + apiKey
					+ "&frequencyType=minute" + "&frequency=1" + "&startDate="
					+ (System.currentTimeMillis() - MS_IN_DAY) + "&endDate=" + System.currentTimeMillis()
					+ "&needExtendedHoursData=true";

		}

		BarSeries barSeries = new BaseBarSeriesBuilder().withName(ticker).build();

		try {

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Authorization", accessToken);
			InputStream is = conn.getInputStream();

			JSONTokener tokener = new JSONTokener(is);
			JSONObject dataAsJSON = new JSONObject(tokener);

			JSONArray candleArray = dataAsJSON.getJSONArray("candles");

			for (int i = 0; i < candleArray.length(); i++) {

				JSONObject currentCandle = candleArray.getJSONObject(i);

				Instant currentInstant = Instant.ofEpochSecond(currentCandle.getLong("datetime") / 1000);

				barSeries.addBar(ZonedDateTime.ofInstant(currentInstant, ZoneId.of("America/New_York")),
						currentCandle.getDouble("open"), currentCandle.getDouble("high"),
						currentCandle.getDouble("low"), currentCandle.getDouble("close"),
						currentCandle.getDouble("volume"));

			}

			return barSeries;

		} catch (Exception e) {

			e.printStackTrace();

			return barSeries;

		}

	}

	public static BarSeries updateBarSeries(BarSeries barSeries, String ticker, String barType) {

		String urlString = "";

		if (barType == "daily") {

			urlString = "https://api.tdameritrade.com/v1/marketdata/" + ticker + "/pricehistory?apikey=" + apiKey
					+ "&frequencyType=daily" + "&frequency=1" + "&startDate="
					+ (System.currentTimeMillis() - (MS_IN_DAY * 30)) + "&endDate=" + System.currentTimeMillis()
					+ "&needExtendedHoursData=true";

		} else if (barType == "minute") {

			urlString = "https://api.tdameritrade.com/v1/marketdata/" + ticker + "/pricehistory?apikey=" + apiKey
					+ "&frequencyType=minute" + "&frequency=1" + "&startDate="
					+ (System.currentTimeMillis() - MS_IN_DAY) + "&endDate=" + System.currentTimeMillis()
					+ "&needExtendedHoursData=true";

		}

		try {

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("Authorization", accessToken);
			InputStream is = conn.getInputStream();

			JSONTokener tokener = new JSONTokener(is);
			JSONObject dataAsJSON = new JSONObject(tokener);

			JSONArray candleArray = dataAsJSON.getJSONArray("candles");
			JSONObject lastCandle = candleArray.getJSONObject(candleArray.length() - 1);

			Instant currentInstant = Instant.ofEpochSecond(lastCandle.getLong("datetime") / 1000);

			barSeries.addBar(ZonedDateTime.ofInstant(currentInstant, ZoneId.of("America/New_York")),
					lastCandle.getDouble("open"), lastCandle.getDouble("high"), lastCandle.getDouble("low"),
					lastCandle.getDouble("close"), lastCandle.getDouble("volume"));

			return barSeries;

		} catch (Exception E) {

			E.printStackTrace();

			return barSeries;

		}

	}
	
	public static String getNewAuthKey() {
		
		System.out.println(getCurrentReadableDate() + " - Getting New Access token");
		
		String urlString = "https://api.tdameritrade.com/v1/oauth2/token"
				+ "?grant_type=refresh_token"
				+ "&refresh_token=" + REFRESH_TOKEN
				+ "&access_type=&code="
				+ "&client_id=" + apiKey
				+ "&redirect_uri=" + REDIRECT_URI;
		
		System.out.println(urlString);

		try {

			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Accept", "application/json");


			InputStream is = conn.getInputStream();

			JSONTokener tokener = new JSONTokener(is);
			JSONObject dataAsJSON = new JSONObject(tokener);

			return dataAsJSON.getString("access_token");

		} catch (Exception e) {

			e.printStackTrace();

			return "auth failed";

		}
		
	}

	private static int convertPeriodToMillisec(String periodType, double length) {

		if (periodType == "minute") {

			return (int) length * MS_IN_MIN;

		} else if (periodType == "daily") {

			return (int) length * MS_IN_DAY;

		} else {

			return 0;

		}

	}
	
	private static String getCurrentReadableDate() {
		
		int currentTimeInSec = (int) (System.currentTimeMillis() / 1000);
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentTimeInSec), ZoneId.of("America/New_York")).toString();
		
	}

}
