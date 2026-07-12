"""Image upload utility with Cloudinary and local filesystem support."""

import os
import uuid

from fastapi import HTTPException, UploadFile

from config import settings

# Local upload directory
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")
MAX_FILE_SIZE = 5 * 1024 * 1024
IMAGE_TYPES = {
    "jpeg": {"mime": "image/jpeg", "extensions": {".jpg", ".jpeg"}},
    "png": {"mime": "image/png", "extensions": {".png"}},
    "webp": {"mime": "image/webp", "extensions": {".webp"}},
    "gif": {"mime": "image/gif", "extensions": {".gif"}},
}


def _detect_image_type(content: bytes) -> str | None:
    """根据文件头识别允许的图片格式。"""
    if content.startswith(b"\xff\xd8\xff"):
        return "jpeg"
    if content.startswith(b"\x89PNG\r\n\x1a\n"):
        return "png"
    if content.startswith((b"GIF87a", b"GIF89a")):
        return "gif"
    if len(content) >= 12 and content[:4] == b"RIFF" and content[8:12] == b"WEBP":
        return "webp"
    return None


async def validate_image_file(file: UploadFile) -> str:
    """校验图片大小、扩展名、MIME 和真实文件头，并复位文件指针。"""
    content = await file.read(MAX_FILE_SIZE + 1)
    try:
        if not content:
            raise HTTPException(status_code=400, detail="图片不能为空文件")
        if len(content) > MAX_FILE_SIZE:
            raise HTTPException(status_code=400, detail="文件大小超过限制（最大 5 MB）")
        image_type = _detect_image_type(content)
        if image_type is None:
            raise HTTPException(status_code=400, detail="文件内容不是支持的图片格式")
        expected = IMAGE_TYPES[image_type]
        extension = os.path.splitext(file.filename or "")[1].lower()
        if extension not in expected["extensions"] or file.content_type != expected["mime"]:
            raise HTTPException(status_code=400, detail="图片扩展名、MIME 与文件内容不一致")
        return image_type
    finally:
        await file.seek(0)


def _ensure_upload_dir() -> None:
    """Create the local uploads directory if it does not exist."""
    os.makedirs(UPLOAD_DIR, exist_ok=True)


async def upload_image(file: UploadFile) -> str:
    """Upload an image and return its accessible URL or path.

    If Cloudinary is configured, uploads to Cloudinary and returns the
    secure URL. Otherwise, saves to the local ``uploads/`` directory and
    returns a relative path.

    Args:
        file: The uploaded file from FastAPI.

    Returns:
        A URL string (Cloudinary) or a relative path string (local).
    """
    if settings.is_cloudinary_configured:
        return await _upload_to_cloudinary(file)
    return await _save_locally(file)


async def _upload_to_cloudinary(file: UploadFile) -> str:
    """Upload a file to Cloudinary and return the secure URL."""
    import cloudinary
    import cloudinary.uploader

    cloudinary.config(
        cloud_name=settings.CLOUDINARY_CLOUD_NAME,
        api_key=settings.CLOUDINARY_API_KEY,
        api_secret=settings.CLOUDINARY_API_SECRET,
    )

    # Read file content for upload
    content = await file.read()

    result = cloudinary.uploader.upload(
        content,
        folder="meal-app",
        resource_type="image",
    )

    return result.get("secure_url", "")


async def _save_locally(file: UploadFile) -> str:
    """Save a file to the local uploads directory and return the relative path."""
    _ensure_upload_dir()

    # Generate a unique filename to avoid collisions
    ext = os.path.splitext(file.filename or "image.jpg")[1]
    unique_name = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(UPLOAD_DIR, unique_name)

    content = await file.read()
    with open(file_path, "wb") as f:
        f.write(content)

    # Return a path relative to the backend root for URL construction
    return f"/uploads/{unique_name}"
