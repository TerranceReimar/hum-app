FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

RUN mkdir -p /data

# Run init_db on startup then launch
CMD ["sh", "-c", "python -c 'from main import init_db; init_db()' && uvicorn main:app --host 0.0.0.0 --port 8000"]
