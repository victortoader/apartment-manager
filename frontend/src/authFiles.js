import React, { useEffect, useRef, useState } from 'react';

// Files behind a login can't be loaded with a plain <img src> or <a href>, because the
// browser doesn't send the Authorization header. These helpers fetch the file with the
// header and hand the page a temporary blob: URL instead.

export async function fetchFileUrl(url, headers) {
  const res = await fetch(url, { headers });
  if (!res.ok) return null;
  return URL.createObjectURL(await res.blob());
}

// Saves a protected file under fileName. The blob URL is released a little later, so the
// browser has time to start the download.
export async function downloadAuthFile(url, headers, fileName) {
  try {
    const blobUrl = await fetchFileUrl(url, headers);
    if (!blobUrl) return;
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = fileName || '';
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(blobUrl), 10000);
  } catch (err) {
    console.error('Download failed:', err);
  }
}

// Loads a protected file once per src and returns its blob URL (null until loaded).
// The URL is released when the component unmounts or src changes.
function useAuthFileUrl(src, headers) {
  const [blobUrl, setBlobUrl] = useState(null);
  const headersRef = useRef(headers);
  headersRef.current = headers;

  useEffect(() => {
    let active = true;
    let created = null;
    setBlobUrl(null);
    fetchFileUrl(src, headersRef.current)
      .then((u) => {
        if (active) {
          created = u;
          setBlobUrl(u);
        } else if (u) {
          URL.revokeObjectURL(u);
        }
      })
      .catch((err) => console.error('File load failed:', err));
    return () => {
      active = false;
      if (created) URL.revokeObjectURL(created);
    };
  }, [src]);

  return blobUrl;
}

// <img> for a protected image.
export function AuthImage({ src, headers, alt, ...props }) {
  const blobUrl = useAuthFileUrl(src, headers);
  if (!blobUrl) return <span className={props.className} aria-label={alt} />;
  return <img src={blobUrl} alt={alt} {...props} />;
}

// Protected image thumbnail that opens the full image in a new tab when clicked. It opens the
// already-loaded blob URL inside the click handler, so popup blockers allow it and the image
// isn't downloaded twice.
export function AuthImageButton({ src, headers, alt, className, buttonClassName }) {
  const blobUrl = useAuthFileUrl(src, headers);
  return (
    <button
      type="button"
      className={buttonClassName}
      disabled={!blobUrl}
      onClick={() => blobUrl && window.open(blobUrl, '_blank', 'noopener,noreferrer')}
      aria-label={alt}
    >
      {blobUrl ? <img src={blobUrl} alt={alt} className={className} /> : <span className={className} />}
    </button>
  );
}
