package currency.exchange.util;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class HttpClientXml {
    @SneakyThrows
    public String getCurrencies(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response =  httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
