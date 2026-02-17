# Omnibus — Load Testing

## Prerequisites

- Python 3.10+
- Omnibus running at `http://localhost:8080` (via Docker Compose or local dev)

## Setup

```bash
cd load-tests
pip install -r requirements.txt
```

## Run with Web UI

```bash
locust -f locustfile.py --host http://localhost:8080
```

Open [http://localhost:8089](http://localhost:8089) to configure users, spawn rate, and view real-time charts.

## Headless (CI/CD)

```bash
# 50 users, ramp 10/sec, run 2 minutes, export CSV
locust -f locustfile.py --host http://localhost:8080 \
    --headless -u 50 -r 10 --run-time 2m \
    --csv=results/load_test
```

## User Classes

| Class                  | Weight | Wait Time  | Description                                  |
|------------------------|--------|------------|----------------------------------------------|
| `PaymentUser`          | 1      | 0.5–2.0s   | Realistic mix: 60% transfers, 30% balance, 10% list |
| `IdempotencyStressUser`| 1      | 1–3s       | Sends 5 identical requests per idempotency key |
| `HighThroughputUser`   | 1      | 0–0.1s     | Burst traffic for peak-load profiling         |

### Run specific class only

```bash
locust -f locustfile.py --host http://localhost:8080 PaymentUser
locust -f locustfile.py --host http://localhost:8080 HighThroughputUser
```

## Recommended Test Scenarios

### Smoke Test
```bash
locust -f locustfile.py --host http://localhost:8080 \
    --headless -u 5 -r 5 --run-time 30s PaymentUser
```

### Sustained Load
```bash
locust -f locustfile.py --host http://localhost:8080 \
    --headless -u 50 -r 5 --run-time 5m \
    --csv=results/sustained
```

### Peak/Spike
```bash
locust -f locustfile.py --host http://localhost:8080 \
    --headless -u 200 -r 50 --run-time 2m \
    --csv=results/spike HighThroughputUser
```

### Idempotency Validation
```bash
locust -f locustfile.py --host http://localhost:8080 \
    --headless -u 20 -r 10 --run-time 2m \
    --csv=results/idempotency IdempotencyStressUser
```

## Key Metrics to Watch

| Metric             | Target (Smoke)   | Target (Sustained) |
|--------------------|------------------|---------------------|
| p50 latency        | < 50ms           | < 100ms             |
| p95 latency        | < 200ms          | < 500ms             |
| p99 latency        | < 500ms          | < 1000ms            |
| Error rate         | 0%               | < 1%                |
| Requests/sec       | > 20             | > 100               |

## Output

CSV files are written to `results/` with:
- `*_stats.csv` — aggregate statistics per endpoint
- `*_stats_history.csv` — time-series data
- `*_failures.csv` — error details
- `*_exceptions.csv` — exception traces
