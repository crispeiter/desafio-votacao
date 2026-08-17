// Teste de carga do endpoint de voto (tarefa bonus 2 da especificacao).
//
// Como rodar:
//
//   1. Suba o banco:
//        docker compose up -d
//
//   2. Suba a aplicacao com o sorteio de autorizacao desligado:
//        ./mvnw spring-boot:run -Dspring-boot.run.arguments=--votacao.cpf.modo=SEMPRE_APTO
//      Sem SEMPRE_APTO o provedor sorteia UNABLE_TO_VOTE e cerca de metade dos votos vira 404.
//      A medicao passaria a ser do sorteio, e nao da aplicacao.
//
//   3. Crie a pauta e a sessao usadas no teste, com duracao maior que a do script:
//        curl -X POST localhost:8080/api/v1/pautas \
//             -H 'Content-Type: application/json' \
//             -d '{"titulo": "Pauta de teste de carga"}'
//        curl -X POST localhost:8080/api/v1/pautas/1/sessao \
//             -H 'Content-Type: application/json' \
//             -d '{"duracaoEmSegundos": 86400}'
//      Sessao encerrada no meio da rampa transformaria todos os votos restantes em 409.
//
//   4. Rode o teste com o id da pauta criada acima:
//        k6 run -e PAUTA_ID=1 performance/carga-votos.js
//
// Variaveis de ambiente aceitas:
//   PAUTA_ID  obrigatoria, id da pauta com sessao aberta
//   BASE_URL  opcional, default http://localhost:8080
//   PAUSA     opcional, think time em segundos entre votos do mesmo VU, default 0.2

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PAUTA_ID = __ENV.PAUTA_ID;
const PAUSA = Number(__ENV.PAUSA || 0.2);

const votosRegistrados = new Counter('votos_registrados');
const votosRecusados = new Counter('votos_recusados');

export const options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '30s', target: 200 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<300'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    if (!PAUTA_ID) {
        throw new Error('Informe a pauta com -e PAUTA_ID=<id>. Veja as instrucoes no topo do script.');
    }
    // Falha antes da rampa, e nao depois de 2 minutos de 409: sessao fechada invalida a corrida
    // inteira, entao o barato e descobrir agora.
    const sessao = http.get(`${BASE_URL}/api/v1/pautas/${PAUTA_ID}/sessao`);
    if (sessao.status !== 200) {
        throw new Error(`A pauta ${PAUTA_ID} nao tem sessao de votacao. Status recebido: ${sessao.status}`);
    }
    if (sessao.json('status') !== 'ABERTA') {
        throw new Error(`A sessao da pauta ${PAUTA_ID} esta encerrada. Abra uma nova sessao antes do teste.`);
    }
}

export default function () {
    const corpo = JSON.stringify({ cpf: proximoCpf(), opcao: Math.random() < 0.5 ? 'SIM' : 'NAO' });
    const resposta = http.post(`${BASE_URL}/api/v1/pautas/${PAUTA_ID}/votos`, corpo, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'registrar-voto' },
    });

    const registrado = check(resposta, { 'voto registrado (201)': (r) => r.status === 201 });
    if (registrado) {
        votosRegistrados.add(1);
    } else {
        votosRecusados.add(1);
    }

    sleep(PAUSA);
}

export function teardown() {
    const resultado = http.get(`${BASE_URL}/api/v1/pautas/${PAUTA_ID}/resultado`);
    if (resultado.status === 200) {
        console.log(`Apuracao final: ${resultado.body}`);
    }
}

// Os nove primeiros digitos saem de VU e iteracao, e nao de sorteio puro: dois VUs sorteando o
// mesmo numero produziriam 409 de voto duplicado e sujariam justamente a taxa de erro que o
// teste mede. O verificador e calculado pela mesma regra do provedor, entao o CPF e valido.
function proximoCpf() {
    const base = String(__VU).padStart(3, '0') + String(__ITER).padStart(6, '0');
    const primeiro = verificador(base, 9);
    const segundo = verificador(base + primeiro, 10);
    return base + primeiro + segundo;
}

function verificador(digitos, quantidade) {
    let soma = 0;
    let peso = quantidade + 1;
    for (let i = 0; i < quantidade; i++) {
        soma += Number(digitos[i]) * peso--;
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
}
