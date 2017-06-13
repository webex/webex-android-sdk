package com.cisco.spark.android.authenticator;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class OAuth2 {

    private static final String MACHINE_CLIENT_ID = "C487d869d1986fd645a496365a1585f6690b67bfcb2f5707cbb83a273a75d8db4";
    private static final String MACHINE_CLIENT_SECRET = "ce17d635e3ded74a68c4ca13b0fd11f1fa2139912e2f16bd3dec47920da0f96e";

    public static final String USER_CLIENT_ID = "C6a77387fb55a2dfda09df58c7fc94998bc83bf9fd0249a356f71dd360569cbc5";
    public static final String USER_CLIENT_SECRET = "eda4fdb4e20514dcf7360546956682d8269179ea18ae3dbe62c36e663d209e92";

    private String clientId;
    private String clientSecret;
    public static final String REDIRECT_URI = "squared://oauth2";
    public static final String OOB_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static final String OOB_SCOPE = "Identity:SCIM";
    public static final String OOB_REQUEST_TYPE = "code";
    public static final String BASE_URL = "https://idbroker.webex.com/idb/oauth2/v1/";
    private static final String CONVERSATION_SCOPES = Strings.join(" ", "webexsquare:get_conversation", OOB_SCOPE);
    private static final String KMS_SCOPES = "spark:kms";
    public static final String UBER_SCOPES = Strings.join(" ", CONVERSATION_SCOPES, KMS_SCOPES);

    private final SecureRandom random = new SecureRandom();
    private static final String TITLE_PATTTERN = "<title>([a-zA-Z0-9]{64})</title>";
    private String state;

    public static OAuth2 userOauth2() {
        return new OAuth2(USER_CLIENT_ID, USER_CLIENT_SECRET);
    }

    public static OAuth2 machineOauth2() {
        return new OAuth2(MACHINE_CLIENT_ID, MACHINE_CLIENT_SECRET);
    }

    public OAuth2(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getState() {
        return state;
    }

    private void buildState() {
        state = new BigInteger(130, random).toString(32);
    }

    public String buildCodeGrantUrl(String email) throws URISyntaxException {
        buildState();
        Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon();

        uriBuilder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("cisService", "spark")
                .appendQueryParameter("scope", UBER_SCOPES)
                .appendQueryParameter("state", state)
                .appendQueryParameter("cisKeepMeSignedInOption", "1")
                .appendQueryParameter("email", email);
        return uriBuilder.toString();
    }

    private String buildAuthorizationHeader() {
        return "Basic " + Base64.encodeToString((clientId + ":" + clientSecret).getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public Response<OAuth2Tokens> getTokens(OAuth2Client oauth2Client, String code) throws IOException {
        Response<OAuth2Tokens> ret = oauth2Client.postCode(buildAuthorizationHeader(), "authorization_code", REDIRECT_URI, UBER_SCOPES, code).execute();
        if (ret.isSuccessful()) {
            ret.body().scopes = UBER_SCOPES;
        }
        return ret;
    }

    public Response<OAuth2AccessToken> getClientCredentials(OAuth2Client oAuth2Client) throws IOException {
        Response<OAuth2AccessToken> ret = oAuth2Client.clientCredentials("client_credentials", UBER_SCOPES, clientId,
                                                                         clientSecret).execute();
        if (ret.isSuccessful()) {
            ret.body().scopes = UBER_SCOPES;
        }
        return ret;
    }

    public Response<OAuth2AccessToken> refreshTokens(OAuth2Client oAuth2Client, String refreshToken) throws IOException {
        return oAuth2Client.refreshToken("refresh_token", refreshToken, clientId, clientSecret).execute();
    }

    public Response<OAuth2AccessToken> getKmsTokens(OAuth2Client oAuth2Client, OAuth2AccessToken tokens) throws IOException {
        return reduceScope(oAuth2Client, tokens, KMS_SCOPES, clientId, clientSecret);
    }

    public Response<OAuth2AccessToken> getConversationTokens(OAuth2Client oAuth2Client, OAuth2AccessToken tokens) throws IOException {
        return reduceScope(oAuth2Client, tokens, CONVERSATION_SCOPES, clientId, clientSecret);
    }

    private Response<OAuth2AccessToken> reduceScope(OAuth2Client oAuth2Client, OAuth2AccessToken tokens, String scope, String clientId, String clientSecret) throws IOException {
        Response<OAuth2AccessToken> ret = oAuth2Client.reduceScope("urn:cisco:oauth:grant-type:scope-reduction", tokens.getAccessToken(), clientId, clientSecret, scope).execute();
        if (ret.isSuccessful()) {
            ret.body().scopes = scope;
        }
        return ret;
    }

    public String getAuthenticationCode(OAuth2Client oAuth2Client, HttpCookie cookie) {
        buildState();
        try {
            Response<ResponseBody> response = oAuth2Client.postCookie(cookie.toString(), OOB_REQUEST_TYPE, OOB_REDIRECT_URI, UBER_SCOPES,
                                                                      clientId, getState()).execute();
            if (response != null && response.isSuccessful()) {
                String html = response.body().string();
                Pattern pattern = Pattern.compile(TITLE_PATTTERN);
                Matcher matcher = pattern.matcher(html);
                String authCode = null;
                if (matcher.find()) {
                    authCode = matcher.group(1);
                }
                Ln.d("authCode");
                return authCode;
            }
        } catch (IOException e) {
            Ln.e(e, "Failed getting auth code");
        }

        return null;
    }

    public void revokeAccessToken(OAuth2Client oAuthClient, String accessToken) throws IOException {
        oAuthClient.revokeToken(buildAuthorizationHeader(), accessToken, "access_token").execute();
    }

    public static boolean isOAuth2Uri(Uri uri) {
        return uri != null && TextUtils.equals(Uri.parse(BASE_URL).getHost(), uri.getHost());
    }
}
