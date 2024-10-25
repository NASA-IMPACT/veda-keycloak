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

async function main() {
  const [_, __, lambdaArn] = process.argv;

  // Set up AWS clients
  const lambda = new LambdaClient({});
  const ecs = new ECSClient({});
  const logs = new CloudWatchLogsClient({});

  // Step 1: Invoke the Lambda function
  let taskArn, clusterArn;
  try {
    const response = await lambda.send(
      new InvokeCommand({
        FunctionName: lambdaArn,
        InvocationType: "RequestResponse",
        Payload: new TextEncoder().encode(JSON.stringify({})),
      })
    );

    const payload = JSON.parse(new TextDecoder().decode(response.Payload));
    taskArn = payload.taskArn;
    clusterArn = payload.clusterArn;
    console.log(
      `Invoked Lambda function. Received taskArn=${taskArn}, clusterArn=${clusterArn}`
    );
  } catch (error) {
    console.error(`Error invoking Lambda function: ${error}`);
    process.exit(1);
  }

  // Step 2: Poll the ECS task until it reaches 'STOPPED' status
  let task;
  try {
    while (true) {
      const response = await ecs.send(
        new DescribeTasksCommand({
          cluster: clusterArn,
          tasks: [taskArn],
        })
      );

      if (!response.tasks || response.tasks.length === 0) {
        console.error(`No tasks found with taskArn: ${taskArn}`);
        process.exit(1);
      }

      task = response.tasks[0];
      const lastStatus = task.lastStatus;
      console.log(`Task status: ${lastStatus}`);

      if (lastStatus === "STOPPED") break;
      await new Promise((resolve) => setTimeout(resolve, 5000));
    }
  } catch (error) {
    console.error(`Error polling ECS task: ${error}`);
    process.exit(1);
  }

  // Retrieve the exit code from the task's containers
  const exitCode = task.containers.find(
    (container) => "exitCode" in container
  )?.exitCode;

  if (exitCode === undefined) {
    console.error("Could not retrieve exit code from the ECS task.");
    process.exit(1);
  }

  // Step 3: Retrieve log configuration from the task definition
  let logGroup, logStreamName, region;
  try {
    const taskDefinitionArn = task.taskDefinitionArn;
    const response = await ecs.send(
      new DescribeTaskDefinitionCommand({
        taskDefinition: taskDefinitionArn,
      })
    );

    const containerDefinition = response.taskDefinition.containerDefinitions[0];
    const logConfiguration = containerDefinition.logConfiguration || {};

    if (logConfiguration.logDriver !== "awslogs") {
      console.error("Log driver is not 'awslogs'.");
      process.exit(1);
    }

    const options = logConfiguration.options || {};
    logGroup = options["awslogs-group"];
    const logStreamPrefix = options["awslogs-stream-prefix"];
    region = options["awslogs-region"];

    const containerName = containerDefinition.name;
    const taskId = taskArn.split("/").pop();
    logStreamName = `${logStreamPrefix}/${containerName}/${taskId}`;
  } catch (error) {
    console.error(`Error retrieving log configuration: ${error}`);
    process.exit(1);
  }

  // Step 4: Retrieve and print the CloudWatch logs
  try {
    const logsClient = new CloudWatchLogsClient({ region });
    let nextToken;
    const events = [];

    while (true) {
      const params = {
        logGroupName: logGroup,
        logStreamName: logStreamName,
        startFromHead: true,
        ...(nextToken && { nextToken }),
      };

      const response = await logsClient.send(new GetLogEventsCommand(params));
      events.push(...response.events);

      if (!response.nextForwardToken || response.nextForwardToken === nextToken)
        break;
      nextToken = response.nextForwardToken;
    }

    events.forEach((event) => console.log(event.message));
  } catch (error) {
    console.error(`Error retrieving CloudWatch logs: ${error}`);
    process.exit(1);
  }

  // Exit with the exit code from the ECS task
  process.exit(exitCode);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
