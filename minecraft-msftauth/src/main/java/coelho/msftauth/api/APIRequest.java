package coelho.msftauth.api;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class APIRequest<R> {
    private static final String TAG = "APIRequest";

    public abstract String getHttpURL();

    public abstract APIEncoding getRequestEncoding();

    public abstract Class<R> getResponseClass();

    public abstract APIEncoding getResponseEncoding();

    public R request(OkHttpClient client) throws Exception {
        Builder requestBuilder = new Builder().url(getHttpURL());

        Log.d(TAG, "Request URL: " + getHttpURL());

        if (getRequestEncoding() != null) {
            getRequestEncoding().encode(requestBuilder, this);
        } else {
            requestBuilder.get();
        }
        if (getHttpAuthorization() != null) {
            requestBuilder.addHeader("Authorization", getHttpAuthorization());
        }
        applyHeader(requestBuilder);

        Response response = client.newCall(requestBuilder.build()).execute();

        ResponseBody body = response.body();
        String bodyText = null;
        try {
            bodyText = body != null ? body.string() : null;
            Log.d(TAG, "Response Code: " + response.code());
            Log.d(TAG, "Response Body: " + (bodyText != null ? bodyText : "null"));
        } catch (Exception e) {
            Log.e(TAG, "Error reading response body", e);
        }

        boolean contains = false;
        for (Class<?> klass : getResponseClass().getInterfaces()) {
            if (klass == APIRequestWithStatus.class) {
                contains = true;
            }
        }
        if (contains || response.code() == 200) {
            Response newResponse = response.newBuilder()
                    .body(okhttp3.ResponseBody.create(bodyText, body != null ? body.contentType() : null))
                    .build();

            R decoded = getResponseEncoding().decode(newResponse, getResponseClass());
            if (decoded instanceof APIResponseExt) {
                ((APIResponseExt) decoded).applyResponse(newResponse);
            }
            if (decoded instanceof APIRequestWithStatus) {
                ((APIRequestWithStatus) decoded).setStatus(response.code());
            }
            return decoded;
        }

        throw new IllegalStateException("status code: " + response.code() +
                (bodyText != null && !bodyText.isEmpty() ? "; body: " + bodyText : ""));
    }

    public void applyHeader(Builder requestBuilder) {
    }

    public String getHttpAuthorization() {
        return null;
    }
}