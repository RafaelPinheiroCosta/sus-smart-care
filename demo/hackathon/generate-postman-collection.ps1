param([string]$OutputPath = "")

$ErrorActionPreference = "Stop"
if (-not $OutputPath) {
  $OutputPath = Join-Path $PSScriptRoot "SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json"
}

function Lines([string]$Text) {
  if (-not $Text) { return @() }
  return @($Text -split "`r?`n")
}

function Event([string]$Listen,[string]$Script) {
  [ordered]@{ listen=$Listen; script=[ordered]@{ type="text/javascript"; exec=(Lines $Script) } }
}

function Request {
  param(
    [string]$Name,[string]$Method,[string]$Url,[string]$Token="",
    [string]$Body="",[string]$Test="",[string]$Pre="",
    [hashtable]$Form=$null,[string]$Description=""
  )
  $headers=@()
  if ($Token) { $headers += [ordered]@{key="Authorization";value="Bearer {{$Token}}";type="text"} }
  if ($Body) { $headers += [ordered]@{key="Content-Type";value="application/json";type="text"} }
  $r=[ordered]@{method=$Method;header=$headers;url=$Url}
  if ($Body) { $r.body=[ordered]@{mode="raw";raw=$Body;options=[ordered]@{raw=[ordered]@{language="json"}}} }
  if ($Form) {
    $r.body=[ordered]@{mode="urlencoded";urlencoded=@()}
    foreach($k in $Form.Keys){ $r.body.urlencoded += [ordered]@{key=$k;value=[string]$Form[$k];type="text"} }
  }
  if ($Description) { $r.description=$Description }
  $i=[ordered]@{name=$Name;request=$r;response=@()}
  $events=@()
  if($Pre){$events+=Event "prerequest" $Pre}
  if($Test){$events+=Event "test" $Test}
  if($events.Count){$i.event=$events}
  return $i
}

function Folder([string]$Name,[array]$Items,[string]$Description="") {
  $f=[ordered]@{name=$Name;item=$Items}
  if($Description){$f.description=$Description}
  return $f
}

$ok200='pm.test("HTTP 200",()=>pm.expect(pm.response.code).to.eql(200));'
$ok201='pm.test("HTTP 201",()=>pm.expect(pm.response.code).to.eql(201));'

function Capture([string]$Var,[int]$Code=201,[string]$Extra="") {
@"
pm.test("HTTP $Code",()=>pm.expect(pm.response.code).to.eql($Code));
const j=pm.response.json();
pm.collectionVariables.set("$Var",j.id);
$Extra
"@
}

function Problem([int]$Code) {
@"
pm.test("Falha esperada HTTP $Code",()=>pm.expect(pm.response.code).to.eql($Code));
const j=pm.response.json();
pm.test("ProblemDetail tratado",()=>pm.expect(j.correlationId).to.exist);
"@
}

function Poll([string]$Var,[string]$Counter,[string]$Extra="") {
@"
if(pm.response.code===404){
 const n=Number(pm.collectionVariables.get("$Counter")||0)+1;
 pm.collectionVariables.set("$Counter",String(n));
 pm.test("Polling: 404/200 esperado",()=>pm.expect([200,404]).to.include(pm.response.code));
 if(n<40) setTimeout(()=>pm.execution.setNextRequest(pm.info.requestName),400);
 else pm.test("Timeout",()=>pm.expect.fail("projecao nao apareceu"));
}else{
 pm.test("Recurso projetado",()=>pm.expect(pm.response.code).to.eql(200));
 const j=pm.response.json();
 pm.collectionVariables.set("$Var",j.id);
 pm.collectionVariables.unset("$Counter");
 $Extra
}
"@
}

function Login([string]$Name,[string]$User,[string]$Pass,[string]$TokenVar,[string]$SubVar="") {
  $extra=""
  if($SubVar){
$extra=@"
const p=j.access_token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');
const d=JSON.parse(atob(p+'='.repeat((4-p.length%4)%4)));
pm.collectionVariables.set('$SubVar',d.sub);
"@
  }
  Request $Name "POST" '{{keycloakUrl}}/realms/{{realm}}/protocol/openid-connect/token' -Form ([ordered]@{
    client_id='{{postmanClientId}}';grant_type='password';username=$User;password=$Pass
  }) -Test @"
pm.test('Token obtido',()=>pm.expect(pm.response.code).to.eql(200));
const j=pm.response.json();pm.collectionVariables.set('$TokenVar',j.access_token);
$extra
"@
}

$varNames=@(
'baseUrl','keycloakUrl','prometheusUrl','grafanaUrl','tempoUrl','realm','postmanClientId','deviceClientId','deviceClientSecret','demoRunId',
'adminToken','representativeToken','representativeUserId','patientToken','patientUserId','triageToken','operatorToken','doctorToken','ambulanceToken','deviceToken',
'mainFacilityId','lucasPatientId','lucasParentRelationshipId','lucasChildVisitId','lucasChildTriageId','lucasTriageDeviceId','lucasTriageDeviceExternalId','lucasTriageSessionId','lucasTriageAssignmentId','lucasQueueEntryId','lucasDisplayCode',
'carlosPatientId','carlosRelationshipId','carlosVisitId','carlosTriageId','carlosQueueEntryId','marianaPatientId','marianaVisitId','marianaTriageId','marianaQueueEntryId','pauloPatientId','pauloVisitId','pauloTriageId','pauloQueueEntryId',
'lucasSelfRelationshipId','lucasAdultVisitId','lucasAdultTriageId','lucasAdultQueueEntryId',
'noPhoneFacilityId','rosaPatientId','rosaVisitId','rosaTriageId','rosaQueueEntryId','rosaDisplayCode','npHighPatientId','npHighVisitId','npHighTriageId','npHighQueueEntryId','npLowPatientId','npLowVisitId','npLowTriageId','npLowQueueEntryId',
'ambulanceFacilityId','ambulancePatientId','ambulanceVisitId','prehospitalEncounterId','ambulanceDeviceId','ambulanceDeviceExternalId','ambulanceTelemetrySessionId','ambulanceAssignmentId','ambulanceEta','ambulanceTriageId'
)
$defaults=@{baseUrl='http://localhost:8080';keycloakUrl='http://localhost:8180';prometheusUrl='http://localhost:9090';grafanaUrl='http://localhost:3000';tempoUrl='http://localhost:3200';realm='sus-smart-care';postmanClientId='sus-smart-care-postman';deviceClientId='device-client';deviceClientSecret='device-secret'}
$variables=@();foreach($v in $varNames){$variables+=[ordered]@{key=$v;value=($(if($defaults.ContainsKey($v)){$defaults[$v]}else{''}));type='string'}}

