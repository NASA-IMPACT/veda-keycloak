# VEDA Keycloak

An experimental Keycloak deployment for the VEDA project.

## Development

- `keycloak-config-cli/config` - Configuration YAML files.
- `keycloak/providers` - Custom Service Provider Interfaces.
- `keycloak/themes` - Custom Keycloak themes.

### Architecture

![Architecture Diagram](./.docs/architecture.excalidraw.svg)

### Configuration

We currently make use of the [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli) to apply configuration at time of deployment.

> `keycloak-config-cli` is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file based on the export realm format.

Configuration is stored within `keycloak-config-cli/config` in YAML files for each Keycloak realm managed.

> [!IMPORTANT]
> At each deployment, the keycloak-config-cli will likely overwrite changes made outside of the configuration stored within this repository for a given realm.

## How To

Details step by step instructions on how to perform various tasks.

### Add a new client

Since this Keycloak uses the configuration as code pattern, we can use all the tooling available for collaborating on code (primarily, Pull Requests, Code Review and Continuous Integration) to collaborate on configuration!

The folks who want a client created are in the best position to have all the information about the client (such as its name, root URL, callback URLs, etc). They create a pull request with this information to start this process.

The file `keycloak-config-cli/config/veda.yaml` has all the config for the VEDA Keycloak instance. In the future, if we support multiple keycloak instances, each would have its own file.

New clients go under the `config` key. The following steps should help you fill out the config.

#### 1. Determine the type of client you need

