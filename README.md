- # AttrTextLayout

- ![Android Support Version](https://img.shields.io/badge/Android_Version-4.1.1+-blue) ![Compat](https://img.shields.io/badge/Compat-AndroidX_&_Support_Library-blue)

- ## Configuration

```kotlin
repository { mavenCentral() }

implementation("com.kotlincrow.android.component:AttrTextLayout:1.9")
```

- ## Function
- [x] Supports configuration of text strategy, style, line wrapping (manual, automatic), special effects (erase, move, circular, continuous, non-continuous) animation
- [x] Supports XML and dynamic creation
- [x] Optimize drawing speed < 3MS
- [x] Add gradient color
- [x] Added high refresh rate animation (only supports left and right movement, up and down movement, controllable speed, best effect when Speed is 8)
- [x] Configure font type
- [x] Configure borders
- [ ] Enable (single column, multiple columns) vertical text
- [ ] Enable text stroke, shadow
- [ ] Configure gradient color RGB

- ## Reference
- [Send async messages to the Android main looper](https://github.com/Kotlin/kotlinx.coroutines/commit/8adbb70765226321bf7db485633007c6d8aba774)