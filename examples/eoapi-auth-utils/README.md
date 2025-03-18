# VEDA Keycloak Example Client

This is an example FastAPI client application that demonstrates integration with VEDA Keycloak.

## Development Setup

This project uses [uv](https://github.com/astral-sh/uv) for fast, reliable Python package management.

### Quick Start

```bash
# Run the setup script to install uv and set up the environment
./scripts/setup.sh

# Activate the virtual environment
source .venv/bin/activate

# Run the application
uvicorn app.main:app --reload
```

### Manual Setup

If you prefer to set up manually:

1. Install uv:
   ```bash
   curl -LsSf https://astral.sh/uv/install.sh | sh
   ```

2. Create and activate a virtual environment:
   ```bash
   uv venv
   source .venv/bin/activate
   ```

3. Install dependencies:
   ```bash
   uv pip install -e .
   ```

## Docker Setup

The application can also be run using Docker:

```bash
# Build and start the container
docker-compose up --build

# Or run in detached mode
docker-compose up -d
```

The application will be available at http://localhost:8000

### Configuration

Before running the application, make sure to:

1. Create a new client in your Keycloak realm
2. Set the client's redirect URI to `http://localhost:8000/auth/callback`
3. Update the following environment variables in `docker-compose.yml`:
   - `KEYCLOAK_CLIENT_ID`
   - `KEYCLOAK_CLIENT_SECRET`

### Why uv?

- Significantly faster than pip (5-100x)
- Reliable dependency resolution
- Built-in virtual environment management
- Compatible with all standard Python packaging formats
- Modern caching for faster subsequent installs 