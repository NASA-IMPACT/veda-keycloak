# Architecture adapted from https://github.com/aws-samples/fargate-ses-relay

from typing import Optional

from aws_cdk import (
    NestedStack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_ecs_patterns as ecs_patterns,
    aws_elasticloadbalancingv2 as elbv2,
    aws_iam as iam,
    aws_ecr_assets as ecr_assets,
    CfnOutput,
)
from constructs import Construct

class SesRelayStack(NestedStack):
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        vpc: ec2.IVpc,
        ses_relay_app_dir: Optional[str] = None,
        **kwargs,
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

    
        cluster = ecs.Cluster(self, "Cluster",
            cluster_name="veda-keycloak-ses-relay",
            vpc=vpc,
        )

        task_role = iam.Role(self, "TaskRole",
            role_name="SesRelayTaskRole",
            assumed_by=iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
            inline_policies={"SesRelayPolicy": iam.PolicyDocument(
                statements=[iam.PolicyStatement(
                    actions=["ses:SendRawEmail","ses:SendEmail"],
                    resources=["*"],
                )]
            )}
        )

        task_image_options=ecs_patterns.NetworkLoadBalancedTaskImageOptions(
            image=ecs.ContainerImage.from_asset(
                    directory=ses_relay_app_dir,
                    platform=ecr_assets.Platform.LINUX_AMD64,
                ),
            container_name="SesRelayContainer",
            container_port=25,
            task_role=task_role,
        )

        nlb_sg=ec2.SecurityGroup(
			scope=self, 
			id="NLB-SG",
			vpc=vpc,
    		description="Allow access to SES Relay",
    		allow_all_outbound=True,
		)

        cidr = vpc.vpc_cidr_block # Allow access from the VPC
        nlb_sg.add_ingress_rule(
            peer = ec2.Peer.ipv4(cidr),
            connection = ec2.Port.tcp(25),
            description="Allow from " + cidr
        )

        nlb = elbv2.NetworkLoadBalancer(
			scope=self,
			id='FG-NLB',
			vpc=vpc,
			security_groups=[nlb_sg],
			internet_facing=False,
		)

        service = ecs_patterns.NetworkLoadBalancedFargateService(
            self, "FargateService",
            service_name="veda-keycloak-ses-relay",
            cluster=cluster,
            task_image_options=task_image_options,
            desired_count=1,
            memory_limit_mib=2048,
            cpu=1024,
            listener_port=25,
            load_balancer=nlb,
        )

        service.service.connections.security_groups[0].add_ingress_rule(
            peer = nlb_sg,
            connection = ec2.Port.tcp(25),
            description="Allow from NLB Security Group"
        )

        CfnOutput(self, "NLBDNS", key="NLBDNS", value=nlb.load_balancer_dns_name)
