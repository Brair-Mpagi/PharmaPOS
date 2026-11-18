import { z } from 'zod';

/**
 * ISO 4217 currency codes supported by the system.
 * UGX is the primary currency (zero-decimal).
 */
export const CurrencyCodeSchema = z.enum(['UGX', 'KES', 'USD', 'EUR', 'GBP', 'TZS', 'RWF']);
export type CurrencyCode = z.infer<typeof CurrencyCodeSchema>;

/**
 * Money schema — amounts are stored in minor units (smallest indivisible currency unit).
 * For UGX (zero-decimal), 50000 minor = 50,000 UGX.
 * For USD (two-decimal), 1050 minor = $10.50.
 */
export const MoneySchema = z.object({
  /** Amount in minor units (cents / smallest unit) */
  amountMinor: z.number().int({ message: 'Amount must be a whole number of minor units' }),
  /** ISO 4217 currency code */
  currency: CurrencyCodeSchema,
});
export type Money = z.infer<typeof MoneySchema>;

/**
 * Positive money — amounts that must be > 0 (prices, payments, totals).
 */
export const PositiveMoneySchema = MoneySchema.extend({
  amountMinor: z
    .number()
    .int({ message: 'Amount must be a whole number of minor units' })
    .positive({ message: 'Amount must be positive' }),
});
export type PositiveMoney = z.infer<typeof PositiveMoneySchema>;

/**
 * Non-negative money — amounts that can be zero (discounts, adjustments).
 */
export const NonNegativeMoneySchema = MoneySchema.extend({
  amountMinor: z
    .number()
    .int({ message: 'Amount must be a whole number of minor units' })
    .nonnegative({ message: 'Amount must not be negative' }),
});
export type NonNegativeMoney = z.infer<typeof NonNegativeMoneySchema>;
