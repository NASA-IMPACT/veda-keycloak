import * as fs from "fs";
import * as path from "path";
import * as yaml from "js-yaml";
import { assert } from "console";

/**
 * Helper function to extract OAuth client secrets from the runtime environment
 * @returns Record<string, string> - A map of OAuth client IDs to the ARN of their secrets
 */
export function getOauthSecrets(): Record<string, string> {
  const oauthSecretPrefix = "IDP_SECRET_ARN_";
  const clientSecrets = Object.entries(process.env)
    .filter(([k, v]) => k.startsWith(oauthSecretPrefix))
    .map(([k, v]) => [k.split(oauthSecretPrefix)[1], v]);
  return Object.fromEntries(clientSecrets);
}

/**
 * Function to read all `*.yaml` files and extract `clientId` values
 * @param configDir Path to the directory containing the YAML files
 * @returns
 */
export function getPrivateClientIds(
  configDir: string
): { realm: string; id: string }[] {
  const clientIds: { realm: string; id: string }[] = [];

  // Read files in the directory
  const files = fs
    .readdirSync(configDir)
    .filter((file) => file.endsWith(".yaml") || file.endsWith(".yml"));

  for (const file of files) {
    try {
      const filePath = path.join(configDir, file);
      const fileContents = fs.readFileSync(filePath, "utf8");

      // Parse YAML
      const data = yaml.load(fileContents) as {
        realm: string;
        clients?: Array<{ clientId: string; secret?: string }>;
      };

      // Check if `clients` array exists
      if (data && Array.isArray(data.clients)) {
        // Extract `clientId` from each object in the `clients` array
        data.clients
          .filter((client) => client.secret)
          .forEach((client) => {
            if (client.clientId) {
              clientIds.push({ id: client.clientId, realm: data.realm });
            } else {
              console.warn(
                `Missing clientId for client ${JSON.stringify(
                  client
                )} in file ${file}`
              );
            }
          });
      }
    } catch (error) {
      console.error(`Failed to process file ${file}:`, error);
    }
  }

  clientIds.forEach((client) => validateClientId(client.id));

  return clientIds;
}

export function arrayStringify(arr: string[]): string {
  return arr.map((val) => `\t- ${val}`).join("\n");
}

export function validateClientId(clientId: string): void {
  assert(clientId.match(/^[a-zA-Z0-9-]+$/), `Invalid clientId: ${clientId}`);
}

export function clientIdToEnvVar(clientId: string): string {
  return clientId.replace("-", "_").toUpperCase();
}
