import os
import uuid

from gevent import spawn_later
from gevent.lock import Semaphore
from locust import HttpUser, between, events, task
from locust.exception import StopUser

STRICT_HTTP_RESPONSES = os.getenv("PERFORMANCE_STRICT_HTTP", "false").lower() == "true"
REQUEST_LIMIT_VALUE = os.getenv("PERFORMANCE_REQUESTS", "").strip()
REQUEST_LIMIT = int(REQUEST_LIMIT_VALUE) if REQUEST_LIMIT_VALUE else None
AT_LEAST_REQUEST_LIMIT = os.getenv("PERFORMANCE_REQUESTS_AT_LEAST", "false").lower() == "true"

_budget_lock = Semaphore()
_reserved_requests = 0
_completed_requests = 0
_environment = None
_stop_scheduled = False


@events.init.add_listener
def initialize_request_budget(environment, **_kwargs):
    global _environment
    _environment = environment


@events.request.add_listener
def stop_after_request_budget(**_kwargs):
    global _completed_requests, _stop_scheduled
    if REQUEST_LIMIT is None:
        return

    with _budget_lock:
        _completed_requests += 1
        budget_reached = _completed_requests >= REQUEST_LIMIT

    if budget_reached and _environment and _environment.runner:
        if AT_LEAST_REQUEST_LIMIT:
            with _budget_lock:
                if _stop_scheduled:
                    return
                _stop_scheduled = True
            spawn_later(1.0, _environment.runner.quit)
        else:
            _environment.runner.quit()


def reserve_request():
    global _reserved_requests
    if REQUEST_LIMIT is None:
        return

    with _budget_lock:
        if _reserved_requests >= REQUEST_LIMIT:
            raise StopUser()
        _reserved_requests += 1


class CompanyCheckUser(HttpUser):
    wait_time = between(0.2, 0.8)

    @task(4)
    def check_company(self):
        reserve_request()
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
        reserve_request()
        with self.client.get(
            f"/verifications/{uuid.uuid4()}",
            name="GET /verifications/{verificationId}",
            catch_response=True,
        ) as response:
            if response.status_code == 404:
                response.success()
            else:
                response.failure(f"expected 404, received {response.status_code}")
