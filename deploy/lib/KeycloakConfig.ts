import { CustomResource, Duration } from "aws-cdk-lib";
import { Platform } from "aws-cdk-lib/aws-ecr-assets";
import {
  ContainerImage,
  FargateTaskDefinition,
  ICluster,
  LogDrivers,
  Secret,
} from "aws-cdk-lib/aws-ecs";
import { Code, Function, Runtime } from "aws-cdk-lib/aws-lambda";
import { ISecret } from "aws-cdk-lib/aws-secretsmanager";
import { Provider } from "aws-cdk-lib/custom-resources";
import { Construct } from "constructs";

interface KeycloakConfigConstructProps {
  cluster: ICluster;
  securityGroupIds: string[];
  subnetIds: string[];
  adminSecret: ISecret;
  hostname: string;
  configDir: string;
}

export class KeycloakConfig extends Construct {
  constructor(
    scope: Construct,
    id: string,
    props: KeycloakConfigConstructProps
  ) {
    super(scope, id);

    const configTaskDef = new FargateTaskDefinition(this, "ConfigTaskDef", {
      cpu: 256,
      memoryLimitMiB: 512,
    });

    const assetImage = ContainerImage.fromAsset(props.configDir, {
      platform: Platform.LINUX_AMD64,
    });

    configTaskDef.addContainer("ConfigContainer", {
      image: assetImage,
      environment: {
        KEYCLOAK_URL: props.hostname,
        KEYCLOAK_AVAILABILITYCHECK_ENABLED: "true",
        KEYCLOAK_AVAILABILITYCHECK_TIMEOUT: "120s",
        IMPORT_FILES_LOCATIONS: "/config/*",
      },
      logging: LogDrivers.awsLogs({ streamPrefix: "KeycloakConfig" }),
      secrets: {
        KEYCLOAK_USER: Secret.fromSecretsManager(props.adminSecret, "username"),
        KEYCLOAK_PASSWORD: Secret.fromSecretsManager(
          props.adminSecret,
          "password"
        ),
      },
    });

    const runTaskLambda = new Function(this, "RunTaskLambda", {
      code: Code.fromInline(`
        const { ECSClient, RunTaskCommand } = require('@aws-sdk/client-ecs');

        const ecsClient = new ECSClient({});

        exports.handler = async function(event) {
          const params = {
            cluster: '${props.cluster.clusterName}',
            taskDefinition: '${configTaskDef.taskDefinitionArn}',
            launchType: 'FARGATE',
            networkConfiguration: {
              awsvpcConfiguration: {
                subnets: ${JSON.stringify(props.subnetIds)},
                securityGroups: ${JSON.stringify(props.securityGroupIds)},
                assignPublicIp: 'ENABLED',
              },
            },
          };
          
          try {
            const result = await ecsClient.send(new RunTaskCommand(params));
            console.log('ECS RunTask result:', result);
            return { status: 'Task started' };
          } catch (error) {
            console.error('Error running ECS task:', error);
            throw new Error('Failed to start ECS task');
          }
        };
      `),
      handler: "index.handler",
      runtime: Runtime.NODEJS_LATEST,
      timeout: Duration.minutes(5),
    });

    configTaskDef.grantRun(runTaskLambda);

    const provider = new Provider(this, "Provider", {
      onEventHandler: runTaskLambda,
    });

    new CustomResource(this, "TriggerConfigTask", {
      serviceToken: provider.serviceToken,
    });
  }
}
