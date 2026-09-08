param([string]$BaseUrl="http://localhost:8080",[string]$KeycloakUrl="http://localhost:8180")
$ErrorActionPreference="Stop"
function GetToken($u,$p){(Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="sus-smart-care-postman";grant_type="password";username=$u;password=$p}).access_token}
$ambulanceToken=GetToken "demo.ambulance" "demo123";$adminToken=GetToken "demo.admin" "admin123"
function Api($token,$m,$path,$body=$null,$allow404=$false){$h=@{Authorization="Bearer $token";"X-Correlation-Id"=[guid]::NewGuid().ToString()};try{if($null-eq$body){return Invoke-RestMethod -Method $m -Uri "$BaseUrl$path" -Headers $h};return Invoke-RestMethod -Method $m -Uri "$BaseUrl$path" -Headers $h -ContentType application/json -Body ($body|ConvertTo-Json -Depth 12 -Compress)}catch{if($allow404-and[int]$_.Exception.Response.StatusCode-eq404){return $null};throw}}
function Poll($token,$path){for($i=0;$i-lt30;$i++){$v=Api $token Get $path $null $true;if($null-ne$v){return$v};Start-Sleep 1};throw "timeout $path"}
$facility=[guid]::NewGuid();$eta=(Get-Date).ToUniversalTime().AddMinutes(12).ToString("o")
Write-Host "Ambulância criando identidade provisória compartilhada..."
$patient=Api $ambulanceToken Post "/api/v1/patients/provisional" @{description="Paciente pré-hospitalar ainda não identificado"}
$visit=Api $ambulanceToken Post "/api/v1/pre-visits" @{patientId=$patient.id;facilityId=$facility;channel="AMBULANCE"}
$enc=Api $ambulanceToken Post "/api/v1/pre-hospital/encounters" @{patientId=$patient.id;visitId=$visit.id;ambulanceId="SAMU-SIM-01";destinationFacilityId=$facility;estimatedArrivalAt=$eta}
$device=Api $ambulanceToken Post "/api/v1/devices" @{externalId="AMB-MON-$([guid]::NewGuid().ToString('N').Substring(0,8))";deviceType="MULTIPARAMETER_MONITOR";manufacturer="Simulator";model="AMB-01"}
$session=Api $ambulanceToken Post "/api/v1/telemetry/sessions" @{patientId=$patient.id;visitId=$visit.id;preHospitalEncounterId=$enc.id;sourceContext="PRE_HOSPITAL"}
Api $ambulanceToken Post "/api/v1/telemetry/sessions/$($session.id)/observations" @{deviceId=$device.id;type="OXYGEN_SATURATION";value=89;unit="%"}|Out-Null
Api $ambulanceToken Post "/api/v1/telemetry/sessions/$($session.id)/observations" @{deviceId=$device.id;type="HEART_RATE";value=126;unit="bpm"}|Out-Null
Write-Host "Atualizando sinal pré-hospitalar para HIGH; hospital deve receber pre-arrival alert."
Api $ambulanceToken Put "/api/v1/pre-hospital/encounters/$($enc.id)/risk" @{riskLevel="HIGH"}|Out-Null
$view=Poll $adminToken "/api/v1/clinical-view/visits/$($visit.id)"
Write-Host "View Data antes da chegada:" -ForegroundColor Cyan
$view|ConvertTo-Json -Depth 10
Write-Host "Agora a ambulância chega; check-in mantém a mesma jornada/patientId."
Api $ambulanceToken Post "/api/v1/pre-visits/$($visit.id)/check-in"|Out-Null
$triage=Poll $adminToken "/api/v1/triages/by-visit/$($visit.id)"
Write-Host "OK - triagem hospitalar criada para a mesma visita: $($triage.id)" -ForegroundColor Green
