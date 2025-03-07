import requests


def test_keycloak_health_check(config):
    base_url = config['server']['base_url']
    endpoints = ['live', 'ready']
    for endpoint in endpoints:
        url = f"{base_url}/health/{endpoint}"
        response = requests.get(url)
        assert response.status_code == 200, f"Expected status code 200 for /health/{endpoint}, got {response.status_code}"
        json_response = response.json()
        assert json_response.get("status") == "UP", f"Expected status 'UP' for /health/{endpoint}, got {json_response.get('status')}"
