param([string]$BaseUrl="http://localhost:8080",[string]$KeycloakUrl="http://localhost:8180")
$ErrorActionPreference="Stop"
function Token($u,$p){(Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" -ContentType "application/x-www-form-urlencoded" -Body @{client_id="sus-smart-care-postman";grant_type="password";username=$u;password=$p}).access_token}
function Status($token,$method,$url,$body=$null){try{$h=@{Authorization="Bearer $token"};if($body){Invoke-WebRequest -SkipHttpErrorCheck -Method $method -Uri $url -Headers $h -ContentType application/json -Body ($body|ConvertTo-Json -Compress)|Select-Object -ExpandProperty StatusCode}else{Invoke-WebRequest -SkipHttpErrorCheck -Method $method -Uri $url -Headers $h|Select-Object -ExpandProperty StatusCode}}catch{return [int]$_.Exception.Response.StatusCode}}
$patient=Token demo.patient demo123;$doctor=Token demo.doctor demo123
$public=(Invoke-WebRequest -SkipHttpErrorCheck "$BaseUrl/api/v1/queues/00000000-0000-0000-0000-000000000001/public-view").StatusCode
$denied=Status $patient Get "$BaseUrl/api/v1/clinical-view/visits/00000000-0000-0000-0000-000000000001"
$doctorStatus=Status $doctor Get "$BaseUrl/api/v1/clinical-view/visits/00000000-0000-0000-0000-000000000001"
if($public-ne200){throw "public queue deveria ser pública"}
if($denied-ne403){throw "PATIENT deveria receber 403 na View Data clínica; recebido $denied"}
if($doctorStatus-ne404){throw "DOCTOR deve ser autorizado e receber 404 apenas porque a view não existe; recebido $doctorStatus"}
Write-Host "OK - público=200, patient-clinical=403, doctor-clinical=404" -ForegroundColor Green
