#!/usr/bin/env -S uv run --script
import os
import logging

from aws_cdk import (
    App,
    DefaultStackSynthesizer,
    PermissionsBoundary,
)

from lib.keycloak import KeycloakStack
from lib.utils import get_oauth_secrets, get_private_client_ids, array_stringify
from lib.settings import Settings

logging.basicConfig(level=logging.INFO)

env_file = os.environ.get("ENV_FILE", ".env")
settings = Settings(_env_file=env_file)

logging.info("Extracting ARNs of IdP secrets from environment...")
idp_oauth_client_secrets = get_oauth_secrets()
if idp_oauth_client_secrets:
    logging.info(
        "Found IdP client secrets in environment:\n%s",
        array_stringify(list(idp_oauth_client_secrets.keys())),
    )
else:
    logging.warning("No IdP client secrets found in the environment.")

logging.info("Extracting OAuth client IDs from Keycloak configuration...")
private_oauth_clients = get_private_client_ids(settings.keycloak_config_cli_config_dir)
if private_oauth_clients:
    logging.info(
        "Found client IDs in %s:\n%s",
        settings.keycloak_config_cli_config_dir,
        array_stringify([client["id"] for client in private_oauth_clients]),
    )
else:
    logging.warning(
        "No client IDs found in %s",
        settings.keycloak_config_cli_config_dir,
    )

app = App()

# Optionally set a custom synthesizer if CDK_BOOTSTRAP_QUALIFIER is present
synthesizer = (
    DefaultStackSynthesizer(qualifier=settings.cdk_bootstrap_qualifier)
    if settings.cdk_bootstrap_qualifier
    else None
)

# Optionally set a permissions boundary if provided
permissions_boundary = (
    PermissionsBoundary.from_arn(settings.permissions_boundary_arn)
    if settings.permissions_boundary_arn
    else None
)

KeycloakStack(
    app,
    f"veda-keycloak-{settings.stage}",
    vpc_id=settings.vpc_id,
    ssl_certificate_arn=settings.ssl_certificate_arn,
    hostname=settings.hostname,
    keycloak_version=settings.keycloak_version,
    keycloak_app_dir=settings.keycloak_app_dir.as_posix(),
    keycloak_config_cli_version=settings.keycloak_config_cli_version,
    keycloak_config_cli_app_dir=settings.keycloak_config_cli_app_dir.as_posix(),
    idp_oauth_client_secrets=idp_oauth_client_secrets,
    private_oauth_clients=private_oauth_clients,
    is_production=settings.is_production,
    # Stack Configuration
    env={
        "account": settings.aws_account_id,
        "region": settings.aws_region,
    },
    synthesizer=synthesizer,
    permissions_boundary=permissions_boundary,
)

app.synth()
