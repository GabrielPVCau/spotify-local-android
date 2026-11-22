# Spotify Local Android

Player de música nativo para Android em **Kotlin + Jetpack Compose + Media3 (ExoPlayer)**, compatível com o **Spotify Local Player (PC)** via sincronização **Syncthing** (`.player-sync/`).

## Recursos
- **Folder Picker (SAF)**: selecione a pasta de músicas sincronizada pelo Syncthing (ex.: `musica`) com permissão persistente.
- **Playlists por subpasta**: cada subpasta vira uma playlist automaticamente, além de **Músicas Curtidas** e **Todas as Músicas**.
- **Sincronização `.player-sync/` (v1)**:
  - Lê todos os arquivos `*.json` em `<pasta_musicas>/.player-sync/` (incluindo `pc.json`).
  - Soma `play_count` entre dispositivos, mantém `last_played_at` mais recente, `liked` com `liked_updated_at` mais recente e `playlists[id]` com `updated_at` mais recente.
  - Escreve **apenas** `<device_id>.json` único por instalação (ex.: `android-a1b2c3d4.json`), permitindo múltiplos dispositivos Android + PC sem conflitos.
- **Reprodução em segundo plano**: `MediaSessionService` + `ExoPlayer` com controles na notificação, tela de bloqueio e Bluetooth.
- **Interface Dark estilo Spotify**:
  - Navegação inferior: Início, Playlists e Buscar.
  - Detalhe da playlist com Tocar, Aleatório, Ordenação (`Ordem padrão`, `Mais reproduzidas`, `Menos reproduzidas`, `Título A-Z`, `Artista A-Z`, `Adicionadas recentemente`), botão **Fixar padrão** persistido, filtro e contagem de reproduções.
  - Mini-player e player em tela cheia com seek bar, shuffle, repeat e curtir.

## Download do APK
A cada push na branch `main`, o GitHub Actions compila o APK automaticamente em **Actions → Build Android Debug APK → Artifacts (`spotify-local-android-debug-apk`)**.
