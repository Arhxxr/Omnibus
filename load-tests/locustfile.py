"""
Omnibus — Locust Load Test Suite
====================================

Simulates realistic payment gateway traffic:
  - User registration + login (session setup)
  - Concurrent money transfers with idempotency keys
  - Account balance queries
  - Mixed read/write workload

Usage:
    # Install dependencies
    pip install -r requirements.txt

    # Run with web UI (default: http://localhost:8089)
    locust -f locustfile.py --host http://localhost:8080

    # Run headless (CI/CD)
    locust -f locustfile.py --host http://localhost:8080 \
        --headless -u 50 -r 10 --run-time 2m \
        --csv=results/load_test

    # Run specific user class only
    locust -f locustfile.py --host http://localhost:8080 PaymentUser
"""

import random
import string
import uuid
from locust import HttpUser, task, between, events, tag


class PaymentUser(HttpUser):
    """
    Simulates a real user: registers, logs in, then performs a mix of
    transfers and account queries throughout the session.

    Weight distribution:
      - 60% transfers (write-heavy, the critical path)
      - 30% balance checks (read)
      - 10% list accounts (read)
    """

    wait_time = between(0.5, 2.0)
    abstract = False

    def on_start(self):
        """Register a new user and capture the JWT + account ID."""
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        self.username = f"locust_{suffix}"
        self.email = f"{self.username}@loadtest.local"
        self.password = "L0custP@ss!2026"
        self.token = None
        self.account_id = None
        self.peer_account_ids = []
        self._idempotency_counter = 0

        # --- Register ---
        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": self.username,
                "email": self.email,
                "password": self.password,
            },
            name="/api/v1/auth/register",
        )
        if resp.status_code == 201:
            data = resp.json()
            self.token = data["token"]
            self._fetch_my_account()
        else:
            # If registration fails (e.g. duplicate), try login
            self._login()

    def _login(self):
        resp = self.client.post(
            "/api/v1/auth/login",
            json={"username": self.username, "password": self.password},
            name="/api/v1/auth/login",
        )
        if resp.status_code == 200:
            data = resp.json()
            self.token = data["token"]
            self._fetch_my_account()

    def _fetch_my_account(self):
        """Fetch the authenticated user's accounts to get the account ID."""
        if not self.token:
            return
        resp = self.client.get(
            "/api/v1/accounts",
            headers=self._auth_headers(),
            name="/api/v1/accounts [setup]",
        )
        if resp.status_code == 200:
            accounts = resp.json()
            if accounts:
                self.account_id = accounts[0]["id"]

    def _auth_headers(self):
        return {"Authorization": f"Bearer {self.token}"}

    def _next_idempotency_key(self):
        self._idempotency_counter += 1
        return f"{self.username}-{self._idempotency_counter}-{uuid.uuid4().hex[:8]}"

    # --- Tasks ---

    @task(6)
    @tag("transfer")
    def transfer_money(self):
        """Execute a transfer to a random peer. Creates peer on-the-fly if needed."""
        if not self.token or not self.account_id:
            return

        # Get or create a peer to transfer to
        target_id = self._get_peer_account()
        if not target_id:
            return

        amount = round(random.uniform(0.01, 50.00), 4)
        key = self._next_idempotency_key()

        self.client.post(
            "/api/v1/transfers",
            json={
                "sourceAccountId": self.account_id,
                "targetAccountId": target_id,
                "amount": amount,
                "currency": "USD",
                "description": f"Load test transfer {key}",
            },
            headers={**self._auth_headers(), "Idempotency-Key": key},
            name="/api/v1/transfers",
        )

    @task(3)
    @tag("read")
    def check_balance(self):
        """Query own account balance."""
        if not self.token or not self.account_id:
            return
        self.client.get(
            f"/api/v1/accounts/{self.account_id}",
            headers=self._auth_headers(),
            name="/api/v1/accounts/{id}",
        )

    @task(1)
    @tag("read")
    def list_accounts(self):
        """List all accounts for the authenticated user."""
        if not self.token:
            return
        self.client.get(
            "/api/v1/accounts",
            headers=self._auth_headers(),
            name="/api/v1/accounts",
        )

    def _get_peer_account(self) -> str | None:
        """
        Returns a peer account ID for transfers.
        Creates a new peer user if the pool is empty or with 20% probability
        to keep growing the peer set.
        """
        if self.peer_account_ids and random.random() > 0.2:
            return random.choice(self.peer_account_ids)

        # Create a new peer
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        peer_user = f"peer_{suffix}"
        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": peer_user,
                "email": f"{peer_user}@loadtest.local",
                "password": "P33rP@ss!2026",
            },
            name="/api/v1/auth/register [peer]",
        )
        if resp.status_code == 201:
            peer_token = resp.json()["token"]
            # Fetch peer's account
            acct_resp = self.client.get(
                "/api/v1/accounts",
                headers={"Authorization": f"Bearer {peer_token}"},
                name="/api/v1/accounts [peer-setup]",
            )
            if acct_resp.status_code == 200:
                accounts = acct_resp.json()
                if accounts:
                    peer_acct = accounts[0]["id"]
                    self.peer_account_ids.append(peer_acct)
                    return peer_acct
        return self.peer_account_ids[0] if self.peer_account_ids else None


