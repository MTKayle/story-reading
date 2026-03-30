import argparse
import json
import os
import random
import re
import sys
import time
import unicodedata
from typing import Dict, List, Optional
from urllib.parse import urljoin
from datetime import datetime

import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

try:
    import psycopg2
    from psycopg2.extras import execute_values
    PSYCOPG2_AVAILABLE = True
except ImportError:
    PSYCOPG2_AVAILABLE = False
    print("Warning: psycopg2 not available. Direct database connection disabled.")

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
WEB_ROOT = os.path.normpath(os.path.join(BASE_DIR, ".."))
PUBLIC_IMAGES_ROOT = os.path.join(WEB_ROOT, "services", "story-service", "public", "images")
DEFAULT_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
}

# Story Service API Configuration
STORY_SERVICE_URL = os.getenv("STORY_SERVICE_URL", "http://localhost:8083")
STORY_SERVICE_USER_ID = os.getenv("STORY_SERVICE_USER_ID", "1")  # Default user ID for crawler

# PostgreSQL Database Configuration (storydb)
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "storydb")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASSWORD = os.getenv("DB_PASSWORD", "postgres123")

def slugify(text):
    text = unicodedata.normalize('NFKD', text)
    text = text.encode('ascii', 'ignore').decode('utf-8')
    text = re.sub(r'[^\w\s-]', '', text).strip().lower()
    return re.sub(r'[-\s]+', '-', text)

def download_image(image_url, save_dir, filename=None):
    os.makedirs(save_dir, exist_ok=True)
    
    if filename is None:
        filename = image_url.split("/")[-1].split("?")[0]
    
    save_path = os.path.join(save_dir, filename)
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Referer": "https://truyenqqgo.com/"
    }
    
    try:
        time.sleep(random.uniform(2, 3))  # Tăng thời gian delay giữa các yêu cầu
        response = requests.get(image_url, headers=headers, stream=True, timeout=10)
        
        if response.status_code == 200:
            with open(save_path, "wb") as f:
                for chunk in response.iter_content(1024):
                    f.write(chunk)
            return save_path
    except Exception as e:
        print(f"Lỗi khi tải ảnh: {e}")
    return None

def download_chapter_images(chapter_url: str, save_dir: str) -> List[str]:
    os.makedirs(save_dir, exist_ok=True)
    print(f"Đang tải ảnh cho chương: {chapter_url}")
    try:
        time.sleep(random.uniform(1.5, 2.5))
        response = requests.get(chapter_url, headers={**DEFAULT_HEADERS, "Referer": chapter_url}, timeout=15)
        response.raise_for_status()
    except requests.RequestException as e:
        print(f"Lỗi khi truy cập chương {chapter_url}: {e}")
        return []

    soup = BeautifulSoup(response.content, "html.parser")
    image_tags = soup.select(".chapter-content img, .chapter_content img, #chapter-content img")
    saved_paths = []

    session = requests.Session()
    session.headers.update(DEFAULT_HEADERS)
    session.headers["Referer"] = chapter_url

    for index, img_tag in enumerate(image_tags, start=1):
        src = img_tag.get("data-src") or img_tag.get("src")
        if not src:
            continue
        if src.startswith("//"):
            src = "https:" + src
        elif not src.startswith("http"):
            src = urljoin(chapter_url, src)

        filename = f"{index:03}.jpg"
        save_path = os.path.join(save_dir, filename)

        try:
            time.sleep(random.uniform(1.0, 2.0))
            img_response = session.get(src, stream=True, timeout=15)
            img_response.raise_for_status()
            with open(save_path, "wb") as img_file:
                for chunk in img_response.iter_content(1024):
                    img_file.write(chunk)
            saved_paths.append(save_path)
            print(f"  Đã lưu ảnh: {save_path}")
        except Exception as e:
            print(f"  Lỗi khi tải ảnh {src}: {e}")

    print(f"Hoàn tất {len(saved_paths)} ảnh cho chương: {chapter_url}")
    return saved_paths

