import logging
import os
import re
import yaml


def get_oauth_secrets() -> dict[str, str]:
    """
    Extracts OAuth client secrets from environment variables starting with 'IDP_SECRET_ARN_'.
    Returns a dictionary mapping each client slug to its secret ARN.
    """
    oauth_secret_prefix = "IDP_SECRET_ARN_"
    client_secrets = {}

    for key, value in os.environ.items():
        if key.startswith(oauth_secret_prefix):
            # The client slug is the remainder of the key after the prefix
            client_slug = key[len(oauth_secret_prefix) :]
            client_secrets[client_slug] = value

    return client_secrets


def get_private_client_ids(config_dir: str) -> list[dict[str, str]]:
    """
    Reads all YAML files in a directory, extracts clients with a 'secret',
    and returns a list of {'realm': <realm>, 'id': <clientId>} objects.
    """
    client_ids = []

    # List YAML/YML files
    for filename in os.listdir(config_dir):
        if not filename.endswith(".yaml") and not filename.endswith(".yml"):
            logging.debug("Ignoring %s due to filename extension", filename)

        # Parse the YAML file
        file_path = os.path.join(config_dir, filename)
        with open(file_path, "r", encoding="utf-8") as f:
            logging.debug("Parsing %s", filename)
            data = yaml.safe_load(f)

        if data and isinstance(data.get("clients"), list):
            for client in data["clients"]:
                # Only collect clients that have a 'secret' field
                if "secret" in client:
                    if "clientId" in client:
                        client_ids.append(
                            {
                                "id": client["clientId"],
                                "realm": data.get("realm", ""),
                            }
                        )
                    else:
                        logging.warning(
                            "Missing clientId for client %s in file %s",
                            client,
                            filename,
                        )

    # Validate each extracted clientId
    for client in client_ids:
        validate_client_id(client["id"])

    return client_ids


def validate_client_id(client_id: str) -> None:
    """
    Raises ValueError if the clientId does not match the /^[a-zA-Z0-9-]+$/ pattern.
    """
    pattern = re.compile(r"^[a-zA-Z0-9-]+$")
    if not pattern.match(client_id):
        raise ValueError(f"Invalid clientId: {client_id}")


def client_id_to_env_var(client_id: str) -> str:
    """
    Converts a clientId to an environment variable-friendly string, replacing
    hyphens with underscores and converting to uppercase.
    """
    return client_id.replace("-", "_").upper()
