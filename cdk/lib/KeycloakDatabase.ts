import * as cdk from "aws-cdk-lib";
import * as rds from "aws-cdk-lib/aws-rds";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import { Construct } from "constructs";

export interface DatabaseProps
  extends Omit<rds.DatabaseInstanceProps, "engine"> {
  vpc: ec2.IVpc;
  databaseName: string;
}

export class KeycloakDatabase extends Construct {
  database: rds.DatabaseInstance;
  constructor(scope: Construct, id: string, props: DatabaseProps) {
    super(scope, id);

    this.database = new rds.DatabaseInstance(this, "KeycloakPostgres", {
      instanceIdentifier: props.instanceIdentifier,
      engine: rds.DatabaseInstanceEngine.postgres({
        version: rds.PostgresEngineVersion.VER_16_4,
      }),
      instanceType: ec2.InstanceType.of(
        ec2.InstanceClass.BURSTABLE4_GRAVITON,
        ec2.InstanceSize.MEDIUM
      ),
      removalPolicy: cdk.RemovalPolicy.DESTROY, // For dev environments
      ...props,
    });
  }
}