def create_story_via_api(title: str, description: str, genres: List[str], cover_image_id: str, 
                         service_url: str = None, user_id: str = None) -> Optional[Dict]:
    """Gọi API story-service để tạo story mới"""
    service_url = service_url or STORY_SERVICE_URL
    user_id = user_id or STORY_SERVICE_USER_ID
    url = f"{service_url}/api/story"
    headers = {
        "Content-Type": "application/json",
        "X-User-Id": user_id
    }
    payload = {
        "title": title,
        "description": description or "",
        "genres": genres or [],
        "coverImageId": cover_image_id,
        "paid": False,
        "price": 0
    }
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=30)
        response.raise_for_status()
        story_data = response.json()
        print(f"✓ Đã tạo story: {story_data.get('id')} - {story_data.get('title')}")
        return story_data
    except Exception as e:
        print(f"✗ Lỗi khi tạo story: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"  Response: {e.response.text}")
        return None

def create_chapter_via_api(story_id: int, chapter_number: int, title: str, image_ids: List[str],
                           service_url: str = None, user_id: str = None) -> Optional[Dict]:
    """Gọi API story-service để tạo chapter mới"""
    service_url = service_url or STORY_SERVICE_URL
    user_id = user_id or STORY_SERVICE_USER_ID
    url = f"{service_url}/api/story/{story_id}/chapters"
    headers = {
        "Content-Type": "application/json",
        "X-User-Id": user_id
    }
    payload = {
        "chapterNumber": int(chapter_number) if chapter_number == int(chapter_number) else chapter_number,
        "title": title,
        "imageIds": image_ids
    }
    
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=30)
        response.raise_for_status()
        chapter_data = response.json()
        print(f"  ✓ Đã tạo chapter {chapter_number}: {chapter_data.get('id')}")
        return chapter_data
    except Exception as e:
        print(f"  ✗ Lỗi khi tạo chapter {chapter_number}: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"    Response: {e.response.text}")
        return None

def get_db_connection():
    """Kết nối đến PostgreSQL database storydb"""
    if not PSYCOPG2_AVAILABLE:
        return None
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD
        )
        return conn
    except Exception as e:
        print(f"✗ Lỗi kết nối database: {e}")
        return None

def create_story_in_db(title: str, description: str, genres: List[str], cover_image_id: str, 
                      author: str = None) -> Optional[int]:
    """Lưu story trực tiếp vào database PostgreSQL"""
    if not PSYCOPG2_AVAILABLE:
        return None
    
    conn = get_db_connection()
    if not conn:
        return None
    
    try:
        cursor = conn.cursor()
        genres_str = ",".join(genres) if genres else None
        author = author or STORY_SERVICE_USER_ID
        now = datetime.now()
        
        # Insert vào bảng stories
        cursor.execute("""
            INSERT INTO stories (title, description, genres, cover_image_id, author, is_paid, price, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING id
        """, (title, description, genres_str, cover_image_id, author, False, 0, now, now))
        
        story_id = cursor.fetchone()[0]
        conn.commit()
        print(f"✓ Đã lưu story vào database: ID={story_id} - {title}")
        return story_id
    except Exception as e:
        conn.rollback()
        print(f"✗ Lỗi khi lưu story vào database: {e}")
        return None
    finally:
        cursor.close()
        conn.close()

def create_chapter_in_db(story_id: int, chapter_number: int, title: str, image_ids: List[str]) -> Optional[int]:
    """Lưu chapter trực tiếp vào database PostgreSQL"""
    if not PSYCOPG2_AVAILABLE:
        return None
    
    conn = get_db_connection()
    if not conn:
        return None
    
    try:
        cursor = conn.cursor()
        image_ids_str = ",".join(image_ids) if image_ids else None
        now = datetime.now()
        
        # Kiểm tra xem chapter đã tồn tại chưa
        cursor.execute("""
            SELECT id FROM chapters 
            WHERE story_id = %s AND chapter_number = %s
        """, (story_id, int(chapter_number)))
        
        existing = cursor.fetchone()
        if existing:
            # Update chapter đã tồn tại
            cursor.execute("""
                UPDATE chapters 
                SET title = %s, image_ids = %s, updated_at = %s
                WHERE id = %s
            """, (title, image_ids_str, now, existing[0]))
            chapter_id = existing[0]
            print(f"  ✓ Đã cập nhật chapter {chapter_number}: ID={chapter_id}")
        else:
            # Insert chapter mới
            cursor.execute("""
                INSERT INTO chapters (story_id, chapter_number, title, image_ids, created_at)
                VALUES (%s, %s, %s, %s, %s)
                RETURNING id
            """, (story_id, int(chapter_number), title, image_ids_str, now))
            
            chapter_id = cursor.fetchone()[0]
            print(f"  ✓ Đã lưu chapter {chapter_number} vào database: ID={chapter_id}")
        
        conn.commit()
        return chapter_id
    except Exception as e:
        conn.rollback()
        print(f"  ✗ Lỗi khi lưu chapter {chapter_number} vào database: {e}")
        return None
    finally:
        cursor.close()
        conn.close()


