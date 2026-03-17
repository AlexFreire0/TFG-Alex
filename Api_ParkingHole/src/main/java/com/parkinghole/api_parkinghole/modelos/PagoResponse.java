package com.parkinghole.api_parkinghole.modelos;

public class PagoResponse {
    private String paymentIntent;
    private String ephemeralKey;
    private String customer;

    public PagoResponse(String paymentIntent, String ephemeralKey, String customer) {
        this.paymentIntent = paymentIntent;
        this.ephemeralKey = ephemeralKey;
        this.customer = customer;
    }

    // --- GETTERS Y SETTERS ---
    public String getPaymentIntent() { return paymentIntent; }
    public void setPaymentIntent(String paymentIntent) { this.paymentIntent = paymentIntent; }

    public String getEphemeralKey() { return ephemeralKey; }
    public void setEphemeralKey(String ephemeralKey) { this.ephemeralKey = ephemeralKey; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
}