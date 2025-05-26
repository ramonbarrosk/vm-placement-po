# Alocação de VMs com BRKGA, Tabu Search

## Autores

- Ramon Barros  
- Afrânio Yago

## Sobre

Este repositório foi desenvolvido como parte da disciplina de **Pesquisa Operacional** no Instituto de Computação da **UFAL**.

O objetivo do projeto é a implementação de técnicas híbridas para resolver problemas de **alocação de máquinas virtuais (VMs) em hosts físicos** com múltiplos objetivos. O sistema utiliza uma abordagem híbrida composta por:

- **BRKGA (Biased Random-Key Genetic Algorithm)** multiobjetivo
- **Busca Tabu** multiobjetivo (planejado)

A meta principal é encontrar um conjunto de **soluções Pareto-ótimas** que otimizem simultaneamente:
- **Minimização do custo** total dos hosts ativos
- **Maximização da confiabilidade** do sistema

## Características do Sistema

### Objetivos Múltiplos
- **Objetivo 1:** Minimizar custo total de ativação dos hosts
- **Objetivo 2:** Maximizar confiabilidade média das VMs
- **Objetivo adicional:** Minimizar consumo de energia
- **Métrica de qualidade:** Balanceamento de carga entre hosts

### Restrições
- **Capacidade dos hosts:** Limitações de CPU, RAM, armazenamento e rede
- **Ativação de hosts:** VMs só podem ser alocadas em hosts ativos
- **Confiabilidade mínima:** Cada VM tem um requisito mínimo de confiabilidade

### Algoritmos Implementados
- **BRKGA Multiobjetivo** com seleção baseada em NSGA-II
- **Estruturas de Pareto** para manter soluções não-dominadas
- **Decodificadores configuráveis** com diferentes estratégias
- **Arquivo adaptativo** de soluções com controle de diversidade

## Estrutura do Projeto

```
src/main/java/com/ramonyago/cloudsim/
├── model/                          # Estruturas de dados
│   ├── VM.java                     # Representação de máquinas virtuais
│   ├── Host.java                   # Representação de hosts físicos
│   ├── AllocationSolution.java     # Solução de alocação completa
│   ├── ProblemInstance.java        # Instância do problema
│   └── ResourceType.java           # Tipos de recursos (CPU, RAM, etc.)
├── algorithm/
│   └── brkga/                      # Algoritmo BRKGA
│       ├── MOBRKGA.java            # Algoritmo principal
│       ├── Individual.java         # Indivíduo com chaves aleatórias
│       ├── BRKGADecoder.java       # Decodificador de soluções
│       └── BRKGAParameters.java    # Parâmetros do algoritmo
├── io/                             # Entrada/saída de dados
│   └── InstanceReader.java         # Leitor de instâncias JSON
├── util/                           # Utilitários
│   └── ParetoArchive.java          # Arquivo de soluções Pareto-ótimas
├── VMAllocationOptimizer.java      # Orquestrador principal
├── OptimizationParameters.java     # Parâmetros globais
├── ExecutionReport.java            # Relatórios de execução
└── App.java                        # Aplicação principal
```

## Compilação e Execução

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6+

### Compilação
```bash
mvn clean compile
```

### Execução dos Testes
```bash
mvn test
```

### Execução da Aplicação Principal
```bash
# Execução básica com instância de exemplo
mvn exec:java -Dexec.mainClass="com.ramonyago.cloudsim.App"

# Execução com arquivo de instância personalizado
mvn exec:java -Dexec.mainClass="com.ramonyago.cloudsim.App" -Dexec.args="examples/sample_instance.json"

# Ou compilar e executar diretamente
mvn package
java -jar target/alocacao-vm-po-1.0-SNAPSHOT.jar [arquivo_instancia.json]
```

## Formato de Entrada

O sistema aceita instâncias em formato JSON com a seguinte estrutura:

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
        "cpu": 2.0,
        "ram": 4.0,
        "storage": 20.0,
        "network": 100.0
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
        "cpu": 8.0,
        "ram": 16.0,
        "storage": 100.0,
        "network": 1000.0
      }
    }
  ]
}
```

## Configuração de Parâmetros

### Parâmetros do BRKGA
```java
OptimizationParameters params = new OptimizationParameters.Builder()
    .brkgaPopulationSize(100)           // Tamanho da população
    .brkgaMaxGenerations(500)           // Número máximo de gerações
    .brkgaEliteRatio(0.15)              // Proporção de indivíduos elite
    .brkgaMutantRatio(0.10)             // Proporção de mutantes
    .brkgaInheritanceProbability(0.7)   // Probabilidade de herança
    .decodingStrategy(BALANCED)         // Estratégia de decodificação
    .archiveSize(100)                   // Tamanho do arquivo Pareto
    .randomSeed(42)                     // Semente aleatória
    .build();
