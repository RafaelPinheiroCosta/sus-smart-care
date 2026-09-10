param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$KeycloakUrl = "http://localhost:8180"
)

$ErrorActionPreference = "Stop"

function Get-Token {
  param(
    [string]$Username,
    [string]$Password
  )

  return (
    Invoke-RestMethod `
      -Method Post `
      -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" `
      -ContentType "application/x-www-form-urlencoded" `
      -Body @{
        client_id = "sus-smart-care-postman"
        grant_type = "password"
        username = $Username
        password = $Password
      }
  ).access_token
}

function Get-DeviceToken {

  return (
    Invoke-RestMethod `
      -Method Post `
      -Uri "$KeycloakUrl/realms/sus-smart-care/protocol/openid-connect/token" `
      -ContentType "application/x-www-form-urlencoded" `
      -Body @{
        client_id = "device-client"
        client_secret = "device-secret"
        grant_type = "client_credentials"
      }
  ).access_token
}

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    [string]$Token,
    [object]$Body
  )

  $Headers = @{
    "X-Correlation-Id" =
      [Guid]::NewGuid().ToString()
  }

  if ($Token) {
    $Headers.Authorization =
      "Bearer $Token"
  }

  $Params = @{
    UseBasicParsing = $true
    Method = $Method
    Uri = "$BaseUrl$Path"
    Headers = $Headers
    TimeoutSec = 20
  }

  if ($null -ne $Body) {
    $Params.ContentType =
      "application/json"

    $Params.Body =
      $Body |
      ConvertTo-Json -Depth 12
  }

  try {

    $R =
      Invoke-WebRequest @Params

    return [PSCustomObject]@{
      Status = [int]$R.StatusCode
      Content = [string]$R.Content
    }
  }
  catch {

    if ($null -ne $_.Exception.Response) {

      return [PSCustomObject]@{
        Status =
          [int]$_.Exception.Response.StatusCode
        Content = ""
      }
    }

    throw
  }
}

function Assert-Http {
  param(
    [string]$Name,
    [int]$Actual,
    [int]$Expected
  )

  if ($Actual -ne $Expected) {
    throw "$Name expected $Expected received $Actual"
  }

  Write-Host "$Name -> PASS" -ForegroundColor Green
}

function Poll-Object {
  param(
    [string]$Description,
    [string]$Path,
    [string]$Token,
    [scriptblock]$Ready,
    [int]$Attempts = 90
  )

  for ($i=1; $i -le $Attempts; $i++) {

    $R =
      Invoke-Api `
        "GET" `
        $Path `
        $Token `
        $null

    if ($R.Status -eq 200) {

      $Object =
        $R.Content |
        ConvertFrom-Json

      $Ok =
        & $Ready $Object

      if ($Ok) {

        Write-Host `
          "$Description -> PASS" `
          -ForegroundColor Green

        return $Object
      }
    }

    Start-Sleep -Seconds 1
  }

  throw "Timeout waiting for $Description"
}

Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "SUS SMART CARE - FINAL HACKATHON E2E" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$Admin =
  Get-Token `
    "demo.admin" `
    "admin123"

$Triage =
  Get-Token `
    "demo.triage" `
    "demo123"

$Doctor =
  Get-Token `
    "demo.doctor" `
    "demo123"

$Operator =
  Get-Token `
    "demo.operator" `
    "demo123"

$PatientToken =
  Get-Token `
    "demo.patient" `
    "demo123"

$DeviceToken =
  Get-DeviceToken

$Suffix =
  Get-Date -Format "yyyyMMddHHmmss"

# ------------------------------------------------------------
# FACILITY
# ------------------------------------------------------------

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/facilities" `
    $Admin `
    @{
      code = "DEMO-$Suffix"
      name = "SUS Smart Care Demo Unit"
      type = "HOSPITAL"
    }

Assert-Http `
  "Create facility" `
  $R.Status `
  201

$FacilityId =
  ($R.Content |
    ConvertFrom-Json).id

# ------------------------------------------------------------
# PATIENT
# ------------------------------------------------------------

$PatientName =
  "Hackathon Demo Patient $Suffix"

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/patients" `
    $Admin `
    @{
      fullName = $PatientName
      birthDate = "1984-06-10"
    }

Assert-Http `
  "Create patient" `
  $R.Status `
  201

$PatientId =
  ($R.Content |
    ConvertFrom-Json).id

# ------------------------------------------------------------
# PRE-VISIT
# ------------------------------------------------------------

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/pre-visits" `
    $Admin `
    @{
      patientId = $PatientId
      facilityId = $FacilityId
      channel = "MOBILE"
    }

Assert-Http `
  "Create pre-visit" `
  $R.Status `
  201

$VisitId =
  ($R.Content |
    ConvertFrom-Json).id

$R =
  Invoke-Api `
    "PUT" `
    "/api/v1/pre-visits/$VisitId/anamnesis" `
    $Admin `
    @{
      text =
        "Patient reports malaise and shortness of breath before arrival."
    }

Assert-Http `
  "Record pre-anamnesis" `
  $R.Status `
  200

# ------------------------------------------------------------
# CHECK-IN
# ------------------------------------------------------------

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/pre-visits/$VisitId/check-in" `
    $Admin `
    $null

Assert-Http `
  "Patient check-in" `
  $R.Status `
  200

# ------------------------------------------------------------
# TRIAGE CREATED BY EVENT
# ------------------------------------------------------------

$TriageEntity =
  Poll-Object `
    "Triage created from check-in event" `
    "/api/v1/triages/by-visit/$VisitId" `
    $Triage `
    {
      param($Value)
      return $null -ne $Value.id
    }

$TriageId =
  $TriageEntity.id

# Journey macro lifecycle.

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/triage/start" `
    $Triage `
    $null

Assert-Http `
  "Start journey triage" `
  $R.Status `
  200

# AI supports the professional.

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/triages/$TriageId/assessment/ai" `
    $Triage `
    $null

Assert-Http `
  "AI triage support" `
  $R.Status `
  200

# Professional decision.
# professionalId is intentionally NOT sent in the body.

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/triages/$TriageId/decision" `
    $Triage `
    @{
      priority = "HIGH"
    }

Assert-Http `
  "Professional clinical priority" `
  $R.Status `
  200

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/triage/complete" `
    $Triage `
    $null

Assert-Http `
  "Complete journey triage" `
  $R.Status `
  200

# ------------------------------------------------------------
# QUEUE CREATED BY CLINICAL PRIORITY EVENT
# ------------------------------------------------------------

$QueueEntry =
  Poll-Object `
    "Queue entry created from clinical priority" `
    "/api/v1/queue-entries/by-visit/$VisitId" `
    $Operator `
    {
      param($Value)
      return $null -ne $Value.id
    }

$QueueEntryId =
  $QueueEntry.id

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/queue" `
    $Operator `
    @{
      stage = "MEDICAL_CARE"
    }

Assert-Http `
  "Journey enters care queue" `
  $R.Status `
  200

# ------------------------------------------------------------
# PRESENCE
# ------------------------------------------------------------

$GatewayId =
  "BLE-DEMO-$Suffix"

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/presence/gateways" `
    $Admin `
    @{
      externalId = $GatewayId
      sourceType = "BLE"
      facilityId = $FacilityId
      zoneId = "TRIAGE-DEMO"
    }

Assert-Http `
  "Register BLE gateway" `
  $R.Status `
  201

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/presence/tracking-sessions" `
    $Admin `
    @{
      visitId = $VisitId
      patientId = $PatientId
      facilityId = $FacilityId
    }

Assert-Http `
  "Start presence tracking" `
  $R.Status `
  201

$Tracking =
  $R.Content |
  ConvertFrom-Json

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/presence/signals" `
    $DeviceToken `
    @{
      signalId =
        [Guid]::NewGuid().ToString()
      trackingToken =
        $Tracking.trackingToken
      sourceType = "BLE"
      state = "INSIDE"
      zoneId = "TRIAGE-DEMO"
      gatewayExternalId = $GatewayId
    }

Assert-Http `
  "BLE indoor presence" `
  $R.Status `
  202

# ------------------------------------------------------------
# TELEMETRY DEVICE
# ------------------------------------------------------------

$ExternalDeviceId =
  "MONITOR-DEMO-$Suffix"

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/devices" `
    $Admin `
    @{
      externalId = $ExternalDeviceId
      deviceType = "MULTIPARAMETER_MONITOR"
      manufacturer = "SUS Smart Care Simulator"
      model = "DEMO-01"
    }

Assert-Http `
  "Register medical device" `
  $R.Status `
  201

$MedicalDevice =
  $R.Content |
  ConvertFrom-Json

$DeviceId =
  $MedicalDevice.id

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/devices/$DeviceId/placements" `
    $Admin `
    @{
      placementType = "FACILITY"
      facilityId = $FacilityId
    }

Assert-Http `
  "Place device in facility" `
  $R.Status `
  201

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/telemetry/sessions" `
    $Triage `
    @{
      patientId = $PatientId
      visitId = $VisitId
      sourceContext = "TRIAGE"
      mode = "CONTINUOUS"
    }

Assert-Http `
  "Start continuous telemetry" `
  $R.Status `
  201

$TelemetrySession =
  $R.Content |
  ConvertFrom-Json

$TelemetrySessionId =
  $TelemetrySession.id

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/telemetry/sessions/$TelemetrySessionId/assignments" `
    $Triage `
    @{
      deviceId = $DeviceId
    }

Assert-Http `
  "Assign device to telemetry session" `
  $R.Status `
  201

# Ten normal continuous samples create one aggregate event.

for ($i=1; $i -le 10; $i++) {

  $R =
    Invoke-Api `
      "POST" `
      "/api/v1/telemetry/sessions/$TelemetrySessionId/observations" `
      $Triage `
      @{
        deviceId = $DeviceId
        type = "HEART_RATE"
        value = (78 + $i)
        unit = "bpm"
      }

  if ($R.Status -ne 202) {
    throw "Continuous telemetry sample $i failed"
  }
}

Write-Host "Continuous aggregation input -> PASS" -ForegroundColor Green

# This signal is intentionally anomalous.

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/telemetry/sessions/$TelemetrySessionId/observations" `
    $Triage `
    @{
      deviceId = $DeviceId
      type = "SPO2"
      value = 85
      unit = "%"
    }

Assert-Http `
  "Anomalous SPO2 signal" `
  $R.Status `
  202

# ------------------------------------------------------------
# CARE
# ------------------------------------------------------------

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/queue-entries/$QueueEntryId/call" `
    $Operator `
    $null

Assert-Http `
  "Queue calls patient" `
  $R.Status `
  200

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/call" `
    $Operator `
    $null

Assert-Http `
  "Journey records patient call" `
  $R.Status `
  200

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/service/start" `
    $Doctor `
    $null

Assert-Http `
  "Start medical service" `
  $R.Status `
  200

$R =
  Invoke-Api `
    "POST" `
    "/api/v1/visits/$VisitId/discharge" `
    $Doctor `
    @{
      note =
        "Patient stabilized and discharged after emergency evaluation."
    }

Assert-Http `
  "Clinical discharge" `
  $R.Status `
  200

# ------------------------------------------------------------
# CONSOLIDATED CQRS VIEW
# ------------------------------------------------------------

$View =
  Poll-Object `
    "Final consolidated Clinical View" `
    "/api/v1/clinical-view/visits/$VisitId" `
    $Doctor `
    {
      param($Value)

      if ($Value.patientName -ne $PatientName) {
        return $false
      }

      if ($Value.clinicalPriority -ne "HIGH") {
        return $false
      }

      if ($Value.journeyStatus -ne "COMPLETED") {
        return $false
      }

      if ($Value.journeyOutcome -ne "DISCHARGED") {
        return $false
      }

      if ($Value.presenceStatus -ne "ENTERED_FACILITY") {
        return $false
      }

      if ($null -eq $Value.continuousTelemetry) {
        return $false
      }

      if ($Value.continuousTelemetry.type -ne "HEART_RATE") {
        return $false
      }

      if ([long]$Value.continuousTelemetry.count -lt 10) {
        return $false
      }

      if ($null -eq $Value.latestAnomaly) {
        return $false
      }

      if ($Value.latestAnomaly.type -ne "SPO2") {
        return $false
      }

      return $true
    }

# Clinical data is not exposed to the patient account.

$R =
  Invoke-Api `
    "GET" `
    "/api/v1/clinical-view/visits/$VisitId" `
    $PatientToken `
    $null

Assert-Http `
  "Patient blocked from professional Clinical View" `
  $R.Status `
  403

# Public queue intentionally contains no patient identity.

$PublicQueue =
  Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/v1/queues/$FacilityId/public-view"

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host "FINAL E2E DEMO COMPLETO" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green

[ordered]@{
  facilityId =
    $FacilityId

  patientId =
    $PatientId

  visitId =
    $VisitId

  triageId =
    $TriageId

  queueEntryId =
    $QueueEntryId

  clinicalPriority =
    $View.clinicalPriority

  journeyStatus =
    $View.journeyStatus

  journeyStage =
    $View.journeyStage

  journeyOutcome =
    $View.journeyOutcome

  presenceStatus =
    $View.presenceStatus

  continuousTelemetry =
    $View.continuousTelemetry

  latestAnomaly =
    $View.latestAnomaly

  publicQueue =
    $PublicQueue
} |
ConvertTo-Json -Depth 12
