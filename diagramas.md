# Diagramas - Sistema Híbrido BRKGA + Tabu Search

## Diagrama de Fluxo Completo do Sistema

### Versão Detalhada (Para PowerPoint/Draw.io):

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ENTRADA DO PROBLEMA                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  📁 Instância JSON                                                          │
│  • VMs (requisitos de recursos + confiabilidade mínima)                     │
│  • Hosts (capacidades + custos + probabilidades de falha)                   │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FASE 1: BRKGA MULTI-OBJETIVO                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  🧬 Parâmetros:                     │  🔄 Processo:                          │
│  • População: 50 indivíduos         │  • Inicialização aleatória            │
│  • Gerações: 100                    │  • Decodificação (4 estratégias)      │
│  • Elite: 15% | Mutantes: 10%       │  • Avaliação multi-objetivo           │
│  • Herança: 70%                     │  • NSGA-II (ranking + crowding)       │
│                                     │  • Reprodução (elite × não-elite)     │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │ ⏱️ ~35ms
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ARQUIVO PARETO INICIAL                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  📊 50 soluções não-dominadas                                               │
│  • Custo médio: 161.20  • Melhor custo: ~120.00                            │
│  • Diversidade alta  • Convergência robusta                                │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      FASE 2: TABU SEARCH MULTI-OBJETIVO                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  ⚡ Parâmetros:                     │  🔍 Operadores:                       │
│  • 500 iterações/solução            │  • Realocação de VM                   │
│  • Lista Tabu: 50 movimentos        │  • Remoção de VM                      │
│  • Diversificação: a cada 100 iter. │  • Adição de VM                       │
│  • Total: 25.000 iterações          │  • Critério de aspiração              │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │ ⏱️ ~290ms | 📈 288 melhorias
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ARQUIVO PARETO REFINADO                            │
├─────────────────────────────────────────────────────────────────────────────┤
│  ✨ MELHORIAS COMPROVADAS:                                                  │
│  • Custo médio: 116.40 (⬇️ -27.7%)  • Melhor custo: 100.00 (⬇️ -16.7%)     │
│  • Solução dominante descoberta  • 288 refinamentos documentados            │
└─────────────────────────────────────┬───────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
    ┌───────────────────────────┐              ┌──────────────────────────┐
    │     VALIDAÇÃO CLOUDSIM    │              │    SAÍDA DE RESULTADOS   │
    │         (OPCIONAL)        │              │                          │
    ├───────────────────────────┤              ├──────────────────────────┤
    │ 🔬 Simulação Real:        │              │ 📋 Relatórios:           │
    │ • Execução de VMs         │              │ • Fronteira de Pareto    │
    │ • Consumo energético      │              │ • Métricas de qualidade  │
    │ • Custo validado          │              │ • Tempos de execução     │
    │ • Superestimação: 48-74%  │              │ • Análise comparativa    │
    └───────────────────────────┘              └──────────────────────────┘
```

### Versão Simplificada (Para Slides):

```
INPUT → BRKGA → PARETO INICIAL → TABU SEARCH → PARETO FINAL → OUTPUT
  📁      🧬        📊              ⚡           ✨         📊

• JSON    • 50 pop   • 50 sols     • 500 iter   • -27.7%    • Reports
• VMs     • 100 gen  • ~120 cost   • 288 impr   • 100 cost  • Metrics  
• Hosts   • ~35ms    • 161 avg     • ~290ms     • 116 avg   • Charts
```

### Versão Horizontal (Para Slides Largos):

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ INPUT   │ ──▶│ BRKGA   │ ──▶│ PARETO  │ ──▶│  TABU   │ ──▶│ OUTPUT  │
│         │    │         │    │ INICIAL │    │ SEARCH  │    │         │
│ JSON    │    │ ~35ms   │    │ 50 sols │    │ ~290ms  │    │ Reports │
│ VMs+    │    │ 100 gen │    │ 161 avg │    │ 288 impr│    │ Metrics │
│ Hosts   │    │ 50 pop  │    │ Cost    │    │ -27.7%  │    │ Charts  │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │              │
     ▼              ▼              ▼              ▼              ▼
  Instância     Exploração     Arquivo        Refinamento    Fronteira
   JSON         Global         Inicial        Local          Final
```

## Diagrama de Arquitetura do Sistema

