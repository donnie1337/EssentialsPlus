# EssentialsPlus

O EssentialsPlus é o meu plugin de utilidades principais do servidor. A ideia é reunir os comandos e sistemas que eu quero ter no servidor sem depender de um Essentials genérico.

Ele foi feito para Spigot 26.2 e é integrado com os outros plugins do servidor, como LoginPlus, ChatPlus e CargoPlus.

## O que tem no plugin

### Sistema de TPA

- `/tpa <jogador>` para pedir teleporte até outro jogador.
- `/tpaqui <jogador>` para pedir que outro jogador venha até você.
- `/tpaceitar [jogador]` para aceitar um pedido.
- `/tpnegar [jogador]` para recusar um pedido.
- `/tpcancelar [jogador]` para cancelar um pedido enviado.
- Suporte aos aliases internos de aceitar, negar e cancelar.
- Pedidos pendentes separados por jogador.
- Limite de pedidos simultâneos configurável.
- Expiração automática dos pedidos.
- Pedido cancelado quando o jogador sai do servidor.
- Teleporte cancelado em situações configuradas, como morte.
- Botões clicáveis no chat para aceitar, recusar e cancelar.
- Mensagens configuráveis.
- Nome do jogador respeitando a cor do cargo.

### Teleporte da staff

- `/tp <jogador>` para ir até outro jogador.
- `/tp <jogador> <jogador>` para teleportar um jogador até outro.
- Sistema protegido por permissão própria.

### Fly

- `/fly` ativa e desativa o voo.
- Permissão própria: `essentialsplus.fly`.
- Ao desligar o fly no ar, o jogador entra em um sistema de descida gradual.
- A velocidade da queda aumenta conforme o jogador se aproxima do chão.
- O sistema para ao chegar no chão, água ou lava.
- A queda causada pelo desligamento do fly não gera dano.

### Vanish

- `/v` ativa ou desativa o vanish.
- Permite esconder o jogador dos demais jogadores.
- Permissão própria: `essentialsplus.vanish`.

### Homes

- `/sethome <nome>` cria ou atualiza uma home.
- `/home <nome>` teleporta para uma home.
- `/homes` abre o menu das homes.
- `/delhome <nome>` remove uma home.
- As homes ficam salvas em `homes.yml`.
- Limite de homes configurável.
- Nomes de homes validados e normalizados.
- Nome de home com uma única palavra e limite configurável.

### Menu de Homes

No `/homes` o jogador consegue:

- Ver todas as homes salvas.
- Teleportar para uma home.
- Abrir o gerenciamento da home.
- Renomear a home.
- Excluir uma home.
- Voltar entre os menus.
- Usar o inventário sem conseguir retirar ou colocar itens do menu.

### Integrações

- **LoginPlus:** verifica se o jogador está autenticado antes de liberar recursos protegidos.
- **ChatPlus:** integração com chat e cores dos jogadores.
- **CargoPlus:** pega informações do cargo para usar nos sistemas do plugin.

## Comandos

| Comando | O que faz |
|---|---|
| `/tpa <jogador>` | Pede teleporte até um jogador. |
| `/tpaqui <jogador>` | Pede para um jogador vir até você. |
| `/tpaceitar [jogador]` | Aceita um pedido de TPA. |
| `/tpnegar [jogador]` | Recusa um pedido de TPA. |
| `/tpcancelar [jogador]` | Cancela um pedido enviado. |
| `/tp <jogador> [jogador]` | Teleporte direto da staff. |
| `/fly` | Liga ou desliga o voo. |
| `/v` | Liga ou desliga o vanish. |
| `/home <nome>` | Teleporta para uma home. |
| `/homes` | Abre o menu de homes. |
| `/sethome <nome>` | Cria ou atualiza uma home. |
| `/delhome <nome>` | Remove uma home. |

## Permissões

| Permissão | O que faz | Padrão |
|---|---|---|
| `essentialsplus.tpa` | Acesso ao sistema de TPA | `true` |
| `essentialsplus.teleport` | Comandos de teleporte da staff | `op` |
| `essentialsplus.fly` | Usar `/fly` | `false` |
| `essentialsplus.vanish` | Usar `/v` | `false` |
| `essentialsplus.home` | Usar `/home` | `true` |
| `essentialsplus.homes` | Usar `/homes` | `true` |
| `essentialsplus.sethome` | Usar `/sethome` | `true` |
| `essentialsplus.delhome` | Usar `/delhome` | `true` |

## Configuração

O `config.yml` controla os principais sistemas do plugin, incluindo:

- TPA e tempo de expiração dos pedidos.
- Limite de pedidos pendentes.
- Tempo de espera do teleporte.
- Cancelamento por movimento, dano, morte e desconexão.
- Sistema de homes e limite de homes.
- Teleporte da staff.
- Mensagens do plugin.
- Botões clicáveis do TPA.

As mensagens ficam configuráveis para eu conseguir ajustar o texto e as cores sem precisar mexer no código.

## Dados

As homes dos jogadores ficam salvas em `homes.yml`, então continuam disponíveis depois que o servidor reinicia.

## Dependências opcionais

- LoginPlus
- ChatPlus
- CargoPlus

O EssentialsPlus continua funcionando sozinho quando essas integrações não estão instaladas.

## Plataforma

- Java 26
- Spigot API 26.2
- Maven
- GitHub Actions

## Build

```bash
mvn -B clean package
```

O `.jar` gerado fica dentro da pasta `target/`.

## Status

O EssentialsPlus ainda está em desenvolvimento. A ideia é continuar colocando nele os sistemas principais que eu quero usar no servidor, mantendo tudo organizado e integrado com os meus outros plugins.
