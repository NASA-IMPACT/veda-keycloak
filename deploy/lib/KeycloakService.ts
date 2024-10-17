import * as cdk from "aws-cdk-lib";
import { ContainerImage, Secret as ecsSecret } from "aws-cdk-lib/aws-ecs";
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns";
import { Secret, ISecret } from "aws-cdk-lib/aws-secretsmanager";
import { Certificate } from "aws-cdk-lib/aws-certificatemanager";
import { Construct } from "constructs";
import { StackInputProps } from "./KeycloakStack";
import { IVpc } from "aws-cdk-lib/aws-ec2";
import { DatabaseInstance } from "aws-cdk-lib/aws-rds";

interface KeycloakServiceProps extends StackInputProps {
  vpc: IVpc;
  databaseName: string;
  databaseInstance: DatabaseInstance;
}

export class KeycloakService extends Construct {
  albService: ApplicationLoadBalancedFargateService;
  adminSecret: Secret;

  constructor(scope: Construct, id: string, props: KeycloakServiceProps) {
    super(scope, id);

    // Secrets for Keycloak admin and DB password
    if (!props.databaseInstance.secret) throw new Error("DB secret not found");

    // Secrets for Keycloak admin and DB password
    this.adminSecret = new Secret(this, "admin-creds", {
      generateSecretString: {
        excludePunctuation: true,
        includeSpace: false,
        secretStringTemplate: JSON.stringify({ username: "admin" }),
        generateStringKey: "password",
        passwordLength: 16,
      },
    });

    // Secret factories for ECS secrets
    const ecsSecretFactory = (secret: ISecret) => (val: string) =>
      ecsSecret.fromSecretsManager(secret, val);
    const ecsDbSecret = ecsSecretFactory(props.databaseInstance.secret);
    const ecsAdminSecret = ecsSecretFactory(this.adminSecret);

    // SSL Certificate for the Load Balancer
    const certificate = Certificate.fromCertificateArn(
      this,
      "SSLCertificate",
      props.sslCertificateArn
    );

    // Keycloak Ports
    const appPort = 8080;
    const healthManagementPort = 9000;

    // Fargate Service with ALB, SSL, and Health Check
    this.albService = new ApplicationLoadBalancedFargateService(
      this,
      "service",
      {
        vpc: props.vpc,
        desiredCount: 1,
        publicLoadBalancer: true,
        listenerPort: 443,
        certificate,
        memoryLimitMiB: 2048,
        cpu: 1024,
        healthCheckGracePeriod: cdk.Duration.seconds(120),
        redirectHTTP: false,
        taskImageOptions: {
          containerName: "keycloak",
          containerPort: appPort,
          image: ContainerImage.fromRegistry(
            "quay.io/keycloak/keycloak:26.0.0"
          ),
          entryPoint: ["/opt/keycloak/bin/kc.sh"],
          command: ["start"],
          environment: {
            KC_DB_URL_DATABASE: props.databaseName,
            KC_HOSTNAME: props.hostname,
            KC_HTTP_ENABLED: "true",
            KC_HTTP_MANAGEMENT_PORT: healthManagementPort.toString(),
          },
          secrets: {
            // Database
            KC_DB: ecsDbSecret("engine"),
            KC_DB_USERNAME: ecsDbSecret("username"),
            KC_DB_PASSWORD: ecsDbSecret("password"),
            KC_DB_URL_HOST: ecsDbSecret("host"),
            KC_DB_URL_PORT: ecsDbSecret("port"),

            // Admin
            KC_BOOTSTRAP_ADMIN_USERNAME: ecsAdminSecret("username"),
            KC_BOOTSTRAP_ADMIN_PASSWORD: ecsAdminSecret("password"),
          },
        },
      }
    );

    // TODO: Configure health check on port 9000 with the path '/health'
    this.albService.targetGroup.configureHealthCheck({
      path: "/admin/master/console/",
      healthyThresholdCount: 3,
    });

    props.databaseInstance.connections.allowDefaultPortFrom(
      this.albService.service
    );
  }
}