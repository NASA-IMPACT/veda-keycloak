import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import { KeycloakDatabase } from "./KeycloakDatabase";
import { KeycloakService } from "./KeycloakService";
import { KeycloakConfig } from "./KeycloakConfig";
import { KeycloakUrl } from "./KeycloakUrl";

export interface StackInputProps {
  hostname: string;
  sslCertificateArn: string;
  keycloakVersion: string;
  configDir: string;
  idpOauthClientSecrets: Record<string, string>;
  privateOauthClients: Array<{ id: string; realm: string }>;
}

interface StackProps extends cdk.StackProps, StackInputProps {
  vpcId?: string;
}

export class KeycloakStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props: StackProps) {
    super(scope, id, props);

    const vpc = props.vpcId
      ? ec2.Vpc.fromLookup(this, "Vpc", { vpcId: props.vpcId })
      : new ec2.Vpc(this, "vpc");

    const databaseName = "keycloak";
    const { database } = new KeycloakDatabase(this, "database", {
      vpc,
      databaseName,
    });

    const { albService, adminSecret } = new KeycloakService(this, "service", {
      vpc,
      databaseName,
      databaseInstance: database,
      version: props.keycloakVersion,
      ...props,
    });

    new KeycloakConfig(this, "config", {
      cluster: albService.cluster,
      securityGroupIds: albService.service.connections.securityGroups.map(
        (sg) => sg.securityGroupId
      ),
      hostname: props.hostname,
      subnetIds: vpc.publicSubnets.map((subnet) => subnet.subnetId),
      adminSecret: adminSecret,
      configDir: props.configDir,
      idpOauthClientSecrets: props.idpOauthClientSecrets,
      privateOauthClients: props.privateOauthClients,
    });

    new KeycloakUrl(this, "url", {
      hostname: props.hostname,
      alb: albService.loadBalancer,
    });
  }
}