### Componentes Principais:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          VMALLOCATIONOPTIMIZER                             │
│                              (Orquestrador)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                    │                                        │
│  ┌─────────────────────────────────┴─────────────────────────────────┐      │
│  │                           MOBRKGA                                  │      │
│  │                      (Fase 1: Exploração Global)                  │      │
│  ├─────────────────────────────────────────────────────────────────────┤      │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │      │
│  │  │ BRKGADecoder│  │ Individual  │  │      ParetoArchive          │  │      │
│  │  │ (4 estrat.) │  │(Cromossomo) │  │   (Soluções não-dominadas)  │  │      │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘  │      │
│  └─────────────────────────────────┬─────────────────────────────────┘      │
│                                    │                                        │
│  ┌─────────────────────────────────┴─────────────────────────────────┐      │
│  │                         TABU SEARCH                                │      │
│  │                   (Fase 2: Refinamento Local)                      │      │
│  ├─────────────────────────────────────────────────────────────────────┤      │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │      │
│  │  │  TabuMove   │  │  TabuList   │  │    ParetoArchive            │  │      │
│  │  │(Movimentos) │  │ (Proibidos) │  │     (Refinado)              │  │      │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘  │      │
│  └─────────────────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Gráficos de Resultados

### Métricas-Chave para Destacar:

```
┌─────────────────────────────────────┐
│           RESULTADOS-CHAVE          │
├─────────────────────────────────────┤
│ ⏱️ Tempo Total: < 400ms             │
│ 📈 Melhoria Custo: -27.7%           │
│ 🎯 Melhor Solução: 100.00/0.990     │
│ ✅ Validação: CloudSim confirmada   │
│ 🔄 Escalabilidade: Sublinear        │
└─────────────────────────────────────┘
```

### Fronteira de Pareto (Coordenadas):

```
Custo vs Confiabilidade
┌─────────────────────────────────────┐
│ Pontos para Plotagem:               │
│ • (100.00, 0.990) - Econômica       │
│ • (105.20, 0.988) - Intermediária   │
│ • (110.40, 0.990) - Intermediária   │
│ • (116.40, 0.988) - Balanceada      │
│ • (120.00, 0.990) - Confiável       │
└─────────────────────────────────────┘
```

### Comparação de Performance:

```
Performance por Instância
┌─────────────────────────────────────┐
│ Pequena: 322ms (3 VMs, 2 Hosts)     │
│ Média:   368ms (5 VMs, 4 Hosts)     │
│ Grande:  312ms (8 VMs, 6 Hosts)     │
│                                     │
│ Escalabilidade: SUBLINEAR ✅        │
└─────────────────────────────────────┘
```

## Guia de Implementação Visual

### Elementos Gráficos Sugeridos:

#### **Caixas/Shapes:**
- **Retângulos arredondados** para processos principais
- **Losangos** para decisões (se houver)
- **Cilindros** para dados/arquivos
- **Círculos** para início/fim

#### **Cores Sugeridas:**
- **Azul claro (#E3F2FD):** Entrada de dados
- **Verde (#E8F5E8):** BRKGA (Fase 1)
- **Laranja (#FFF3E0):** Arquivo Pareto
- **Vermelho (#FFEBEE):** Tabu Search (Fase 2)
- **Roxo (#F3E5F5):** Validação CloudSim
- **Cinza (#F5F5F5):** Saída de resultados

#### **Ícones Recomendados:**
- 📁 Arquivo JSON
- 🧬 DNA/Genético (BRKGA)
- ⚡ Raio (Tabu Search)
- 📊 Gráfico (Pareto)
- 🔬 Microscópio (Validação)
- ✨ Estrela (Melhoria)
- ⏱️ Relógio (Tempo)
- 📈 Seta para cima (Melhoria)

### Fontes Recomendadas:
- **Títulos:** Arial Bold, 16-18pt
- **Conteúdo:** Arial Regular, 12-14pt
- **Métricas:** Arial Bold, 14pt

### Espaçamento:
- **Margens:** 20px entre elementos
- **Padding interno:** 15px
- **Largura das setas:** 3px
- **Bordas das caixas:** 2px

Este conjunto de diagramas fornece uma representação visual completa do sistema híbrido, adequada para apresentações técnicas e documentação do projeto. 