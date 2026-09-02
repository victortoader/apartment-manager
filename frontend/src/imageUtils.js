import heic2any from 'heic2any';

const MAX_DIMENSION = 1920;
const JPEG_QUALITY = 0.85;

export function isHeic(file) {
  const type = (file && (file.type || '').toLowerCase()) || '';
  if (type === 'image/heic' || type === 'image/heif') return true;
  const name = (file && file.name || '').toLowerCase();
  return name.endsWith('.heic') || name.endsWith('.heif');
}

async function loadImage(file) {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(file, { imageOrientation: 'from-image' });
    } catch (e) {
      // Some browsers fail createImageBitmap on certain images; fall through to <img>.
    }
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('Could not load image'));
    };
    img.src = url;
  });
}

export async function prepareImage(file) {
  if (!file) return null;

  let source = file;
  if (isHeic(file)) {
    try {
      const converted = await heic2any({
        blob: file,
        toType: 'image/jpeg',
        quality: JPEG_QUALITY
      });
      source = Array.isArray(converted) ? converted[0] : converted;
    } catch (e) {
      const err = new Error(`HEIC conversion failed: ${e && e.message ? e.message : e}`);
      err.isHeicConversionError = true;
      throw err;
    }
  }

  const image = await loadImage(source);

  let width = image.width || image.naturalWidth;
  let height = image.height || image.naturalHeight;

  if (!width || !height) {
    throw new Error('Could not read image dimensions');
  }

  const scale = Math.min(1, MAX_DIMENSION / Math.max(width, height));

  // If the image is already small and it is a normal (non-HEIC) source,
  // return it as-is to avoid unnecessary recompression.
  if (scale === 1 && !isHeic(file)) {
    return file;
  }

  const targetWidth = Math.round(width * scale);
  const targetHeight = Math.round(height * scale);

  const canvas = document.createElement('canvas');
  canvas.width = targetWidth;
  canvas.height = targetHeight;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(image, 0, 0, targetWidth, targetHeight);

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY));
  if (!blob) {
    throw new Error('Could not encode resized image');
  }

  const baseName = (file.name || 'photo').replace(/\.[^/.]+$/, '') || 'photo';
  return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg' });
}
