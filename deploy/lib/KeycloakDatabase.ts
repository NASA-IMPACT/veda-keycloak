import * as cdk from "aws-cdk-lib";
import {
  DatabaseInstance,
  DatabaseInstanceEngine,
  DatabaseInstanceProps,
  PostgresEngineVersion,
} from "aws-cdk-lib/aws-rds";
import {
  InstanceType,
  InstanceClass,
  InstanceSize,
  IVpc,
} from "aws-cdk-lib/aws-ec2";
import { Construct } from "constructs";

export interface DatabaseProps extends Omit<DatabaseInstanceProps, "engine"> {
  vpc: IVpc;
  databaseName: string;
}

export class KeycloakDatabase extends Construct {
  database: DatabaseInstance;
  constructor(scope: Construct, id: string, props: DatabaseProps) {
    super(scope, id);

    this.database = new DatabaseInstance(this, "KeycloakPostgres", {
      instanceIdentifier: props.instanceIdentifier,
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_16_4,
      }),
      instanceType: InstanceType.of(
        InstanceClass.BURSTABLE4_GRAVITON,
        InstanceSize.MEDIUM
      ),
      removalPolicy: cdk.RemovalPolicy.DESTROY, // For dev environments
      ...props,
    });
  }
}
