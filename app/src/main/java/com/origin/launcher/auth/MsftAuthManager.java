package com.origin.launcher.auth;

import android.content.Context;
import android.util.Base64;
import android.util.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import android.util.Log;
import java.util.Map;

import coelho.msftauth.api.oauth20.OAuth20Authorize;
import coelho.msftauth.api.oauth20.OAuth20Token;
import coelho.msftauth.api.oauth20.OAuth20TokenRequestByCode;
import coelho.msftauth.api.oauth20.OAuth20TokenRequestByRefresh;
import coelho.msftauth.api.oauth20.OAuth20Util;
import coelho.msftauth.api.xbox.XboxDevice;
import coelho.msftauth.api.xbox.XboxDeviceAuthRequest;
import coelho.msftauth.api.xbox.XboxDeviceKey;
import coelho.msftauth.api.xbox.XboxDeviceToken;
import coelho.msftauth.api.xbox.XboxToken;
import coelho.msftauth.api.xbox.XboxUserAuthRequest;
import coelho.msftauth.api.xbox.XboxXSTSAuthRequest;
import coelho.msftauth.api.xbox.XboxTitleAuthRequest;
import coelho.msftauth.api.xbox.XboxTitleToken;

import com.origin.launcher.auth.storage.MsaTokenStore;
import com.origin.launcher.auth.storage.XalStorageManager;
import com.origin.launcher.auth.storage.UserTokenStore;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.HttpUrl;

public class MsftAuthManager {

    public static final String DEFAULT_CLIENT_ID = "0000000048183522";
    public static final String DEFAULT_SCOPE = "service::user.auth.xboxlive.com::mbi_ssl";
    public static final String DEFAULT_XSTS_RELYING_PARTY = "https://multiplayer.minecraft.net/";

   public record XboxAuthResult(XboxToken xstsToken, String gamertag, String avatarUrl, XboxDevice device) {}

    public static String buildAuthorizeUrl(String clientId, String scope, String codeChallenge, String state) {
        OAuth20Authorize auth = new OAuth20Authorize(clientId, "code", scope);
        HttpUrl base = HttpUrl.parse(auth.getURL());
        return base.newBuilder()
                .addQueryParameter("code_challenge_method", "S256")
                .addQueryParameter("code_challenge", codeChallenge)
                .addQueryParameter("state", state)
                .build()
                .toString();
    }

    public static OAuth20Token exchangeCodeForToken(OkHttpClient client, String clientId, String code, String codeVerifier, String scope) throws Exception {
        OAuth20TokenRequestByCode req = new OAuth20TokenRequestByCode(clientId, code, codeVerifier, scope);
        return req.request(client);
    }

    public static OAuth20Token exchangeTokenByRefresh(OkHttpClient client, String clientId, String refreshToken, String scope) throws Exception {
        OAuth20TokenRequestByRefresh req = new OAuth20TokenRequestByRefresh(clientId, refreshToken, scope);
        return req.request(client);
    }

