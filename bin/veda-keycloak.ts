#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { KeycloakStack } from "../lib/keycloak-stack";

const app = new cdk.App();
new KeycloakStack(app, `VedaKeycloakStack-${process.env.STAGE || "dev"}`, {
  /* If you don't specify 'env', this stack will be environment-agnostic.
   * Account/Region-dependent features and context lookups will not work,
   * but a single synthesized template can be deployed anywhere. */
  /* Uncomment the next line to specialize this stack for the AWS Account
   * and Region that are implied by the current CLI configuration. */
  // env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },
  /* Uncomment the next line if you know exactly what Account and Region you
   * want to deploy the stack to. */
  env: { account: "853558080719", region: "us-west-2" },
  /* For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html */
  sslCertificateArn:
    "arn:aws:acm:us-west-2:853558080719:certificate/012bf180-dc4c-4dee-9cba-d3eb64607e13",
  hostname: "https://keycloak.delta-backend.xyz",
  keycloakVersion: "26.0.0",
});
