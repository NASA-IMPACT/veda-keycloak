FROM python:3.11-slim

WORKDIR /app

# Copy project files
COPY pyproject.toml .
COPY app app/

# Create venv and install dependencies
RUN pip install -e .

# Set environment variables
ENV PYTHONPATH=/app
ENV PATH="/app/venv/bin:$PATH"

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"] 