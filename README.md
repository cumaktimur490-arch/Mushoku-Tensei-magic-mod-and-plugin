# Mushoku Tensei Magic Mod and Plugin

Мод для Minecraft по магии из "Реинкарнации безработного" (Mushoku Tensei).

## Восстановление проекта из JAR

Проект восстановлен из оригинального JAR мода:

### Ресурсы (извлечены как есть)
- `assets/mushoku_magic/textures/item/` - текстуры посохов и кристаллов
- `assets/mushoku_magic/models/item/` - модели предметов
- `assets/mushoku_magic/lang/` - локализация (en_us, ru_ru)
- `pack.mcmeta` - метаданные ресурспака
- `META-INF/mods.toml` - описание мода

### Классы (декомпилированы)
- `com.cumaktimur.mushokumagic.MushokuMagicMod` - главный класс мода
- `com.cumaktimur.mushokumagic.item.StaffItem` - базовый класс посохов
- `com.cumaktimur.mushokumagic.item.ModItems` - регистрация предметов
- `com.cumaktimur.mushokumagic.item.ModCreativeTabs` - креативная вкладка

Декомпиляция выполнена с помощью ForgeGradle и восстановлена в читаемый вид.

### Исходный JAR
Оригинальный файл `mushoku-magic-1.0.0-sources.jar` (53142 bytes) был найден в артефактах сборки #8 (коммит 724a796).
Также существовал релиз `latest-build` (коммит 81db79d, ветка arena/01a0dda9).

## Обновление посохов

По заданию обновлены множители магии:

| Посох | ID | Множитель | Описание |
|-------|----|-----------|----------|
| Палочка | `wand` | **×2** | Базовый посох для новичков |
| Посох Мигурда | `migurd_staff` | **×15** | Посох племени мигурдов, сильно усиливает магию |
| Посох Аква Хартия | `aqua_hartia_staff` | **×50** | Легендарный посох Рокси и Рудэуса |

Множители заданы в `StaffItem.java`:
```java
public static final int WAND_MULTIPLIER = 2;
public static final int MIGURD_STAFF_MULTIPLIER = 15;
public static final int AQUA_HARTIA_STAFF_MULTIPLIER = 50;
```

Логика расчета урона:
```java
public float calculateMagicDamage(float baseDamage) {
    return baseDamage * magicMultiplier;
}
```

## Сборка

Требования:
- Java 17
- Gradle 8.8 (в комплекте wrapper)
- Minecraft 1.20.1
- Forge 47.1.0

Сборка:
```bash
./gradlew build
```

Результат: `build/libs/mushoku_magic-1.0.0.jar`

## CI

GitHub Actions workflow `.github/workflows/build.yml`:
- Собирает мод на каждый push в main и arena/**
- Загружает артефакты (mod jar, sources jar)
- Создает релиз `latest-build` при пуше в main

Запуск CI: автоматически при пуше или вручную через workflow_dispatch.

## Установка

1. Скачайте jar из релиза `latest-build` или из артефактов CI
2. Положите в папку `mods` вашего Minecraft 1.20.1 Forge сервера/клиента
3. Запустите игру

## Лицензия

All Rights Reserved
