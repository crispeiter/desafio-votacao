# Resultado do teste de carga

Execução de 17/08/2026, em máquina de desenvolvimento, com a aplicação, o PostgreSQL em
contêiner e o gerador de carga disputando os mesmos recursos. Saída bruta do `k6 run`:

```
INFO[0150] Apuracao final: {"pautaId":1,"titulo":"teste1","statusSessao":"ABERTA","totalVotos":84459,"votosSim":42352,"votosNao":42107,"resultado":"APROVADA"}  source=console
  █ THRESHOLDS
    http_req_duration
    ✓ 'p(95)<300' p(95)=72.41ms
    http_req_failed
    ✓ 'rate<0.01' rate=0.00%
  █ TOTAL RESULTS
    checks_total.......: 84459   561.868662/s
    checks_succeeded...: 100.00% 84459 out of 84459
    checks_failed......: 0.00%   0 out of 84459
    ✓ voto registrado (201)
    CUSTOM
    votos_registrados..............: 84459 561.868662/s
    HTTP
    http_req_duration..............: avg=29.8ms  min=6.99ms   med=21.99ms  max=440.08ms p(90)=53.24ms  p(95)=72.41ms
      { expected_response:true }...: avg=29.8ms  min=6.99ms   med=21.99ms  max=440.08ms p(90)=53.24ms  p(95)=72.41ms
    http_req_failed................: 0.00% 0 out of 84461
    http_reqs......................: 84461 561.881967/s
    EXECUTION
    iteration_duration.............: avg=231.1ms min=207.18ms med=223.29ms max=647.48ms p(90)=255.03ms p(95)=274.38ms
    iterations.....................: 84459 561.868662/s
    vus............................: 2     min=2          max=200
    vus_max........................: 200   min=200        max=200
    NETWORK
    data_received..................: 23 MB 152 kB/s
    data_sent......................: 15 MB 101 kB/s
running (2m30.3s), 000/200 VUs, 84459 complete and 0 interrupted iterations
```
