# Alocação de VMs com BRKGA e Tabu Search usando CloudSim Plus

## Autores

- Ramon Barros  
- Afrânio Yago

## Sobre

Este repositório foi desenvolvido como parte da disciplina de **Pesquisa Operacional** no Instituto de Computação da **UFAL**.

O objetivo do projeto é a implementação de técnicas de **BRKGA (Biased Random-Key Genetic Algorithm)** e **Tabu Search** para resolver problemas de alocação de máquinas virtuais (VMs) em hosts físicos. A meta principal é **maximizar a confiabilidade do sistema** durante essa alocação.

Para simular o ambiente de computação em nuvem e validar as soluções propostas, utilizamos o **CloudSim Plus**, uma ferramenta de simulação amplamente usada para modelagem e experimentação em ambientes de nuvem.


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
