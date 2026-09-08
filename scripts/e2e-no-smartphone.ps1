param([string]$BaseUrl="http://localhost:8080",[string]$KeycloakUrl="http://localhost:8180")
$ErrorActionPreference="Stop"
function Token($user,$pass){(Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="sus-smart-care-postman";grant_type="password";username=$user;password=$pass}).access_token}
$token=Token "demo.admin" "admin123"
function Api($m,$path,$body=$null){$h=@{Authorization="Bearer $token";"X-Correlation-Id"=[guid]::NewGuid().ToString()};if($null-eq$body){return Invoke-RestMethod -Method $m -Uri "$BaseUrl$path" -Headers $h};Invoke-RestMethod -Method $m -Uri "$BaseUrl$path" -Headers $h -ContentType application/json -Body ($body|ConvertTo-Json -Depth 10 -Compress)}
function Poll($path){for($i=0;$i-lt 30;$i++){try{return Api Get $path}catch{if([int]$_.Exception.Response.StatusCode-ne404){throw};Start-Sleep 1}};throw "timeout $path"}
$facility=[guid]::NewGuid();$professional=[guid]::NewGuid()
$patient=Api Post "/api/v1/patients" @{fullName="Paciente sem smartphone";identifierType="CNS";identifierValue="700000000000001"}
Api Put "/api/v1/notifications/preferences" @{patientId=$patient.id;hasSmartphone=$false;preferredChannel="PUBLIC_DISPLAY";fallbackChannel="STAFF_CONSOLE"}|Out-Null
$visit=Api Post "/api/v1/pre-visits" @{patientId=$patient.id;facilityId=$facility;channel="RECEPTION"}
Api Post "/api/v1/pre-visits/$($visit.id)/check-in"|Out-Null
$triage=Poll "/api/v1/triages/by-visit/$($visit.id)"
Api Post "/api/v1/triages/$($triage.id)/assessment" @{recommendation="MEDIUM";confidence=0.70;reasoning="Avaliação demonstrativa registrada pelo profissional"}|Out-Null
Api Post "/api/v1/triages/$($triage.id)/decision" @{priority="MEDIUM";professionalId=$professional}|Out-Null
$entry=Poll "/api/v1/queue-entries/by-visit/$($visit.id)"
Write-Host "Telão público (sem PII):" -ForegroundColor Cyan
Invoke-RestMethod "$BaseUrl/api/v1/queues/$facility/public-view" | ConvertTo-Json -Depth 8
Write-Host "Registrando saída assistida e janela de retorno..."
Api Post "/api/v1/queue-entries/$($entry.id)/leave" @{expectedReturnMinutes=15;graceMinutes=10}|Out-Null
Start-Sleep 2
Write-Host "Notificações:"
Api Get "/api/v1/notifications/patients/$($patient.id)" | ConvertTo-Json -Depth 8
Write-Host "Retorno registrado na recepção; prioridade clínica deve permanecer MEDIUM."
Api Post "/api/v1/presence/events" @{visitId=$visit.id;patientId=$patient.id;eventType="PATIENT_RETURNED";source="RECEPTION"}|Out-Null
Start-Sleep 1
$after=Api Get "/api/v1/queue-entries/by-visit/$($visit.id)"
if($after.clinicalPriority-ne"MEDIUM"){throw "Prioridade clínica foi alterada indevidamente"}
Write-Host "OK - experiência equivalente sem smartphone, código $($after.displayCode)" -ForegroundColor Green
