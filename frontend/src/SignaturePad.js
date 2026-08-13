import React, { useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';

function SignaturePad({ value, onChange, width = 320, height = 150 }) {
  const { t } = useTranslation();
  const canvasRef = useRef(null);
  const drawingRef = useRef(false);

  const getPos = (e) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    return {
      x: (e.clientX - rect.left) * (width / rect.width),
      y: (e.clientY - rect.top) * (height / rect.height)
    };
  };

  const redraw = useCallback((dataUrl) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, width, height);
    if (!dataUrl) return;
    const img = new Image();
    img.onload = () => ctx.drawImage(img, 0, 0, width, height);
    img.src = dataUrl;
  }, [width, height]);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const ratio = window.devicePixelRatio || 1;
    canvas.width = width * ratio;
    canvas.height = height * ratio;
    ctx.scale(ratio, ratio);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    redraw(value);
  }, [width, height, value, redraw]);

  const strokeStart = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.target.setPointerCapture && e.target.setPointerCapture(e.pointerId);
    drawingRef.current = true;
    const ctx = canvasRef.current.getContext('2d');
    const pos = getPos(e);
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = '#000';
    ctx.beginPath();
    ctx.moveTo(pos.x, pos.y);
  };

  const strokeMove = (e) => {
    if (!drawingRef.current) return;
    e.preventDefault();
    e.stopPropagation();
    const ctx = canvasRef.current.getContext('2d');
    const pos = getPos(e);
    ctx.lineTo(pos.x, pos.y);
    ctx.stroke();
  };

  const strokeEnd = (e) => {
    if (!drawingRef.current) return;
    e.preventDefault();
    e.stopPropagation();
    drawingRef.current = false;
    onChange(canvasRef.current.toDataURL('image/png'));
  };

  const clear = () => {
    const ctx = canvasRef.current.getContext('2d');
    ctx.clearRect(0, 0, width, height);
    onChange('');
  };

  return (
    <div className="signature-pad-wrap">
      <canvas
        ref={canvasRef}
        className="signature-pad"
        draggable={false}
        onPointerDown={strokeStart}
        onPointerMove={strokeMove}
        onPointerUp={strokeEnd}
        onPointerCancel={strokeEnd}
        onWheel={e => e.preventDefault()}
        onContextMenu={e => e.preventDefault()}
        onDragStart={e => e.preventDefault()}
      />
      <button type="button" className="btn-cancel small" onClick={clear}>{t('inspection.clearSignature')}</button>
    </div>
  );
}

export default SignaturePad;
