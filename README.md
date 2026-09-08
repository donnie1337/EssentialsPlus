# EssentialsPlus

Essentials customizado para **Paper 26.2**, desenvolvido do zero com foco em utilidades de teleporte, homes, integração e uma experiência simples para o jogador.

## ✨ Funcionalidades

### 🧭 Sistema de TPA
- `/tpa <jogador>` solicita teleporte até outro jogador.
- `/tpapara <jogador>` solicita que outro jogador se teleporte até você.
- `/tpaceitar [jogador]` aceita uma solicitação.
- `/tpnegar [jogador]` recusa uma solicitação.
- `/tpacancelar [jogador]` cancela uma solicitação enviada.
- Solicitações pendentes por jogador.
- Limite de solicitações simultâneas.
- Expiração automática das solicitações.
- Mensagens configuráveis.
- Botões clicáveis de aceitar e recusar no chat.

### 🏠 Sistema de Homes
- `/sethome <nome>` salva uma home na localização atual.
- `/home <nome>` teleporta para uma home salva.
- `/homes` abre a interface gráfica de gerenciamento das homes.
- `/delhome <nome>` remove uma home.
- Limite de homes configurável por jogador.
- Nomes de homes normalizados e validados.
- Homes persistidas em `homes.yml`.

### 🖥️ Interface gráfica das Homes
A interface `/homes` permite:

- Visualizar as homes salvas.
- **Clique esquerdo:** teleportar.
- **Clique direito:** abrir o gerenciamento da home.
- **Shift + clique direito:** deletar imediatamente, sem confirmação.
- Alterar o nome da home através do menu de gerenciamento.
- Retornar entre as interfaces usando o botão de voltar.
- Interface protegida contra retirada e movimentação dos itens.

### 🔗 Integração
- Integração com **LoginPlus** para respeitar o estado de autenticação.
- Integração com **ChatPlus** para trabalhar dentro do ecossistema de chat do servidor.

## 🎮 Comandos

| Comando | Função |
|---|---|
| `/tpa <jogador>` | Solicita teleporte até um jogador. |
| `/tpapara <jogador>` | Solicita que um jogador venha até você. |
| `/tpaceitar [jogador]` | Aceita uma solicitação. |
| `/tpnegar [jogador]` | Recusa uma solicitação. |
| `/tpacancelar [jogador]` | Cancela uma solicitação enviada. |
| `/home <nome>` | Teleporta para uma home. |
| `/homes` | Abre o menu de gerenciamento das homes. |
| `/sethome <nome>` | Cria ou atualiza uma home. |
| `/delhome <nome>` | Remove uma home. |

## 🔑 Permissões

| Permissão | Função | Padrão |
|---|---|---|
| `essentialsplus.tpa` | Usar `/tpa` | `true` |
| `essentialsplus.tpahere` | Usar `/tpapara` | `true` |
| `essentialsplus.tpaccept` | Aceitar TPA | `true` |
| `essentialsplus.tpdeny` | Recusar TPA | `true` |
| `essentialsplus.tpacancel` | Cancelar TPA | `true` |
| `essentialsplus.home` | Usar `/home` | `true` |
| `essentialsplus.homes` | Usar `/homes` | `true` |
| `essentialsplus.sethome` | Usar `/sethome` | `true` |
| `essentialsplus.delhome` | Usar `/delhome` | `true` |

## ⚙️ Configuração

O sistema de TPA e homes possui configurações no `config.yml`, incluindo limite de solicitações, tempo de expiração e quantidade padrão de homes.

## 🔗 Dependências

- LoginPlus
- ChatPlus

## 🏗️ Plataforma

- Java 26
- Paper 26.2
- Maven

## 🧪 Build

```bash
mvn -B clean package
```

O projeto possui workflow de build no GitHub Actions.

> **Nota:** recursos de atraso de teleporte, cancelamento por movimento e cancelamento por dano não fazem parte do fluxo atualmente implementado.