$items=@()

# 00 -------------------------------------------------------------------------
$auth=@()
$auth+=Request '00.0 RESET DEMO RUN + Gateway Health' 'GET' '{{baseUrl}}/actuator/health' -Test @'
pm.test("Gateway saudável",()=>pm.expect(pm.response.code).to.eql(200));
pm.collectionVariables.set("demoRunId",Date.now().toString());
const keep=new Set(["baseUrl","keycloakUrl","prometheusUrl","grafanaUrl","tempoUrl","realm","postmanClientId","deviceClientId","deviceClientSecret","adminToken","representativeToken","representativeUserId","patientToken","patientUserId","triageToken","operatorToken","doctorToken","ambulanceToken","deviceToken","demoRunId"]);
pm.collectionVariables.each(v=>{if(!keep.has(v.key))pm.collectionVariables.set(v.key,"");});
console.log("DEMO RUN",pm.collectionVariables.get("demoRunId"));
'@ -Description 'Execute uma vez no inicio de cada ensaio.'
$auth+=Login '00.1 Login ADMIN' 'demo.admin' 'admin123' 'adminToken'
$auth+=Login '00.2 Login REPRESENTATIVE (Ana)' 'demo.representative' 'demo123' 'representativeToken' 'representativeUserId'
$auth+=Login '00.3 Login PATIENT (Lucas futuro)' 'demo.patient' 'demo123' 'patientToken' 'patientUserId'
$auth+=Login '00.4 Login TRIAGE_NURSE' 'demo.triage' 'demo123' 'triageToken'
$auth+=Login '00.5 Login OPERATOR' 'demo.operator' 'demo123' 'operatorToken'
$auth+=Login '00.6 Login DOCTOR' 'demo.doctor' 'demo123' 'doctorToken'
$auth+=Login '00.7 Login AMBULANCE_TEAM' 'demo.ambulance' 'demo123' 'ambulanceToken'
$auth+=Request '00.8 Login DEVICE' 'POST' '{{keycloakUrl}}/realms/{{realm}}/protocol/openid-connect/token' -Form ([ordered]@{client_id='{{deviceClientId}}';client_secret='{{deviceClientSecret}}';grant_type='client_credentials'}) -Test 'pm.test("Token DEVICE",()=>pm.expect(pm.response.code).to.eql(200));const j=pm.response.json();pm.collectionVariables.set("deviceToken",j.access_token);'
$auth+=Request '00.9 Criar unidade principal' 'POST' '{{baseUrl}}/api/v1/facilities' 'adminToken' '{"code":"DEMO-MAIN-{{demoRunId}}","name":"SUS Smart Care - Unidade Principal","type":"HOSPITAL"}' (Capture 'mainFacilityId')
$items+=Folder '00 - AUTENTICACAO E PREPARACAO' $auth

