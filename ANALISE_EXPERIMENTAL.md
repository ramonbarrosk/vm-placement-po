# Análise do Ambiente Experimental - Alocação de VMs com BRKGA e Tabu Search

## 1. Ambiente Experimental

### 1.1 Configuração do Sistema
- **Linguagem:** Java 17
- **Framework:** CloudSim Plus 8.5.5
- **Bibliotecas:** Apache Commons Math 3.6.1, Jackson 2.15.2
- **Arquitetura:** Sistema híbrido multiobjetivo

### 1.2 Estrutura do Problema
O sistema resolve o problema de **alocação de máquinas virtuais (VMs) em hosts físicos** com múltiplos objetivos:

#### Variáveis de Decisão:
- `x_vh`: Variável binária indicando se a VM `v` está alocada no host `h`
- `y_h`: Variável binária indicando se o host `h` está ativo

#### Restrições:
- **Capacidade dos hosts:** Limitações de CPU, RAM, armazenamento e rede
- **Ativação de hosts:** VMs só podem ser alocadas em hosts ativos
- **Confiabilidade mínima:** Cada VM tem um requisito mínimo de confiabilidade
- **Unicidade:** Cada VM pode estar em no máximo um host

### 1.3 Formato das Instâncias
```json
{
  "metadata": {
    "description": "Descrição da instância",
    "difficulty": "easy|medium|hard"
  },
  "vms": [
    {
      "id": 1,
      "minReliability": 0.95,
      "priority": 1.0,
      "resourceDemands": {
        "cpu": 2.0, "ram": 4.0, "storage": 20.0, "network": 100.0
      }
    }
  ],
  "hosts": [
    {
      "id": 1,
      "activationCost": 100.0,
      "failureProbability": 0.02,
      "energyConsumption": 150.0,
      "resourceCapacities": {
        "cpu": 8.0, "ram": 16.0, "storage": 100.0, "network": 1000.0
      }
    }
  ]
}
```

## 2. Parâmetros dos Algoritmos

### 2.1 BRKGA (Biased Random-Key Genetic Algorithm) Multiobjetivo

#### Parâmetros Principais:
- **População (`brkgaPopulationSize`):** 100 (padrão), 50 (rápido), 200 (intensivo)
- **Gerações (`brkgaMaxGenerations`):** 500 (padrão), 100 (rápido), 2000 (intensivo)
- **Taxa de Elite (`brkgaEliteRatio`):** 0.15 (15% da população)
- **Taxa de Mutantes (`brkgaMutantRatio`):** 0.10 (10% da população)
- **Probabilidade de Herança (`brkgaInheritanceProbability`):** 0.7 (70%)

#### Estratégias de Decodificação:
- **GREEDY_COST:** Prioriza hosts com menor custo de ativação
- **GREEDY_RELIABILITY:** Prioriza hosts mais confiáveis
- **BALANCED:** Balanceamento entre custo e confiabilidade
- **FIRST_FIT:** Primeira alocação viável encontrada

#### Seleção Multiobjetivo:
- **NSGA-II:** Classificação por frentes de não-dominância + crowding distance
- **Arquivo Pareto:** Mantém soluções não-dominadas (tamanho: 100-200)

### 2.2 Tabu Search Multiobjetivo

#### Parâmetros Principais:
- **Iterações Máximas (`tabuMaxIterations`):** 500 (padrão), 100 (rápido), 1000 (intensivo)
- **Tamanho da Lista Tabu (`tabuListSize`):** 50
- **Frequência de Diversificação (`diversificationFrequency`):** A cada 100 iterações
- **Pesos dos Objetivos:**
  - `costWeight`: Peso para minimização de custo
  - `reliabilityWeight`: Peso para maximização de confiabilidade

#### Operadores de Vizinhança:
- **Realocação de VM:** Mover VM de um host para outro
- **Remoção de VM:** Desalocar VM (se não obrigatória)
- **Adição de VM:** Alocar VM não alocada em host disponível

#### Critérios de Aspiração:
- Soluções que melhoram o arquivo Pareto mesmo sendo tabu
- Soluções que dominam a melhor solução conhecida

## 3. Métricas de Avaliação

### 3.1 Objetivos Principais

#### Objetivo 1: Minimização do Custo Total
```java
totalCost = Σ(y_h × activationCost_h) para todos os hosts h
```
- **Descrição:** Soma dos custos de ativação de todos os hosts ativos
- **Unidade:** Unidades monetárias
- **Meta:** Minimizar

#### Objetivo 2: Maximização da Confiabilidade Média
```java
totalReliability = (1/|VMs|) × Σ(1 - failureProbability_h) para VM v em host h
```
- **Descrição:** Média da confiabilidade de todas as VMs alocadas
- **Faixa:** [0, 1]
- **Meta:** Maximizar

### 3.2 Objetivos Secundários

#### Consumo de Energia
```java
energyConsumption = Σ(y_h × energyConsumption_h) para todos os hosts h
```
- **Descrição:** Soma do consumo energético de todos os hosts ativos
- **Unidade:** Watts
- **Meta:** Minimizar

