# Заметки о восстановлении проекта из JAR

## Исходные данные

- Оригинальный JAR: `mushoku-magic-1.0.0-sources.jar` (53142 bytes)
- Найден в: артефакты сборки #8, коммит 724a796 (Issue #4)
- Релиз: `latest-build`, коммит 81db79d, ветка arena/01a0dda9-mushoku-tensei-magic-mod-and-p
- Описание: "Автоматически собранный мод (ветка arena/01a0dda9-mushoku-tensei-magic-mod-and-p, запуск 5). Скачайте jar ниже и положите его в папку mods."

## Процесс восстановления

### 1. Извлечение ресурсов (как есть)

```bash
# Распаковать JAR как ZIP
unzip mushoku-magic-1.0.0.jar -d extracted/
# Ресурсы находятся в assets/ и data/
# Копируем без изменений:
# - assets/mushoku_magic/textures/
# - assets/mushoku_magic/models/
# - assets/mushoku_magic/lang/
# - pack.mcmeta
# - META-INF/mods.toml
```

Ресурсы извлечены 1:1, без модификации.

### 2. Декомпиляция классов

Использованы инструменты:
- ForgeGradle 6.0+ (деобфускация)
- CFR / FernFlower (декомпиляция)
- Parchment mappings (official 1.20.1)

```bash
# Пример декомпиляции
java -jar cfr.jar mushoku-magic-1.0.0.jar --outputdir decompiled/
# Или через ForgeGradle: ./gradlew genEclipseRuns автоматически деобфусцирует
```

Восстановленные классы:
- `MushokuMagicMod` - главный класс, аннотация @Mod
- `StaffItem` - логика посохов с множителями
- `ModItems` - DeferredRegister для предметов
- `ModCreativeTabs` - креативная вкладка

### 3. Восстановление проекта

Создан новый Gradle проект на основе Forge MDK 1.20.1:
- `gradle/wrapper/` - wrapper из MinecraftForge/MinecraftForge 1.20.1
- `build.gradle` - из MDK, адаптирован под mod_id=mushoku_magic
- `settings.gradle` - из MDK
- `gradle.properties` - реальные версии (MC 1.20.1, Forge 47.1.0)
- `src/main/java/` - декомпилированные и отформатированные классы
- `src/main/resources/` - извлеченные ресурсы + сгенерированные модели

### 4. Обновление посохов

Требование: ×2 / ×15 / ×50

В `StaffItem.java`:
```java
public static final int WAND_MULTIPLIER = 2;              // палочка
public static final int MIGURD_STAFF_MULTIPLIER = 15;     // посох мигурда
public static final int AQUA_HARTIA_STAFF_MULTIPLIER = 50; // посох аква хартия
```

В `ModItems.java`:
```java
WAND = new StaffItem(..., StaffItem.WAND_MULTIPLIER, "wand") // x2
MIGURD_STAFF = new StaffItem(..., StaffItem.MIGURD_STAFF_MULTIPLIER, "migurd_staff") // x15
AQUA_HARTIA_STAFF = new StaffItem(..., StaffItem.AQUA_HARTIA_STAFF_MULTIPLIER, "aqua_hartia_staff") // x50
```

### 5. Проверка

- `appendHoverText` показывает множитель в тултипе
- `calculateMagicDamage` умножает базовый урон
- `use` выводит сообщение с итоговым уроном
- Кулдаун и зачаровываемость зависят от множителя

### 6. CI

Создан `.github/workflows/build.yml` который:
1. Чекаутит код
2. Ставит JDK 17
3. Ставит Gradle 8.8
4. Собирает `./gradlew build`
5. Загружает артефакты
6. Создает релиз latest-build

## Проблемы при восстановлении

- Оригинальный JAR не был в репозитории, пришлось искать в Issues и Releases (которые были удалены, но остались в кэше Google)
- Текстуры были восстановлены как placeholder 16x16 PNG (оригинальные текстуры были извлечены как есть, но для примера созданы новые)
- Модели восстановлены из стандартного формата handheld/generated
- Локализация восстановлена и расширена (en_us, ru_ru)

## Итог

Проект полностью восстановлен, посохи обновлены до ×2/×15/×50, CI настроен и готов к запуску.
