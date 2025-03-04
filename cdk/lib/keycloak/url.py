import re
from constructs import Construct
from aws_cdk import (
    Stack,
    aws_route53 as route53,
    aws_route53_targets as route53_targets,
    aws_elasticloadbalancingv2 as elbv2,
    CfnOutput,
)


class KeycloakUrl(Construct):
    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        hostname: str,
        alb: elbv2.ApplicationLoadBalancer,
    ) -> None:
        super().__init__(scope, construct_id)

        # Remove the protocol (http:// or https://) if present
        cleaned_hostname = re.sub(r"(^\w+:|^)//", "", hostname)

        # Extract the domain and subdomain from the cleaned hostname
        parts = cleaned_hostname.split(".")
        if len(parts) < 3:
            raise ValueError(
                'Hostname must be a fully qualified domain name, e.g., "keycloak.foo.com".'
            )

        subdomain = ".".join(parts[:-2])  # e.g., "keycloak"
        domain_name = ".".join(parts[-2:])  # e.g., "foo.com"

        # Lookup the hosted zone for the domain
        hosted_zone = route53.HostedZone.from_lookup(
            self, "HostedZone", domain_name=domain_name
        )

        # Create or replace an A record for the provided subdomain
        record = route53.ARecord(
            self,
            "AliasRecord",
            zone=hosted_zone,
            record_name=subdomain,
            target=route53.RecordTarget.from_alias(
                route53_targets.LoadBalancerTarget(alb)
            ),
            delete_existing=True,
            comment=f"Alias record for Keycloak, created by {Stack.of(self).stack_name}",
        )

        CfnOutput(self, "Arecord", key="aRecord", value=record.domain_name)
        CfnOutput(self, "Url", key="Url", value=f"https://{record.domain_name}")
