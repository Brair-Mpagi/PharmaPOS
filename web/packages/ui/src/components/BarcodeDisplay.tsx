'use client';

import * as React from 'react';
import { Copy, Check } from 'lucide-react';
import { cn } from '../lib/utils';
import { Button } from './Button';

export interface BarcodeDisplayProps {
  value: string;
  label?: string;
  className?: string;
}

function BarcodeDisplay({ value, label, className }: BarcodeDisplayProps) {
  const [copied, setCopied] = React.useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback for environments without clipboard API
      const textArea = document.createElement('textarea');
      textArea.value = value;
      textArea.style.position = 'fixed';
      textArea.style.left = '-9999px';
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className={cn('flex items-center gap-2', className)}>
      {label && <span className="text-sm text-muted-foreground">{label}</span>}
      <code className="rounded bg-muted px-2 py-1 font-mono text-sm tracking-widest">{value}</code>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="h-7 w-7"
        onClick={handleCopy}
        aria-label={copied ? 'Copied' : `Copy barcode ${value}`}
      >
        {copied ? <Check className="h-3.5 w-3.5 text-green-600" /> : <Copy className="h-3.5 w-3.5" />}
      </Button>
    </div>
  );
}

export { BarcodeDisplay };
