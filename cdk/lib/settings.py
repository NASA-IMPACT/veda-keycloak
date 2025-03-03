from typing import Optional
from pydantic import DirectoryPath
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    aws_account_id: str
    aws_region: str = "us-west-2"
    cdk_bootstrap_qualifier: Optional[str] = None
    vpc_id: Optional[str] = None
    permissions_boundary_arn: Optional[str] = None
    ssl_certificate_arn: str
    hostname: str
    stage: str = "dev"
    keycloak_version: str = "26.0.5"
    keycloak_app_dir: DirectoryPath = DirectoryPath("keycloak")
    keycloak_config_cli_version: str = "latest-26"
    keycloak_config_cli_app_dir: DirectoryPath = DirectoryPath("keycloak-config-cli")

    @property
    def is_production(self) -> bool:
        return self.stage == "prod"

    @property
    def keycloak_config_cli_config_dir(self):
        return self.keycloak_config_cli_app_dir / "config"