    public static XboxAuthResult refreshAndAuth(OkHttpClient client, MsftAccountStore.MsftAccount account, Context context) throws Exception {
        if (account == null || account.msUserId == null || account.msUserId.isEmpty()) {
            throw new IllegalArgumentException("No user id available for the selected account");
        }
        String refreshToken = MsaTokenStore.findRefreshToken(context, account.msUserId);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("No refresh token found in MSA store for the selected account");
        }
        OAuth20Token token = exchangeTokenByRefresh(client, DEFAULT_CLIENT_ID, refreshToken, DEFAULT_SCOPE);
        return performXboxAuth(client, token, context);
    }

    private static XboxDeviceAuthRequest buildDeviceAuth(AuthConfig cfg, XboxDeviceKey deviceKey) {
        return new XboxDeviceAuthRequest(
                cfg.deviceAuthRP(), cfg.tokenType(),
                "Android", "0.0.0.0", deviceKey
        );
    }

    private static XboxUserAuthRequest buildUserAuth(AuthConfig cfg, String rpsTicket) {
        return new XboxUserAuthRequest(
                cfg.userAuthRP(), cfg.tokenType(),
                cfg.authMethodRps(), cfg.siteNameRps(),
                rpsTicket
        );
    }

    private static XboxXSTSAuthRequest buildXstsForRP(AuthConfig cfg, String relyingParty, java.util.List<XboxToken> userTokens) {
        return new XboxXSTSAuthRequest(
                relyingParty, cfg.tokenType(), cfg.sandbox(), userTokens
        );
    }

    public static XboxAuthResult performXboxAuth(OkHttpClient client, OAuth20Token token,  Context context) throws Exception {
        AuthConfig cfg = AuthConfig.productionRetailJwtDefault();
        XboxDeviceKey deviceKey = new XboxDeviceKey(context);
        XboxDeviceAuthRequest deviceAuthReq = buildDeviceAuth(cfg, deviceKey);

        XalStorageManager.saveDeviceIdentity(context, token.getUserId(), deviceKey);

        XboxDeviceToken deviceToken = deviceAuthReq.request(client);
        XalStorageManager.saveDeviceToken(context, token.getUserId(), deviceKey, deviceToken, cfg);
        XboxDevice device = new XboxDevice(deviceKey, deviceToken);
        XboxToken userToken = buildUserAuth(cfg, "t="  +token.getAccessToken()).request(client);

        XboxToken xstsTokenMain = null;
        try{
            xstsTokenMain = buildXstsForRP(cfg, DEFAULT_XSTS_RELYING_PARTY, Collections.singletonList(userToken)).request(client);
            String defaultUserId = token.getUserId();
            XalStorageManager.saveDefaultTitleUser(context, defaultUserId);
        }catch(Exception e){
            XboxTitleToken titleToken = new XboxTitleAuthRequest( cfg.userAuthRP(), cfg.tokenType(), cfg.authMethodRps(), cfg.siteNameRps() ,"t=" + token.getAccessToken(),deviceToken,deviceKey).request(client);
            XalStorageManager.saveTitleToken(context, token.getUserId(), deviceKey, titleToken, cfg);

            xstsTokenMain = buildXstsForRP(cfg, DEFAULT_XSTS_RELYING_PARTY, Collections.singletonList(userToken)).request(client);
            String defaultUserId = token.getUserId();
            XalStorageManager.saveDefaultTitleUser(context, defaultUserId);
        }
        XboxToken xstsXboxLive = buildXstsForRP(cfg, "http://xboxlive.com", Collections.singletonList(userToken)).request(client);
        XboxToken xstsPlayfab = buildXstsForRP(cfg, "https://b980a380.minecraft.playfabapi.com/", Collections.singletonList(userToken)).request(client);
        XboxToken xstsRealms = buildXstsForRP(cfg, "https://pocket.realms.minecraft.net/", Collections.singletonList(userToken)).request(client);

        UserTokenStore.save(context, deviceKey, token.getUserId(), cfg, userToken, xstsXboxLive, xstsPlayfab, xstsRealms);
        MsaTokenStore.save(context, token);

        String gamertag = null;
        String avatarUrl = null;
        String xuid = extractXuid(userToken);

        Pair<String, String> profile = fetchXboxProfile(client, xstsXboxLive, xuid);
        if (profile != null) {
            gamertag = profile.first;
            avatarUrl = sanitizeUrl(profile.second);
        }

        if (gamertag == null || gamertag.isEmpty()) gamertag = "Unknown";

        return new XboxAuthResult(xstsTokenMain, gamertag, avatarUrl, device);
    }

    public static Pair<String, String> fetchMinecraftIdentity(OkHttpClient client, XboxToken xstsToken) throws Exception {
        String identityToken = xstsToken.toIdentityToken();

        JsonObject data = new JsonObject();
        data.addProperty("identityPublicKey",
                Base64.encodeToString(EncryptionUtils.createKeyPair().getPublic().getEncoded(), Base64.NO_WRAP));

        Request.Builder builder = new Request.Builder().url("https://multiplayer.minecraft.net/authentication");
        builder.get();
        builder.addHeader("Client-Version", "1.21.110");
        builder.addHeader("Authorization", identityToken);
        builder.post(RequestBody.create(data.toString(), MediaType.parse("application/json")));
        Response response = client.newCall(builder.build()).execute();

        String respBody = response.body().string();
        return parseUsernameAndXuidFromChain(respBody);
    }

   public static void saveAccount(Context ctx, OAuth20Token token, String gamertag, String minecraftUsername, String xuid, String avatarUrl) {
        MsftAccountStore.addOrUpdate(ctx, token.getUserId(), token.getRefreshToken(), gamertag, minecraftUsername, xuid, avatarUrl);
    }

    public static Pair<String, String> parseUsernameAndXuidFromChain(String chains) {
        JsonObject body = JsonParser.parseString(chains).getAsJsonObject();
        if (!body.has("chain")) {
            throw new IllegalArgumentException("no field named \"chain\" found in json: " + chains);
        }
        JsonArray chainArray = body.getAsJsonArray("chain");
        for (int i = 0; i < chainArray.size(); i++) {
            String chain = chainArray.get(i).getAsString();
            String[] parts = chain.split("\\.");
            if (parts.length < 2) continue;
            byte[] decoded = Base64.decode(parts[1], Base64.DEFAULT);
            String json = new String(decoded, StandardCharsets.UTF_8);
            JsonObject chainBody = JsonParser.parseString(json).getAsJsonObject();
            if (chainBody.has("extraData")) {
                JsonObject extraData = chainBody.getAsJsonObject("extraData");
                String displayName = extraData.has("displayName") && !extraData.get("displayName").isJsonNull() ? extraData.get("displayName").getAsString() : null;
                String xuid = extraData.has("XUID") && !extraData.get("XUID").isJsonNull() ? extraData.get("XUID").getAsString() : null;
                return new Pair<>(displayName, xuid);
            }
        }
        return null;
    }

    private static String extractXuid(XboxToken xbl) {
        try {
            JsonArray xui = xbl.getDisplayClaims().get("xui").getAsJsonArray();
            JsonObject obj = xui.get(0).getAsJsonObject();
            JsonElement xidEl = obj.get("xid");
            if (xidEl != null && !xidEl.isJsonNull()) return xidEl.getAsString();
        } catch (Throwable t) {
        }
        return null;
    }

    private static Pair<String, String> fetchXboxProfile(OkHttpClient client, XboxToken xstsXboxLive, String xuid) throws Exception {
        String identity = xstsXboxLive.toIdentityToken();
        String profileUrl = (xuid != null && !xuid.isEmpty())
                ? "https://profile.xboxlive.com/users/xuid(" + xuid + ")/profile/settings?settings=Gamertag,PublicGamerpic"
                : "https://profile.xboxlive.com/users/me/profile/settings?settings=Gamertag,PublicGamerpic";
        Request.Builder builder = new Request.Builder().url(profileUrl);
        builder.get();
        builder.addHeader("x-xbl-contract-version", "3");
        builder.addHeader("Authorization", identity);
        builder.addHeader("content-type", "application/json");
        try (Response resp = client.newCall(builder.build()).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                Log.e("XboxProfile", "Failed to fetch Xbox profile: " + resp.code() + " " + resp.message());
                return null;
            }
            String body = resp.body().string();
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String gamertag = null;
            String picUrl = null;
            if (json.has("profileUsers")) {
                JsonArray users = json.getAsJsonArray("profileUsers");
                if (users.size() > 0) {
                    JsonObject user0 = users.get(0).getAsJsonObject();
                    if (user0.has("settings")) {
                        JsonArray settings = user0.getAsJsonArray("settings");
                        for (int i = 0; i < settings.size(); i++) {
                            JsonObject s = settings.get(i).getAsJsonObject();
                            String id = s.has("id") && !s.get("id").isJsonNull() ? s.get("id").getAsString() : null;
                            if ("Gamertag".equals(id)) {
                                gamertag = s.has("value") && !s.get("value").isJsonNull() ? s.get("value").getAsString() : null;
                            } else if ("PublicGamerpic".equals(id)) {
                                picUrl = s.has("value") && !s.get("value").isJsonNull() ? s.get("value").getAsString() : null;
                            }
                        }
                    }
                }
            }
            return new Pair<>(gamertag, picUrl);
        }
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return null;
        String u = url.replace("`", "").trim();
        if (!(u.startsWith("http://") || u.startsWith("https://"))) return null;
        return u;
    }
}