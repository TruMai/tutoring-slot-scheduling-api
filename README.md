API Tests (test-requests.http)
Open the project in VS Code.

How to run:
Delete tutoring.db
mvn spring-boot:run

REST Client extension installed

Open test-requests.http and click "Send Request" above each block sequentially.

Set notification.mock.down=true in application properties.

Restart the app and run requests #1 and #4 from above.

Expected Results:

DB shows the booking successfully created (commit-then-notify pattern).

Console logs show retry attempts, timeout warnings, and fallback execution.
