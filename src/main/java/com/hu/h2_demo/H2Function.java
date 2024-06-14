package com.hu.h2_demo;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

//@UserDefinedFunctions

public class H2Function {
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    public static BigDecimal divFunction(BigDecimal b1, BigDecimal b2){
        if(b1 == null || b2 == null){
            return null;
        }else if (b2.compareTo(BigDecimal.ZERO) == 0){
            throw new IllegalArgumentException("分母不可为零");
        }
        return b1.divide(b2,16, RoundingMode.HALF_UP);
    }

    public static String http(String url) {
        HttpGet request = new HttpGet(url);

        // add request headers
        request.addHeader("custom-key", "mkyong");
        request.addHeader(HttpHeaders.USER_AGENT, "Googlebot");

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            // Get HttpResponse Status
            System.out.println(response.getStatusLine().toString());

            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            System.out.println(headers);

            if (entity != null) {
                // return it as a String
                String result = EntityUtils.toString(entity);
                System.out.println(result);
                return result;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}
