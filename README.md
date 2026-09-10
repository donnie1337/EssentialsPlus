# EssentialsPlus

Essentials customizado para **Paper 26.2**, desenvolvido do zero com foco em teleporte, TPA, homes, integração entre plugins e uma experiência simples para o jogador.

## ✨ Funcionalidades

### 🧭 Sistema de TPA
- `/tpa <jogador>` solicita teleporte até outro jogador.
- `/tpaqui <jogador>` solicita que outro jogador se teleporte até você.
- `/tpaceitar [jogador]` aceita uma solicitação.
- `/tpnegar [jogador]` recusa uma solicitação.
- `/tpcancelar [jogador]` cancela uma solicitação enviada.
- As formas internas `/tpaccept`, `/tpdeny` e `/tpacancel` também são suportadas.
- Solicitações pendentes por jogador.
- Limite de solicitações simultâneas configurável.
- Expiração automática das solicitações.
- Cancelamento da solicitação quando o jogador desconecta.
- Cancelamento do teleporte após morte.
- Botões clicáveis de aceitar, recusar e cancelar diretamente no chat.
- Mensagens configuráveis pelo `config.yml`.
- Nome do jogador nos pedidos de TPA utiliza a mesma cor definida pelo cargo no ChatPlus.

### 🛡️ Teleporte da Staff
- `/tp <jogador>` teleporta você até outro jogador.
- `/tp <jogador> <jogador>` teleporta um jogador até outro.
- `/tphere <jogador>` teleporta um jogador até você.
- Sistema protegido por permissão própria para staff.

### 👻 Vanish
- `/v` ativa ou desativa o modo vanish.
- Permite ocultar o jogador dos demais jogadores.
- Possui permissão própria: `essentialsplus.vanish`.

### 🏠 Sistema de Homes
- `/sethome <nome>` salva uma home na localização atual.
- `/home <nome>` teleporta para uma home salva.
- `/homes` abre a interface gráfica de gerenciamento das homes.
- `/delhome <nome>` remove uma home.
- Homes persistidas em `homes.yml`.
- Limite padrão de homes configurável.
- Estrutura de limites preparada para futura integração por cargo.
- Nomes de homes normalizados e validados.
- Suporte a letras, números, `_` e `-` nos nomes.

### 🖥️ Interface gráfica das Homes
A interface `/homes` permite:

- Visualizar as homes salvas.
- Identificar a home e sua localização.
- **Clique esquerdo:** teleportar para a home.
- **Clique direito:** abrir o gerenciamento da home.
- **Shift + clique direito:** excluir a home imediatamente.
- Alterar o nome da home pelo menu de gerenciamento.
- Retornar entre as interfaces pelo botão de voltar.
- Proteção contra retirada, movimentação e inserção indevida de itens.
- Proteção contra ações de inventário durante a utilização do menu.

### 🔗 Integrações
- **LoginPlus:** verifica o estado de autenticação antes de permitir recursos protegidos.
- **ChatPlus:** integração com o sistema de chat e cores dos cargos.
- **CargoPlus:** integração para informações visuais de cargo utilizadas no sistema de teleporte.

## 🎮 Comandos

| Comando | Função |
|---|---|
| `/tpa <jogador>` | Solicita teleporte até um jogador. |
| `/tpaqui <jogador>` | Solicita que um jogador venha até você. |
| `/tpaceitar [jogador]` | Aceita uma solicitação de TPA. |
| `/tpnegar [jogador]` | Recusa uma solicitação de TPA. |
| `/tpcancelar [jogador]` | Cancela uma solicitação de TPA enviada. |
| `/tp <jogador> [jogador]` | Teleporte direto da staff. |
| `/tphere <jogador>` | Teleporta um jogador até você. |
| `/v` | Ativa ou desativa o vanish. |
| `/home <nome>` | Teleporta para uma home. |
| `/homes` | Abre o menu de gerenciamento das homes. |
| `/sethome <nome>` | Cria ou atualiza uma home. |
| `/delhome <nome>` | Remove uma home. |

## 🔑 Permissões

### TPA

| Permissão | Função | Padrão |
|---|---|---|
| `essentialsplus.tpa` | Permite usar todo o sistema de TPA | `true` |

A permissão principal `essentialsplus.tpa` possui como filhas as permissões específicas dos comandos de TPA.

### Teleporte e Vanish

| Permissão | Função | Padrão |
|---|---|---|
| `essentialsplus.teleport` | Comandos `/tp` e `/tphere` | `op` |
| `essentialsplus.vanish` | Comando `/v` | `false` |

### Homes

| Permissão | Função | Padrão |
|---|---|---|
| `essentialsplus.home` | Usar `/home` | `true` |
| `essentialsplus.homes` | Usar `/homes` | `true` |
| `essentialsplus.sethome` | Usar `/sethome` | `true` |
| `essentialsplus.delhome` | Usar `/delhome` | `true` |

## ⚙️ Configuração

O `config.yml` permite configurar, entre outros recursos:

- Ativação do sistema de TPA.
- Tempo de expiração das solicitações.
- Limite de solicitações pendentes.
- Atraso antes do teleporte.
- Cancelamento do teleporte por movimento.
- Cancelamento por dano.
- Cancelamento por morte.
- Cancelamento por desconexão.
- Ativação do sistema de Homes.
- Limite padrão de homes.
- Teleporte direto da staff.
- Mensagens do sistema.
- Botões clicáveis do TPA.

### Configuração atual do teleporte

Por padrão, o atraso do teleporte está configurado em **0 segundos**. O cancelamento por movimento e dano também está desativado, enquanto morte e desconexão cancelam o fluxo quando aplicável.

## 💾 Persistência

As homes dos jogadores são armazenadas em `homes.yml`, permitindo que continuem disponíveis após reinicializações do servidor.

## 🔗 Dependências

As seguintes integrações são opcionais (`softdepend`):

- LoginPlus
- ChatPlus
- CargoPlus

O EssentialsPlus continua funcionando como plugin independente, enquanto utiliza essas integrações quando disponíveis.

## 🏗️ Plataforma

- Java 26
- Paper 26.2
- Maven
- GitHub Actions para build automatizado

## 🧪 Build

```bash
mvn -B clean package
```

O artefato gerado pelo Maven fica disponível no diretório `target/`.

## 📌 Status

O EssentialsPlus está em desenvolvimento ativo. A base atual já contempla TPA, teleporte da staff, vanish, homes persistentes, interfaces gráficas e integrações com os demais plugins do servidor.