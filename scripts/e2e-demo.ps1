param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$KeycloakUrl = "http://localhost:8180",
  [string]$Username = "demo.admin",
  [string]$Password = "admin123"
)
$ErrorActionPreference = "Stop"

function Json($obj) { return ($obj | ConvertTo-Json -Depth 12 -Compress) }
function Invoke-Api($Method, $Path, $Body=$null, $Allow404=$false) {
  $headers = @{ Authorization = "Bearer $script:Token"; "X-Correlation-Id" = [guid]::NewGuid().ToString() }
  try {
    if ($null -eq $Body) { return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -Headers $headers }
    return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -Headers $headers -ContentType "application/json" -Body (Json $Body)
  } catch {
    if ($Allow404 -and [int]$_.Exception.Response.StatusCode -eq 404) { return $null }
    throw
  }
}
function Poll($Description, [scriptblock]$Action, [int]$Attempts=30, [int]$DelayMs=1000) {
  for ($i=1; $i -le $Attempts; $i++) {
    $value = & $Action
    if ($null -ne $value) { Write-Host "OK - $Description" -ForegroundColor Green; return $value }
    Start-Sleep -Milliseconds $DelayMs
  }
  throw "Timeout aguardando: $Description"
}

Write-Host "1/12 Autenticando..."
$tokenResponse = Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{
  client_id="sus-smart-care-postman"; grant_type="password"; username=$Username; password=$Password
}
$script:Token = $tokenResponse.access_token

$facilityId = [guid]::NewGuid()
$cpf = "9" + (Get-Random -Minimum 1000000000 -Maximum 1999999999)

Write-Host "2/12 Registrando paciente canônico..."
$patient = Invoke-Api Post "/api/v1/patients" @{ fullName="Paciente Demo E2E"; birthDate="1988-04-10"; identifierType="CPF"; identifierValue=$cpf }
$patientId = $patient.id

Write-Host "3/12 Criando pré-atendimento e anamnese..."
$visit = Invoke-Api Post "/api/v1/pre-visits" @{ patientId=$patientId; facilityId=$facilityId; channel="MOBILE" }
$visitId = $visit.id
Invoke-Api Put "/api/v1/pre-visits/$visitId/anamnesis" @{ text="Dor e mal-estar informados no pré-atendimento. Conteúdo apenas demonstrativo." } | Out-Null

Write-Host "4/12 Realizando check-in; triagem será criada por evento..."
Invoke-Api Post "/api/v1/pre-visits/$visitId/check-in" | Out-Null
$triage = Poll "triagem criada por patient-checked-in" { Invoke-Api Get "/api/v1/triages/by-visit/$visitId" $null $true }
$triageId = $triage.id

Write-Host "5/12 Registrando dispositivo e sessão de telemetria..."
$device = Invoke-Api Post "/api/v1/devices" @{ externalId="DEV-E2E-$([guid]::NewGuid().ToString('N').Substring(0,8))"; deviceType="MULTIPARAMETER_MONITOR"; manufacturer="SUS Smart Care Simulator"; model="SIM-01" }
$session = Invoke-Api Post "/api/v1/telemetry/sessions" @{ patientId=$patientId; visitId=$visitId; sourceContext="TRIAGE" }

Write-Host "6/12 Enviando biometria normalizada..."
Invoke-Api Post "/api/v1/telemetry/sessions/$($session.id)/observations" @{ deviceId=$device.id; type="OXYGEN_SATURATION"; value=94.0; unit="%" } | Out-Null
Invoke-Api Post "/api/v1/telemetry/sessions/$($session.id)/observations" @{ deviceId=$device.id; type="HEART_RATE"; value=108.0; unit="bpm" } | Out-Null
Invoke-Api Post "/api/v1/telemetry/sessions/$($session.id)/observations" @{ deviceId=$device.id; type="BODY_TEMPERATURE"; value=38.2; unit="C" } | Out-Null

Write-Host "7/12 Executando boundary de IA (provider demo seguro)..."
$triage = Invoke-Api Post "/api/v1/triages/$triageId/assessment/ai"

Write-Host "8/12 Confirmando prioridade por profissional..."
$triage = Invoke-Api Post "/api/v1/triages/$triageId/decision" @{ priority="HIGH" }

Write-Host "9/12 Aguardando entrada automática na fila..."
$queueEntry = Poll "fila criada por clinical-priority-confirmed" { Invoke-Api Get "/api/v1/queue-entries/by-visit/$visitId" $null $true }

Write-Host "10/12 Testando presença/saída e retorno sem alterar prioridade clínica..."
Invoke-Api Post "/api/v1/presence/events" @{ visitId=$visitId; patientId=$patientId; eventType="LEFT_FACILITY"; source="RECEPTION" } | Out-Null
Start-Sleep -Milliseconds 700
Invoke-Api Post "/api/v1/presence/events" @{ visitId=$visitId; patientId=$patientId; eventType="PATIENT_RETURNED"; source="RECEPTION" } | Out-Null

Write-Host "11/12 Aguardando View Data clínica CQRS..."
$view = Poll "ClinicalPatientView atualizada" { Invoke-Api Get "/api/v1/clinical-view/visits/$visitId" $null $true }

Write-Host "12/12 Consultando telão público sem dados pessoais..."
$public = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/queues/$facilityId/public-view"

$result = [ordered]@{
  patientId=$patientId; visitId=$visitId; triageId=$triageId; queueEntryId=$queueEntry.id;
  clinicalPriority=$triage.finalPriority; queueDisplayCode=$queueEntry.displayCode;
  projectedPatientName=$view.patientName; latestVitals=$view.latestVitals; publicQueue=$public
}
Write-Host "`nE2E concluído:" -ForegroundColor Cyan
$result | ConvertTo-Json -Depth 12
