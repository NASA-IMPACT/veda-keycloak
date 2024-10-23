# VEDA Keycloak

An experimental Keycloak deployment for the VEDA project.

## Development

* `config/src` - Configuration YAML files.
* `keycloak/providers` - Custom Service Provider Interfaces.
* `keycloak/themes` - Custom Keycloak themes.

### Configuration

We currently make use of the [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli) to apply configuration at time of deployment.

> `keycloak-config-cli` is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file based on the export realm format.

> [!IMPORTANT]
> At each deployment, the keycloak-config-cli will likely overwrite changes made outside of the configuration stored within this repository for a given realm.

### Service Provider Interfaces

Beyond configuration, customization of Keycloak (e.g. a custom Identity Providers) may require development of custom Service Provider Interfaces (SPIs).

> [!TIP]
> See the Service Provider Interfaces section in the [Server Developer Guide](https://www.keycloak.org/docs/latest/server_development/#_providers) for more details about how to create custom themes.

### Themes

> [!TIP]
> See the theme section in the [Server Developer Guide](https://www.keycloak.org/docs/latest/server_development/#_themes) for more details about how to create custom themes.

## Useful commands

- `npm run build` compile typescript to js
- `npm run watch` watch for changes and compile
- `npm run test` perform the jest unit tests
- `npx cdk deploy` deploy this stack to your default AWS account/region
- `npx cdk diff` compare deployed stack with current state
- `npx cdk synth` emits the synthesized CloudFormation template
