# NavidromeApp 🎵

Reproductor de música multiplataforma para Navidrome escrito en Kotlin Multiplatform.

## Tecnologías

- **Kotlin Multiplatform (KMP)** - Lógica compartida entre iOS y Android
- **SwiftUI** - UI para iOS
- **Jetpack Compose** - UI para Android
- **Kotlin Coroutines** - Programación asíncrona
- **Kotlinx Serialization** - Parsing de JSON

## Estructura del proyecto

```
navidrome-app/
├── shared/                    # Código común
│   └── src/commonMain/kotlin/
│       └── com/navidrome/api/
│           └── NavidromeClient.kt   # Cliente API
├── androidApp/                # App Android
└── iosApp/                   # App iOS
```

## API

Este proyecto consume la [API Subsonic](https://www.navidrome.org/docs/developers/subsonic-api/) implementada por Navidrome.

### Autenticación

La API usa autenticación tokenizada (v1.13.0+):
- Token: `MD5(password + salt)`
- Salt: String aleatoria por request

### Endpoints principales

| Endpoint | Descripción |
|----------|-------------|
| `ping` | Test de conectividad |
| `getArtists` | Lista de artistas |
| `getArtist(id)` | Detalles de artista |
| `getAlbum(id)` | Detalles de álbum |
| `getSong(id)` | Detalles de canción |
| `stream(id)` | Stream de audio |
| `getCoverArt(id)` | Portada de álbum |
| `star/unstar` | Favoritos |

## Build

### Requisitos

- JDK 17+
- Android Studio Arctic Fox+
- Xcode 15+ (para iOS)
- Kotlin 1.9.20

### Android

```bash
./gradlew :androidApp: assembleDebug
```

### iOS

```bash
cd iosApp
pod install
open NavidromeApp.xcworkspace
```

## Estado

**EN DESARROLLO** - Esqueleto inicial creado.

## Autor

Eneko - @alopeziko
