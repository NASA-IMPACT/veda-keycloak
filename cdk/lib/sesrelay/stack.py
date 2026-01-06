# Architecture inspired by https://github.com/aws-samples/fargate-ses-relay

from typing import Optional

from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_ecs_patterns as ecs_patterns,
    aws_elasticloadbalancingv2 as elbv2,
    aws_iam as iam,
    CfnOutput,
)
from constructs import Construct

class SesRelayStack(Stack):
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        vpc_id: Optional[str] = None,
        **kwargs,
    ) -> None:
        super().__init__(scope, construct_id, **kwargs)

    
        vpc = (
            ec2.Vpc.from_lookup(self, "Vpc", vpc_id=vpc_id)
            if vpc_id
            else ec2.Vpc(self, "vpc")
        )

        cluster = ecs.Cluster(self, "Cluster",
            cluster_name="ses-relay",
            vpc=vpc,
        )

        task_role = iam.Role(self, "TaskRole",
            role_name="SesRelayTaskRole",
            inline_policies={"SesRelayPolicy": iam.PolicyDocument(
                statements=[iam.PolicyStatement(
                    actions=["ses:SendRawEmail","ses:SendEmail"],
                    resources=["*"],
                )]
            )}
        )

        task_image_options=ecs_patterns.NetworkLoadBalancedTaskImageOptions(
            image=ecs.ContainerImage.from_registry("loopingz/smtp-relay:v2.2.5"),
            command=["configs/aws-smtp-relay.jsonc"],
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
			id='FGNLB',
			vpc=vpc,
			security_groups=[nlb_sg],
			internet_facing=False,
		)

        service = ecs_patterns.NetworkLoadBalancedFargateService(
            self, "FargateService",
            service_name="ses-relay",
            cluster=cluster,
            task_image_options=task_image_options,
            desired_count=1,
            memory_limit_mib=2048,
            cpu=1024,
            listener_port=25,
            load_balancer=nlb.from_network_load_balancer_attributes(scope=self, id='NLB',load_balancer_arn=nlb.load_balancer_arn, vpc=vpc),
        )

        service.service.connections.security_groups[0].add_ingress_rule(
            peer = nlb_sg,
            connection = ec2.Port.tcp(25),
            description="Allow from NLB Security Group"
        )

        CfnOutput(self, "NLB DNS", key="NLB DNS", value=nlb.load_balancer_dns_name)
