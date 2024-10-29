#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { KeycloakStack } from "./lib/KeycloakStack";
import { join } from "path";
import assert = require("assert");

const {
  AWS_ACCOUNT_ID,
  AWS_REGION,
  SSL_CERTIFICATE_ARN,
  HOSTNAME,
  STAGE = "dev",
  KEYCLOAK_VERSION = "26.0.0",
} = process.env;

assert(SSL_CERTIFICATE_ARN, "SSL_CERTIFICATE_ARN env var is required");
assert(HOSTNAME, "HOSTNAME env var is required");

const oAuthClientSecrets = getOauthSecrets();
console.log(
  `Found ${
    Object.keys(oAuthClientSecrets).length
  } OAuth client secrets for clients: ${Object.keys(oAuthClientSecrets).join(
    ", "
  )}`
);

const app = new cdk.App();
new KeycloakStack(app, `VedaKeycloakStack-${STAGE}`, {
  /* If you don't specify 'env', this stack will be environment-agnostic.
   * Account/Region-dependent features and context lookups will not work,
   * but a single synthesized template can be deployed anywhere. */
  /* Uncomment the next line to specialize this stack for the AWS Account
   * and Region that are implied by the current CLI configuration. */
  // env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },
  /* Uncomment the next line if you know exactly what Account and Region you
   * want to deploy the stack to. */
  env: { account: AWS_ACCOUNT_ID, region: AWS_REGION },
  /* For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html */
  sslCertificateArn: SSL_CERTIFICATE_ARN,
  hostname: HOSTNAME,
  keycloakVersion: KEYCLOAK_VERSION,
  configDir: join(__dirname, "..", "config"),
  oAuthClientSecrets,
});

/**
 * Helper function to extract OAuth client secrets from the runtime environment
 * @returns Record<string, string> - A map of OAuth client IDs to the ARN of their secrets
 */
function getOauthSecrets(): Record<string, string> {
  const oauthSecretPrefix = "IDP_SECRET_ARN_";
  const clientSecrets = Object.entries(process.env)
    .filter(([k, v]) => k.startsWith(oauthSecretPrefix))
    .map(([k, v]) => [k.split(oauthSecretPrefix)[1], v]);
  return Object.fromEntries(clientSecrets);
}
