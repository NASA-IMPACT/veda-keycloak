import configparser
import os
import pytest
from pathlib import Path


def pytest_addoption(parser):
    parser.addoption("--env", action="store", default="local", help="Environment to run tests against")


@pytest.fixture(scope='session')
def config(pytestconfig):
    env = pytestconfig.getoption("--env")
    config_file = Path(__file__).parent / "config" / f"settings_{env}.ini"

    if not os.path.exists(config_file):
        raise FileNotFoundError(f"Config file '{config_file}' not found!")

    parser = configparser.ConfigParser()
    parser.read(config_file)

    print(f"Loaded config from {config_file}: {dict(parser.items('server'))}")

    return parser
