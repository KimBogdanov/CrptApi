package ru.kim;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.google.gson.Gson;

@Slf4j
public class CrptApi {

    private final int requestLimit;
    private final long timeIntervalMillis;
    private int requestCount;
    private long lastRequestTime;
    private final ReentrantLock lock;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
        this.lock = new ReentrantLock();
    }

    public void createDocument(Document document, String signature) {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime < timeIntervalMillis && requestCount >= requestLimit) {
                log.warn("Превышен лимит запросов. Вызов заблокирован.");
                return;
            }

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
                httpPost.setEntity(EntityBuilder.create()
                        .setText(new Gson().toJson(document))
                        .setContentType(ContentType.APPLICATION_JSON)
                        .build());

                // Возможное взаимодействие с подписью
                // httpPost.addHeader("Signature", signature);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.info("Ответ сервера: {}", responseBody);
                }
            } catch (IOException | ParseException e) {
                log.error("Ошибка при выполнении запроса: {}", e.getMessage(), e);
            }

            if (currentTime - lastRequestTime >= timeIntervalMillis) {
                requestCount = 1;
                lastRequestTime = currentTime;
            } else {
                requestCount++;
            }
        } finally {
            lock.unlock();
        }
    }

    @Getter
    @Setter
    @Builder
    public static class Document {
        private Description description;
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

        @Getter
        @Setter
        @Builder
        public static class Description {
            private String participantInn;
        }

        @Getter
        @Setter
        @Builder
        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }
}