#### Balanceamento de Carga
```java
loadBalance = sqrt(variance(cpuUtilization_h)) para hosts ativos h
```
- **Descrição:** Desvio padrão da utilização de CPU entre hosts ativos
- **Meta:** Minimizar (melhor balanceamento)

### 3.3 Métricas de Qualidade da Solução

#### Viabilidade
- **Violação de Capacidade:** Soma das violações de recursos em todos os hosts
- **Violação de Confiabilidade:** Número de VMs com confiabilidade abaixo do mínimo
- **Solução Viável:** `constraintViolation == 0`

#### Dominância de Pareto
```java
// Solução A domina B se:
// A.cost ≤ B.cost AND A.reliability ≥ B.reliability
// E pelo menos uma desigualdade é estrita
```

### 3.4 Métricas de Performance

#### Tempo de Execução
- **BRKGA:** Tempo total de execução do algoritmo genético
- **Tabu Search:** Tempo de refinamento das soluções
- **Total:** Tempo completo da otimização híbrida

#### Qualidade do Arquivo Pareto
- **Tamanho do Arquivo:** Número de soluções não-dominadas
- **Diversidade:** Distribuição das soluções no espaço objetivo
- **Convergência:** Evolução do tamanho do arquivo ao longo das gerações

## 4. Análise Comparativa dos Algoritmos

### 4.1 BRKGA vs Tabu Search

| Aspecto | BRKGA | Tabu Search |
|---------|-------|-------------|
| **Tipo** | Metaheurística populacional | Metaheurística de busca local |
| **Exploração** | Global (população diversa) | Local (vizinhança) |
| **Multiobjetivo** | Nativo (NSGA-II) | Adaptado (pesos/arquivo) |
| **Complexidade** | O(pop × gen × decode) | O(iter × neighborhood) |
| **Memória** | Alta (população) | Baixa (solução atual + tabu) |
| **Convergência** | Lenta mas robusta | Rápida mas local |

### 4.2 Estratégias de Decodificação (BRKGA)

| Estratégia | Vantagens | Desvantagens | Uso Recomendado |
|------------|-----------|--------------|-----------------|
| **GREEDY_COST** | Soluções de baixo custo | Pode sacrificar confiabilidade | Orçamento limitado |
| **GREEDY_RELIABILITY** | Alta confiabilidade | Custos elevados | Aplicações críticas |
| **BALANCED** | Compromisso equilibrado | Pode não otimizar extremos | Uso geral |
| **FIRST_FIT** | Rápida execução | Qualidade inferior | Testes rápidos |

### 4.3 Configurações Recomendadas

#### Para Instâncias Pequenas (≤ 20 VMs, ≤ 10 hosts):
```java
OptimizationParameters.createQuick()
// BRKGA: pop=50, gen=100
// Tabu: iter=100
// Tempo esperado: < 10 segundos
```

#### Para Instâncias Médias (20-100 VMs, 10-50 hosts):
```java
OptimizationParameters.createDefault()
// BRKGA: pop=100, gen=500
// Tabu: iter=500
// Tempo esperado: 1-5 minutos
```

#### Para Instâncias Grandes (> 100 VMs, > 50 hosts):
```java
OptimizationParameters.createIntensive()
// BRKGA: pop=200, gen=2000
// Tabu: iter=1000
// Tempo esperado: 10-60 minutos
```

### 4.4 Análise de Trade-offs

#### Custo vs Confiabilidade
- **Correlação:** Geralmente negativa (hosts mais confiáveis são mais caros)
- **Frente de Pareto:** Conjunto de soluções que representam diferentes compromissos
- **Decisão:** Depende dos requisitos específicos da aplicação

#### Tempo vs Qualidade
- **BRKGA:** Mais gerações = melhor qualidade, mas maior tempo
- **Tabu Search:** Mais iterações = refinamento local, retorno decrescente
- **Híbrido:** BRKGA para exploração + Tabu para refinamento

#### Diversidade vs Convergência
- **Alta diversidade:** Arquivo Pareto amplo, mas convergência lenta
- **Convergência rápida:** Soluções concentradas, pode perder diversidade
- **Balanceamento:** Ajuste de parâmetros de elite/mutante no BRKGA

## 5. Relatórios e Análise de Resultados

### 5.1 Formato de Saída
- **Texto:** Relatório completo com estatísticas detalhadas
- **CSV:** Dados estruturados para análise estatística
- **JSON:** Soluções completas para visualização

### 5.2 Métricas de Avaliação de Performance
- **Soluções por segundo:** Eficiência computacional
- **Gerações por segundo (BRKGA):** Taxa de evolução
- **Taxa de melhoria (Tabu):** Efetividade do refinamento
- **Qualidade do arquivo final:** Diversidade e não-dominância

Esta análise fornece uma base completa para entender o ambiente experimental, configurar adequadamente os algoritmos e interpretar os resultados obtidos. 