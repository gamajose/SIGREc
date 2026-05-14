param(
    [Parameter(Mandatory = $true)]
    [string]$Arquivo,
    [string]$OutSql = "procedimentos-import.sql"
)

$linhas = Get-Content -LiteralPath $Arquivo -Encoding Default
$saida = New-Object System.Collections.Generic.List[string]
$saida.Add("SET search_path TO regulacao_tfd;")
$saida.Add("BEGIN;")

foreach ($linha in $linhas) {
    if ($linha.Length -lt 12) { continue }
    $codigo = $linha.Substring(0, 10).Trim()
    $descricao = $linha.Substring(10, [Math]::Min(250, $linha.Length - 10)).Trim()
    if (-not $codigo -or -not $descricao) { continue }
    $descricao = $descricao.Replace("'", "''")
    $saida.Add("INSERT INTO procedimentos (codigo, descricao, tipo, origem, ativo) VALUES ('$codigo', '$descricao', 'SIGTAP', 'tb_procedimento', true) ON CONFLICT (codigo) DO UPDATE SET descricao = EXCLUDED.descricao, origem = EXCLUDED.origem;")
}

$saida.Add("COMMIT;")
Set-Content -LiteralPath $OutSql -Value $saida -Encoding UTF8
Write-Host "Arquivo SQL gerado em $OutSql com $($saida.Count - 3) procedimentos."
