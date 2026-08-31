# SMS AutoFill Demo

Готовый Android-проект (Kotlin), демонстрирующий автоподстановку кода
из SMS через **SMS User Consent API** — тот самый системный диалог
"Разрешить приложению ... прочитать сообщение и ввести код?".

## Как собрать APK на GitHub

1. Создайте пустой репозиторий на github.com.
2. В папке проекта:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/ВАШ_ЛОГИН/ВАШ_РЕПО.git
   git push -u origin main
   ```
3. Откройте вкладку **Actions** в репозитории — сборка запустится
   автоматически при пуше в `main` (или запустите вручную кнопкой
   "Run workflow").
4. После завершения сборки зайдите в конкретный workflow run →
   раздел **Artifacts** внизу страницы → скачайте
   `sms-autofill-debug-apk` (zip с `app-debug.apk` внутри).

## Как это работает

- `SmsUserConsentHelper.startListening()` включает у Google Play
  Services 5-минутное окно ожидания входящей SMS.
- Как только приходит SMS, показывается системный диалог
  подтверждения (скриншот из вашего вопроса).
- После нажатия "Разрешить" полный текст SMS прилетает в приложение,
  и регуляркой из него вытаскивается код.
- Разрешения READ_SMS/RECEIVE_SMS не нужны.

`MainActivity` вызывает `startListening()` сам, при открытии экрана —
пользователю/разработчику не нужно нажимать отдельную кнопку "включить
прослушивание" каждый раз.