# 01 Lucas -------------------------------------------------------------------
$l=@()
$l+=Request '01.01 Cadastrar Patient Lucas' 'POST' '{{baseUrl}}/api/v1/patients' 'adminToken' '{"fullName":"Lucas Demo {{demoRunId}}","birthDate":"2014-05-20","identifierType":"OTHER","identifierValue":"LUCAS-{{demoRunId}}"}' (Capture 'lucasPatientId')
$l+=Request '01.02 Vincular Ana como PARENT' 'POST' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}/representatives' 'adminToken' '{"representativeUserId":"{{representativeUserId}}","type":"PARENT"}' (Capture 'lucasParentRelationshipId')
$l+=Request '01.03 Provar acesso ACTIVE_REPRESENTATION' 'GET' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}/access' 'representativeToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));pm.test("ACTIVE_REPRESENTATION",()=>pm.expect(pm.response.json().reason).to.eql("ACTIVE_REPRESENTATION"));'
$l+=Request '01.04 Perfil de comunicacao MOBILE' 'PUT' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}/communication-profile' 'representativeToken' '{"hasSmartphone":true,"queueCallMode":"MOBILE"}' $ok200
$l+=Request '01.05 Ana cria PreVisit' 'POST' '{{baseUrl}}/api/v1/pre-visits' 'representativeToken' '{"patientId":"{{lucasPatientId}}","facilityId":"{{mainFacilityId}}","channel":"MOBILE"}' (Capture 'lucasChildVisitId')
$l+=Request '01.06 Ana registra pre-anamnese' 'PUT' '{{baseUrl}}/api/v1/pre-visits/{{lucasChildVisitId}}/anamnesis' 'representativeToken' '{"text":"Febre baixa, dor de garganta e mal-estar leve desde ontem."}' $ok200
$l+=Request '01.07 Check-in de Lucas' 'POST' '{{baseUrl}}/api/v1/pre-visits/{{lucasChildVisitId}}/check-in' 'representativeToken' -Test $ok200
$l+=Request '01.08 FALHA ESPERADA - segundo check-in' 'POST' '{{baseUrl}}/api/v1/pre-visits/{{lucasChildVisitId}}/check-in' 'representativeToken' -Test (Problem 409)
$l+=Request '01.09 Poll triagem criada por evento' 'GET' '{{baseUrl}}/api/v1/triages/by-visit/{{lucasChildVisitId}}' 'triageToken' -Test (Poll 'lucasChildTriageId' 'pollLucasTriage')
$l+=Request '01.10 Iniciar triagem' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/triage/start' 'triageToken' -Test $ok200
$l+=Request '01.11 Registrar monitor de triagem' 'POST' '{{baseUrl}}/api/v1/devices' 'adminToken' '{"externalId":"TRIAGE-LUCAS-{{demoRunId}}","deviceType":"MULTIPARAMETER_MONITOR","manufacturer":"SUS Smart Care Simulator","model":"TRIAGE-DEMO-01"}' (Capture 'lucasTriageDeviceId' 201 'pm.collectionVariables.set("lucasTriageDeviceExternalId",j.externalId);')
$l+=Request '01.12 Posicionar monitor na unidade' 'POST' '{{baseUrl}}/api/v1/devices/{{lucasTriageDeviceId}}/placements' 'adminToken' '{"placementType":"FACILITY","facilityId":"{{mainFacilityId}}"}' $ok201
$l+=Request '01.13 Abrir sessao SPOT' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions' 'triageToken' '{"patientId":"{{lucasPatientId}}","visitId":"{{lucasChildVisitId}}","sourceContext":"TRIAGE","mode":"SPOT"}' (Capture 'lucasTriageSessionId')
$l+=Request '01.14 Associar monitor a sessao' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{lucasTriageSessionId}}/assignments' 'triageToken' '{"deviceId":"{{lucasTriageDeviceId}}"}' (Capture 'lucasTriageAssignmentId') -Description 'Agora execute simulate-triage-vitals.ps1 com SessionId e DeviceId.'
$fallback=@()
$fallback+=Request 'HR' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{lucasTriageSessionId}}/observations' 'deviceToken' '{"deviceId":"{{lucasTriageDeviceId}}","type":"HEART_RATE","value":94,"unit":"bpm"}' -Test 'pm.test("202",()=>pm.expect(pm.response.code).to.eql(202));'
$fallback+=Request 'SPO2' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{lucasTriageSessionId}}/observations' 'deviceToken' '{"deviceId":"{{lucasTriageDeviceId}}","type":"SPO2","value":97,"unit":"%"}' -Test 'pm.test("202",()=>pm.expect(pm.response.code).to.eql(202));'
$fallback+=Request 'Temperatura' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{lucasTriageSessionId}}/observations' 'deviceToken' '{"deviceId":"{{lucasTriageDeviceId}}","type":"TEMPERATURE","value":37.4,"unit":"C"}' -Test 'pm.test("202",()=>pm.expect(pm.response.code).to.eql(202));'
$l+=Folder '01.15 FALLBACK Postman - sinais vitais' $fallback 'Use apenas se nao executar o script PowerShell.'
$l+=Request '01.16 Apoio de IA' 'POST' '{{baseUrl}}/api/v1/triages/{{lucasChildTriageId}}/assessment/ai' 'triageToken' -Test $ok200
$l+=Request '01.17 Profissional confirma LOW' 'POST' '{{baseUrl}}/api/v1/triages/{{lucasChildTriageId}}/decision' 'triageToken' '{"priority":"LOW"}' $ok200
$l+=Request '01.18 Concluir triagem' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/triage/complete' 'triageToken' -Test $ok200
$l+=Request '01.19 Poll QueueEntry' 'GET' '{{baseUrl}}/api/v1/queue-entries/by-visit/{{lucasChildVisitId}}' 'operatorToken' -Test (Poll 'lucasQueueEntryId' 'pollLucasQueue' 'pm.collectionVariables.set("lucasDisplayCode",j.displayCode);')
$l+=Request '01.20 Journey entra em MEDICAL_CARE' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/queue' 'operatorToken' '{"stage":"MEDICAL_CARE"}' $ok200
$l+=Request '01.21 MEDICO - Clinical View durante atendimento' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasChildVisitId}}' 'doctorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const j=pm.response.json();pm.test("Lucas",()=>pm.expect(j.patientId).to.eql(pm.collectionVariables.get("lucasPatientId")));'
$l+=Request '01.22 FILA AGORA - Lucas' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId"));pm.test("LOW",()=>pm.expect(l.clinicalPriority).to.eql("LOW"));'
$l+=Request '01.23 Ana registra saida temporaria' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{lucasQueueEntryId}}/leave' 'representativeToken' '{"expectedReturnMinutes":15,"graceMinutes":10}' 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const j=pm.response.json();pm.test("OUTSIDE/RETURN_REQUIRED",()=>{pm.expect(j.presence).to.eql("OUTSIDE_FACILITY");pm.expect(j.status).to.eql("RETURN_REQUIRED");});'
$l+=Request '01.24 Notificacoes RETURN_REQUIRED' 'GET' '{{baseUrl}}/api/v1/notifications/patients/{{lucasPatientId}}' 'representativeToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json();pm.test("RETURN_REQUIRED",()=>pm.expect(a.some(x=>x.type==="RETURN_REQUIRED")).to.be.true);'
$l+=Request '01.25 FALHA ESPERADA - chamar Lucas ausente' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{lucasQueueEntryId}}/call' 'operatorToken' -Test (Problem 409)

