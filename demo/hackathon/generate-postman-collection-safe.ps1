param(
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "generate-postman-collection.ps1"

if (-not (Test-Path $sourcePath)) {
    throw "Fonte da colecao nao encontrada: $sourcePath"
}

if (-not $OutputPath) {
    $OutputPath = Join-Path $PSScriptRoot "SUS-Smart-Care-Hackathon-Demo-FINAL.postman_collection.json"
}


function Get-LeafItems {

    param($Items)

    $result = @()

    foreach ($item in @($Items)) {

        if ($null -ne $item.request) {

            $result += $item
        }
        elseif ($null -ne $item.item) {

            foreach ($child in @(Get-LeafItems $item.item)) {
                $result += $child
            }
        }
    }

    return @($result)
}


function New-SequenceFolder {

    param(
        [int]$Number,
        [array]$Requests
    )

    $first = $Requests[0].name
    $last  = $Requests[$Requests.Count - 1].name

    $name = "SEQ " + $Number.ToString("00") + " - " + $first

    if ($Requests.Count -gt 1) {
        $name += " ... " + $last
    }

    return [pscustomobject]@{
        name = $name
        item = @($Requests)
    }
}


function Normalize-RunnerStructure {

    param($Collection)

    foreach ($journey in @($Collection.item)) {

        $before = @(
            Get-LeafItems $journey.item |
            ForEach-Object { $_.name }
        )

        $newItems = @()
        $buffer = @()
        $sequence = 1

        foreach ($child in @($journey.item)) {

            if ($null -ne $child.request) {

                $buffer += $child
                continue
            }


            if ($buffer.Count -gt 0) {

                $newItems += New-SequenceFolder `
                    -Number $sequence `
                    -Requests $buffer

                $sequence++
                $buffer = @()
            }


            if ($null -eq $child.item) {
                throw "Item invalido em $($journey.name): $($child.name)"
            }


            $newItems += [pscustomobject]@{
                name = $child.name
                item = @(Get-LeafItems $child.item)
            }
        }


        if ($buffer.Count -gt 0) {

            $newItems += New-SequenceFolder `
                -Number $sequence `
                -Requests $buffer
        }


        $journey.item = @($newItems)


        $after = @(
            Get-LeafItems $journey.item |
            ForEach-Object { $_.name }
        )


        if ($before.Count -ne $after.Count) {
            throw "Quantidade mudou em $($journey.name)"
        }


        for ($i = 0; $i -lt $before.Count; $i++) {

            if ($before[$i] -ne $after[$i]) {

                throw "Ordem mudou em $($journey.name), indice $i"
            }
        }
    }

    return $Collection
}


function Assert-RunnerStructure {

    param($Collection)

    foreach ($journey in @($Collection.item)) {

        foreach ($folder in @($journey.item)) {

            if ($null -ne $folder.request) {
                throw "Request direto encontrado em $($journey.name)"
            }

            if ($null -eq $folder.item) {
                throw "Pasta executavel invalida: $($folder.name)"
            }

            foreach ($request in @($folder.item)) {

                if ($null -eq $request.request) {
                    throw "Terceiro nivel encontrado em $($folder.name)"
                }
            }
        }
    }
}


# ============================================================
# GERADOR ORIGINAL
# ============================================================

$source = [System.IO.File]::ReadAllText(
    (Resolve-Path $sourcePath).Path,
    [System.Text.UTF8Encoding]::new($false)
)

$source = $source.Replace(
    '[hashtable]$Form=$null',
    '[System.Collections.IDictionary]$Form=$null'
)


$temp = Join-Path `
    $env:TEMP `
    ("ssc-postman-" + [guid]::NewGuid().ToString("N") + ".ps1")


try {

    [System.IO.File]::WriteAllText(
        $temp,
        $source,
        [System.Text.UTF8Encoding]::new($false)
    )


    # Validar sintaxe do gerador original normalizado.

    $tokens = $null
    $errors = $null

    [System.Management.Automation.Language.Parser]::ParseFile(
        $temp,
        [ref]$tokens,
        [ref]$errors
    ) | Out-Null


    if ($errors.Count -gt 0) {

        $errors | ForEach-Object {
            Write-Host ("[ERRO] linha " + $_.Extent.StartLineNumber + ": " + $_.Message) -ForegroundColor Red
        }

        throw "Gerador possui erro de sintaxe."
    }


    # Gerar JSON original.

    & $temp -OutputPath $OutputPath


    if (-not (Test-Path $OutputPath)) {
        throw "Colecao nao foi criada."
    }


    # Carregar JSON.

    $collection = [System.IO.File]::ReadAllText(
        (Resolve-Path $OutputPath).Path,
        [System.Text.UTF8Encoding]::new($false)
    ) | ConvertFrom-Json


    # Validar as seis jornadas principais.

    $expected = @(
        "00 - AUTENTICACAO E PREPARACAO",
        "01 - LUCAS CRIANCA - JORNADA COMPLETA",
        "02 - LUCAS FUTURO - REPRESENTACAO PARA SELF",
        "03 - SEM SMARTPHONE - DONA ROSA",
        "04 - AMBULANCIA + MQTT + REDIS",
        "05 - OBSERVABILIDADE E SEGURANCA"
    )


    $actual = @(
        $collection.item |
        ForEach-Object { $_.name }
    )


    foreach ($name in $expected) {

        if ($actual -notcontains $name) {
            throw "Jornada obrigatoria ausente: $name"
        }
    }


    # Contagem original.

    $beforeCount = @(Get-LeafItems $collection.item).Count


    # Transformacao FOLDERED deterministica.

    $collection = Normalize-RunnerStructure $collection

    Assert-RunnerStructure $collection


    # Contagem posterior.

    $afterCount = @(Get-LeafItems $collection.item).Count


    if ($beforeCount -ne $afterCount) {

        throw "Quantidade de requests foi alterada."
    }


    # Gravar JSON definitivo.

    $json = $collection | ConvertTo-Json -Depth 100

    [System.IO.File]::WriteAllText(
        (Resolve-Path $OutputPath).Path,
        $json,
        [System.Text.UTF8Encoding]::new($false)
    )


    # Ler novamente e validar o arquivo efetivamente gravado.

    $verify = [System.IO.File]::ReadAllText(
        (Resolve-Path $OutputPath).Path,
        [System.Text.UTF8Encoding]::new($false)
    ) | ConvertFrom-Json


    Assert-RunnerStructure $verify

    $finalCount = @(Get-LeafItems $verify.item).Count


    if ($finalCount -ne $beforeCount) {
        throw "Contagem final inconsistente."
    }


    Write-Host ""
    Write-Host "OK - colecao pronta para importar no Postman:" -ForegroundColor Green
    Write-Host "  $OutputPath"
    Write-Host "Pastas principais: $($verify.item.Count)"
    Write-Host "Requests totais: $finalCount"
    Write-Host "Estrutura: Jornada -> Pasta executavel -> Requests" -ForegroundColor Green
}
finally {

    Remove-Item $temp -ErrorAction SilentlyContinue
}