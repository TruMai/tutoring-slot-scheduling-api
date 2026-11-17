API Tests (test-requests.http) includes testing instructions
Open the project in VS Code w/ REST Client extension

How to run:
Delete tutoring.db if exist
Set API KEY w/ Google Ai Studio

Powershell: 
setx GEMINI_API_KEY "api-key-from-aistudio"
mvn spring-boot:run

Open test-requests.http and click "Send Request" above each block sequentially.

Expected Results are included
