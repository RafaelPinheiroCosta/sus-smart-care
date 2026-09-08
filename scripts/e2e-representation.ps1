param([string]$BaseUrl="http://localhost:8080",[string]$KeycloakUrl="http://localhost:8180")
$ErrorActionPreference="Stop"
$t=(Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="sus-smart-care-postman";grant_type="password";username="demo.admin";password="admin123"}).access_token
function Api($m,$p,$b=$null){$h=@{Authorization="Bearer $t"};if($null-eq$b){return Invoke-RestMethod -Method $m -Uri "$BaseUrl$p" -Headers $h};Invoke-RestMethod -Method $m -Uri "$BaseUrl$p" -Headers $h -ContentType application/json -Body ($b|ConvertTo-Json -Depth 8 -Compress)}
$parent=[guid]::NewGuid();$futureSelf=[guid]::NewGuid()
$patient=Api Post "/api/v1/patients" @{fullName="Paciente dependente";birthDate="2014-05-20";identifierType="CNS";identifierValue="700000000000002"}
$rel=Api Post "/api/v1/patients/$($patient.id)/representatives" @{representativeUserId=$parent;type="PARENT"}
Write-Host "Paciente $($patient.id) inicialmente representado pelo pai/tutor $parent"
Api Delete "/api/v1/patients/representatives/$($rel.id)"|Out-Null
$self=Api Post "/api/v1/patients/$($patient.id)/representatives" @{representativeUserId=$futureSelf;type="SELF"}
$loaded=Api Get "/api/v1/patients/$($patient.id)"
if($loaded.id-ne$patient.id){throw "PatientId mudou indevidamente"}
Write-Host "OK - representação mudou, histórico/PatientId permaneceu $($loaded.id)" -ForegroundColor Green
