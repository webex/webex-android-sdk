package com.ciscowebex.androidsdk;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;

import com.ciscowebex.androidsdk.auth.JWTAuthenticator;

import java.util.Map;

public class WebexTest {

    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbmRyb2lkX3Rlc3R1c2VyXzEiLCJuYW1lIjoiQW5kcm9pZFRlc3RVc2VyMSIsImlzcyI6ImNkNWM5YWY3LThlZDMtNGUxNS05NzA1LTAyNWVmMzBiMWI2YSJ9.eJ99AY9iNDhG4HjDJsY36wgqOnNQSes_PIu0DKBHBzs";

    @Test
    public void test() throws InterruptedException {
        System.out.println("123");

        Map<String, String> ret = new Gson().fromJson("", new TypeToken<Map<String, String>>(){}.getType());

        System.out.println(ret);


    }

}