# Nexus Drive

Aplicativo Android nativo (Kotlin) para auxílio a motoristas de app — calcula
em tempo real a rentabilidade de cada oferta de corrida nos apps Uber Driver,
99 Driver e inDrive, exibindo um overlay (verde/amarelo/vermelho) com lucro líquido
por hora e por km.

## Como compilar

Cada `git push` para `main` dispara o workflow [.github/workflows/build.yml](.github/workflows/build.yml)
que gera um APK de debug. Para baixar:

1. Aba **Actions** no GitHub.
2. Clica no último workflow run com check verde.
3. Na seção **Artifacts**, baixa `nexus-drive-debug`.
4. Descompacta o zip — dentro tem `app-debug.apk`.

Para compilar localmente, ver `PLAY_STORE_GUIDE.md`.

## Permissões

Ver [PLAY_STORE_GUIDE.md](PLAY_STORE_GUIDE.md) para detalhes do uso de
`BIND_ACCESSIBILITY_SERVICE` e `SYSTEM_ALERT_WINDOW`.
