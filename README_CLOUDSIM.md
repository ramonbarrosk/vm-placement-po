# CloudSim Integration - VM Placement Optimization

Este documento descreve a integração do **CloudSim Plus** ao sistema de otimização de alocação de VMs, permitindo simulações reais de datacenter baseadas nas soluções otimizadas.

## 🚀 Visão Geral

A integração CloudSim adiciona capacidades de **simulação real** ao projeto, permitindo:

- **Validação prática** das soluções de otimização
- **Análise de performance** em ambiente simulado
- **Comparação** entre custos teóricos e simulados
- **Métricas detalhadas** de utilização de recursos
- **Simulação de workloads** realistas

## 📋 Componentes Principais

### 1. CloudSimSimulator
Classe principal que integra o CloudSim Plus:
- Converte modelos do projeto para entidades CloudSim
- Executa simulações completas de datacenter
- Coleta estatísticas detalhadas de performance

### 2. SimulationIntegrator
Orquestra o fluxo completo otimização → simulação:
- Executa otimização BRKGA + Busca Tabu
- Seleciona melhores soluções para simulação
- Compara resultados teóricos vs simulados

### 3. Exemplos Práticos
Demonstrações de uso em `CloudSimExample.java`:
- Simulação básica
- Comparação de soluções
- Análise de performance

## 🛠️ Como Usar

### Exemplo Básico

```java
// 1. Criar instância do problema
ProblemInstance instance = InstanceReader.createSampleInstance();

// 2. Executar otimização
OptimizationParameters optParams = OptimizationParameters.createQuick();
VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, optParams);
VMAllocationOptimizer.OptimizationResult optResult = optimizer.optimize();

// 3. Configurar simulação
CloudSimSimulator.SimulationParameters simParams = 
    CloudSimSimulator.SimulationParameters.createDefault();

// 4. Simular melhor solução
AllocationSolution bestSolution = optResult.getBestCostSolution();
CloudSimSimulator simulator = new CloudSimSimulator(instance, simParams);
CloudSimSimulator.SimulationResults simResults = simulator.runSimulation(bestSolution);

// 5. Analisar resultados
System.out.println(simResults.generateReport());
```

### Exemplo Integrado

```java
// Execução completa: otimização + simulação
ProblemInstance instance = InstanceReader.createSampleInstance();
SimulationIntegrator integrator = SimulationIntegrator.createQuick(instance);
SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();

// Relatório completo
System.out.println(results.generateCompleteReport());

// Análise de diferenças de custo
Map<AllocationSolution, SimulationIntegrator.CostDifference> costDiffs = 
    results.getCostDifferences();
```

## 📊 Métricas Coletadas

### Estatísticas de VMs
- **Utilização de CPU, RAM e Bandwidth**
- **Tempo de execução**
- **Host de alocação**

### Estatísticas de Hosts
- **Utilização de recursos**
- **Consumo de energia**
- **Número de VMs hospedadas**

### Estatísticas de Cloudlets
- **Tempo de execução e espera**
- **Status de conclusão**
- **VM de execução**

### Métricas Globais
- **Custo total da simulação**
- **Consumo total de energia**
- **Tempo de execução da simulação**

## 🔧 Configuração de Parâmetros

### Parâmetros de Simulação

```java
SimulationParameters params = new SimulationParameters();
params.setCostPerSecond(0.01);        // Custo por segundo de CPU
params.setCostPerMem(0.05);           // Custo por MB de RAM
params.setCostPerStorage(0.001);      // Custo por MB de storage
params.setCloudletsPerVm(5);          // Cloudlets por VM
params.setCloudletLength(10000);      // Tamanho dos cloudlets (MI)
```

### Parâmetros de Otimização

```java
OptimizationParameters optParams = new OptimizationParameters.Builder()
    .brkgaPopulationSize(50)
    .brkgaMaxGenerations(100)
    .archiveSize(50)
    .decodingStrategy(BRKGADecoder.DecodingStrategy.BALANCED)
    .randomSeed(42)
    .build();
```

## 📈 Análise de Resultados

### Comparação Otimização vs Simulação

A integração permite comparar:
- **Custos teóricos** (calculados pela otimização)
- **Custos reais** (medidos na simulação)
- **Diferenças percentuais** entre ambos

### Validação de Soluções

- **Viabilidade prática** das alocações
- **Performance real** dos algoritmos
- **Eficiência energética** das soluções

## 🎯 Casos de Uso

### 1. Validação de Algoritmos
Verificar se as soluções otimizadas funcionam na prática:
```java
// Comparar múltiplas estratégias
List<AllocationSolution> solutions = generateMultipleSolutions(instance);
SimulationIntegrator.ComparisonResults comparison = 
    integrator.compareSolutions(solutions);
```

### 2. Análise de Custos
Avaliar precisão das estimativas de custo:
```java
Map<AllocationSolution, CostDifference> differences = 
    results.getCostDifferences();
double avgDifference = differences.values().stream()
    .mapToDouble(diff -> Math.abs(diff.percentageDifference))
    .average().orElse(0.0);
```

### 3. Otimização de Parâmetros
Ajustar configurações baseado em resultados simulados:
```java
// Testar diferentes configurações de cloudlets
for (int cloudlets = 3; cloudlets <= 10; cloudlets += 2) {
    simParams.setCloudletsPerVm(cloudlets);
    SimulationResults results = simulator.runSimulation(solution);
    // Analisar impacto na performance
}
```

## 🚀 Executando os Exemplos

### Aplicação Principal
```bash
mvn compile exec:java -Dexec.mainClass="com.ramonyago.cloudsim.App"
```

### Exemplo CloudSim
```bash
mvn compile exec:java -Dexec.mainClass="com.ramonyago.cloudsim.examples.CloudSimExample"
```

## 📋 Dependências

O projeto já inclui todas as dependências necessárias no `pom.xml`:

- **CloudSim Plus 8.5.5** - Framework de simulação
- **Apache Commons Math 3.6.1** - Operações matemáticas
- **Jackson 2.15.2** - Parsing JSON
- **SLF4J 2.0.9** - Logging

## 🔍 Estrutura de Arquivos

```
src/main/java/com/ramonyago/cloudsim/
├── simulation/
│   ├── CloudSimSimulator.java      # Simulador principal
│   └── SimulationIntegrator.java   # Integração otimização+simulação
├── examples/
│   └── CloudSimExample.java       # Exemplos práticos
├── model/                          # Modelos existentes (VM, Host, etc.)
├── algorithm/                      # Algoritmos de otimização
└── App.java                       # Aplicação principal (atualizada)
```

## 🎯 Próximos Passos

1. **Executar exemplos** para familiarizar-se com a API
2. **Experimentar parâmetros** diferentes de simulação
3. **Criar instâncias customizadas** para seus casos de uso
4. **Analisar métricas** para validar soluções
5. **Comparar estratégias** de otimização via simulação

## 📚 Recursos Adicionais

- [CloudSim Plus Documentation](https://cloudsimplus.org/)
- [CloudSim Plus Examples](https://github.com/cloudsimplus/cloudsimplus-examples)
- [Artigos sobre VM Placement](https://scholar.google.com/scholar?q=vm+placement+optimization)

---

**Nota**: A integração CloudSim permite validação prática das soluções de otimização, fornecendo insights valiosos sobre a performance real dos algoritmos em ambientes simulados de datacenter. 