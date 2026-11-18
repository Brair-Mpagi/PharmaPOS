'use client';

import * as React from 'react';
import { Minus, Plus } from 'lucide-react';
import { cn } from '../lib/utils';
import { Button } from './Button';

export interface QuantityInputProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  unit?: string;
  disabled?: boolean;
  className?: string;
  label?: string;
}

function QuantityInput({
  value,
  onChange,
  min = 0,
  max = 999999,
  step = 1,
  unit,
  disabled = false,
  className,
  label = 'Quantity',
}: QuantityInputProps) {
  const handleDecrement = () => {
    const next = value - step;
    if (next >= min) onChange(next);
  };

  const handleIncrement = () => {
    const next = value + step;
    if (next <= max) onChange(next);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    if (raw === '') {
      onChange(min);
      return;
    }
    const parsed = Number.parseFloat(raw);
    if (!Number.isNaN(parsed)) {
      const clamped = Math.min(Math.max(parsed, min), max);
      onChange(clamped);
    }
  };

  return (
    <div className={cn('flex items-center gap-1', className)}>
      <Button
        type="button"
        variant="outline"
        size="icon"
        className="h-8 w-8"
        onClick={handleDecrement}
        disabled={disabled || value <= min}
        aria-label={`Decrease ${label}`}
      >
        <Minus className="h-3 w-3" />
      </Button>

      <div className="relative">
        <input
          type="number"
          value={value}
          onChange={handleInputChange}
          min={min}
          max={max}
          step={step}
          disabled={disabled}
          aria-label={label}
          className={cn(
            'h-8 w-16 rounded-md border border-input bg-background px-2 text-center text-sm tabular-nums',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            'disabled:cursor-not-allowed disabled:opacity-50',
            '[appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none',
            unit && 'pr-8',
          )}
        />
        {unit && (
          <span className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-xs text-muted-foreground">
            {unit}
          </span>
        )}
      </div>

      <Button
        type="button"
        variant="outline"
        size="icon"
        className="h-8 w-8"
        onClick={handleIncrement}
        disabled={disabled || value >= max}
        aria-label={`Increase ${label}`}
      >
        <Plus className="h-3 w-3" />
      </Button>
    </div>
  );
}

export { QuantityInput };
