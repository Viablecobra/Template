package com.origin.launcher.auth;

public class AuthConfig {
    private final String environment;
    private final String sandbox;
    private final String tokenType;
    private final String deviceAuthRP;
    private final String userAuthRP;
    private final String xstsRP;
    private final String siteNameRps;
    private final String authMethodRps;
    private final String defaultTitleTid;

    public AuthConfig(String environment,
                      String sandbox,
                      String tokenType,
                      String deviceAuthRP,
                      String userAuthRP,
                      String xstsRP,
                      String siteNameRps,
                      String authMethodRps,
                      String defaultTitleTid) {
        this.environment = environment;
        this.sandbox = sandbox;
        this.tokenType = tokenType;
        this.deviceAuthRP = deviceAuthRP;
        this.userAuthRP = userAuthRP;
        this.xstsRP = xstsRP;
        this.siteNameRps = siteNameRps;
        this.authMethodRps = authMethodRps;
        this.defaultTitleTid = defaultTitleTid;
    }

    public static AuthConfig productionRetailJwtDefault() {
        return new AuthConfig(
                "Production",
                "RETAIL",
                "JWT",
                "http://auth.xboxlive.com",
                "http://auth.xboxlive.com",
                MsftAuthManager.DEFAULT_XSTS_RELYING_PARTY,
                "user.auth.xboxlive.com",
                "RPS",
                "1739947436"
        );
    }

    public String environment() { return environment; }
    public String sandbox() { return sandbox; }
    public String tokenType() { return tokenType; }
    public String deviceAuthRP() { return deviceAuthRP; }
    public String userAuthRP() { return userAuthRP; }
    public String xstsRP() { return xstsRP; }
    public String siteNameRps() { return siteNameRps; }
    public String authMethodRps() { return authMethodRps; }
    public String defaultTitleTid() { return defaultTitleTid; }
}