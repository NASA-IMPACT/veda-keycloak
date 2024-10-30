/**
 * Helper function to extract OAuth client secrets from the runtime environment
 * @returns Record<string, string> - A map of OAuth client IDs to the ARN of their secrets
 */
export function getOauthSecrets(): Record<string, string> {
  const oauthSecretPrefix = "IDP_SECRET_ARN_";
  const clientSecrets = Object.entries(process.env)
    .filter(([k, v]) => k.startsWith(oauthSecretPrefix))
    .map(([k, v]) => [k.split(oauthSecretPrefix)[1], v]);
  return Object.fromEntries(clientSecrets);
}
