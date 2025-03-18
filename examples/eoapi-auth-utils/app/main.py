from dataclasses import dataclass

from eoapi.auth_utils import OpenIdConnectAuth
from fastapi import FastAPI
from fastapi.routing import APIRoute


class AuthSettings:
    client_id: str = "eoapi-auth-utils-example-client"
    use_pkce: bool = True
    openid_configuration_url: str = (
        "http://localhost:8080/realms/veda/.well-known/openid-configuration"
    )
    openid_configuration_internal_url: str = (
        "http://keycloak:8080/realms/veda/.well-known/openid-configuration"
    )


settings = AuthSettings()

app = FastAPI(
    docs_url="/api.html",
    swagger_ui_init_oauth={
        "clientId": settings.client_id,
        "usePkceWithAuthorizationCodeGrant": settings.use_pkce,
    },
)


@dataclass
class MockEoApiApp:
    app: FastAPI


api = MockEoApiApp(app=app)


@app.get("/")
def root():
    """Example endpoint. In a typical eoAPI application, this would be created by the eoAPI server."""
    return {"ok": True}


@app.get("/scope-example")
def root():
    """Example endpoint that requires a specific scope. In a typical eoAPI application, this would be created by the eoAPI server."""
    return {"ok": True}


if settings.openid_configuration_url:
    oidc_auth = OpenIdConnectAuth(
        openid_configuration_url=settings.openid_configuration_url,
        openid_configuration_internal_url=settings.openid_configuration_internal_url,
    )

    # Implement your custom app-specific auth logic here...
    restricted_routes = {
        "/": ("GET", []),
        "/scope-example": ("GET", ["eoapi:example:read"]),
    }
    api_routes = {
        route.path: route for route in api.app.routes if isinstance(route, APIRoute)
    }
    for endpoint, (method, scopes) in restricted_routes.items():
        route = api_routes.get(endpoint)
        if route and method in route.methods:
            oidc_auth.apply_auth_dependencies(route, required_token_scopes=scopes)
