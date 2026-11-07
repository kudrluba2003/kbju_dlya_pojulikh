#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Парсер базы продуктов pbprog.ru -> products_seed.json для Android-приложения.

Собирает названия продуктов/блюд и их КБЖУ на 100 грамм, складывает в один файл:

    [
      {"name": "Овсяная каша", "calories": 88.0, "protein": 3.0, "fat": 1.7,
       "carbs": 15.0, "category": "Каши"},
      ...
    ]

ЧТО ВАЖНО ЗНАТЬ ПРО САЙТ (проверено обходом в октябре 2026):
  * https://pbprog.ru/tk/pm — сервис «Плановое меню»: список блюд подгружается
    JavaScript-ом, в статическом HTML таблиц КБЖУ НЕТ (ссылки на блюда —
    javascript:void(0)).
  * Коллекции тех. карт лежат по адресам https://pbprog.ru/tk/t1 ... /tk/t24,
    /tk/ta, /tk/tt и т.д. — их скрипт находит автоматически.
  * Поэтому для каждой страницы скрипт пробует три стратегии:
      1) статические HTML-таблицы <table> (колонки распознаются по ключевым
         словам в заголовках: «Белки», «Калории» и т.д.);
      2) JSON, встроенный в <script>/data-атрибуты (Bitrix так часто делает);
      3) режим --render: страница дорисовывается headless-браузером Playwright
         и разбирается уже готовый DOM. Именно он нужен для /tk/pm.

Установка:
    pip install requests beautifulsoup4
    # только для режима --render:
    pip install playwright && playwright install chromium

Запуск:
    python tools/scrape_pbprog.py --output products_seed.json --delay 1.5
    python tools/scrape_pbprog.py --limit-collections 3        # быстрый тест
    python tools/scrape_pbprog.py --render                     # если статика пустая

