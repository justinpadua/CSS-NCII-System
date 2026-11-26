package com.cssncii.service;

public interface CloudinaryImageClient {
    void upload(byte[] bytes, String publicId, String format, String deliveryType);
    byte[] download(String publicId, String format, String deliveryType);
}