def sanitize_genre_name(name: str) -> Optional[str]:
    if not name:
        return None
    cleaned = name.strip()
    return cleaned or None


def ensure_genres_via_api(genres: List[str], service_url: str = None):
    normalized = [sanitize_genre_name(g) for g in genres]
    normalized = [g for g in normalized if g]
    if not normalized:
        return

    service_url = (service_url or STORY_SERVICE_URL).rstrip("/")
    endpoint = f"{service_url}/api/story/admin/genres"
    existing = {}

    try:
        response = requests.get(endpoint, timeout=15)
        response.raise_for_status()
        data = response.json()
        if isinstance(data, list):
            for item in data:
                key = (item.get("name") or "").strip().lower()
                if key:
                    existing[key] = True
    except Exception as e:
        print(f"⚠ Không thể tải danh sách thể loại từ API: {e}")

    for name in normalized:
        key = name.lower()
        if existing.get(key):
            continue
        payload = {"name": name}
        try:
            response = requests.post(endpoint, json=payload, timeout=15)
            response.raise_for_status()
            created = response.json()
            created_name = (created.get("name") or "").strip().lower()
            if created_name:
                existing[created_name] = True
            print(f"✓ Đã thêm thể loại mới qua API: {name}")
        except Exception as e:
            print(f"⚠ Không thể thêm thể loại '{name}' qua API: {e}")


def _generate_unique_slug_db(cursor, base_slug: str) -> str:
    if not base_slug:
        base_slug = "genre"
    candidate = base_slug
    counter = 1
    while True:
        cursor.execute("SELECT 1 FROM genres WHERE slug = %s", (candidate,))
        if cursor.fetchone() is None:
            return candidate
        candidate = f"{base_slug}-{counter}"
        counter += 1


