package com.cisco.spark.android.lyra.model;

public class AdvertisementByToken {
    private Identity advertiser;
    private Links links;
    private Token token;

    private String proof;

    public Identity getAdvertiser() {
        return advertiser;
    }

    public void setAdvertiser(Identity advertiser) {
        this.advertiser = advertiser;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public String getProof() {
        return proof;
    }

    public void setProof(String proof) {
        this.proof = proof;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }
}
