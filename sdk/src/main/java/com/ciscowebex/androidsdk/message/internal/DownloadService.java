package com.ciscowebex.androidsdk.message.internal;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.message.MessagesAttachments;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.lang3.StringUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Url;
import com.ciscowebex.androidsdk.internal.ResultImpl;


public class DownloadService {

    private Authenticator _authenticator;

    private DownloadServiceInt _service;

    public DownloadService(Authenticator authenticator) {

        _authenticator = authenticator;
        _service = new ServiceBuilder().build(DownloadServiceInt.class);
    }

    public void downloadFile(@Nullable String url,String path,@NonNull CompletionHandler<String> handler) {

        _authenticator.getToken(token -> {
            if (token!=null) {

                _service.getFile("Bearer "+ token.getData() ,url)
                        .enqueue(new Callback<ResponseBody>() {


                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                String fileName =  StringUtils.substringBetween(response.headers().get("Content-Disposition"), "\"", "\"");
                                writeResponseBodyToDisk(response.body(),path,fileName);
                                handler.onComplete(ResultImpl.success("File has been downloaded"));
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                handler.onComplete(ResultImpl.error("Error"));

                            }
                        });
            }
        });
    }

    public void getFileDetails (@Nullable String url, @NonNull CompletionHandler<MessagesAttachments> handler) {

        _authenticator.getToken(token -> {

            if (token!=null) {
                _service.fileDetails("Bearer " + token.getData(),url)
                        .enqueue(new Callback<Void>() {

                            public void onResponse(Call<Void> call, Response<Void> response) {

                                MessagesAttachments messagesAttachments = new MessagesAttachments();
                                messagesAttachments.setFilename(StringUtils.substringBetween(response.headers().get("Content-Disposition"), "\"", "\""));
                                messagesAttachments.setContentType(response.headers().get("Content-Type"));
                                messagesAttachments.setSize(Integer.parseInt(response.headers().get("Content-Length")));

                                handler.onComplete(ResultImpl.success(messagesAttachments));

                            }

                            public void onFailure(Call<Void> call, Throwable t) {

                                handler.onComplete(ResultImpl.error("Failure get file details: " + t.getMessage() ));

                            } });
            }
        });

    }





    private boolean writeResponseBodyToDisk(ResponseBody body,String path, String fileName) {

        File file = new File(path,fileName);
        InputStream inputStream = null;
        OutputStream outputStream = null;


        try {
            byte[] fileReader = new byte[4096];

            long fileSize = body.contentLength();
            int fileSizeDownloaded = 0;

            inputStream = body.byteStream();
            outputStream = new FileOutputStream(file);

            while (true) {
                int read = inputStream.read(fileReader);

                if (read == -1)
                    break;

                outputStream.write(fileReader,0,read);
                fileSizeDownloaded+= read;

                Log.d("File Download","File download " + fileSizeDownloaded + " of " + fileSize);

            }

            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if(inputStream!=null) {
                    inputStream.close();
                }
                if(outputStream!=null){
                    outputStream.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
                return false;

            }
        }

        return true;
    }

    private interface DownloadServiceInt {

        @HEAD
        Call<Void> fileDetails (@Header("Authorization") String authorization, @Url String url);

        @GET
        Call<ResponseBody> getFile (@Header("Authorization") String authorization, @Url String url);
    }
}