OAuth2 offers [two types of clients](https://oauth.net/2/client-types/) and Keycloak supports both.

##### Confidential Client

The most common type of client is a [confidential client](https://oauth.net/2/client-types/), that can keep a shared client secret secret. Most web applications fall under this category, and we expect this to be the most used kind of client among VEDA services.

For each confidential client, the client secret will be automatically securely generated and stored in [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html). This secret can be be used directly by the client either by directly mounting it from AWS Secrets Manager, or by the service admins copy pasting it as necessary. By using AWS Secrets Manager as the 'source of truth' for client secrets, we are able to keep the entire config for each client in this repository, making self service possible and debugging easy.

This secret will be made avaiable at an environment variable of `$SLUG_CLIENT_SECRET` where `$SLUG` represents a slugify version of the `clientId` value (e.g. a client with an id of `stac-api` will have a secret available at `STAC_API_CLIENT_SECRET`). The config will need to refer to this secret from the env var correctly, as seen in this example.

A minimum example of a confidential client (note `publicClient: false` and `secret`):

```yaml
clients:
  - clientId: grafana
    name: Grafana
    publicClient: false
    secret: $(env:GRAFANA_CLIENT_SECRET)
    rootUrl: https://example.com
    redirectUris:
      - https://example.com/*
    webOrigins:
      - https://example.com
    protocol: openid-connect
    fullScopeAllowed: true
```

##### Public Client

A [public client](https://oauth.net/2/client-types/) is usually a browser single page application (SPA) or mobile app that can not keep a client secret actually secret. A minimal example of setting up such a client is:

```yaml
clients:
  - clientId: grafana
    name: Grafana
    publicClient: true
    rootUrl: https://example.com
    redirectUris:
      - https://example.com/*
    webOrigins:
      - https://example.com
    protocol: openid-connect
    fullScopeAllowed: true
```

#### 2. Specify Scopes, Roles, and Groups

Clients will typically have associated Scopes, Roles, and Groups.

- Scopes can be thought of as individual permissions used by a client.
- Roles are collections of permissions that enable a typical function (e.g. system administration)
- Groups are collections of users that we want to grant with roles.

You don't have to specify this in your initial PR to create the client - this can be filled in later as you know more about your application's needs.

An example:

```yaml
clients:
  - clientId: grafana
    # ... omitted for brevity
    fullScopeAllowed: true
    defaultClientScopes:
      - web-origins
      - acr
      - profile
      - roles
      - basic
      - email
      - grafana:admin
      - grafana:editor
      - grafana:viewer

roles:
  client:
    grafana:
      - name: Administrator
        description: Grafana Administrator
      - name: Editor
        description: Grafana Editor
      - name: Viewer
        description: Grafana Viewer

clientScopeMappings:
  grafana:
    - clientScope: grafana:admin
      roles:
        - Administrator
    - clientScope: grafana:editor
      roles:
        - Editor
    - clientScope: grafana:viewer
      roles:
        - Viewer

clientScopes:
  - name: grafana:admin
    description: Admin access to Grafana
    protocol: openid-connect
  - name: grafana:editor
    description: Editor access to Grafana
    protocol: openid-connect
  - name: grafana:viewer
    description: Viewer access to Grafana
    protocol: openid-connect

groups:
  - name: System Administrators
    clientRoles:
      grafana:
        - Administrator

  - name: Developers
    clientRoles:
      grafana:
        - Editor

  - name: Data Editors
    clientRoles:
      grafana:
        - Viewer
```

#### 3. Make the Pull Request & get it merged

Once you have even an outline of your config set up, make a pull request with your change! It'll be reviewed by the folks maintaining the keycloak instance, and there may be a collaborative back and forth to get the config in shape. If you have used GitHub for code review before, this would be the exact same workflow! There would also be a clear trail left of choices made in the form of git commits and github comments.

Eventually, your PR would get merged! Congratulations! The automation in this repository would now have created the client, and it's ready to be used.

#### 4. Use the client secret

If you're using a confidential client, you will now need access to the client secret. There are two ways to do this:

1. If you're running on the same AWS account as this keycloak instance, you can directly read the secret from AWS Secrets Manager
2. If you'd prefer to not do that, the client secret will be shared from the AWS Secrets Manager instance with you out of band.

### Change configuration of Identity Provider OAuth Clients (such as CILogon or GitHub)

When a third party service operates as an Identity Provider (IdP, e.g. CILogon or GitHub) for Keycloak, we must register that IdP within the Keycloak configuration. This involves registering the IdP's OAuth client ID and client secret within Keycloak's configuration (along with additional information about the OAuth endpoints used within the login process).

At time of deployment, environment variables starting with `IDP_SECRET_ARN_` will be treated as ARNs to Secrets stored within AWS Secrets Manager. These secrets should be JSON objects containing both an `id` and `secret` key. These values will be injected into the docker instance running the Keycloak Config CLI, making them avaiable under `{CLIENTID}_CLIENT_ID` and `{CLIENTID}_CLIENT_SECRET` environment variables, allowing for their usage within a Keycloak configuration YAML file.

<details>

<summary>Example of injecting an IdP OAuth2 Client Secret</summary>

For this example, let's imagine we're attempting to insert the Client ID and Client Secret for a Github Identity Provider. To achieve this, we would take the following steps:

1. Submit these values to AWS Secrets Manager:

   ```sh
   $ aws secretsmanager \
      create-secret \
      --name veda-keycloak-github-idp-creds \
      --secret-string '{"id": "cl13nt1d", "secret": "cl13ntS3cr3t!"}'
   ```

   AWS will respond with the ARN of the newly created Secret.

1. Register the secret with the Github environment, named `IDP_SECRET_ARN_$CLIENTID`, where `$CLIENTID` is a unique identifier for that IDP (for this example, we'll use `GH`). This can be done via the Github CLI if run from within the project repo:

   ```sh
   # Add variable value for the current repository in an interactive prompt
   $ gh variable set IDP_SECRET_ARN_GH --env dev
   ```

1. Update the Github Actions workflow to inject this variable into the runtime environment when calling `cdk deploy`:

   ```diff
    - name: Deploy CDK to dev environment
      run: |
         cdk deploy --require-approval never --outputs-file outputs.json
      env:
         # ...
   +     IDP_SECRET_ARN_GH: ${{ vars.IDP_SECRET_ARN_GH }}
   ```

1. The `id` and `secret` will now be available when configuring Keycloak. We can add a secrtion like the following to make use of these variables with `keycloak-config-cli/config/master.yaml`:

   ```yaml
   identityProviders:
   # GitHub with Org Check
   - alias: github-org-check # NOTE: this alias appears in the redirect_uri for the auth flow, update Github OAuth settings accordingly
      displayName: GitHub [NASA-IMPACT]
      providerId: github-org
      enabled: true
      updateProfileFirstLoginMode: on
      trustEmail: false
      storeToken: false
      addReadTokenRoleOnCreate: false
      authenticateByDefault: false
      linkOnly: false
      config:
         clientId: $(env:GH_CLIENT_ID)
         clientSecret: $(env:GH_CLIENT_SECRET)
         defaultScope: openid read:org user:email
         organization: nasa-impact
         caseSensitiveOriginalUsername: "false"
         syncMode: FORCE
   ```

</details>

### Further customization with Service Provider Interfaces

Beyond configuration, customization of Keycloak (e.g. a custom Identity Providers) may require development of custom Service Provider Interfaces (SPIs). These are well supported and documented [Java interfaces](https://www.keycloak.org/docs/latest/server_development/#_providers) and [Javascript interfaces](https://www.keycloak.org/docs/latest/server_development/#_script_providers) that allow direct control over the entire authentication and authorization process.

*If* our needs get complicated enough that we need to use more than config, this is the upstream supported path available to us.

To prove this is possible, see the `keycloak/providers/` directory in this repository.

### Look and Feel customization with Themes

If we want to customize the look and feek of Keycloak, that is possible via [themes](https://www.keycloak.org/docs/latest/server_development/#_themes)

## Useful commands

- `npm run build` compile typescript to js
- `npm run watch` watch for changes and compile
- `npm run test` perform the jest unit tests
- `npx cdk deploy` deploy this stack to your default AWS account/region
- `npx cdk diff` compare deployed stack with current state
- `npx cdk synth` emits the synthesized CloudFormation template
