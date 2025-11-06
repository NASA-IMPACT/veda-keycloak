import re
from constructs import Construct
from aws_cdk import (
    Duration,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_ecs_patterns as ecs_patterns,
    aws_ecr_assets as ecr_assets,
    aws_secretsmanager as secretsmanager,
    aws_certificatemanager as acm,
    aws_elasticloadbalancingv2 as elbv2,
    aws_rds as rds,
)


class KeycloakService(Construct):
    """
    Responsible for creating infrastructure to deploy a Keycloak instance.
    """

    albService: ecs_patterns.ApplicationLoadBalancedFargateService
    adminSecret: secretsmanager.Secret

    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        vpc: ec2.IVpc,
        database_name: str,
        database_instance: rds.DatabaseInstance,
        app_dir: str,
        version: str,
        hostname: str,
        ssl_certificate_arn: str,
        **kwargs,
    ) -> None:
        """
        :param scope: Construct or Stack scope
        :param construct_id: Identifier for this construct
        :param vpc: The VPC to deploy into
        :param database_name: Name of the Keycloak database
        :param database_instance: The RDS DatabaseInstance used by Keycloak
        :param version: The Keycloak version (e.g. "21.1.2")
        :param hostname: The Keycloak hostname
        :param ssl_certificate_arn: ARN of the SSL Certificate for the ALB
        """
        super().__init__(scope, construct_id, **kwargs)

        if not database_instance.secret:
            raise ValueError("Database instance must have an attached secret")

        # Secrets for Keycloak admin and DB password
        self.admin_secret = secretsmanager.Secret(
            self,
            "admin-creds",
            generate_secret_string=secretsmanager.SecretStringGenerator(
                exclude_punctuation=True,
                include_space=False,
                secret_string_template='{"username":"admin"}',
                generate_string_key="password",
                password_length=16,
            ),
        )

        # Helper to create ECS secret mappings from Secrets Manager
        def ecs_secret_factory(secret: secretsmanager.ISecret):
            return lambda key: ecs.Secret.from_secrets_manager(secret, key)

        ecs_db_secret = ecs_secret_factory(database_instance.secret)
        ecs_admin_secret = ecs_secret_factory(self.admin_secret)

        # SSL certificate for the Application Load Balancer
        certificate = acm.Certificate.from_certificate_arn(
            self, "SSLCertificate", ssl_certificate_arn
        )

        # Keycloak ports
        app_port = 8080
        health_management_port = 9000

        # Production has a public NAT Gateway subnet, which causes the default load
        # balancer creation to fail with too many subnets being selected per AZ. We
        # create our own load balancer to allow us to select subnets and avoid the issue.
        load_balancer = elbv2.ApplicationLoadBalancer(
            self,
            "LoadBalancer",
            vpc=vpc,
            internet_facing=True,
            vpc_subnets=ec2.SubnetSelection(
                subnet_type=ec2.SubnetType.PUBLIC, one_per_az=True
            ),
        )

        # Fargate Service with ALB, SSL, and Health Check
        kc_major_version = int(version.split(".")[0]) if version else 0
        self.alb_service = ecs_patterns.ApplicationLoadBalancedFargateService(
            self,
            "service",
            vpc=vpc,
            load_balancer=load_balancer,
            desired_count=1,
            public_load_balancer=True,
            listener_port=443,
            certificate=certificate,
            memory_limit_mib=2048,
            cpu=1024,
            health_check_grace_period=Duration.seconds(120),
            redirect_http=False,
            task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                container_name="keycloak",
                container_port=app_port,
                image=ecs.ContainerImage.from_asset(
                    directory=app_dir,
                    platform=ecr_assets.Platform.LINUX_AMD64,
                    build_args={
                        "KEYCLOAK_VERSION": version,
                    },
                ),
                entry_point=["/opt/keycloak/bin/kc.sh"],
                command=["start"],
                environment={
                    "KC_DB_URL_DATABASE": database_name,
                    # Strip protocol from hostname for KC_HOSTNAME (it should be just the domain)
                    "KC_HOSTNAME": re.sub(r"(^\w+:|^)//", "", hostname),
                    "KC_HTTP_ENABLED": "true",
                    "KC_HTTP_MANAGEMENT_PORT": str(health_management_port),
                    "KC_HEALTH_ENABLED": "true",
                    # Trust proxy headers from ALB (X-Forwarded-* headers)
                    "KC_PROXY_HEADERS": "xforwarded",
                    "KC_HOSTNAME_STRICT": "false",
                },
                secrets={
                    # Database credentials
                    "KC_DB": ecs_db_secret("engine"),
                    "KC_DB_USERNAME": ecs_db_secret("username"),
                    "KC_DB_PASSWORD": ecs_db_secret("password"),
                    "KC_DB_URL_HOST": ecs_db_secret("host"),
                    "KC_DB_URL_PORT": ecs_db_secret("port"),
                    # Admin credentials, depends on Keycloak version
                    **(
                        {
                            "KEYCLOAK_ADMIN": ecs_admin_secret("username"),
                            "KEYCLOAK_ADMIN_PASSWORD": ecs_admin_secret("password"),
                        }
                        if kc_major_version >= 26
                        else {
                            "KC_BOOTSTRAP_ADMIN_USERNAME": ecs_admin_secret("username"),
                            "KC_BOOTSTRAP_ADMIN_PASSWORD": ecs_admin_secret("password"),
                        }
                    ),
                },
            ),
        )

        self.alb_service.task_definition.default_container.add_port_mappings(
            ecs.PortMapping(
                container_port=health_management_port,  # 9000
                protocol=ecs.Protocol.TCP,
            )
        )

        self.alb_service.target_group.configure_health_check(
            path="/health",
            port=str(health_management_port),  # 9000
            protocol=elbv2.Protocol.HTTP,
            healthy_threshold_count=3,
            unhealthy_threshold_count=2,
            timeout=Duration.seconds(5),
            interval=Duration.seconds(60),
        )

        self.alb_service.service.connections.allow_from(
            load_balancer,
            ec2.Port.tcp(health_management_port),
            "Health check on port 9000",
        )

        database_instance.connections.allow_default_port_from(self.alb_service.service)
