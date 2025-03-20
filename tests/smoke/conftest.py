import os
import pytest
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    hostname: str = os.getenv("HOSTNAME", "http://localhost:8080")

    model_config = SettingsConfigDict(
        env_file=".env",  # Load from .env if available (only if HOSTNAME is not set)
        extra="ignore"
    )


@pytest.fixture(scope='session')
def settings():
    """Returns an instance of Settings"""
    return Settings()


@pytest.fixture(scope='session')
def base_url(settings):
    """Extracts base_url from settings"""
    return settings.hostname
