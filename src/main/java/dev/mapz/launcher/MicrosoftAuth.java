package dev.mapz.launcher;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft Services authentication.
 *
 * Set MAPZ_MICROSOFT_CLIENT_ID to a Microsoft public-client application ID
 * that is approved for the XboxLive.signin scope before shipping the launcher.
 */
final class MicrosoftAuth {
    private static final String CLIENT_ID = System.getenv().getOrDefault("MAPZ_MICROSOFT_CLIENT_ID", "");
    private static final String DEVICE_ENDPOINT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_ENDPOINT = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_ENDPOINT = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_ENDPOINT = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String PROFILE_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    interface Listener {
        void onDeviceCode(String message, String url);
        void onSuccess(MinecraftProfile profile);
        void onError(String message);
    }

    void signIn(Listener listener) {
        Thread.startVirtualThread(() -> {
            try {
                if (CLIENT_ID.isBlank()) {
                    throw new IOException("Microsoft sign-in is not configured yet. Set MAPZ_MICROSOFT_CLIENT_ID to your approved Microsoft public-client ID.");
                }

                DeviceCode device = requestDeviceCode();
                listener.onDeviceCode(device.message, device.verificationUri);
                String microsoftToken = pollForToken(device);

                String xblJson = postJson(XBL_ENDPOINT,
                        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
                                + escape(microsoftToken) + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}");
                String xblToken = required(xblJson, "Token");
                String uhs = requiredNestedXui(xblJson);

                String xstsJson = postJson(XSTS_ENDPOINT,
                        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
                                + escape(xblToken) + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}");
                String xstsToken = required(xstsJson, "Token");

                String mcJson = postJson(MC_LOGIN_ENDPOINT,
                        "{\"identityToken\":\"XBL3.0 x=" + escape(uhs) + ";" + escape(xstsToken) + "\"}");
                String mcToken = required(mcJson, "access_token");

                HttpRequest profileRequest = HttpRequest.newBuilder(URI.create(PROFILE_ENDPOINT))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + mcToken)
                        .header("Accept", "application/json")
                        .GET().build();
                HttpResponse<String> profileResponse = http.send(profileRequest, HttpResponse.BodyHandlers.ofString());
                if (profileResponse.statusCode() != 200) {
                    throw new IOException("Minecraft profile could not be loaded. Make sure this Microsoft account owns Minecraft: Java Edition.");
                }

                String profileJson = profileResponse.body();
                String name = required(profileJson, "name");
                String uuid = required(profileJson, "id");
                String skinUrl = firstSkinUrl(profileJson);
                String model = profileJson.contains("\"variant\":\"SLIM\"") ? "SLIM" : "CLASSIC";
                listener.onSuccess(new MinecraftProfile(name, uuid, skinUrl, model, mcToken));
            } catch (Exception ex) {
                listener.onError(ex.getMessage() == null ? "Microsoft sign-in failed." : ex.getMessage());
            }
        });
    }

    private DeviceCode requestDeviceCode() throws Exception {
        String form = form("client_id", CLIENT_ID, "scope", "XboxLive.signin offline_access");
        HttpRequest request = HttpRequest.newBuilder(URI.create(DEVICE_ENDPOINT))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Microsoft rejected the sign-in request.");
        String json = response.body();
        return new DeviceCode(required(json, "device_code"), required(json, "user_code"),
                required(json, "verification_uri"), required(json, "message"), number(json, "interval", 5),
                number(json, "expires_in", 900));
    }

    private String pollForToken(DeviceCode device) throws Exception {
        long deadline = System.currentTimeMillis() + device.expiresIn * 1000L;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(Math.max(5, device.interval) * 1000L);
            String form = form("grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", CLIENT_ID, "device_code", device.deviceCode);
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return required(response.body(), "access_token");
            String error = optional(response.body(), "error");
            if (!"authorization_pending".equals(error) && !"slow_down".equals(error)) {
                throw new IOException("Microsoft sign-in was cancelled or failed.");
            }
            if ("slow_down".equals(error)) Thread.sleep(5000);
        }
        throw new IOException("Microsoft sign-in timed out. Please try again.");
    }

    private String postJson(String endpoint, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Minecraft authentication was rejected (HTTP " + response.statusCode() + ").");
        }
        return response.body();
    }

    private static String form(String... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i += 2) {
            if (i > 0) out.append('&');
            out.append(URLEncoder.encode(values[i], StandardCharsets.UTF_8));
            out.append('=').append(URLEncoder.encode(values[i + 1], StandardCharsets.UTF_8));
        }
        return out.toString();
    }

    private static String required(String json, String key) throws IOException {
        String value = optional(json, key);
        if (value == null || value.isBlank()) throw new IOException("Microsoft returned an incomplete sign-in response.");
        return value;
    }

    private static String optional(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"") : null;
    }

    private static String requiredNestedXui(String json) throws IOException {
        Matcher matcher = Pattern.compile("\\\"xui\\\"\\s*:\\s*\\[\\s*\\{\\s*\\\"uhs\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        if (!matcher.find()) throw new IOException("Xbox Live did not return a user identity.");
        return matcher.group(1);
    }

    private static String firstSkinUrl(String json) {
        Matcher matcher = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"(https?://textures\\.minecraft\\.net/texture/[^\\\"]+)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static long number(String json, String key, long fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\d+)").matcher(json);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : fallback;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    record DeviceCode(String deviceCode, String userCode, String verificationUri, String message, long interval, long expiresIn) {}
    record MinecraftProfile(String name, String uuid, String skinUrl, String model, String accessToken) {}
}
