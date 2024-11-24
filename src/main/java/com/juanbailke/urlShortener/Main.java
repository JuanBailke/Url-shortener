package com.juanbailke.urlShortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();
        Map<String, String> bodyMap;

        try{
            bodyMap = objectMapper.readValue(body, Map.class);
        }catch (Exception e){
            throw new RuntimeException("Error parsing JSON body: " + e.getMessage(), e);
        }

        String originalUrl = bodyMap.get("OriginalUrl");
        String expirationTime = bodyMap.get("expirationTime");
        long expirationTimeSeconds = Long.parseLong(expirationTime);

        String shortUrlCode = UUID.randomUUID().toString().substring(0,8);

        UrlData UrlData = new UrlData(originalUrl, expirationTimeSeconds);

        try {
            String json = objectMapper.writeValueAsString(UrlData);
            PutObjectRequest request = PutObjectRequest.builder().
                    bucket("bucket-storage-url-shortener").
                    key(shortUrlCode + "json").
                    build();

            s3Client.putObject(request, RequestBody.fromString(json));

        } catch(Exception e) {
            throw new RuntimeException("Error saving data to S3: " + e.getMessage(), e);
        }

        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlCode);

        return response;
    }
}