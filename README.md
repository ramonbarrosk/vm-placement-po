# Sistema Híbrido BRKGA + Tabu Search para Alocação de VMs

Sistema de otimização multiobjetivo para alocação de máquinas virtuais em hosts físicos, combinando algoritmo genético (BRKGA) e busca tabu para minimizar custos e maximizar confiabilidade.

## 🚀 Como Rodar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Execução Rápida
```bash
# Clonar o repositório
git clone <repo-url>
cd vm-placement-po

# Compilar
mvn clean compile

# Executar exemplo principal
mvn exec:java -Dexec.mainClass="com.ramonyago.cloudsim.App"
```

### Executar Exemplos Específicos
```bash
# Exemplo básico de otimização
mvn exec:java -Dexec.mainClass="com.ramonyago.cloudsim.examples.CloudSimExample"

# Exemplo de alocação em tempo real
mvn exec:java -Dexec.mainClass="com.ramonyago.cloudsim.examples.RealTimeAllocationExample"
```

## 📁 Estrutura do Projeto

```
src/main/java/com/ramonyago/cloudsim/
├── algorithm/
│   └── brkga/               # Algoritmo BRKGA multiobjetivo
├── examples/                # Exemplos de uso
├── io/                      # Leitura/escrita de instâncias
├── model/                   # Modelos (VM, Host, Solução)
├── simulation/              # Integração CloudSim
└── util/                    # Utilitários

examples/                    # Instâncias de teste (JSON)
├── small_instance.json      # 3 VMs, 2 hosts
├── sample_instance.json     # 5 VMs, 4 hosts
└── large_instance.json      # 8 VMs, 6 hosts
```

## 📊 Instâncias de Teste

### Pequena (3 VMs, 2 hosts)
- **Tempo:** ~300ms
- **Resultado:** Custo 100.0, Confiabilidade 0.98

### Média (5 VMs, 4 hosts)  
- **Tempo:** ~370ms
- **Resultado:** Custo 120.0, Confiabilidade 0.99

### Grande (8 VMs, 6 hosts)
- **Tempo:** ~320ms
- **Resultado:** Custo 260.0, Confiabilidade 0.991

## 🔧 Como Funciona

1. **BRKGA** gera população inicial e explora o espaço de soluções
2. **Tabu Search** refina as soluções encontradas
3. **CloudSim** valida as soluções com simulação real
4. **Arquivo Pareto** mantém as melhores soluções não-dominadas

## 📈 Resultados Típicos

```
=== OTIMIZAÇÃO COMPLETA ===
Tempo BRKGA: 35ms
Tempo Tabu Search: 290ms (288 melhorias)
Melhoria de Custo: -27.7%
Fronteira de Pareto: 50 soluções

=== DISTRIBUIÇÃO FINAL ===
Host 2 → VMs 1,2,3 (Custo: 100.0)
Confiabilidade: 0.98
```

## ⚙️ Configurações

### Parâmetros BRKGA
- População: 50 indivíduos
- Gerações: 100
- Elite: 15%, Mutantes: 10%

### Parâmetros Tabu Search  
- Iterações: 500 por solução
- Lista Tabu: 50 movimentos
- Diversificação: a cada 100 iterações

## 📝 Arquivos de Documentação

- `EXPERIMENTOS_E_RESULTADOS.md` - Resultados detalhados dos experimentos
- `ANALISE_EXPERIMENTAL.md` - Análise técnica dos algoritmos
- `diagramas.md` - Diagramas de fluxo do sistema

## 🎯 Uso Personalizado

### Criar Nova Instância
```java
ProblemInstance instance = new ProblemInstance("minha-instancia");

// Adicionar VMs
VM vm1 = new VM(1, 0.95, 1.0); // ID, confiabilidade mín, prioridade
vm1.setDemand(ResourceType.CPU, 2.0);
instance.addVM(vm1);

// Adicionar Hosts  
Host host1 = new Host(1, 100.0, 0.02, 150.0); // ID, custo, prob falha, energia
host1.setCap(ResourceType.CPU, 8.0);
instance.addHost(host1);
```

### Executar Otimização
```java
VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance);
OptimizationResult result = optimizer.optimize();
AllocationSolution bestSolution = result.getBestCostSolution();
```

## 🏆 Principais Vantagens

- ✅ **Rápido:** < 400ms para instâncias até 8 VMs
- ✅ **Eficaz:** 27.7% melhoria de custo vs só BRKGA  
- ✅ **Validado:** Simulação CloudSim confirma resultados
- ✅ **Flexível:** Múltiplas estratégias de decodificação
- ✅ **Escalável:** Performance sublinear com tamanho da instância

---

**Para mais detalhes técnicos, consulte os arquivos de documentação na raiz do projeto.** 