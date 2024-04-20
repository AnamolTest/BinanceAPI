import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class BinanceOrderBook {
    private static final String API_BASE_URL = "https://api.binance.com/api/v3";
    private static final OkHttpClient httpClient = new OkHttpClient();

    public static void main(String[] args) {
        String btcUsdtOrderBookUrl = API_BASE_URL + "/depth?symbol=BTCUSDT&limit=50";
        String ethUsdtOrderBookUrl = API_BASE_URL + "/depth?symbol=ETHUSDT&limit=50";

        try {
            // Fetch order book for BTC/USDT
            JSONObject btcUsdtOrderBook = getOrderBook(btcUsdtOrderBookUrl);
            System.out.println("BTC/USDT Order Book: " + btcUsdtOrderBook);

            // Fetch order book for ETH/USDT
            JSONObject ethUsdtOrderBook = getOrderBook(ethUsdtOrderBookUrl);
            System.out.println("ETH/USDT Order Book: " + ethUsdtOrderBook);

            // TODO: Implement WebSocket for real-time updates
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getOrderBook(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Unexpected response code: " + response);

            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }
}
