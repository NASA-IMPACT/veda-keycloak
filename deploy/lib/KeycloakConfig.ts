import * as cdk from "aws-cdk-lib";
import * as ecrAssets from "aws-cdk-lib/aws-ecr-assets";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as secretsManager from "aws-cdk-lib/aws-secretsmanager";
import { Construct } from "constructs";

interface KeycloakConfigConstructProps {
  cluster: ecs.ICluster;
  securityGroupIds: string[];
  subnetIds: string[];
  adminSecret: secretsManager.ISecret;
  hostname: string;
  configDir: string;
  idpOauthClientSecrets: Record<string, string>;
  createdOauthClients: string[];
}

type clientSecretTuple = Array<[string, secretsManager.ISecret]>;

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

    // Create a client secret for each private client
    const createdClientSecrets: clientSecretTuple =
      props.createdOauthClients.map((clientSlug) => [
        clientSlug,
        new secretsManager.Secret(this, `${clientSlug}-client-secret`, {
          generateSecretString: {
            excludePunctuation: true,
            includeSpace: false,
            secretStringTemplate: JSON.stringify({ id: clientSlug }),
            generateStringKey: "secret",
            passwordLength: 16,
          },
        }),
      ]);

    // Import the client secrets for each public clients
    const importedClientSecrets: clientSecretTuple = Object.entries(
      props.idpOauthClientSecrets
    ).map(([clientSlug, secretArn]): [string, secretsManager.ISecret] => [
      clientSlug,
      secretsManager.Secret.fromSecretCompleteArn(
        this,
        `${clientSlug}-client-secret`,
        secretArn
      ),
    ]);

    // Create env vars from secrets for each client, e.g. GRAFANA_CLIENT_ID, GRAFANA_CLIENT_SECRET
    const taskClientSecrets = Object.fromEntries(
      [...createdClientSecrets, ...importedClientSecrets].flatMap(
        ([clientSlug, secret]) =>
          ["id", "secret"].map((key) => [
            `${clientSlug}_CLIENT_${key}`.toUpperCase(),
            ecs.Secret.fromSecretsManager(secret, key),
          ])
      )
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
        // Inject the client ID and secret for any OAuth clients
        ...taskClientSecrets,
      },
    });

    // Helper to simplify triggering the ECS task
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