function Add-Competitor([string]$Code,[string]$Label,[string]$PVar,[string]$VVar,[string]$TVar,[string]$QVar,[string]$Priority,[bool]$Represented){
  $m=@();$r=@()
  $m+=Request "$Code.1 Cadastrar $Label" 'POST' '{{baseUrl}}/api/v1/patients' 'adminToken' "{`"fullName`":`"$Label Demo {{demoRunId}}`",`"identifierType`":`"OTHER`",`"identifierValue`":`"$Code-{{demoRunId}}`"}" (Capture $PVar)
  if($Represented){
    $m+=Request "$Code.2 Vincular CAREGIVER" 'POST' "{{baseUrl}}/api/v1/patients/{{$PVar}}/representatives" 'adminToken' '{"representativeUserId":"{{representativeUserId}}","type":"CAREGIVER"}' (Capture 'carlosRelationshipId')
    $m+=Request "$Code.3 PreVisit via REPRESENTATIVE" 'POST' '{{baseUrl}}/api/v1/pre-visits' 'representativeToken' "{`"patientId`":`"{{$PVar}}`",`"facilityId`":`"{{mainFacilityId}}`",`"channel`":`"MOBILE`"}" (Capture $VVar)
    $m+=Request "$Code.4 Check-in via REPRESENTATIVE" 'POST' "{{baseUrl}}/api/v1/pre-visits/{{$VVar}}/check-in" 'representativeToken' -Test $ok200
  }else{
    $m+=Request "$Code.2 PreVisit operacional" 'POST' '{{baseUrl}}/api/v1/pre-visits' 'adminToken' "{`"patientId`":`"{{$PVar}}`",`"facilityId`":`"{{mainFacilityId}}`",`"channel`":`"RECEPTION`"}" (Capture $VVar)
    $m+=Request "$Code.3 Check-in" 'POST' "{{baseUrl}}/api/v1/pre-visits/{{$VVar}}/check-in" 'adminToken' -Test $ok200
  }
  $r+=Request "$Code.R1 Poll triagem" 'GET' "{{baseUrl}}/api/v1/triages/by-visit/{{$VVar}}" 'triageToken' -Test (Poll $TVar "poll$TVar")
  $r+=Request "$Code.R2 Start triage" 'POST' "{{baseUrl}}/api/v1/visits/{{$VVar}}/triage/start" 'triageToken' -Test $ok200
  $r+=Request "$Code.R3 Assessment $Priority" 'POST' "{{baseUrl}}/api/v1/triages/{{$TVar}}/assessment" 'triageToken' "{`"recommendation`":`"$Priority`",`"confidence`":0.88,`"reasoning`":`"Cenario demonstrativo $Label`"}" $ok200
  $r+=Request "$Code.R4 Decision $Priority" 'POST' "{{baseUrl}}/api/v1/triages/{{$TVar}}/decision" 'triageToken' "{`"priority`":`"$Priority`"}" $ok200
  $r+=Request "$Code.R5 Complete" 'POST' "{{baseUrl}}/api/v1/visits/{{$VVar}}/triage/complete" 'triageToken' -Test $ok200
  $r+=Request "$Code.R6 Poll queue" 'GET' "{{baseUrl}}/api/v1/queue-entries/by-visit/{{$VVar}}" 'operatorToken' -Test (Poll $QVar "poll$QVar")
  $r+=Request "$Code.R7 Journey queue" 'POST' "{{baseUrl}}/api/v1/visits/{{$VVar}}/queue" 'operatorToken' '{"stage":"MEDICAL_CARE"}' $ok200
  return @((Folder "$Code MANUAL - entrada $Label" $m),(Folder "$Code RUNNER - $Label ate fila $Priority" $r))
}
foreach($x in (Add-Competitor '01.30' 'Carlos' 'carlosPatientId' 'carlosVisitId' 'carlosTriageId' 'carlosQueueEntryId' 'HIGH' $true)){$l+=$x}
$l+=Request '01.32 FILA AGORA - Carlos > Lucas' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),c=a.find(x=>x.visitId===pm.collectionVariables.get("carlosVisitId")),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId"));pm.test("HIGH passa LOW",()=>pm.expect(c.estimatedPosition).to.be.below(l.estimatedPosition));'
foreach($x in (Add-Competitor '01.40' 'Mariana' 'marianaPatientId' 'marianaVisitId' 'marianaTriageId' 'marianaQueueEntryId' 'MEDIUM' $false)){$l+=$x}
$l+=Request '01.42 FILA AGORA - HIGH > MEDIUM > LOW' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),c=a.find(x=>x.visitId===pm.collectionVariables.get("carlosVisitId")),m=a.find(x=>x.visitId===pm.collectionVariables.get("marianaVisitId")),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId"));pm.test("ordem",()=>{pm.expect(c.estimatedPosition).to.be.below(m.estimatedPosition);pm.expect(m.estimatedPosition).to.be.below(l.estimatedPosition);});'
foreach($x in (Add-Competitor '01.50' 'Paulo' 'pauloPatientId' 'pauloVisitId' 'pauloTriageId' 'pauloQueueEntryId' 'LOW' $false)){$l+=$x}
$l+=Request '01.52 FILA AGORA - Lucas LOW antes de Paulo LOW' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId")),p=a.find(x=>x.visitId===pm.collectionVariables.get("pauloVisitId"));pm.test("FIFO dentro de LOW",()=>pm.expect(l.estimatedPosition).to.be.below(p.estimatedPosition));pm.test("Lucas segue fora",()=>pm.expect(l.presence).to.eql("OUTSIDE_FACILITY"));'
$l+=Request '01.60 Ana registra retorno' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{lucasQueueEntryId}}/return' 'representativeToken' -Test $ok200
$l+=Request '01.61 FILA AGORA - retorno preservado' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId")),p=a.find(x=>x.visitId===pm.collectionVariables.get("pauloVisitId"));pm.test("Lucas antes Paulo",()=>pm.expect(l.estimatedPosition).to.be.below(p.estimatedPosition));'
$call=@();$call+=Request 'Carlos Queue call' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{carlosQueueEntryId}}/call' 'operatorToken' -Test $ok200;$call+=Request 'Carlos Journey call' 'POST' '{{baseUrl}}/api/v1/visits/{{carlosVisitId}}/call' 'operatorToken' -Test $ok200;$call+=Request 'Mariana Queue call' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{marianaQueueEntryId}}/call' 'operatorToken' -Test $ok200;$call+=Request 'Mariana Journey call' 'POST' '{{baseUrl}}/api/v1/visits/{{marianaVisitId}}/call' 'operatorToken' -Test $ok200
$l+=Folder '01.62 RUNNER - chamar Carlos e Mariana' $call
$l+=Request '01.63 FILA AGORA - Lucas 1 Paulo 2' 'GET' '{{baseUrl}}/api/v1/queues/{{mainFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),l=a.find(x=>x.visitId===pm.collectionVariables.get("lucasChildVisitId")),p=a.find(x=>x.visitId===pm.collectionVariables.get("pauloVisitId"));pm.test("Lucas primeiro",()=>pm.expect(l.estimatedPosition).to.eql(1));pm.test("Paulo atras",()=>pm.expect(p.estimatedPosition).to.be.above(l.estimatedPosition));'
$l+=Request '01.70 Chamar Lucas na Queue' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{lucasQueueEntryId}}/call' 'operatorToken' -Test $ok200
$l+=Request '01.71 Registrar chamada na Journey' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/call' 'operatorToken' -Test $ok200
$l+=Request '01.72 Notificacoes PATIENT_CALLED' 'GET' '{{baseUrl}}/api/v1/notifications/patients/{{lucasPatientId}}' 'representativeToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json();pm.test("PATIENT_CALLED",()=>pm.expect(a.some(x=>x.type==="PATIENT_CALLED")).to.be.true);'
$l+=Request '01.73 Medico inicia atendimento' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/service/start' 'doctorToken' -Test $ok200
$l+=Request '01.74 Medico da alta' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasChildVisitId}}/discharge' 'doctorToken' '{"note":"Quadro leve, orientacoes fornecidas e alta clinica."}' $ok200
$l+=Request '01.75 MEDICO - Clinical View FINAL infancia' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasChildVisitId}}' 'doctorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const j=pm.response.json();pm.test("DISCHARGED",()=>pm.expect(j.journeyOutcome).to.eql("DISCHARGED"));pm.test("mesmo Patient",()=>pm.expect(j.patientId).to.eql(pm.collectionVariables.get("lucasPatientId")));'
$items+=Folder '01 - LUCAS CRIANCA - JORNADA COMPLETA' $l