```

### Estratégias de Decodificação
- **GREEDY_COST:** Prioriza hosts com menor custo
- **GREEDY_RELIABILITY:** Prioriza hosts mais confiáveis
- **BALANCED:** Balanceamento entre custo e confiabilidade
- **FIRST_FIT:** Primeira alocação viável encontrada

## Exemplos de Uso

### Uso Programático Básico
```java
// Criar instância de exemplo
ProblemInstance instance = InstanceReader.createSampleInstance();

// Configurar parâmetros
OptimizationParameters params = OptimizationParameters.createDefault();

// Criar otimizador
VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);

// Executar otimização
OptimizationResult result = optimizer.optimize();

// Obter melhores soluções
AllocationSolution bestCost = result.getBestCostSolution();
AllocationSolution bestReliability = result.getBestReliabilitySolution();
AllocationSolution balanced = result.getBalancedSolution();
```

### Análise de Resultados
```java
// Estatísticas do arquivo Pareto
ParetoArchive.ArchiveStatistics stats = result.getArchive().getStatistics();
System.out.println("Número de soluções: " + stats.size);
System.out.println("Faixa de custos: [" + stats.minCost + ", " + stats.maxCost + "]");
System.out.println("Faixa de confiabilidade: [" + stats.minReliability + ", " + stats.maxReliability + "]");

// Relatório detalhado
System.out.println(result.getReport().generateTextReport());
```

## Saídas do Sistema

### Soluções Pareto-ótimas
- Conjunto de soluções não-dominadas
- Valores de todos os objetivos para cada solução
- Detalhamento completo da alocação (qual VM em qual host)

### Relatórios de Execução
- **Formato texto:** Relatório completo com estatísticas
- **Formato CSV:** Dados estruturados para análise
- **Logs detalhados:** Progresso da otimização com timestamps

### Métricas de Qualidade
- Tempo de execução por fase do algoritmo
- Evolução do tamanho do arquivo Pareto
- Estatísticas de convergência
- Métricas de diversidade das soluções

## Tecnologias Utilizadas

- **Java 17:** Linguagem principal
- **Maven:** Gerenciamento de dependências e build
- **SLF4J + Logback:** Sistema de logging
- **Jackson:** Processamento de JSON
- **Apache Commons Math:** Operações matemáticas
- **JUnit 5:** Testes unitários

## Extensibilidade

O sistema foi projetado para ser facilmente extensível:

### Adicionando Novos Objetivos
```java
// Em AllocationSolution.java
public double getNovoObjetivo() {
    // Implementar cálculo do novo objetivo
}
```

### Implementando Novos Algoritmos
```java
public class NovoAlgoritmo {
    public ParetoArchive run(ParetoArchive initialSolutions) {
        // Implementar novo algoritmo
    }
}
```

### Personalizando Estratégias de Decodificação
```java
// Em BRKGADecoder.java - adicionar nova estratégia
public enum DecodingStrategy {
    // ... estratégias existentes
    NOVA_ESTRATEGIA
}
```

## Trabalhos Futuros

- [ ] Implementação da **Busca Tabu multiobjetivo**
- [ ] Integração com **solvers MILP** (CPLEX, Gurobi, GLPK)
- [ ] **Interface gráfica** para visualização das fronteiras de Pareto
- [ ] **Gerador automático** de instâncias de teste
- [ ] **Análise de sensibilidade** dos parâmetros
- [ ] **Paralelização** dos algoritmos
- [ ] **Métricas de qualidade** avançadas (hipervolume, spread)

## Referências

- Gonçalves, J. F., & Resende, M. G. (2011). Biased random-key genetic algorithms for combinatorial optimization.
- Deb, K., et al. (2002). A fast and elitist multiobjective genetic algorithm: NSGA-II.
- Glover, F., & Laguna, M. (1997). Tabu search.

## Contato

Para dúvidas e sugestões, entre em contato com os autores:
- Ramon Barros
- Afrânio Yago

---

**Instituto de Computação - UFAL**  
**Disciplina: Pesquisa Operacional**

