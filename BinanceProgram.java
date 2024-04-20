import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RealTimeOrderBook {
    private static final String BINANCE_WS_ENDPOINT = "wss://stream.binance.com:9443/ws/";
    private static final String REST_API_ENDPOINT = "https://api.binance.com/api/v3/";

    private static final String BTC_USDT_SYMBOL = "btcusdt";
    private static final String ETH_USDT_SYMBOL = "ethusdt";

  String btcUsdtOrderBookUrl = API_BASE_URL + "/depth?symbol=BTCUSDT&limit=50";
  String ethUsdtOrderBookUrl = API_BASE_URL + "/depth?symbol=ETHUSDT&limit=50";

    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Map<String, OrderBook> orderBooks = new HashMap<>();

    public static void main(String[] args) {
        subscribeToWebSocket(btcUsdtOrderBookUrl);
        subscribeToWebSocket(ethUsdtOrderBookUrl);

        // Schedule task to print order book and total volume change every 10 seconds
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                printOrderBooks();
            }
        }, 0, 10000);
    }

    private static void subscribeToWebSocket(String symbol) {
        String wsUrl = BINANCE_WS_ENDPOINT + symbol + "@depth";
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket connected for " + symbol);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleWebSocketMessage(symbol, text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                System.out.println("Received bytes: " + bytes.hex());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket closing: " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
            }
        };

        Request request = new Request.Builder().url(wsUrl).build();
        OkHttpClient client = new OkHttpClient();
        WebSocket ws = client.newWebSocket(request, listener);
    }

    private static void handleWebSocketMessage(String symbol, String message) {
        JSONObject jsonObject = new JSONObject(message);
        JSONArray bids = jsonObject.getJSONArray("bids");
        JSONArray asks = jsonObject.getJSONArray("asks");

        OrderBook orderBook = new OrderBook();
        orderBook.updateOrderBook(bids, asks);

        orderBooks.put(symbol, orderBook);
    }

    private static void printOrderBooks() {
        for (Map.Entry<String, OrderBook> entry : orderBooks.entrySet()) {
            String symbol = entry.getKey();
            OrderBook orderBook = entry.getValue();

            double totalVolumeChange = orderBook.getTotalVolumeChangeInUSDT();
            System.out.println("Order Book for " + symbol.toUpperCase());
            System.out.println(orderBook);
            System.out.println("Total Volume Change in USDT: " + totalVolumeChange);
            System.out.println();
        }
    }

    private static class OrderBook {
        private Map<Double, Double> bids = new HashMap<>();
        private Map<Double, Double> asks = new HashMap<>();

        public void updateOrderBook(JSONArray bidsArray, JSONArray asksArray) {
            bids.clear();
            asks.clear();

            for (int i = 0; i < bidsArray.length(); i++) {
                JSONArray bid = bidsArray.getJSONArray(i);
                double price = bid.getDouble(0);
                double quantity = bid.getDouble(1);
                bids.put(price, quantity);
            }

            for (int i = 0; i < asksArray.length(); i++) {
                JSONArray ask = asksArray.getJSONArray(i);
                double price = ask.getDouble(0);
                double quantity = ask.getDouble(1);
                asks.put(price, quantity);
            }
        }

        public double getTotalVolumeChangeInUSDT() {
            double totalBidVolume = bids.keySet().stream().mapToDouble(price -> price * bids.get(price)).sum();
            double totalAskVolume = asks.keySet().stream().mapToDouble(price -> price * asks.get(price)).sum();
            return totalBidVolume - totalAskVolume;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Bids:\n");
            for (Map.Entry<Double, Double> entry : bids.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("Asks:\n");
            for (Map.Entry<Double, Double> entry : asks.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }
}