def ensure_genres_in_db(genres: List[str]):
    if not PSYCOPG2_AVAILABLE:
        return
    normalized = [sanitize_genre_name(g) for g in genres]
    normalized = [g for g in normalized if g]
    if not normalized:
        return

    conn = get_db_connection()
    if not conn:
        return

    try:
        cursor = conn.cursor()
        for name in normalized:
            cursor.execute("SELECT 1 FROM genres WHERE LOWER(name) = LOWER(%s)", (name,))
            if cursor.fetchone():
                continue
            slug = _generate_unique_slug_db(cursor, slugify(name))
            now = datetime.now()
            cursor.execute(
                """
                INSERT INTO genres (name, slug, description, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (name, slug, None, now, now)
            )
            print(f"✓ Đã thêm thể loại mới vào bảng genres: {name}")
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"⚠ Lỗi khi đồng bộ thể loại vào bảng genres: {e}")
    finally:
        cursor.close()
        conn.close()

def print_comic_summary(comic_data: Dict):
    print("\n--- Thông tin truyện đã crawl ---")
    for key, value in comic_data.items():
        if isinstance(value, list):
            print(f"{key}:")
            for item in value:
                if isinstance(item, dict):
                    summary = ", ".join(f"{k}={v}" for k, v in item.items())
                    print(f"  - {summary}")
                else:
                    print(f"  - {item}")
        else:
            print(f"{key}: {value}")



def get_info_by_icon(soup, icon_class):
    items = soup.select("ul.list-info li.row")
    for item in items:
        icon = item.select_one("i")
        if icon and icon_class in icon["class"]:
            value_tag = item.select_one("p.col-xs-9")
            if value_tag:
                return value_tag.text.strip()
    return "Đang cập nhật"

def crawl_comic_chapters(comic_url: str) -> List[Dict]:
    session = requests.Session()
    retries = Retry(total=5, backoff_factor=0.3, status_forcelist=[429, 500, 502, 503, 504])
    session.mount('https://', HTTPAdapter(max_retries=retries))
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    try:
        response = session.get(comic_url, headers=headers)
        response.raise_for_status()
    except requests.exceptions.RequestException as e:
        print(f"Lỗi khi truy cập URL truyện: {e}")
        return []

    soup = BeautifulSoup(response.content, "html.parser")
    chapters = []
    
    # Lấy danh sách chương theo cấu trúc HTML
    chapter_items = soup.select("div.list_chapter div.works-chapter-item")
    
    for item in chapter_items:
        chapter_link = item.select_one("div.name-chap a")
        if not chapter_link:
            continue
            
        chapter_url = urljoin(comic_url, chapter_link["href"])
        chapter_title = chapter_link.text.strip()
        
        # Trích xuất số chương từ tiêu đề (vd: "Chương 13" -> 13)
        chapter_number_match = re.search(r'Chương (\d+(?:\.\d+)?)', chapter_title, re.IGNORECASE)
        chapter_number = float(chapter_number_match.group(1)) if chapter_number_match else 0

        
        chapters.append({
            "chapter_number": chapter_number,
            "title": chapter_title,
            "url": chapter_url
        })
    
    return chapters

def crawl_truyen_info(url: str, crawl_chapters: bool = True, skip_api: bool = False,
                      service_url: str = None, user_id: str = None, use_db: bool = False) -> Optional[Dict]:
    slug = slugify(url.split("/")[-1])
    save_directory = os.path.join(PUBLIC_IMAGES_ROOT, slug)
    os.makedirs(save_directory, exist_ok=True)
    session = requests.Session()
    retries = Retry(total=5, backoff_factor=0.3, status_forcelist=[429, 500, 502, 503, 504])
    session.mount('https://', HTTPAdapter(max_retries=retries))
    
    try:
        response = session.get(url, headers=DEFAULT_HEADERS)
        response.raise_for_status()
    except requests.exceptions.RequestException as e:
        print(f"Lỗi khi truy cập URL: {e}")
        return None

    soup = BeautifulSoup(response.content, "html.parser")
    
    try:
        # Lấy thông tin cơ bản của truyện
        title = soup.select_one("div.book_other h1[itemprop='name']").text.strip()
        cover = soup.select_one("div.book_avatar img[itemprop='image']")["src"]
        
        # Sửa URL ảnh bìa nếu cần
        if cover.startswith("//"):
            cover = "https:" + cover
        elif not cover.startswith("http"):
            cover = urljoin(url, cover)
            
        author = get_info_by_icon(soup, "fa-user")
        status = get_info_by_icon(soup, "fa-rss")
        likes = get_info_by_icon(soup, "fa-thumbs-up")
        follows = get_info_by_icon(soup, "fa-heart")
        views = get_info_by_icon(soup, "fa-eye")
        genres = [a.text.strip() for a in soup.select("ul.list01 li.li03 a")]
        genres = [g for g in genres if g]

        service_url = service_url or STORY_SERVICE_URL
        user_id = user_id or STORY_SERVICE_USER_ID
        if genres:
            if use_db and PSYCOPG2_AVAILABLE:
                ensure_genres_in_db(genres)
            else:
                ensure_genres_via_api(genres, service_url)

        # Tải ảnh bìa - lưu với tên "cover.jpg" để khớp với story-service
        cover_path = download_image(cover, save_directory, "cover.jpg")
        # URL path để khớp với story-service: /public/images/{slug}/cover.jpg
        cover_relative = f"/public/images/{slug}/cover.jpg" if cover_path else cover

        comic_data = {
            "Tên truyện": title,
            "Ảnh bìa": cover_relative or cover,
            "Tác giả": author,
            "Tình trạng": status,
            "Lượt xem": views,
            "Lượt theo dõi": follows,
            "Thể loại": genres,
            "Thư mục lưu": os.path.relpath(save_directory, WEB_ROOT) if os.path.isabs(save_directory) else save_directory
        }
        if likes:
            comic_data["Lượt thích"] = likes

        # Tạo story trong database TRƯỚC khi crawl chapters (để có thể lưu từng chapter ngay)
        story_id = None
        
        if not skip_api:
            if use_db and PSYCOPG2_AVAILABLE:
                # Lưu trực tiếp vào database PostgreSQL
                print("\n--- Đang tạo story trong database storydb ---")
                story_id = create_story_in_db(
                    title=title,
                    description=status or "",
                    genres=genres,
                    cover_image_id=cover_relative,
                    author=user_id
                )
                
                if story_id:
                    comic_data["Story ID"] = story_id
                    print(f"✓ Đã tạo story ID={story_id} trong database, bắt đầu crawl chapters...")
                else:
                    print("⚠ Không thể tạo story trong database, thử qua API...")
                    use_db = False  # Fallback to API
            
            if not use_db or not story_id:
                # Lưu qua API story-service
                print("\n--- Đang tạo story qua API story-service ---")
                story_response = create_story_via_api(
                    title=title,
                    description=status or "",
                    genres=genres,
                    cover_image_id=cover_relative,
                    service_url=service_url,
                    user_id=user_id
                )
                
                if story_response:
                    story_id = story_response.get("id")
                    comic_data["Story ID"] = story_id
                    print(f"✓ Đã tạo story ID={story_id} qua API, bắt đầu crawl chapters...")
                else:
                    print("⚠ Không thể tạo story qua API, chỉ lưu file local (không lưu vào database)")

        # Crawl chapters và lưu vào database NGAY SAU MỖI CHAPTER
        chapters_data: List[Dict] = []
        if crawl_chapters:
            chapters = crawl_comic_chapters(url)
            total_chapters = len(chapters)
            print(f"\n--- Bắt đầu crawl {total_chapters} chapters ---")
            
            for idx, chapter in enumerate(chapters, start=1):
                chapter_number = chapter["chapter_number"]
                chapter_title = chapter["title"]
                print(f"\n[{idx}/{total_chapters}] Đang crawl chương {chapter_number}: {chapter_title}")
                
                # Sử dụng số chương trực tiếp (không có prefix "chapter_") để khớp với story-service
                chapter_num_str = str(int(chapter_number)) if chapter_number == int(chapter_number) else str(chapter_number)
                chapter_dir = os.path.join(save_directory, chapter_num_str)
                image_paths = download_chapter_images(chapter["url"], chapter_dir)
                
                # Tạo URL paths để khớp với story-service: /public/images/{slug}/{chapterNumber}/{filename}
                image_rel_paths = []
                for path_idx, path in enumerate(image_paths, start=1):
                    filename = os.path.basename(path)
                    url_path = f"/public/images/{slug}/{chapter_num_str}/{filename}"
                    image_rel_paths.append(url_path)
                
                chapter_info = {
                    "Số chương": chapter_number,
                    "Tiêu đề": chapter_title,
                    "URL": chapter["url"],
                    "Thư mục": os.path.relpath(chapter_dir, WEB_ROOT) if os.path.isabs(chapter_dir) else chapter_dir,
                    "Ảnh đã tải": image_rel_paths
                }
                chapters_data.append(chapter_info)
                
                # Lưu chapter vào database NGAY SAU KHI crawl xong
                if story_id and not skip_api:
                    if use_db and PSYCOPG2_AVAILABLE:
                        create_chapter_in_db(
                            story_id=story_id,
                            chapter_number=chapter_number,
                            title=chapter_title,
                            image_ids=image_rel_paths
                        )
                    else:
                        create_chapter_via_api(
                            story_id=story_id,
                            chapter_number=chapter_number,
                            title=chapter_title,
                            image_ids=image_rel_paths,
                            service_url=service_url,
                            user_id=user_id
                        )
                    print(f"  ✓ Đã lưu chương {chapter_number} vào database")
                else:
                    print(f"  ✓ Đã crawl chương {chapter_number} ({len(image_rel_paths)} ảnh)")

        if chapters_data:
            comic_data["Danh sách chương"] = chapters_data
            if story_id:
                print(f"\n✓ Hoàn tất crawl {len(chapters_data)} chapters cho story ID={story_id}")

        info_path = os.path.join(save_directory, "info.json")
        try:
            with open(info_path, "w", encoding="utf-8") as info_file:
                json.dump(
                    {k: v for k, v in comic_data.items() if k != "Thư mục lưu"},
                    info_file,
                    ensure_ascii=False,
                    indent=2
                )
            comic_data["Thông tin lưu"] = os.path.relpath(info_path, WEB_ROOT) if os.path.isabs(info_path) else info_path
        except Exception as e:
            print(f"Lỗi khi lưu thông tin truyện: {e}")

        return comic_data
    except Exception as e:
        print(f"Lỗi khi lấy thông tin truyện: {e}")
        return None


def parse_args():
    parser = argparse.ArgumentParser(description="Crawl thông tin truyện từ truyenqq.")
    parser.add_argument("--url", help="Đường dẫn truyện cần crawl")
    parser.add_argument(
        "--skip-chapters",
        action="store_true",
        help="Bỏ qua việc tải ảnh các chương"
    )
    parser.add_argument(
        "--skip-api",
        action="store_true",
        help="Bỏ qua việc gọi API story-service (chỉ crawl và lưu file)"
    )
    parser.add_argument(
        "--user-id",
        default=STORY_SERVICE_USER_ID,
        help=f"User ID để tạo story (mặc định: {STORY_SERVICE_USER_ID})"
    )
    parser.add_argument(
        "--story-service-url",
        default=STORY_SERVICE_URL,
        help=f"URL của story-service (mặc định: {STORY_SERVICE_URL})"
    )
    parser.add_argument(
        "--use-db",
        action="store_true",
        help="Lưu trực tiếp vào database PostgreSQL thay vì qua API"
    )
    parser.add_argument(
        "--db-host",
        default=DB_HOST,
        help=f"PostgreSQL host (mặc định: {DB_HOST})"
    )
    parser.add_argument(
        "--db-port",
        default=DB_PORT,
        help=f"PostgreSQL port (mặc định: {DB_PORT})"
    )
    parser.add_argument(
        "--db-name",
        default=DB_NAME,
        help=f"Database name (mặc định: {DB_NAME})"
    )
    parser.add_argument(
        "--db-user",
        default=DB_USER,
        help=f"Database user (mặc định: {DB_USER})"
    )
    parser.add_argument(
        "--db-password",
        default=DB_PASSWORD,
        help=f"Database password (mặc định: {DB_PASSWORD})"
    )
    return parser.parse_args()


def main():
    global STORY_SERVICE_URL, STORY_SERVICE_USER_ID, DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    args = parse_args()
    url = args.url

    if not url:
        print("Vui lòng cung cấp URL truyện bằng tham số --url.", file=sys.stderr)
        sys.exit(1)

    # Cập nhật config từ args
    STORY_SERVICE_URL = args.story_service_url
    STORY_SERVICE_USER_ID = args.user_id
    DB_HOST = args.db_host
    DB_PORT = args.db_port
    DB_NAME = args.db_name
    DB_USER = args.db_user
    DB_PASSWORD = args.db_password

    print(f"Bắt đầu crawl truyện từ URL: {url}")
    if args.use_db:
        print(f"Database: {DB_HOST}:{DB_PORT}/{DB_NAME} (user: {DB_USER})")
    else:
        print(f"Story Service URL: {STORY_SERVICE_URL}")
        print(f"User ID: {STORY_SERVICE_USER_ID}")
    if args.skip_api:
        print("⚠ Bỏ qua việc lưu vào database (--skip-api)")
    
    data = crawl_truyen_info(
        url, 
        crawl_chapters=not args.skip_chapters, 
        skip_api=args.skip_api,
        service_url=STORY_SERVICE_URL,
        user_id=STORY_SERVICE_USER_ID,
        use_db=args.use_db
    )

    if data:
        
        print_comic_summary(data)
        info_path = data.get("Thông tin lưu")
        if info_path:
            print(f"\nFile thông tin: {info_path}")
        sys.exit(0)

    print("Không thể crawl dữ liệu từ URL đã cho.", file=sys.stderr)
    sys.exit(2)


if __name__ == "__main__":
    main()