# 02 Lucas SELF ---------------------------------------------------------------
$a=@()
$a+=Request '02.01 Revogar PARENT de Ana' 'DELETE' '{{baseUrl}}/api/v1/patients/representatives/{{lucasParentRelationshipId}}' 'adminToken' -Test 'pm.test("204",()=>pm.expect(pm.response.code).to.eql(204));'
$a+=Request '02.02 FALHA ESPERADA - Ana sem vinculo' 'GET' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}' 'representativeToken' -Test (Problem 403)
$a+=Request '02.03 Vincular SELF a conta de Lucas' 'POST' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}/self-link' 'adminToken' '{"userId":"{{patientUserId}}"}' (Capture 'lucasSelfRelationshipId')
$a+=Request '02.04 Provar SELF' 'GET' '{{baseUrl}}/api/v1/patients/{{lucasPatientId}}/access' 'patientToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));pm.test("SELF",()=>pm.expect(pm.response.json().reason).to.eql("SELF"));'
$ar=@()
$ar+=Request 'A1 PreVisit pelo proprio PATIENT' 'POST' '{{baseUrl}}/api/v1/pre-visits' 'patientToken' '{"patientId":"{{lucasPatientId}}","facilityId":"{{mainFacilityId}}","channel":"MOBILE"}' (Capture 'lucasAdultVisitId')
$ar+=Request 'A2 Anamnese' 'PUT' '{{baseUrl}}/api/v1/pre-visits/{{lucasAdultVisitId}}/anamnesis' 'patientToken' '{"text":"Novo atendimento em etapa futura, operado pela propria conta."}' $ok200
$ar+=Request 'A3 Check-in' 'POST' '{{baseUrl}}/api/v1/pre-visits/{{lucasAdultVisitId}}/check-in' 'patientToken' -Test $ok200
$ar+=Request 'A4 Poll triagem' 'GET' '{{baseUrl}}/api/v1/triages/by-visit/{{lucasAdultVisitId}}' 'triageToken' -Test (Poll 'lucasAdultTriageId' 'pollLucasAdultTriage')
$ar+=Request 'A5 Start triage' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/triage/start' 'triageToken' -Test $ok200
$ar+=Request 'A6 Assessment MEDIUM' 'POST' '{{baseUrl}}/api/v1/triages/{{lucasAdultTriageId}}/assessment' 'triageToken' '{"recommendation":"MEDIUM","confidence":0.84,"reasoning":"Atendimento futuro demonstrativo"}' $ok200
$ar+=Request 'A7 Decision MEDIUM' 'POST' '{{baseUrl}}/api/v1/triages/{{lucasAdultTriageId}}/decision' 'triageToken' '{"priority":"MEDIUM"}' $ok200
$ar+=Request 'A8 Complete' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/triage/complete' 'triageToken' -Test $ok200
$ar+=Request 'A9 Poll queue' 'GET' '{{baseUrl}}/api/v1/queue-entries/by-visit/{{lucasAdultVisitId}}' 'operatorToken' -Test (Poll 'lucasAdultQueueEntryId' 'pollLucasAdultQueue')
$ar+=Request 'A10 Journey queue' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/queue' 'operatorToken' '{"stage":"MEDICAL_CARE"}' $ok200
$ar+=Request 'A11 Queue call' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{lucasAdultQueueEntryId}}/call' 'operatorToken' -Test $ok200
$ar+=Request 'A12 Journey call' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/call' 'operatorToken' -Test $ok200
$ar+=Request 'A13 Service start' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/service/start' 'doctorToken' -Test $ok200
$ar+=Request 'A14 Discharge' 'POST' '{{baseUrl}}/api/v1/visits/{{lucasAdultVisitId}}/discharge' 'doctorToken' '{"note":"Atendimento futuro concluido."}' $ok200
$ar+=Request 'A15 Clinical View' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasAdultVisitId}}' 'doctorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const j=pm.response.json();pm.test("mesmo Patient",()=>pm.expect(j.patientId).to.eql(pm.collectionVariables.get("lucasPatientId")));'
$a+=Folder '02.05 RUNNER - novo atendimento via SELF' $ar 'No Runner marque Keep variable values.'
$a+=Request '02.06 Clinical View infancia preservada' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasChildVisitId}}' 'doctorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));pm.test("mesmo Patient",()=>pm.expect(pm.response.json().patientId).to.eql(pm.collectionVariables.get("lucasPatientId")));'
$a+=Request '02.07 Clinical View atendimento futuro' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasAdultVisitId}}' 'doctorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));pm.test("mesmo Patient",()=>pm.expect(pm.response.json().patientId).to.eql(pm.collectionVariables.get("lucasPatientId")));'
$items+=Folder '02 - LUCAS FUTURO - REPRESENTACAO PARA SELF' $a

