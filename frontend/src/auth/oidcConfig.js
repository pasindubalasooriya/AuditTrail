// OIDC settings for logging in against WSO2 Identity Server (Authorization Code + PKCE).
// We provide the WSO2 endpoints explicitly (rather than relying on discovery) so there are
// no surprises with the issuer path.

const WSO2 = "https://localhost:9443";

export const oidcConfig = {
  authority: `${WSO2}/oauth2/token`,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: "http://localhost:5173/",
  post_logout_redirect_uri: "http://localhost:5173/",
  response_type: "code",
  scope: "openid roles",

  // Explicit WSO2 endpoints (public SPA client, PKCE handled by oidc-client-ts)
  metadata: {
    issuer: `${WSO2}/oauth2/token`,
    authorization_endpoint: `${WSO2}/oauth2/authorize`,
    token_endpoint: `${WSO2}/oauth2/token`,
    end_session_endpoint: `${WSO2}/oidc/logout`,
    jwks_uri: `${WSO2}/oauth2/jwks`,
    userinfo_endpoint: `${WSO2}/oauth2/userinfo`,
  },

  automaticSilentRenew: false,

  // After WSO2 redirects back with ?code=..., strip the query params from the URL bar
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
