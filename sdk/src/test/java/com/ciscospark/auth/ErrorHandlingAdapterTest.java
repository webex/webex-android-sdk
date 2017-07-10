package com.ciscospark.auth;

import org.junit.Before;
import org.junit.Test;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;

import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCall;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallAdapterFactory;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallback;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Created on 10/07/2017.
 */
public class ErrorHandlingAdapterTest {
    private MockWebServer mockWebServer;
    private AuthService mAuthService;
    private ErrorHandlingCall<ResponseBody> mCall;

    interface AuthService {
        @POST("login")
        ErrorHandlingCall<ResponseBody> getError();
    }

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("").toString())
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .build();
        mAuthService = retrofit.create(AuthService.class);
        mCall = mAuthService.getError();
    }


    @Test
    public void testSuccess() throws Exception {
        ErrorHandlingAdapter adapter = new ErrorHandlingAdapter();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        mCall.clone().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                System.out.println("success");
                assertTrue(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testError401() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        mAuthService.getError().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                System.out.print("unauthenticated");
                assertTrue(true);
            }

            @Override
            public void clientError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testError400() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        mCall.clone().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                System.out.print("clientError");
                assertTrue(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testError404() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        mCall.clone().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                System.out.print("clientError");
                assertTrue(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testError500() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mCall.clone().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void serverError(Response<?> response) {
                System.out.print("serverError");
                assertTrue(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testErrorUnexpectedResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(999));
        mCall.clone().enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                assertFalse(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                System.out.print("serverError");
                assertTrue(true);
            }
        });
        Thread.sleep(2 * 1000);
    }

    @Test
    public void testErrorIOError() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mCall.enqueue(new ErrorHandlingCallback<ResponseBody>() {
            @Override
            public void success(Response<ResponseBody> response) {
                assertFalse(true);
            }

            @Override
            public void unauthenticated(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void clientError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void serverError(Response<?> response) {
                assertFalse(true);
            }

            @Override
            public void networkError(IOException e) {
                System.out.print("networkError");
                assertTrue(true);
            }

            @Override
            public void unexpectedError(Throwable t) {
                assertFalse(true);
            }
        });
        mCall.cancel();
        Thread.sleep(2 * 1000);
    }
}