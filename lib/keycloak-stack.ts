import * as cdk from "aws-cdk-lib";
import { Vpc, IVpc } from "aws-cdk-lib/aws-ec2";
import {
  FargateTaskDefinition,
  ContainerImage,
  Secret as ecsSecret,
  LogDriver,
  AwsLogDriver,
  ListenerConfig,
} from "aws-cdk-lib/aws-ecs";
import { ApplicationLoadBalancedFargateService } from "aws-cdk-lib/aws-ecs-patterns";
import { Secret, ISecret } from "aws-cdk-lib/aws-secretsmanager";
import { LogGroup, RetentionDays } from "aws-cdk-lib/aws-logs";
import {
  DatabaseInstance,
  DatabaseInstanceEngine,
  DatabaseInstanceProps,
  PostgresEngineVersion,
} from "aws-cdk-lib/aws-rds";
import { InstanceType, InstanceClass, InstanceSize } from "aws-cdk-lib/aws-ec2";
import { Certificate } from "aws-cdk-lib/aws-certificatemanager";
import { Construct } from "constructs";
import {
  ApplicationProtocol,
  ApplicationTargetGroup,
} from "aws-cdk-lib/aws-elasticloadbalancingv2";

export class KeycloakStack extends cdk.Stack {
  constructor(scope: cdk.App, id: string, props: StackProps) {
    super(scope, id, props);

    const vpc = props.vpcId
      ? Vpc.fromLookup(this, "Vpc", { vpcId: props.vpcId })
      : new Vpc(this, "vpc");

    // RDS PostgreSQL Instance
    const databaseName = "keycloak";
    const { database } = new KeycloakDatabase(this, "KeycloakDatabase", {
      vpc,
      databaseName,
    });

    const { albService } = new KeycloakService(this, "KeycloakService", {
      vpc,
      databaseName,
      databaseInstance: database,
      ...props,
    });
  }
}

class KeycloakDatabase extends Construct {
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

class KeycloakService extends Construct {
  albService: ApplicationLoadBalancedFargateService;

  config = JSON.stringify(
    `
<?xml version="1.0" encoding="UTF-8"?>
<infinispan
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="urn:infinispan:config:11.0 http://www.infinispan.org/schemas/infinispan-config-11.0.xsd"
  xmlns="urn:infinispan:config:11.0"
>
  <jgroups>
    <stack name="jdbc-ping-tcp" extends="tcp">
      <JDBC_PING 
        connection_driver="org.postgresql.Driver"
        connection_username="$\{env.KC_DB_USERNAME}"
        connection_password="$\{env.KC_DB_PASSWORD}"
        connection_url="jdbc:postgres://$\{env.KC_DB_URL_HOST}/$\{env.KC_DB_URL_DATABASE}" level="debug"
        initialize_sql="CREATE TABLE IF NOT EXISTS JGROUPSPING (own_addr varchar(200) NOT NULL, cluster_name varchar(200) NOT NULL, ping_data BYTEA, constraint PK_JGROUPSPING PRIMARY KEY (own_addr, cluster_name));"
        info_writer_sleep_time="500"
        remove_all_data_on_view_change="true"
        clear_table_on_view_change="true"
        stack.combine="REPLACE"
        stack.position="MPING" />
    </stack>
  </jgroups>
  <cache-container name="keycloak">
    <transport lock-timeout="60000" stack="jdbc-ping-tcp" />
    <local-cache name="realms">
      <encoding>
        <key media-type="application/x-java-object" />
        <value media-type="application/x-java-object" />
      </encoding>
      <memory max-count="10000" />
    </local-cache>
    <local-cache name="users">
      <encoding>
        <key media-type="application/x-java-object" />
        <value media-type="application/x-java-object" />
      </encoding>
      <memory max-count="10000" />
    </local-cache>
    <distributed-cache name="sessions" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <distributed-cache name="authenticationSessions" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <distributed-cache name="offlineSessions" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <distributed-cache name="clientSessions" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <distributed-cache name="offlineClientSessions" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <distributed-cache name="loginFailures" owners="3">
      <expiration lifespan="-1" />
    </distributed-cache>
    <local-cache name="authorization">
      <encoding>
        <key media-type="application/x-java-object" />
        <value media-type="application/x-java-object" />
      </encoding>
      <memory max-count="10000" />
    </local-cache>
    <replicated-cache name="work">
      <expiration lifespan="-1" />
    </replicated-cache>
    <local-cache name="keys">
      <encoding>
        <key media-type="application/x-java-object" />
        <value media-type="application/x-java-object" />
      </encoding>
      <expiration max-idle="3600000" /> <memory max-count="1000" />
    </local-cache>
    <distributed-cache name="actionTokens" owners="3">
      <encoding>
        <key media-type="application/x-java-object" />
        <value media-type="application/x-java-object" />
      </encoding>
      <expiration max-idle="-1" lifespan="-1" interval="300000" />
      <memory max-count="-1" />
    </distributed-cache> 
  </cache-container>
</infinispan>`
      .replaceAll(/\n/g, "")
      .replaceAll("${", "\\${")
  );

  constructor(scope: Construct, id: string, props: KeycloakServiceProps) {
    super(scope, id);

    // Secrets for Keycloak admin and DB password
    if (!props.databaseInstance.secret) throw new Error("DB secret not found");

    // Secrets for Keycloak admin and DB password
    const adminSecret = new Secret(this, "KeycloakAdminCredentials", {
      secretName: "KeycloakAdminCredentials",
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
    const ecsAdminSecret = ecsSecretFactory(adminSecret);

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
      "KeycloakFargateService",
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
          // entryPoint: [
          //   "sh",
          //   "-c",
          //   `cd /opt/keycloak && touch cache-ispn-jdbc-ping.xml && echo ${this.config} > cache-ispn-jdbc-ping.xml && cp cache-ispn-jdbc-ping.xml /opt/keycloak/conf/cache-ispn-jdbc-ping.xml && /opt/keycloak/bin/kc.sh build && /opt/keycloak/bin/kc.sh start`,
          // ],
          entryPoint: ["/opt/keycloak/bin/kc.sh"],
          command: ["start"],
          environment: {
            KC_DB_URL_DATABASE: props.databaseName,
            KC_HOSTNAME: props.hostname,
            // KC_HOSTNAME_STRICT_BACKCHANNEL: "true",
            // KC_PROXY: "edge",
            // KC_CACHE_CONFIG_FILE: "cache-ispn-jdbc-ping.xml",
            KC_HTTP_ENABLED: "true",
            // KC_HEALTH_ENABLED: "true",
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

    // [appPort, healthManagementPort].forEach((containerPort) =>
    //   this.albService.taskDefinition.defaultContainer?.addPortMappings({
    //     containerPort,
    //   })
    // );

    // Configure health check on port 9000 with the path '/health'
    this.albService.targetGroup.configureHealthCheck({
      // path: "/health",
      // port: healthManagementPort.toString(),
      path: "/admin/master/console/",
      healthyThresholdCount: 3,
    });

    props.databaseInstance.connections.allowDefaultPortFrom(
      this.albService.service
    );
  }
}

interface DatabaseProps extends Omit<DatabaseInstanceProps, "engine"> {
  vpc: IVpc;
  databaseName: string;
}

interface StackInputProps {
  hostname: string;
  sslCertificateArn: string;
  keycloakVersion: string;
}

interface KeycloakServiceProps extends StackInputProps {
  vpc: IVpc;
  databaseName: string;
  databaseInstance: DatabaseInstance;
}

interface StackProps extends cdk.StackProps, StackInputProps {
  vpcId?: string;
}
