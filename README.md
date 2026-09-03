# Jenkins CI/CD — тестовое задание 8

Репозиторий демонстрирует автоматическую цепочку из трёх Jenkins Pipeline jobs. Код берётся из ветки `master`, два тестовых файла удаляются, после чего изменение коммитится и отправляется обратно в GitHub.

## Соответствие заданию

| Требование | Реализация |
|---|---|
| Установить Git на сервер либо использовать общедоступный | Удалённым Git-сервером выбран GitHub; Git также установлен в Ubuntu для работы Jenkins с репозиторием |
| Развернуть Jenkins | Jenkins работает как `systemd`-сервис на локальном стенде Ubuntu 24.04 в WSL2 |
| Перенести код из `master` в локальное хранилище | `Job_1` делает checkout ветки `master` и сохраняет полный проект как Jenkins artifact |
| Удалить из проекта 1–2 файла | `Job_2` получает artifact конкретной сборки `Job_1` и удаляет `delete_me_1.txt` и `delete_me_2.txt` |
| Вернуть проект в Git | `Job_3` получает artifact конкретной сборки `Job_2`, создаёт коммит и выполняет push в `master` |
| Запускать jobs автоматически | `Job_1` запускается по таймеру `H/2 * * * *`, затем синхронно вызывает `Job_2`, а `Job_2` — `Job_3` |

## Файлы

- [`jenkins/Job_1.groovy`](jenkins/Job_1.groovy) — получение ветки `master`, архивация и вызов `Job_2`.
- [`jenkins/Job_2.groovy`](jenkins/Job_2.groovy) — получение исходного artifact, удаление файлов и вызов `Job_3`.
- [`jenkins/Job_3.groovy`](jenkins/Job_3.groovy) — получение изменённого проекта, commit и push.
- [`docs/task-8-evidence.md`](docs/task-8-evidence.md) — зафиксированный результат проверки.

## Стенд

- Windows 11 и Ubuntu 24.04 в WSL2;
- `systemd`;
- Git 2.43.0;
- OpenJDK 21;
- Jenkins LTS;
- ветка репозитория: `master`;
- удалённый репозиторий: `git@github.com:Vladislav-Yurievich/jenkins-test-task.git`.

## Необходимые плагины Jenkins

- Pipeline;
- Git;
- Copy Artifact;
- SSH Agent.

## Настройка Jenkins

1. Добавить SSH credential со следующими параметрами:
   - Kind: **SSH Username with private key**;
   - Username: `git`;
   - ID: `github-ssh`;
   - приватный ключ: deploy key репозитория с правом записи.
2. Добавить публичный host key `github.com` в `/var/lib/jenkins/.ssh/known_hosts` и выставить владельца `jenkins:jenkins`.
3. Создать Pipeline jobs с точными именами `Job_1`, `Job_2` и `Job_3`.
4. Для `Job_2` и `Job_3` включить строковый параметр `SOURCE_BUILD` с пустым значением по умолчанию. Он нужен уже при первом автоматическом вызове дочерней job.
5. В каждую job вставить соответствующий Groovy-скрипт из каталога [`jenkins`](jenkins) и оставить включённым **Use Groovy Sandbox**.
6. Не добавлять отдельные триггеры в `Job_2` и `Job_3`: они вызываются предыдущими jobs. Расписание `Job_1` уже задано в Pipeline.

Все artifacts выбираются по точному номеру родительской сборки. Это не позволяет параллельным или более поздним запускам подменить входные данные цепочки. Повторный запуск безопасен: если удаляемых файлов уже нет, `Job_3` сообщает об отсутствии изменений и не создаёт пустой коммит.

## Проверенный результат

Первоначальный проект с двумя тестовыми файлами находится в коммите [`20f9a20`](https://github.com/Vladislav-Yurievich/jenkins-test-task/commit/20f9a20). Автоматическая цепочка удалила их и отправила результат в коммите [`9f3343e`](https://github.com/Vladislav-Yurievich/jenkins-test-task/commit/9f3343e) с сообщением `ci: remove test files`.

Сразу после автоматического выполнения в `master` остались `README.md` и `app.py`; затем в репозиторий была добавлена эта документация. Все три Jenkins jobs завершились успешно. Скриншот и команды проверки приведены в [отчёте](docs/task-8-evidence.md).
