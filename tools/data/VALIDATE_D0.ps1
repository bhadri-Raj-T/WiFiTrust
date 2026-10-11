$ErrorActionPreference = "Stop"

$repo = "C:\Projects\WiFiTrustFusion"
$expectedBranch = "data-acquisition"

$requiredFiles = @(
    "docs\protocols\D0_COLLECTION_PROTOCOL.md",
    "docs\protocols\METADATA_DICTIONARY.md",
    "docs\protocols\NAMING_CONVENTION.md",
    "docs\protocols\SESSION_REJECTION_RULES.md",
    "docs\handovers\D0_HANDOVER_CHECKLIST.md",
    "data\manifests\SESSION_MANIFEST_TEMPLATE.csv"
)

$requiredDirs = @(
    "docs\protocols",
    "docs\handovers",
    "data\manifests",
    "data\accepted_sanitized",
    "data\quality_reports"
)

$expectedHeader = "session_id,state,collector_version,protocol_version,schema_version,device_id,network_code,location_code,position_code,orientation_code,environment_condition,scenario_id,ground_truth,start_time_utc,end_time_utc,operator_id,request_count,observation_count,missing_required_fields,stale_observation_count,duplicate_observation_count,rejection_code,notes"

$results = New-Object System.Collections.Generic.List[object]

function Add-Check {
    param(
        [string]$Check,
        [bool]$Passed,
        [string]$Details
    )
    $results.Add([PSCustomObject]@{
        Check = $Check
        Status = if ($Passed) { "PASS" } else { "FAIL" }
        Details = $Details
    })
}

if (-not (Test-Path $repo)) {
    throw "Repository not found: $repo"
}

Set-Location $repo

$branch = (git branch --show-current).Trim()
Add-Check "Git branch" ($branch -eq $expectedBranch) "Current: $branch; Expected: $expectedBranch"

foreach ($dir in $requiredDirs) {
    Add-Check "Directory: $dir" (Test-Path $dir -PathType Container) $(if (Test-Path $dir -PathType Container) { "Present" } else { "Missing" })
}

foreach ($file in $requiredFiles) {
    Add-Check "File: $file" (Test-Path $file -PathType Leaf) $(if (Test-Path $file -PathType Leaf) { "Present" } else { "Missing" })
}

$manifest = "data\manifests\SESSION_MANIFEST_TEMPLATE.csv"
if (Test-Path $manifest) {
    $header = (Get-Content $manifest -TotalCount 1).Trim([char]0xFEFF)
    Add-Check "Manifest header" ($header -eq $expectedHeader) $(if ($header -eq $expectedHeader) { "Exact expected schema" } else { "Header differs from expected schema" })
} else {
    Add-Check "Manifest header" $false "Manifest file missing"
}

# Basic privacy leak check for MAC-address-like identifiers in Git-managed docs/data.
$macRegex = '(?i)(?:[0-9A-F]{2}[:-]){5}[0-9A-F]{2}'
$privacyHits = @()

$textFiles = Get-ChildItem "docs","data" -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -in @(".md", ".csv", ".txt", ".json") }

foreach ($f in $textFiles) {
    $matches = Select-String -Path $f.FullName -Pattern $macRegex -AllMatches -ErrorAction SilentlyContinue
    if ($matches) {
        foreach ($m in $matches) {
            $privacyHits += "$($f.FullName):$($m.LineNumber)"
        }
    }
}

Add-Check "MAC-address privacy scan" ($privacyHits.Count -eq 0) $(if ($privacyHits.Count -eq 0) { "No MAC-address-like values found in docs/data" } else { "Potential values found at: " + ($privacyHits -join "; ") })

# Confirm there are no raw acquisition files in the sanitized/manifest area yet.
$unexpectedRawExt = @(".pcap", ".pcapng", ".cap")
$rawHits = Get-ChildItem "data" -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension.ToLowerInvariant() -in $unexpectedRawExt }

Add-Check "Raw capture file scan" ($rawHits.Count -eq 0) $(if ($rawHits.Count -eq 0) { "No packet-capture files in Git-managed data folders" } else { ($rawHits.FullName -join "; ") })

# Git working-tree check
$gitStatus = git status --porcelain
Add-Check "Git working tree" ([string]::IsNullOrWhiteSpace(($gitStatus -join ""))) $(if ([string]::IsNullOrWhiteSpace(($gitStatus -join ""))) { "Clean" } else { "Uncommitted changes exist" })

Write-Host "`n=== WiFiTrustFusion D0 VALIDATION ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$failures = @($results | Where-Object { $_.Status -eq "FAIL" })

Write-Host ""
if ($failures.Count -eq 0) {
    Write-Host "D0 AUTOMATED CHECK: PASS" -ForegroundColor Green
    Write-Host "This validates structure and basic integrity only. It does NOT freeze PROTOCOL_V1 or authorize D1 collection." -ForegroundColor Yellow
    exit 0
}
else {
    Write-Host "D0 AUTOMATED CHECK: FAIL ($($failures.Count) check(s))" -ForegroundColor Red
    Write-Host "Fix the failed checks before moving forward." -ForegroundColor Yellow
    exit 1
}
