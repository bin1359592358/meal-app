"""Image upload route."""

from fastapi import APIRouter, Depends, UploadFile

from image_upload import upload_image
from middleware import get_current_user
from models import User
from schemas import ApiResponse

router = APIRouter()


@router.post("/upload/image")
async def upload(
    file: UploadFile,
    current_user: User = Depends(get_current_user),
):
    """Upload an image file and return its URL."""
    url = await upload_image(file)
    return ApiResponse(data={"url": url})
