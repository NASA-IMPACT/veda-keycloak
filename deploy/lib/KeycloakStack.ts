import * as cdk from "aws-cdk-lib";
import { Vpc } from "aws-cdk-lib/aws-ec2";
import { KeycloakDatabase } from "./KeycloakDatabase";
import { KeycloakService } from "./KeycloakService";
import { KeycloakConfig } from "./KeycloakConfig";
import { KeycloakUrl } from "./KeycloakUrl";

export interface StackInputProps {
  hostname: string;
  sslCertificateArn: string;
  keycloakVersion: string;
  configDir: string;
}

interface StackProps extends cdk.StackProps, StackInputProps {
  vpcId?: string;
}

export class KeycloakStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props: StackProps) {
    super(scope, id, props);

    const vpc = props.vpcId
      ? Vpc.fromLookup(this, "Vpc", { vpcId: props.vpcId })
      : new Vpc(this, "vpc");

    const databaseName = "keycloak";
    const { database } = new KeycloakDatabase(this, "database", {
      vpc,
      databaseName,
    });

    const { albService, adminSecret } = new KeycloakService(this, "service", {
      vpc,
      databaseName,
      databaseInstance: database,
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
    });

    new KeycloakUrl(this, "url", {
      hostname: props.hostname,
      alb: albService.loadBalancer,
    });
  }
}
