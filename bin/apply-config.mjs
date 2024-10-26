/**
 * This script calls the applyConfig Lambda function, waits for the ECS task to complete,
 * and fetches the logs from CloudWatch Logs.
 *
 * Usage: npm run apply-config <lambdaArn>
 */

import { LambdaClient, InvokeCommand } from "@aws-sdk/client-lambda";
import {
  ECSClient,
  DescribeTasksCommand,
  DescribeTaskDefinitionCommand,
} from "@aws-sdk/client-ecs";
import {
  CloudWatchLogsClient,
  GetLogEventsCommand,
} from "@aws-sdk/client-cloudwatch-logs";

let exitCode = null;

main()
  .then((logs) => {
    console.log(`Task output:\n${"-".repeat(100)}`);
    logs.forEach((l) => console.log(l));
  })
  .catch((error) => {
    console.error(`Error: ${error}`);
    exitCode = 1;
  })
  .finally(() => process.exit(exitCode));

async function main() {
  const [, , lambdaArn] = process.argv;
  const { taskArn, clusterArn } = await invokeLambda(lambdaArn);
  const ecs = new ECSClient({});
  const task = await pollEcsTask(ecs, taskArn, clusterArn);
  exitCode = task.containers[0]?.exitCode ?? null;
  if (exitCode === null)
    throw new Error("Could not retrieve exit code from the ECS task.");

  const { logGroup, logStreamPrefix, region, containerName } =
    await getLogConfig(ecs, task.taskDefinitionArn);
  const taskId = taskArn.split("/").pop();
  const logStreamName = `${logStreamPrefix}/${containerName}/${taskId}`;
  return fetchCloudWatchLogs(logGroup, logStreamName, region);
}

async function invokeLambda(lambdaArn) {
  const lambda = new LambdaClient({});
  const response = await lambda.send(
    new InvokeCommand({
      FunctionName: lambdaArn,
      InvocationType: "RequestResponse",
      Payload: new TextEncoder().encode(JSON.stringify({})),
    })
  );
  return JSON.parse(new TextDecoder().decode(response.Payload));
}

async function pollEcsTask(ecs, taskArn, clusterArn) {
  while (true) {
    const { tasks } = await ecs.send(
      new DescribeTasksCommand({ cluster: clusterArn, tasks: [taskArn] })
    );
    const task = tasks?.[0];
    if (!task) throw new Error(`No tasks found with taskArn: ${taskArn}`);
    console.log(`Task status: ${task.lastStatus}`);
    if (task.lastStatus === "STOPPED") return task;
    await new Promise((resolve) => setTimeout(resolve, 5000));
  }
}

async function getLogConfig(ecs, taskDefinitionArn) {
  const { taskDefinition } = await ecs.send(
    new DescribeTaskDefinitionCommand({ taskDefinition: taskDefinitionArn })
  );
  const containerDef = taskDefinition.containerDefinitions[0];
  if (containerDef.logConfiguration?.logDriver !== "awslogs")
    throw new Error("Log driver is not 'awslogs'.");
  return {
    logGroup: containerDef.logConfiguration.options["awslogs-group"],
    logStreamPrefix:
      containerDef.logConfiguration.options["awslogs-stream-prefix"],
    region: containerDef.logConfiguration.options["awslogs-region"],
    containerName: containerDef.name,
  };
}

async function fetchCloudWatchLogs(logGroup, logStreamName, region) {
  const logsClient = new CloudWatchLogsClient({ region });
  let nextToken,
    events = [];
  do {
    const { events: newEvents, nextForwardToken } = await logsClient.send(
      new GetLogEventsCommand({
        logGroupName: logGroup,
        logStreamName,
        startFromHead: true,
        nextToken,
      })
    );
    events.push(...newEvents);
    nextToken = nextForwardToken !== nextToken ? nextForwardToken : null;
  } while (nextToken);
  return events.map((event) => event.message);
}
