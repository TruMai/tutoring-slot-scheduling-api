A way To Run (Window): 
./mvnw spring-boot: run

Clean State: Delete tutoring.db in between runs
Persistent State: Preserve tutoring.db 

Test endpoints inside IDE:
test-request.http via send request with REST Client Extension (VSCODE)

Run ConcurrencyTest.java
./mvnw test -Dtest=ConcurrencyTest