# 03 sem smartphone -----------------------------------------------------------
$n=@()
$n+=Request '03.01 Criar unidade inclusiva' 'POST' '{{baseUrl}}/api/v1/facilities' 'adminToken' '{"code":"DEMO-NOPHONE-{{demoRunId}}","name":"SUS Smart Care - Unidade Inclusiva","type":"UPA"}' (Capture 'noPhoneFacilityId')
$n+=Request '03.02 Cadastrar Dona Rosa' 'POST' '{{baseUrl}}/api/v1/patients' 'operatorToken' '{"fullName":"Dona Rosa Demo {{demoRunId}}","birthDate":"1952-09-18","identifierType":"OTHER","identifierValue":"ROSA-{{demoRunId}}"}' (Capture 'rosaPatientId')
$n+=Request '03.03 FALHA ESPERADA - sem smartphone + MOBILE' 'PUT' '{{baseUrl}}/api/v1/patients/{{rosaPatientId}}/communication-profile' 'operatorToken' '{"hasSmartphone":false,"queueCallMode":"MOBILE"}' (Problem 400)
$n+=Request '03.04 Corrigir DISPLAY_AND_VERBAL' 'PUT' '{{baseUrl}}/api/v1/patients/{{rosaPatientId}}/communication-profile' 'operatorToken' '{"hasSmartphone":false,"queueCallMode":"DISPLAY_AND_VERBAL"}' $ok200
$n+=Request '03.05 PreVisit RECEPTION' 'POST' '{{baseUrl}}/api/v1/pre-visits' 'operatorToken' '{"patientId":"{{rosaPatientId}}","facilityId":"{{noPhoneFacilityId}}","channel":"RECEPTION"}' (Capture 'rosaVisitId')
$n+=Request '03.06 Check-in' 'POST' '{{baseUrl}}/api/v1/pre-visits/{{rosaVisitId}}/check-in' 'operatorToken' -Test $ok200
$n+=Request '03.07 Poll triagem' 'GET' '{{baseUrl}}/api/v1/triages/by-visit/{{rosaVisitId}}' 'triageToken' -Test (Poll 'rosaTriageId' 'pollRosaTriage')
$n+=Request '03.08 Start triage' 'POST' '{{baseUrl}}/api/v1/visits/{{rosaVisitId}}/triage/start' 'triageToken' -Test $ok200
$n+=Request '03.09 Assessment MEDIUM' 'POST' '{{baseUrl}}/api/v1/triages/{{rosaTriageId}}/assessment' 'triageToken' '{"recommendation":"MEDIUM","confidence":0.79,"reasoning":"Avaliacao Dona Rosa"}' $ok200
$n+=Request '03.10 Decision MEDIUM' 'POST' '{{baseUrl}}/api/v1/triages/{{rosaTriageId}}/decision' 'triageToken' '{"priority":"MEDIUM"}' $ok200
$n+=Request '03.11 Complete' 'POST' '{{baseUrl}}/api/v1/visits/{{rosaVisitId}}/triage/complete' 'triageToken' -Test $ok200
$n+=Request '03.12 Poll QueueEntry' 'GET' '{{baseUrl}}/api/v1/queue-entries/by-visit/{{rosaVisitId}}' 'operatorToken' -Test (Poll 'rosaQueueEntryId' 'pollRosaQueue' 'pm.collectionVariables.set("rosaDisplayCode",j.displayCode);')
$n+=Request '03.13 Journey queue' 'POST' '{{baseUrl}}/api/v1/visits/{{rosaVisitId}}/queue' 'operatorToken' '{"stage":"MEDICAL_CARE"}' $ok200

