"""Image upload route."""

import os

from fastapi import APIRouter, Depends, HTTPException, UploadFile

from image_upload import upload_image
from middleware import get_current_user
from models import User
from schemas import ApiResponse

router = APIRouter()

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}
ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
MAX_FILE_SIZE = 5 * 1024 * 1024  # 5MB


@router.post("/upload/image")
async def upload(
    file: UploadFile,
    current_user: User = Depends(get_current_user),
):
    """Upload an image file and return its URL."""
    # Validate content type
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=400,
            detail="不支持的文件类型，仅允许 jpg/jpeg/png/webp/gif",
        )

    # Validate extension
    ext = os.path.splitext(file.filename or "")[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail="不支持的文件扩展名，仅允许 .jpg/.jpeg/.png/.webp/.gif",
        )

    # Validate file size
    content = await file.read()
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail="文件大小超过限制（最大5MB）",
        )
    # Reset file position for downstream processing
    await file.seek(0)

    url = await upload_image(file)
    return ApiResponse(data={"url": url})
