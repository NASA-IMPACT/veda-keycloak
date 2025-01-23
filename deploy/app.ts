#!/usr/bin/env node
import "source-map-support/register";
import { join } from "path";
import * as assert from "assert";
import * as cdk from "aws-cdk-lib";
import { KeycloakStack } from "./lib/KeycloakStack";
import {
  getOauthSecrets,
  getPrivateClientIds,
  arrayStringify,
} from "./lib/utils";

const {
  AWS_ACCOUNT_ID,
  AWS_REGION,
  SSL_CERTIFICATE_ARN,
  HOSTNAME,
  STAGE = "dev",
  KEYCLOAK_VERSION = "26.0.5",
  KEYCLOAK_CONFIG_CLI_VERSION = "latest-26",
  CONFIG_DIR = join(__dirname, "..", "config"),
} = process.env;

assert(SSL_CERTIFICATE_ARN, "SSL_CERTIFICATE_ARN env var is required");
assert(HOSTNAME, "HOSTNAME env var is required");

console.log(`Extracting ARNs of IdP secrets from environment...`);
const idpOauthClientSecrets = getOauthSecrets();
Object.keys(idpOauthClientSecrets).length
  ? console.log(
      `Found IdP client secrets in environment:\n${arrayStringify(
        Object.keys(idpOauthClientSecrets)
      )}`
    )
  : console.warn("No IdP client secrets found in the environment");

console.log(`Extracting OAuth client IDs from keycloak configuration...`);
const privateOauthClients = getPrivateClientIds(join(CONFIG_DIR, "src"));
privateOauthClients.length
  ? console.log(
      `Found client IDs in ${CONFIG_DIR}:\n${arrayStringify(
        privateOauthClients.map(({ id }) => id)
      )}`
    )
  : console.warn(`No client IDs found in ${CONFIG_DIR}`);

const app = new cdk.App();
new KeycloakStack(app, `veda-keycloak-${STAGE}`, {
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
  keycloakConfigCliVersion: KEYCLOAK_CONFIG_CLI_VERSION,
  configDir: CONFIG_DIR,
  idpOauthClientSecrets,
  privateOauthClients,
});
