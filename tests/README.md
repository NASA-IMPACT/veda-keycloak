# Keycloak Smoke Tests

## Setup Instructions

1. **Clone the Repository**

   If you haven't already, clone the repository:

   ```bash
   git clone git@github.com:NASA-IMPACT/veda-keycloak.git
   ```

   Navigate to the project directory:

   ```bash
   cd veda-keycloak
   ```

2. **Install Dependencies**

   Using uv, install the required dependencies:

   ```bash
   uv sync
   ```

## Running the Tests

1. **Run All Tests**

   Execute all tests using `pytest`:

   ```bash
   uv run pytest
   ```

   This command will discover and run all test files matching the pattern `test_*.py` or `*_test.py` within the current
   directory and its subdirectories.


2. **Run a Specific Test File**

   To execute tests in a particular test file:

   ```bash
   uv run pytest tests/smoke/test_health_check.py
   ```

3. **Run a Specific Test Function**

   To run a specific test function within a test file:

   ```bash
   uv run pytest tests/smoke/test_health_check.py::test_keycloak_health_check
   ```

## Additional Pytest Options

- **Verbose Output**: For more detailed output, add the `-v` flag:

  ```bash
  uv run pytest -v
  ```

- **Stop After First Failure**: To halt the test run after the first failure, use the `-x` option:

  ```bash
  uv run pytest -x
  ```

- **Show Local Variables on Failure**: To display local variables in tracebacks, include the `-l` flag:

  ```bash
  uv run pytest -l
  ```