class IdempotencyStressUser(HttpUser):
    """
    Stress-tests idempotency: sends the same request multiple times
    with the same idempotency key to verify exactly-once processing.

    Lower weight — runs fewer instances than PaymentUser.
    """

    wait_time = between(1, 3)
    weight = 1  # vs PaymentUser default weight of 1
    abstract = False

    def on_start(self):
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        self.username = f"idem_{suffix}"
        self.token = None
        self.account_id = None
        self.peer_account_id = None

        # Register self
        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": self.username,
                "email": f"{self.username}@loadtest.local",
                "password": "Id3mP@ss!2026",
            },
            name="/api/v1/auth/register",
        )
        if resp.status_code == 201:
            self.token = resp.json()["token"]
            self._setup_accounts()

    def _setup_accounts(self):
        if not self.token:
            return
        # Get own account
        resp = self.client.get(
            "/api/v1/accounts",
            headers={"Authorization": f"Bearer {self.token}"},
            name="/api/v1/accounts [setup]",
        )
        if resp.status_code == 200 and resp.json():
            self.account_id = resp.json()[0]["id"]

        # Create peer
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        peer_user = f"idempeer_{suffix}"
        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": peer_user,
                "email": f"{peer_user}@loadtest.local",
                "password": "Id3mP33r!2026",
            },
            name="/api/v1/auth/register [peer]",
        )
        if resp.status_code == 201:
            peer_token = resp.json()["token"]
            acct_resp = self.client.get(
                "/api/v1/accounts",
                headers={"Authorization": f"Bearer {peer_token}"},
                name="/api/v1/accounts [peer-setup]",
            )
            if acct_resp.status_code == 200 and acct_resp.json():
                self.peer_account_id = acct_resp.json()[0]["id"]

    @task
    @tag("idempotency")
    def replay_same_key(self):
        """
        Sends 5 identical requests with the same idempotency key.
        Expects: 1 x 201 CREATED + 4 x 200 OK with Idempotency-Replayed header.
        """
        if not self.token or not self.account_id or not self.peer_account_id:
            return

        key = f"stress-{uuid.uuid4().hex}"
        amount = round(random.uniform(1.00, 10.00), 2)

        created_count = 0
        replayed_count = 0

        for i in range(5):
            resp = self.client.post(
                "/api/v1/transfers",
                json={
                    "sourceAccountId": self.account_id,
                    "targetAccountId": self.peer_account_id,
                    "amount": amount,
                    "currency": "USD",
                },
                headers={
                    **{"Authorization": f"Bearer {self.token}"},
                    "Idempotency-Key": key,
                },
                name="/api/v1/transfers [idempotency-replay]",
            )
            if resp.status_code == 201:
                created_count += 1
            elif resp.status_code == 200:
                replayed_count += 1

        # Exactly 1 should have been created, rest replayed
        if created_count != 1:
            events.request.fire(
                request_type="VALIDATION",
                name="idempotency_exactly_once",
                response_time=0,
                response_length=0,
                exception=Exception(
                    f"Expected 1 CREATED, got {created_count} "
                    f"(replayed={replayed_count})"
                ),
            )


class HighThroughputUser(HttpUser):
    """
    Simulates burst traffic — no wait between requests.
    Used for peak-load profiling.
    """

    wait_time = between(0, 0.1)
    weight = 1
    abstract = False

    def on_start(self):
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        self.username = f"burst_{suffix}"
        self.token = None
        self.account_id = None
        self.peer_id = None
        self._counter = 0

        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": self.username,
                "email": f"{self.username}@loadtest.local",
                "password": "Bur5tP@ss!2026",
            },
            name="/api/v1/auth/register",
        )
        if resp.status_code == 201:
            self.token = resp.json()["token"]
            self._setup()

    def _setup(self):
        if not self.token:
            return
        resp = self.client.get(
            "/api/v1/accounts",
            headers={"Authorization": f"Bearer {self.token}"},
            name="/api/v1/accounts [setup]",
        )
        if resp.status_code == 200 and resp.json():
            self.account_id = resp.json()[0]["id"]

        # Create peer
        suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=12))
        resp = self.client.post(
            "/api/v1/auth/register",
            json={
                "username": f"bpeer_{suffix}",
                "email": f"bpeer_{suffix}@loadtest.local",
                "password": "Bur5tP33r!2026",
            },
            name="/api/v1/auth/register [peer]",
        )
        if resp.status_code == 201:
            peer_token = resp.json()["token"]
            acct = self.client.get(
                "/api/v1/accounts",
                headers={"Authorization": f"Bearer {peer_token}"},
                name="/api/v1/accounts [peer-setup]",
            )
            if acct.status_code == 200 and acct.json():
                self.peer_id = acct.json()[0]["id"]

    @task
    @tag("burst")
    def burst_transfer(self):
        if not self.token or not self.account_id or not self.peer_id:
            return
        self._counter += 1
        self.client.post(
            "/api/v1/transfers",
            json={
                "sourceAccountId": self.account_id,
                "targetAccountId": self.peer_id,
                "amount": 0.01,
                "currency": "USD",
            },
            headers={
                **{"Authorization": f"Bearer {self.token}"},
                "Idempotency-Key": f"burst-{self.username}-{self._counter}",
            },
            name="/api/v1/transfers [burst]",
        )
