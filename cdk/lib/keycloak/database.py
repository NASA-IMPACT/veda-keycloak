from constructs import Construct
from aws_cdk import (
    RemovalPolicy,
    aws_rds as rds,
    aws_ec2 as ec2,
)


class KeycloakDatabase(Construct):

    def __init__(
        self,
        scope: Construct,
        construct_id: str,
        *,
        vpc: ec2.IVpc,
        database_name: str,
        instance_identifier: str = None,
        is_production: bool = False,
        snapshot_identifier: str = None,
        **kwargs,
    ) -> None:
        """
        :param scope: Construct scope
        :param construct_id: Identifier for this construct
        :param vpc: The VPC to deploy into
        :param database_name: Name of the database to create
        :param instance_identifier: Optional identifier for the RDS instance
        :param is_production: Whether the database is in production
        :param kwargs: Additional DatabaseInstanceProps (except 'engine', which is set to Postgres)
        """
        super().__init__(scope, construct_id)

        self.database_name = database_name
        database_instance_props = {
            "instance_identifier": instance_identifier,
            "engine": rds.DatabaseInstanceEngine.postgres(
                version=rds.PostgresEngineVersion.VER_16_4
            ),
            "instance_type": ec2.InstanceType.of(
                ec2.InstanceClass.BURSTABLE4_GRAVITON, ec2.InstanceSize.MEDIUM
            ),
            "removal_policy": (
                RemovalPolicy.RETAIN if is_production else RemovalPolicy.DESTROY
            ),
            "vpc": vpc,
            "database_name": self.database_name,
            **kwargs,  # Pass along any additional props
        }
        self.database = (
            rds.DatabaseInstance(
                self,
                "KeycloakPostgres",
                storage_encrypted=True,
                **database_instance_props,
            )
            if snapshot_identifier is None
            else rds.DatabaseInstanceFromSnapshot(
                self,
                "KeycloakPostgres",
                snapshot_identifier=snapshot_identifier,
                **database_instance_props,
            )
        )
