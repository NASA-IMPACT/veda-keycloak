from aws_cdk import (
    Duration,
    CfnOutput,
    aws_ecr_assets as ecr_assets,
    aws_ecs as ecs,
    aws_lambda as _lambda,
    aws_secretsmanager as secretsmanager,
)
from constructs import Construct


class KeycloakConfig(Construct):
    """
    Responsible for creating infrastructure to apply configuration to a Keycloak instance.
    """
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        cluster: ecs.ICluster,
        security_group_ids: list[str],
        subnet_ids: list[str],
        admin_secret: secretsmanager.ISecret,
        hostname: str,
        app_dir: str,
        idp_oauth_client_secrets: dict[str, str],
        private_oauth_clients: list[dict[str, str]],
        version: str,
        **kwargs,
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create a client secret for each private OAuth client
        created_client_secrets = []
        for client_info in private_oauth_clients:
            client_slug = client_info["id"]
            realm = client_info["realm"]
            secret = secretsmanager.Secret(
                self,
                f"{client_slug}-client-secret",
                secret_name=f"{self.node.try_get_context('stackName') or ''}-client-{client_slug}",
                generate_secret_string=secretsmanager.SecretStringGenerator(
                    exclude_punctuation=True,
                    include_space=False,
                    secret_string_template=(
                        f"{{"
                        f'"id": "{client_slug}",'
                        f'"auth_url": "{hostname}/realms/{realm}/protocol/openid-connect/auth",'
                        f'"token_url": "{hostname}/realms/{realm}/protocol/openid-connect/token",'
                        f'"userinfo_url": "{hostname}/realms/{realm}/protocol/openid-connect/userinfo"'
                        f"}}"
                    ),
                    generate_string_key="secret",
                    password_length=16,
                ),
            )
            created_client_secrets.append((client_slug, secret))

        # Import the client secrets for existing/public clients
        imported_client_secrets = []
        for client_slug, secret_arn in idp_oauth_client_secrets.items():
            imported_secret = secretsmanager.Secret.from_secret_complete_arn(
                self, f"{client_slug}-client-secret", secret_arn
            )
            imported_client_secrets.append((client_slug, imported_secret))

        # Combine newly created and imported secrets, then construct ECS secrets
        task_client_secrets = {}
        for client_slug, secret in created_client_secrets + imported_client_secrets:
            for key in ["id", "secret"]:
                # Example: GRAFANA_CLIENT_ID or GRAFANA_CLIENT_SECRET
                env_var = f"{client_slug}_CLIENT_{key}".upper()
                task_client_secrets[env_var] = ecs.Secret.from_secrets_manager(
                    secret, key
                )

        # Create the Fargate task definition for applying Keycloak configuration
        config_task_def = ecs.FargateTaskDefinition(
            self, "ConfigTaskDef", cpu=256, memory_limit_mib=512
        )

        container_name = "ConfigContainer"
        config_task_def.add_container(
            container_name,
            container_name=container_name,
            image=ecs.ContainerImage.from_asset(
                directory=app_dir,
                platform=ecr_assets.Platform.LINUX_AMD64,
                build_args={"KEYCLOAK_CONFIG_CLI_VERSION": version},
            ),
            environment={
                "KEYCLOAK_URL": hostname,
                "KEYCLOAK_AVAILABILITYCHECK_ENABLED": "true",
                "KEYCLOAK_AVAILABILITYCHECK_TIMEOUT": "120s",
                "IMPORT_FILES_LOCATIONS": "/config/*",
                "IMPORT_CACHE_ENABLED": "false",
                "IMPORT_VARSUBSTITUTION_ENABLED": "true",
            },
            logging=ecs.LogDrivers.aws_logs(stream_prefix="KeycloakConfig"),
            secrets={
                "KEYCLOAK_USER": ecs.Secret.from_secrets_manager(
                    admin_secret, "username"
                ),
                "KEYCLOAK_PASSWORD": ecs.Secret.from_secrets_manager(
                    admin_secret, "password"
                ),
                **task_client_secrets,  # Merge the generated client secrets
            },
        )

        # Lambda to trigger ECS RunTask
        apply_config_lambda = _lambda.Function(
            self,
            "ApplyConfigLambda",
            code=_lambda.Code.from_inline(
                """
                const { ECSClient, RunTaskCommand } = require('@aws-sdk/client-ecs');

                const ecsClient = new ECSClient({});

                exports.handler = async function(event) {
                  console.log('Received event:', event);
                  const params = {
                    cluster: '%s',
                    taskDefinition: '%s',
                    launchType: 'FARGATE',
                    overrides: {
                      containerOverrides: [
                        {
                          name: '%s',
                          environment: Object.entries(event).map(([name, value]) => ({
                            name,
                            value: String(value),
                          })),
                        },
                      ],
                    },
                    networkConfiguration: {
                      awsvpcConfiguration: {
                        subnets: %s,
                        securityGroups: %s,
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
                """
                % (
                    cluster.cluster_name,
                    config_task_def.task_definition_arn,
                    container_name,
                    str(subnet_ids),
                    str(security_group_ids),
                )
            ),
            handler="index.handler",
            runtime=_lambda.Runtime.NODEJS_18_X,  # or NODEJS_LATEST if using a newer CDK
            timeout=Duration.minutes(5),
        )

        # Grant the Lambda permission to run the ECS task
        config_task_def.grant_run(apply_config_lambda)

        # Output the Lambda ARN
        CfnOutput(
            self,
            "ConfigLambdaArn",
            value=apply_config_lambda.function_arn,
        )
