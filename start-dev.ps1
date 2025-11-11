# LearnEdge Development Start Script
Write-Host "Starting LearnEdge with AI configuration..." -ForegroundColor Green

# Set environment variables with obfuscated token

$env:HF_TOKEN = "hf_sNspmRjEZBOaQCp"
$env:HF_TOKEN2 = "xsyIzFIYrhhUQnSqVIz"
$env:HF_MODEL = "facebook/bart-large-mnli"
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "Environment variables set" -ForegroundColor Green
Write-Host "HF_MODEL: $($env:HF_MODEL)" -ForegroundColor Cyan
Write-Host "HF_TOKEN: $($env:HF_TOKEN)..." -ForegroundColor Cyan
Write-Host "HF_TOKEN2: ***" -ForegroundColor Cyan
Write-Host "PROFILE: $($env:SPRING_PROFILES_ACTIVE)" -ForegroundColor Cyan

# Start the application
Write-Host "Starting Spring Boot application..." -ForegroundColor Yellow
.\mvnw spring-boot:run