function NoPhoneCompetitor([string]$Tag,[string]$P,[string]$V,[string]$T,[string]$Q,[string]$Priority){
 $z=@();$z+=Request "$Tag Criar" 'POST' '{{baseUrl}}/api/v1/patients' 'operatorToken' "{`"fullName`":`"$Tag {{demoRunId}}`",`"identifierType`":`"OTHER`",`"identifierValue`":`"$Tag-{{demoRunId}}`"}" (Capture $P);$z+=Request "$Tag PreVisit" 'POST' '{{baseUrl}}/api/v1/pre-visits' 'operatorToken' "{`"patientId`":`"{{$P}}`",`"facilityId`":`"{{noPhoneFacilityId}}`",`"channel`":`"RECEPTION`"}" (Capture $V);$z+=Request "$Tag Check-in" 'POST' "{{baseUrl}}/api/v1/pre-visits/{{$V}}/check-in" 'operatorToken' -Test $ok200;$z+=Request "$Tag Poll triage" 'GET' "{{baseUrl}}/api/v1/triages/by-visit/{{$V}}" 'triageToken' -Test (Poll $T "poll$T");$z+=Request "$Tag Start" 'POST' "{{baseUrl}}/api/v1/visits/{{$V}}/triage/start" 'triageToken' -Test $ok200;$z+=Request "$Tag Assessment" 'POST' "{{baseUrl}}/api/v1/triages/{{$T}}/assessment" 'triageToken' "{`"recommendation`":`"$Priority`",`"confidence`":0.85,`"reasoning`":`"Concorrente $Priority`"}" $ok200;$z+=Request "$Tag Decision" 'POST' "{{baseUrl}}/api/v1/triages/{{$T}}/decision" 'triageToken' "{`"priority`":`"$Priority`"}" $ok200;$z+=Request "$Tag Complete" 'POST' "{{baseUrl}}/api/v1/visits/{{$V}}/triage/complete" 'triageToken' -Test $ok200;$z+=Request "$Tag Poll queue" 'GET' "{{baseUrl}}/api/v1/queue-entries/by-visit/{{$V}}" 'operatorToken' -Test (Poll $Q "poll$Q");$z+=Request "$Tag Journey queue" 'POST' "{{baseUrl}}/api/v1/visits/{{$V}}/queue" 'operatorToken' '{"stage":"MEDICAL_CARE"}' $ok200;return $z
}
$npc=@();$npc+=NoPhoneCompetitor 'NP-HIGH' 'npHighPatientId' 'npHighVisitId' 'npHighTriageId' 'npHighQueueEntryId' 'HIGH';$npc+=NoPhoneCompetitor 'NP-LOW' 'npLowPatientId' 'npLowVisitId' 'npLowTriageId' 'npLowQueueEntryId' 'LOW'
$n+=Folder '03.15 RUNNER - concorrentes da fila' $npc
$n+=Request '03.16 Visao operacional - Dona Rosa normal' 'GET' '{{baseUrl}}/api/v1/queues/{{noPhoneFacilityId}}/view' 'operatorToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),r=a.find(x=>x.visitId===pm.collectionVariables.get("rosaVisitId")),h=a.find(x=>x.visitId===pm.collectionVariables.get("npHighVisitId")),l=a.find(x=>x.visitId===pm.collectionVariables.get("npLowVisitId"));pm.test("Rosa na mesma fila",()=>pm.expect(r).to.exist);pm.test("HIGH > MEDIUM > LOW",()=>{pm.expect(h.estimatedPosition).to.be.below(r.estimatedPosition);pm.expect(r.estimatedPosition).to.be.below(l.estimatedPosition);});console.log("Dona Rosa",pm.collectionVariables.get("rosaDisplayCode"),r);'
$n+=Request '03.17 TELAO public-view sem PII' 'GET' '{{baseUrl}}/api/v1/queues/{{noPhoneFacilityId}}/public-view' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),c=pm.collectionVariables.get("rosaDisplayCode");pm.test("senha Rosa aparece",()=>pm.expect(a.some(x=>x.displayCode===c)).to.be.true);pm.test("sem PII",()=>a.forEach(x=>{pm.expect(x).to.not.have.property("patientId");pm.expect(x).to.not.have.property("patientName");pm.expect(x).to.not.have.property("clinicalPriority");}));'
$n+=Request '03.18 Totem - estimativa publica' 'GET' '{{baseUrl}}/api/v1/queues/{{noPhoneFacilityId}}/estimate' -Test $ok200
$n+=Request '03.19 OPCIONAL chamar Dona Rosa' 'POST' '{{baseUrl}}/api/v1/queue-entries/{{rosaQueueEntryId}}/call' 'operatorToken' -Test $ok200
$n+=Request '03.20 OPCIONAL notificacoes assistidas' 'GET' '{{baseUrl}}/api/v1/notifications/patients/{{rosaPatientId}}' 'operatorToken' -Test $ok200
$items+=Folder '03 - SEM SMARTPHONE - DONA ROSA' $n

# 04 ambulancia ---------------------------------------------------------------
$am=@()
$am+=Request '04.01 Criar unidade destino' 'POST' '{{baseUrl}}/api/v1/facilities' 'adminToken' '{"code":"DEMO-AMB-{{demoRunId}}","name":"SUS Smart Care - Destino SAMU","type":"HOSPITAL"}' (Capture 'ambulanceFacilityId')
$am+=Request '04.02 Patient provisorio' 'POST' '{{baseUrl}}/api/v1/patients/provisional' 'ambulanceToken' '{"description":"Paciente pre-hospitalar ainda nao identificado - {{demoRunId}}"}' (Capture 'ambulancePatientId')
$am+=Request '04.03 PreVisit AMBULANCE' 'POST' '{{baseUrl}}/api/v1/pre-visits' 'ambulanceToken' '{"patientId":"{{ambulancePatientId}}","facilityId":"{{ambulanceFacilityId}}","channel":"AMBULANCE"}' (Capture 'ambulanceVisitId')
$am+=Request '04.04 Encounter pre-hospitalar' 'POST' '{{baseUrl}}/api/v1/pre-hospital/encounters' 'ambulanceToken' '{"patientId":"{{ambulancePatientId}}","visitId":"{{ambulanceVisitId}}","ambulanceId":"SAMU-SIM-01","destinationFacilityId":"{{ambulanceFacilityId}}","estimatedArrivalAt":"{{ambulanceEta}}"}' (Capture 'prehospitalEncounterId') -Pre 'pm.collectionVariables.set("ambulanceEta",new Date(Date.now()+12*60*1000).toISOString());'
$am+=Request '04.05 Registrar monitor embarcado' 'POST' '{{baseUrl}}/api/v1/devices' 'adminToken' '{"externalId":"AMB-MON-{{demoRunId}}","deviceType":"MULTIPARAMETER_MONITOR","manufacturer":"SUS Smart Care Simulator","model":"AMB-01"}' (Capture 'ambulanceDeviceId' 201 'pm.collectionVariables.set("ambulanceDeviceExternalId",j.externalId);')
$am+=Request '04.06 Posicionar na ambulancia' 'POST' '{{baseUrl}}/api/v1/devices/{{ambulanceDeviceId}}/placements' 'adminToken' '{"placementType":"AMBULANCE","ambulanceId":"SAMU-SIM-01"}' $ok201
$am+=Request '04.07 Abrir sessao CONTINUOUS' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions' 'ambulanceToken' '{"patientId":"{{ambulancePatientId}}","visitId":"{{ambulanceVisitId}}","preHospitalEncounterId":"{{prehospitalEncounterId}}","sourceContext":"PRE_HOSPITAL","mode":"CONTINUOUS"}' (Capture 'ambulanceTelemetrySessionId')
$am+=Request '04.08 Associar monitor a sessao' 'POST' '{{baseUrl}}/api/v1/telemetry/sessions/{{ambulanceTelemetrySessionId}}/assignments' 'ambulanceToken' '{"deviceId":"{{ambulanceDeviceId}}"}' (Capture 'ambulanceAssignmentId' 201 'console.log("ACK: .\\demo\\hackathon\\listen-ambulance-acks.ps1 -DeviceExternalId \""+pm.collectionVariables.get("ambulanceDeviceExternalId")+"\"");console.log("ROBO: .\\demo\\hackathon\\simulate-ambulance-mqtt.ps1 -DeviceExternalId \""+pm.collectionVariables.get("ambulanceDeviceExternalId")+"\" -SessionId \""+pm.collectionVariables.get("ambulanceTelemetrySessionId")+"\"");')
$am+=Request '04.09 Agregados apos MQTT' 'GET' '{{baseUrl}}/api/v1/telemetry/sessions/{{ambulanceTelemetrySessionId}}/aggregates' 'ambulanceToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json(),h=a.find(x=>x.type==="HEART_RATE");pm.test("HR agregado >= 10",()=>{pm.expect(h).to.exist;pm.expect(h.sampleCount).to.be.at.least(10);});'
$am+=Request '04.10 Anomalias apos MQTT' 'GET' '{{baseUrl}}/api/v1/telemetry/sessions/{{ambulanceTelemetrySessionId}}/anomalies' 'ambulanceToken' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=pm.response.json();pm.test("SPO2 85 anomalo",()=>pm.expect(a.some(x=>x.type==="SPO2"&&Number(x.value)===85)).to.be.true);'
$am+=Request '04.11 Risco pre-hospitalar HIGH' 'PUT' '{{baseUrl}}/api/v1/pre-hospital/encounters/{{prehospitalEncounterId}}/risk' 'ambulanceToken' '{"riskLevel":"HIGH"}' $ok200
$am+=Request '04.12 MEDICO - Clinical View ANTES da chegada' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{ambulanceVisitId}}' 'doctorToken' -Test $ok200
$am+=Request '04.13 Marcar chegada encounter' 'POST' '{{baseUrl}}/api/v1/pre-hospital/encounters/{{prehospitalEncounterId}}/arrive' 'ambulanceToken' -Test $ok200
$am+=Request '04.14 Check-in MESMA Visit' 'POST' '{{baseUrl}}/api/v1/pre-visits/{{ambulanceVisitId}}/check-in' 'ambulanceToken' -Test $ok200
$am+=Request '04.15 Poll triagem mesma Visit' 'GET' '{{baseUrl}}/api/v1/triages/by-visit/{{ambulanceVisitId}}' 'triageToken' -Test (Poll 'ambulanceTriageId' 'pollAmbTriage')
$items+=Folder '04 - AMBULANCIA + MQTT + REDIS' $am

# 05 observabilidade/seguranca ------------------------------------------------
$o=@()
$o+=Request '05.01 Prometheus targets' 'GET' '{{prometheusUrl}}/api/v1/targets' -Test 'pm.test("200",()=>pm.expect(pm.response.code).to.eql(200));const a=(pm.response.json().data||{}).activeTargets||[];console.log("targets",a.length,"down",a.filter(x=>x.health!=="up"));pm.test("ha targets",()=>pm.expect(a.length).to.be.above(0));'
$o+=Request '05.02 DOCTOR Clinical View -> 200' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasChildVisitId}}' 'doctorToken' -Test $ok200
$o+=Request '05.03 PATIENT Clinical View -> 403' 'GET' '{{baseUrl}}/api/v1/clinical-view/visits/{{lucasChildVisitId}}' 'patientToken' -Test 'pm.test("403 esperado",()=>pm.expect(pm.response.code).to.eql(403));'
$items+=Folder '05 - OBSERVABILIDADE E SEGURANCA' $o

$collection=[ordered]@{
 info=[ordered]@{_postman_id=[guid]::NewGuid().ToString();name='SUS Smart Care - Hackathon Demo FINAL';description='Colecao alinhada ao roteiro do video. Requests manuais demonstram regras; pastas RUNNER preparam estados repetitivos. Marque Keep variable values.';schema='https://schema.getpostman.com/json/collection/v2.1.0/collection.json'}
 event=@(Event 'prerequest' 'pm.request.headers.upsert({key:"X-Correlation-Id",value:pm.variables.replaceIn("{{$guid}}")});')
 variable=$variables
 item=$items
}

$collection | ConvertTo-Json -Depth 100 | Set-Content -Encoding UTF8 $OutputPath

# Valida o JSON gerado no proprio PowerShell.
Get-Content -Raw $OutputPath | ConvertFrom-Json | Out-Null
Write-Host "Colecao gerada e JSON validado:" -ForegroundColor Green
Write-Host "  $OutputPath"
