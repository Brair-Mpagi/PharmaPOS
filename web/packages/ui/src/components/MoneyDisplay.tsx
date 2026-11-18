'use client';

import * as React from 'react';
import { cn } from '../lib/utils';

/**
 * Formats a money amount stored in minor units (e.g., cents) into a human-readable string.
 * UGX (Ugandan Shilling) is a zero-decimal currency — displayed as whole numbers only.
 */

/** Map of ISO 4217 currencies to their minor-unit exponent */
const CURRENCY_DECIMALS: Record<string, number> = {
  UGX: 0,
  KES: 2,
  USD: 2,
  EUR: 2,
  GBP: 2,
  TZS: 2,
  RWF: 0,
  JPY: 0,
};

function formatMoney(amountMinor: number, currency: string): string {
  const decimals = CURRENCY_DECIMALS[currency] ?? 2;
  const divisor = Math.pow(10, decimals);
  const amount = amountMinor / divisor;

  return new Intl.NumberFormat('en-UG', {
    style: 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(amount);
}

export interface MoneyDisplayProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Amount in minor units (e.g. 50000 UGX = 50,000 shillings, 1050 USD = $10.50) */
  amountMinor: number;
  /** ISO 4217 currency code */
  currency: string;
  /** Show positive amounts with a "+" sign */
  showSign?: boolean;
  /** Additional CSS class names */
  className?: string;
}

function MoneyDisplay({ amountMinor, currency, showSign = false, className, ...props }: MoneyDisplayProps) {
  const formatted = formatMoney(Math.abs(amountMinor), currency);
  const isNegative = amountMinor < 0;
  const sign = isNegative ? '−' : showSign && amountMinor > 0 ? '+' : '';

  return (
    <span
      className={cn(
        'tabular-nums font-medium',
        isNegative && 'text-destructive',
        className,
      )}
      aria-label={`${sign}${formatted}`}
      {...props}
    >
      {sign}{formatted}
    </span>
  );
}

export { MoneyDisplay, formatMoney, CURRENCY_DECIMALS };
