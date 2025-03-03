# Keycloak Smoke Tests

## Setup Instructions

1. **Clone the Repository**

   If you haven't already, clone the repository:

   ```bash
   git clone git@github.com:NASA-IMPACT/veda-keycloak.git
   ```

   Navigate to the project test directory:

   ```bash
   cd veda-keycloak/tests
   ```

2. **Create and Activate a Virtual Environment**

   It's recommended to use a virtual environment to manage dependencies. Create and activate one using the following
   commands:

    - On **macOS/Linux**:

      ```bash
      python3 -m venv venv
      source venv/bin/activate
      ```

    - On **Windows**:

      ```bash
      python -m venv venv
      .\venv\Scripts\activate
      ```

3. **Install Dependencies**

   With the virtual environment active, install the required Python packages:

   ```bash
   pip install -r requirements.txt
   ```

4. **Configure the Test Settings**

   Ensure that the `config/settings_<env>.ini` file exists and is correctly set up.

## Running the Tests

1. **Run All Tests**

   Execute all tests using `pytest`:

   ```bash
   pytest
   ```

   This command will discover and run all test files matching the pattern `test_*.py` or `*_test.py` within the current
   directory and its subdirectories.


2. **Run a Specific Test File**

   To execute tests in a particular test file:

   ```bash
   pytest smoke/test_health_check.py --env=prod
   ```

3. **Run a Specific Test Function**

   To run a specific test function within a test file:

   ```bash
   pytest smoke/test_health_check.py::test_keycloak_health_check --env=local
   ```

## Additional Pytest Options

- **Verbose Output**: For more detailed output, add the `-v` flag:

  ```bash
  pytest -v
  ```

- **Stop After First Failure**: To halt the test run after the first failure, use the `-x` option:

  ```bash
  pytest -x
  ```

- **Show Local Variables on Failure**: To display local variables in tracebacks, include the `-l` flag:

  ```bash
  pytest -l
  ```