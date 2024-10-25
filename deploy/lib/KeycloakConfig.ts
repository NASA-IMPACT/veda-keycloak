import * as cdk from "aws-cdk-lib";
import * as ecrAssets from "aws-cdk-lib/aws-ecr-assets";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as secretsManager from "aws-cdk-lib/aws-secretsmanager";
import * as customResources from "aws-cdk-lib/custom-resources";
import { Construct } from "constructs";

interface KeycloakConfigConstructProps {
  cluster: ecs.ICluster;
  securityGroupIds: string[];
  subnetIds: string[];
  adminSecret: secretsManager.ISecret;
  hostname: string;
  configDir: string;
  githubOauthClientSecret: string;
}

export class KeycloakConfig extends Construct {
  constructor(
    scope: Construct,
    id: string,
    props: KeycloakConfigConstructProps
  ) {
    super(scope, id);

    const configTaskDef = new ecs.FargateTaskDefinition(this, "ConfigTaskDef", {
      cpu: 256,
      memoryLimitMiB: 512,
    });

    const assetImage = ecs.ContainerImage.fromAsset(props.configDir, {
      platform: ecrAssets.Platform.LINUX_AMD64,
    });

    const githubOauthClientSecret = secretsManager.Secret.fromSecretCompleteArn(
      this,
      "GithubClientSecret",
      props.githubOauthClientSecret
    );

    configTaskDef.addContainer("ConfigContainer", {
      image: assetImage,
      environment: {
        KEYCLOAK_URL: props.hostname,
        KEYCLOAK_AVAILABILITYCHECK_ENABLED: "true",
        KEYCLOAK_AVAILABILITYCHECK_TIMEOUT: "120s",
        IMPORT_FILES_LOCATIONS: "/config/*",
        IMPORT_CACHE_ENABLED: "false",
        IMPORT_VARSUBSTITUTION_ENABLED: "true",
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: "KeycloakConfig" }),
      secrets: {
        KEYCLOAK_USER: ecs.Secret.fromSecretsManager(
          props.adminSecret,
          "username"
        ),
        KEYCLOAK_PASSWORD: ecs.Secret.fromSecretsManager(
          props.adminSecret,
          "password"
        ),
        GH_CLIENT_ID: ecs.Secret.fromSecretsManager(
          githubOauthClientSecret,
          "id"
        ),
        GH_CLIENT_SECRET: ecs.Secret.fromSecretsManager(
          githubOauthClientSecret,
          "secret"
        ),
      },
    });

    const applyConfigLambda = new lambda.Function(this, "ApplyConfigLambda", {
      code: lambda.Code.fromInline(`
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
            const { taskArn, clusterArn } = result.tasks[0];
            return { taskArn, clusterArn };
          } catch (error) {
            console.error('Error running ECS task:', error);
            throw new Error('Failed to start ECS task');
          }
        };
      `),
      handler: "index.handler",
      runtime: lambda.Runtime.NODEJS_LATEST,
      timeout: cdk.Duration.minutes(5),
    });

    configTaskDef.grantRun(applyConfigLambda);

    new cdk.CfnOutput(this, "ConfigLambdaArn", {
      key: "ConfigLambdaArn",
      value: applyConfigLambda.functionArn,
    });
  }
}
