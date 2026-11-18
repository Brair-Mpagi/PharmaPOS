'use client';

import * as React from 'react';
import { Badge, type BadgeProps } from './Badge';
import { cn } from '../lib/utils';

/** Batch lifecycle statuses */
export type BatchStatus =
  | 'AVAILABLE'
  | 'QUARANTINED'
  | 'RECALLED'
  | 'EXPIRED'
  | 'DEPLETED';

/** Sale lifecycle statuses */
export type SaleStatus =
  | 'DRAFT'
  | 'PARKED'
  | 'FINALISED'
  | 'VOIDED'
  | 'RETURNED';

/** Payment statuses */
export type PaymentStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'FAILED'
  | 'REFUNDED';

type AllStatus = BatchStatus | SaleStatus | PaymentStatus;

const STATUS_CONFIG: Record<AllStatus, { label: string; variant: NonNullable<BadgeProps['variant']> }> = {
  // Batch statuses
  AVAILABLE:    { label: 'Available',    variant: 'success' },
  QUARANTINED:  { label: 'Quarantined',  variant: 'warning' },
  RECALLED:     { label: 'Recalled',     variant: 'destructive' },
  EXPIRED:      { label: 'Expired',      variant: 'destructive' },
  DEPLETED:     { label: 'Depleted',     variant: 'secondary' },
  // Sale statuses
  DRAFT:        { label: 'Draft',        variant: 'outline' },
  PARKED:       { label: 'Parked',       variant: 'info' },
  FINALISED:    { label: 'Finalised',    variant: 'success' },
  VOIDED:       { label: 'Voided',       variant: 'destructive' },
  RETURNED:     { label: 'Returned',     variant: 'warning' },
  // Payment statuses
  PENDING:      { label: 'Pending',      variant: 'warning' },
  CONFIRMED:    { label: 'Confirmed',    variant: 'success' },
  FAILED:       { label: 'Failed',       variant: 'destructive' },
  REFUNDED:     { label: 'Refunded',     variant: 'info' },
};

export interface StatusBadgeProps extends Omit<BadgeProps, 'variant'> {
  status: AllStatus;
}

function StatusBadge({ status, className, ...props }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? { label: status, variant: 'outline' as const };

  return (
    <Badge variant={config.variant} className={cn(className)} {...props}>
      {config.label}
    </Badge>
  );
}

export { StatusBadge };
