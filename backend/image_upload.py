"""Image upload utility with Cloudinary and local filesystem support."""

import os
import uuid

from fastapi import UploadFile

from config import settings

# Local upload directory
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")


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
