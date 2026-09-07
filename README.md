# EssentialsPlus

Essentials customizado para Paper, desenvolvido do zero com foco em segurança, modularidade e integração com o ecossistema do servidor.

## Plataforma

- Java 26
- Paper 26.2
- Maven

## TPA

O primeiro módulo implementado reproduz o fluxo conhecido do EssentialsX, mas com implementação própria:

- `/tpa <jogador>`
- `/tpahere <jogador>`
- `/tpaccept [jogador]`
- `/tpdeny [jogador]`
- `/tpacancel [jogador]`
- solicitações pendentes por jogador
- limite de solicitações simultâneas
- expiração automática
- cooldown
- atraso configurável antes do teleporte
- cancelamento por movimento, dano, morte e desconexão
- botões clicáveis de ACEITAR e RECUSAR no chat
- mensagens em português e configuráveis
- permissões independentes para integração com CargoPlus

O projeto não copia a implementação do EssentialsX; a referência é comportamental e arquitetural. O código é próprio do EssentialsPlus.
