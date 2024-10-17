import * as route53 from "aws-cdk-lib/aws-route53";
import * as route53Targets from "aws-cdk-lib/aws-route53-targets";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import { Construct } from "constructs";
import { CfnOutput, Stack } from "aws-cdk-lib";

interface KeycloakUrlProps {
  /**
   * The full domain name, e.g., "keycloak.foo.com"
   */
  hostname: string;
  alb: elbv2.ApplicationLoadBalancer;
}

export class KeycloakUrl extends Construct {
  constructor(scope: Construct, id: string, props: KeycloakUrlProps) {
    super(scope, id);

    // Remove the protocol (http:// or https://) if present
    const cleanedHostname = props.hostname.replace(/(^\w+:|^)\/\//, "");

    // Extract the domain and subdomain from the cleaned hostname
    const parts = cleanedHostname.split(".");
    if (parts.length < 3) {
      throw new Error(
        'Hostname must be a fully qualified domain name, e.g., "keycloak.foo.com".'
      );
    }

    const subdomain = parts.slice(0, -2).join("."); // "keycloak" (or any subdomain prefix)
    const domainName = parts.slice(-2).join("."); // "foo.com"

    // Lookup the hosted zone for the domain
    const hostedZone = route53.HostedZone.fromLookup(this, "HostedZone", {
      domainName: domainName,
    });

    // Create or replace an A record for the provided subdomain
    const record = new route53.ARecord(this, "AliasRecord", {
      zone: hostedZone,
      recordName: subdomain, // "keycloak" part of "keycloak.foo.com"
      target: route53.RecordTarget.fromAlias(
        new route53Targets.LoadBalancerTarget(props.alb)
      ),
      deleteExisting: true,
      comment: `Alias record for Keycloak, created by ${
        Stack.of(this).stackName
      }`,
    });

    new CfnOutput(this, "Arecord", {
      key: "aRecord",
      value: record.domainName,
    });
    new CfnOutput(this, "Url", {
      key: "Url",
      value: `https://${record.domainName}`,
    });
  }
}