Этика: скрипт однопоточный, с паузами 1.5–3 с и ротацией User-Agent; используйте
данные для личного офлайн-справочника и уважайте условия использования сайта.
"""

import argparse
import json
import random
import re
import sys
import time
from dataclasses import asdict, dataclass
from typing import Optional
from urllib.parse import urljoin

try:
    import requests
    from bs4 import BeautifulSoup
except ImportError:
    sys.exit("Нужны зависимости: pip install requests beautifulsoup4")

START_URL = "https://pbprog.ru/tk/pm"
FALLBACK_INDEX_URL = "https://pbprog.ru/tk/"
BASE_URL = "https://pbprog.ru"

# Коллекции тех. карт: /tk/t1 ... /tk/t24, /tk/ta, /tk/tt и т.п.
COLLECTION_HREF_RE = re.compile(r"^/tk/t[\w]+$")

# Фейковые User-Agent — ротация, чтобы не выглядеть ботом с одной подписью.
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Edge/123.0",
]

# Колонки/ключи распознаём по ключевым словам: сайт называет их по-русски и по-разному.
NUTRIENT_KEYWORDS = {
    "calories": ("калор", "ккал", "энергет", "calories", "kcal", "energy"),
    "protein": ("белк", "protein"),
    "fat": ("жир", "fat"),
    "carbs": ("углев", "carbohydr", "carbs"),
}
NAME_KEYWORDS = ("name", "назван", "наимен", "title", "блюд", "продукт")

# Блоки JSON внутри <script>: плоские массивы/объекты без вложенности.
JSON_BLOCK_RE = re.compile(r"\[[^\[\]]{10,}\]|\{[^{}]{10,}\}", re.S)


def log(message: str) -> None:
    print(message, file=sys.stderr, flush=True)


def to_number(text) -> Optional[float]:
    """«12,5 г» -> 12.5, «-» -> None. Не бросает исключений."""
    if text is None:
        return None
    match = re.search(r"-?\d+(?:[.,]\d+)?", str(text))
    if not match:
        return None
    try:
        return float(match.group(0).replace(",", "."))
    except ValueError:
        return None


def matches(text: str, keywords) -> bool:
    low = text.lower()
    return any(keyword in low for keyword in keywords)


@dataclass
class Product:
    name: str
    calories: float
    protein: float
    fat: float
    carbs: float
    category: str


class Scraper:
    def __init__(self, delay: float, timeout: int = 25):
        self.delay = delay
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": random.choice(USER_AGENTS),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "ru-RU,ru;q=0.9",
        })

    def fetch(self, url: str) -> Optional[BeautifulSoup]:
        """GET с повторами, бэкоффом и вежливой паузой. None — не получилось."""
        for attempt in range(1, 4):
            try:
                self.session.headers["User-Agent"] = random.choice(USER_AGENTS)
                response = self.session.get(url, timeout=self.timeout)
                if response.status_code == 429:
                    wait = int(response.headers.get("Retry-After", 30))
                    log(f"  429 Too Many Requests — ждём {wait} с")
                    time.sleep(wait)
                    continue
                if response.status_code == 404:
                    log(f"  404: {url}")
                    return None
                response.raise_for_status()
                # Пауза после КАЖДОГО успешного запроса, чтобы не создать нагрузку.
                time.sleep(self.delay + random.uniform(0, self.delay))
                # Отдаём в BeautifulSoup сырые байты — он сам подберёт кодировку
                # (у сайта она объявлена не всегда корректно).
                return BeautifulSoup(response.content, "html.parser")
            except requests.RequestException as error:
                log(f"  ошибка запроса ({attempt}/3): {error}")
                time.sleep(2 * attempt)
        return None

    def discover_collections(self, soup: BeautifulSoup) -> list:
        found = {}
        for anchor in soup.find_all("a", href=True):
            href = anchor["href"].strip()
            if COLLECTION_HREF_RE.match(href):
                title = anchor.get_text(" ", strip=True) or href
                found.setdefault(urljoin(BASE_URL, href), title)
        return list(found.items())

    # ----- Стратегия 1: статические таблицы -----

    def extract_from_tables(self, soup: BeautifulSoup, category: str) -> list:
        products = []
        for table in soup.find_all("table"):
            header_cells = [c.get_text(" ", strip=True).lower() for c in table.find_all("th")]
            columns = {}
            for key, keywords in NUTRIENT_KEYWORDS.items():
                for index, header in enumerate(header_cells):
                    if matches(header, keywords):
                        columns[key] = index
                        break
            if "calories" not in columns:
                continue  # таблица не про КБЖУ
            for row in table.find_all("tr"):
                cells = row.find_all("td")
                if not cells:
                    continue
                name = cells[0].get_text(" ", strip=True)
                if not name:
                    continue

                def value(key):
                    index = columns.get(key)
                    if index is None or index >= len(cells):
                        return None
                    return to_number(cells[index].get_text(" ", strip=True))

                kcal = value("calories")
                if kcal is None or kcal < 0:
                    continue
                products.append(Product(
                    name=name,
                    calories=kcal,
                    protein=value("protein") or 0.0,
                    fat=value("fat") or 0.0,
                    carbs=value("carbs") or 0.0,
                    category=category,
                ))
        return products

    # ----- Стратегия 2: JSON, встроенный в <script> -----

    def extract_from_scripts(self, soup: BeautifulSoup, category: str) -> list:
        products = []
        for script in soup.find_all("script"):
            text = script.string or script.get_text()
            if not text:
                continue
            for raw in JSON_BLOCK_RE.findall(text):
                try:
                    data = json.loads(raw)
                except (json.JSONDecodeError, ValueError):
                    continue
                items = data if isinstance(data, list) else [data]
                for item in items:
                    if isinstance(item, dict):
                        product = self.product_from_dict(item, category)
                        if product:
                            products.append(product)
        return products

    def product_from_dict(self, item: dict, category: str) -> Optional[Product]:
        name = next(
            (v for k, v in item.items()
             if isinstance(v, str) and 2 <= len(v.strip()) <= 120 and matches(k, NAME_KEYWORDS)),
            None,
        )
        if not name:
            return None

        def number(key):
            for k, v in item.items():
                if matches(k, NUTRIENT_KEYWORDS[key]):
                    value = to_number(v)
                    if value is not None:
                        return value
            return None

        kcal = number("calories")
        if kcal is None or kcal < 0:
            return None
        return Product(
            name=name.strip(),
            calories=kcal,
            protein=number("protein") or 0.0,
            fat=number("fat") or 0.0,
            carbs=number("carbs") or 0.0,
            category=category,
        )

    def extract_any(self, soup: BeautifulSoup, category: str) -> list:
        found = self.extract_from_tables(soup, category)
        if not found:
            found = self.extract_from_scripts(soup, category)
        return found


def render_html(url: str, timeout_ms: int = 20000) -> Optional[BeautifulSoup]:
    """Стратегия 3 (опционально): дорисовываем JS headless-браузером.

    Нужна именно для pbprog.ru/tk/pm: там блюда появляются только после
    выполнения JavaScript, requests их не увидит никогда.
    """
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        log("Режим --render требует Playwright: pip install playwright && playwright install chromium")
        return None
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        try:
            page = browser.new_page(user_agent=random.choice(USER_AGENTS))
            page.goto(url, timeout=timeout_ms, wait_until="networkidle")
            time.sleep(1.5)  # даём JS дорисовать таблицы
            return BeautifulSoup(page.content(), "html.parser")
        except Exception as error:
            log(f"  ошибка рендера {url}: {error}")
            return None
        finally:
            browser.close()


def merge_duplicates(products: list) -> list:
    by_name = {}
    for product in products:
        key = product.name.strip().lower()
        if key and key not in by_name:
            by_name[key] = product
    return sorted(by_name.values(), key=lambda p: (p.category, p.name))


def fetch_page(url: str, scraper: Scraper, render: bool) -> Optional[BeautifulSoup]:
    return render_html(url) if render else scraper.fetch(url)


def main() -> None:
    parser = argparse.ArgumentParser(description="Скрапер базы продуктов pbprog.ru -> products_seed.json")
    parser.add_argument("--output", default="products_seed.json", help="куда сохранить результат")
    parser.add_argument("--start-url", default=START_URL)
    parser.add_argument("--delay", type=float, default=1.5, help="пауза между запросами, сек")
    parser.add_argument("--limit-collections", type=int, default=0, help="для теста: первые N коллекций")
    parser.add_argument("--render", action="store_true", help="рендерить страницы браузером (для JS-данных)")
    args = parser.parse_args()

    scraper = Scraper(delay=args.delay)

    log(f"Стартовая страница: {args.start_url}")
    soup = fetch_page(args.start_url, scraper, args.render)
    if soup is None:
        sys.exit("Не удалось загрузить стартовую страницу")

    collections = scraper.discover_collections(soup)
    if not collections:
        log("Коллекций на стартовой странице нет — пробуем индекс /tk/")
        index = fetch_page(FALLBACK_INDEX_URL, scraper, args.render)
        if index is not None:
            collections = scraper.discover_collections(index)
    if not collections:
        collections = [(args.start_url, "Продукты")]
    if args.limit_collections:
        collections = collections[:args.limit_collections]
    log(f"Коллекций к обходу: {len(collections)}")

    all_products = []
    for url, category in collections:
        log(f"→ {category} ({url})")
        try:
            page = fetch_page(url, scraper, args.render)
            if page is None:
                continue
            found = scraper.extract_any(page, category)
            log(f"   найдено продуктов: {len(found)}")
            all_products.extend(found)
        except Exception as error:  # одна категория не должна валить весь сбор
            log(f"   ОШИБКА обработки {url}: {error}")

    result = merge_duplicates(all_products)
    if not result:
        sys.exit(
            "Ничего не собрано. Данные, скорее всего, подгружаются JavaScript-ом: "
            "запустите с флагом --render. Если на сайте нужна авторизация — "
            "проверьте это в браузере и используйте другой открытый источник."
        )

    with open(args.output, "w", encoding="utf-8") as file:
        json.dump([asdict(product) for product in result], file, ensure_ascii=False, indent=2)

    log(f"Готово: {len(result)} уникальных продуктов -> {args.output}")


if __name__ == "__main__":
    main()
