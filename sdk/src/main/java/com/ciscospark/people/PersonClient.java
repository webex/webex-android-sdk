/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscospark.people;

import com.ciscospark.CompletionHandler;
import com.ciscospark.Spark;
import com.ciscospark.SparkError;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.ciscospark.Utils.checkNotNull;
import static com.ciscospark.people.PersonClient.ErrorCode.UNAUTHORIZED;
import static com.ciscospark.people.PersonClient.ErrorCode.UNEXPECTED_ERROR;

public class PersonClient {
    final String BASE_URL = "https://api.ciscospark.com/v1/";
    private PersonService mPersonService;
    private Spark mSpark;


    public enum ErrorCode {
        UNAUTHORIZED,
        UNEXPECTED_ERROR
    }

    public PersonClient(Spark spark) {
        checkNotNull(spark, "spark is null");
        this.mSpark = spark;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mPersonService = retrofit.create(PersonService.class);
    }

    private class PersonList {
        @SerializedName("items")
        private List<Person> personList;

        List<Person> getPersonList() {
            return personList;
        }
    }

    /**
     * @param email
     * @param displayName
     * @param max
     * @param handler
     */
    void list(String email, String displayName, int max, CompletionHandler<List<Person>> handler) {
        checkNotNull(handler, "handler is null");

        if (!mSpark.isAuthorized()) {
            handler.onError(new SparkError<>(UNAUTHORIZED, "Spark is not authorized!"));
            return;
        }

        mSpark.getAuthenticator().getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(String result) {
                String authorizationHeader = "Bearer " + result;
                mPersonService.list(authorizationHeader, email, displayName, null, null, max)
                        .enqueue(new Callback<PersonList>() {
                            @Override
                            public void onResponse(Call<PersonList> call,
                                                   Response<PersonList> response) {
                                if (response.isSuccessful()) {
                                    handler.onComplete(response.body().getPersonList());
                                } else {
                                    handler.onError(new SparkError<>(UNEXPECTED_ERROR, "list person error:" + response.code()));
                                }
                            }

                            @Override
                            public void onFailure(Call<PersonList> call, Throwable t) {
                                handler.onError(new SparkError<>(UNEXPECTED_ERROR, "list person error:" + t.toString()));
                            }
                        });
            }

            @Override
            public void onError(SparkError error) {
                handler.onError(error);
            }
        });
    }

    /**
     * @param personId
     */
    void get(String personId, CompletionHandler<Person> handler) {
        checkNotNull(handler, "handler is null");
        checkNotNull(personId, "person id is null");

        if (!mSpark.isAuthorized()) {
            handler.onError(new SparkError<>(UNAUTHORIZED, "Spark is not authorized!"));
            return;
        }

        mSpark.getAuthenticator().getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(String result) {
                String authorizationHeader = "Bearer " + result;
                mPersonService.get(authorizationHeader, personId).enqueue(new Callback<Person>() {
                    @Override
                    public void onResponse(Call<Person> call, Response<Person> response) {
                        if (response.isSuccessful()) {
                            handler.onComplete(response.body());
                        } else {
                            handler.onError(new SparkError<>(UNEXPECTED_ERROR, "get person error:" + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<Person> call, Throwable t) {
                        handler.onError(new SparkError<>(UNEXPECTED_ERROR, "get person error"));
                    }
                });
            }

            @Override
            public void onError(SparkError error) {
                handler.onError(error);
            }
        });

    }

    /**
     *
     */
    void getMe(CompletionHandler<Person> handler) {
        checkNotNull(handler, "handler is null");

        if (!mSpark.isAuthorized()) {
            handler.onError(new SparkError<>(UNAUTHORIZED, "Spark is not authorized!"));
            return;
        }

        mSpark.getAuthenticator().getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(String result) {
                String authorizationHeader = "Bearer " + result;
                mPersonService.getMe(authorizationHeader).enqueue(new Callback<Person>() {
                    @Override
                    public void onResponse(Call<Person> call, Response<Person> response) {
                        if (response.isSuccessful()) {
                            handler.onComplete(response.body());
                        } else {
                            handler.onError(new SparkError<>(UNEXPECTED_ERROR, "get person error"));
                        }
                    }

                    @Override
                    public void onFailure(Call<Person> call, Throwable t) {
                        handler.onError(new SparkError<>(UNEXPECTED_ERROR, "getMe failed"));
                    }
                });
            }

            @Override
            public void onError(SparkError error) {
                handler.onError(error);
            }
        });
    }

    private interface PersonService {
        @GET("people/{personId}")
        @Headers("Content-Type:application/json; charset=utf-8")
        Call<Person> get(@Header("Authorization") String authorizationHeader,
                         @Path("personId") String personId);

        @GET("people")
        @Headers("Content-Type:application/json; charset=utf-8")
        Call<PersonList> list(@Header("Authorization") String authorizationHeader,
                              @Query("email") String email,
                              @Query("displayName") String displayName,
                              @Query("id") String id,
                              @Query("orgId") String orgId,
                              @Query("max") int max);

        @GET("people/me")
        @Headers("Content-Type:application/json; charset=utf-8")
        Call<Person> getMe(@Header("Authorization") String authorizationHeader);
    }
}
