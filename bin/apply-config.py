#!/usr/bin/env python3

"""
This script invokes a Lambda function to apply ECS configuration changes, waits for the ECS task
to finish, and fetches its logs from CloudWatch Logs.

Usage:
    python apply_config.py <lambdaArn> <configEnvironmentJson>

Example:
    python apply_config.py arn:aws:lambda:us-east-1:123456789012:function:applyConfig '{"key":"value"}'
"""

import sys
import json
import time
import traceback
import boto3


def main(lambda_arn: str, config_env_json: str):
    # Default exit code is None, which we'll interpret as 0 if no errors occur.
    exit_code = None

    try:
        # 1) Invoke the Lambda function
        response_payload = invoke_lambda(lambda_arn, config_env_json)

        # Response should contain { "taskArn": "...", "clusterArn": "..." }
        task_arn = response_payload.get("taskArn")
        cluster_arn = response_payload.get("clusterArn")
        if not task_arn or not cluster_arn:
            print("Lambda did not return expected 'taskArn' or 'clusterArn'")
            return 1

        # 2) Poll ECS to wait until the task is STOPPED
        #    Then ensure we can retrieve the container exit code
        attempts = 0
        ecs_client = boto3.client("ecs")
        while True:
            attempts += 1
            task = poll_ecs_task(ecs_client, task_arn, cluster_arn)
            containers = task.get("containers", [])
            if containers:
                exit_code = containers[0].get("exitCode")
                print(f"Task exit code: {exit_code}")
            else:
                print("No container found in task; cannot retrieve exit code.")
                exit_code = None

            # If we have an exit code, stop checking
            if exit_code is not None:
                break

            if attempts >= 3:
                print("Could not retrieve exit code from the ECS task. Moving on...")
                break

            # Wait briefly before re-checking
            time.sleep(0.5)

        # 3) Fetch the log configuration from the task definition
        log_config = get_log_config(ecs_client, task.get("taskDefinitionArn"))
        if not log_config:
            return 1

        log_group = log_config["logGroup"]
        log_stream_prefix = log_config["logStreamPrefix"]
        region = log_config["region"]
        container_name = log_config["containerName"]

        # 4) Retrieve CloudWatch logs
        task_id = task_arn.split("/")[-1]
        log_stream_name = f"{log_stream_prefix}/{container_name}/{task_id}"
        logs = fetch_cloudwatch_logs(log_group, log_stream_name, region)

        # Print the logs to stdout
        print("Task output:\n" + "-" * 100)
        for line in logs:
            print(line)

        return exit_code or 0

    except Exception as e:
        print("Error:", str(e))
        print("Stack trace:", traceback.format_exc())
        return 1


def invoke_lambda(lambda_arn, env_json):
    """
    Invokes the specified Lambda function with the JSON payload.
    Returns the parsed JSON response from Lambda.
    """
    lambda_client = boto3.client("lambda")
    response = lambda_client.invoke(
        FunctionName=lambda_arn,
        InvocationType="RequestResponse",
        Payload=env_json.encode("utf-8"),
    )

    payload = response["Payload"].read()
    return json.loads(payload.decode("utf-8") or "{}")


def poll_ecs_task(ecs_client, task_arn, cluster_arn):
    """
    Polls ECS until the given task is in the STOPPED state, returning its description.
    """
    while True:
        response = ecs_client.describe_tasks(cluster=cluster_arn, tasks=[task_arn])
        tasks = response.get("tasks", [])
        if not tasks:
            raise RuntimeError(f"No tasks found with taskArn: {task_arn}")

        task = tasks[0]
        last_status = task.get("lastStatus")
        print(f"Task status: {last_status}")
        if last_status == "STOPPED":
            return task

        time.sleep(5)


def get_log_config(ecs_client, task_definition_arn):
    """
    Retrieves the AWS logs configuration from the first container definition.
    Returns dict with logGroup, logStreamPrefix, region, and containerName keys.
    """
    response = ecs_client.describe_task_definition(taskDefinition=task_definition_arn)
    task_definition = response.get("taskDefinition")
    if not task_definition:
        print(f"Could not retrieve task definition: {task_definition_arn}")
        return None

    container_defs = task_definition.get("containerDefinitions", [])
    if not container_defs:
        print("No container definitions found in task definition.")
        return None

    container_def = container_defs[0]
    log_config = container_def.get("logConfiguration")
    if not log_config or log_config.get("logDriver") != "awslogs":
        print("Log driver is not 'awslogs'. Cannot fetch logs.")
        return None

    options = log_config.get("options", {})
    return {
        "logGroup": options.get("awslogs-group"),
        "logStreamPrefix": options.get("awslogs-stream-prefix"),
        "region": options.get("awslogs-region"),
        "containerName": container_def.get("name"),
    }


def fetch_cloudwatch_logs(log_group, log_stream_name, region):
    """
    Fetches all log events from the specified CloudWatch log stream.
    Returns a list of log messages.
    """
    logs_client = boto3.client("logs", region_name=region)
    all_events = []
    next_token = None

    while True:
        response = logs_client.get_log_events(
            logGroupName=log_group,
            logStreamName=log_stream_name,
            startFromHead=True,
            **({"nextToken": next_token} if next_token else {}),
        )

        events = response.get("events", [])
        all_events.extend(events)

        new_token = response.get("nextForwardToken")
        # If there's no new token or it hasn't changed, we're done
        if not new_token or new_token == next_token:
            break

        next_token = new_token

    # Return just the messages
    return [event.get("message", "") for event in all_events]


if __name__ == "__main__":
    # Parse command-line arguments
    if len(sys.argv) < 2:
        print("Usage: python apply_config.py <lambdaArn> [configEnvironmentJson]")
        sys.exit(1)

    lambda_arn = sys.argv[1]
    assert lambda_arn, f"Must provide valid Lambda ARN, got {lambda_arn=}"
    config_env_json = sys.argv[2] if len(sys.argv) > 2 else "{}"

    print(f"{lambda_arn=}")
    print(f"{config_env_json=}")
    sys.exit(main(lambda_arn, config_env_json))
