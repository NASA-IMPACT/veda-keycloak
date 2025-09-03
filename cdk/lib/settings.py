from typing import Optional
from pydantic import DirectoryPath, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    aws_account_id: str
    aws_region: str = "us-west-2"
    cdk_bootstrap_qualifier: Optional[str] = None
    vpc_id: Optional[str] = None
    permissions_boundary_arn: Optional[str] = None
    ssl_certificate_arn: str
    hostname: Optional[str] = None
    stage: str = "dev"
    keycloak_version: str = "26.0.5"
    keycloak_app_dir: DirectoryPath = DirectoryPath("keycloak")
    keycloak_config_cli_version: str = "latest-26"
    keycloak_config_cli_app_dir: DirectoryPath = DirectoryPath("keycloak-config-cli")
    rds_snapshot_identifier: Optional[str] = Field(
        default=None,
        pattern=r"^arn:aws:rds:[a-z0-9-]+:\d{12}:snapshot:.+$",
    )

    @field_validator("rds_snapshot_identifier", mode="before")
    @classmethod
    def convert_empty_string_to_none(cls, v):
        if v == "":
            return None
        return v

    model_config = SettingsConfigDict(extra="ignore")

    @property
    def is_production(self) -> bool:
        return self.stage == "prod"

    @property
    def keycloak_config_cli_config_dir(self):
        return self.keycloak_config_cli_app_dir / "config"
