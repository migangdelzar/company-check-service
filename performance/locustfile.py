import os
import uuid

from locust import HttpUser, between, task

STRICT_HTTP_RESPONSES = os.getenv("PERFORMANCE_STRICT_HTTP", "false").lower() == "true"


class CompanyCheckUser(HttpUser):
    wait_time = between(0.2, 0.8)

    @task(4)
    def check_company(self):
        with self.client.get(
            "/backend-service",
            params={"verificationId": str(uuid.uuid4()), "query": "Acme"},
            name="GET /backend-service",
            catch_response=True,
        ) as response:
            allowed_statuses = {200} if STRICT_HTTP_RESPONSES else {200, 502, 503}
            if response.status_code in allowed_statuses:
                response.success()
            else:
                response.failure(f"unexpected status {response.status_code}")

    @task(1)
    def read_unknown_verification(self):
        with self.client.get(
            f"/verifications/{uuid.uuid4()}",
            name="GET /verifications/{verificationId}",
            catch_response=True,
        ) as response:
            if response.status_code == 404:
                response.success()
            else:
                response.failure(f"expected 404, received {response.status_code}")
