package com.selsup;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private final ScheduledExecutorService scheduler;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/1k/documents/create";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        long delay = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> requestCount.set(0), delay, delay, TimeUnit.MILLISECONDS);
    }

    public synchronized HttpResponse<String> createDocument(
            Document document,
            String signature) throws IOException, InterruptedException {
        
        while (requestCount.get() >= requestLimit) {
            wait();
        }
        requestCount.incrementAndGet();

        var jsonDocument = objectMapper.writeValueAsString(document);
        var jsonNode = objectMapper.createObjectNode();
        jsonNode.put("description", jsonDocument);
        jsonNode.put("signature", signature);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonNode.toString()))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        requestCount.decrementAndGet();
        notifyAll();

        return response;
    }
    
    @Data
    public static class Document {
        private String participantInn;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;
    
        @Data
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String nved_code;
            private String uit_code;
            private String uitu_code;
        }
    }
}
