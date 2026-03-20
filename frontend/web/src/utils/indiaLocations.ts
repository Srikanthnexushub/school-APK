// India States and Districts utility
// Comprehensive data from india-state-district package (772 districts across 36 states/UTs)
// Real-time pincode lookup via India Post API (free, no auth)

import { getAllStates, getDistricts as getDistrictsByCode } from 'india-state-district';

// Build state list from package
const _allStates = getAllStates(); // [{code, name}, ...]

export const INDIA_STATES: string[] = _allStates.map((s) => s.name);

// Build districts map: state name → district list
export const DISTRICTS_BY_STATE: Record<string, string[]> = Object.fromEntries(
  _allStates.map((s) => [s.name, getDistrictsByCode(s.code) as string[]])
);

/** Return the full list of Indian states / UTs */
export function getStates(): string[] {
  return INDIA_STATES;
}

/** Return all districts for a given state name, or empty array if not found */
export function getDistricts(state: string): string[] {
  return DISTRICTS_BY_STATE[state] ?? [];
}

/**
 * Suggest states whose names include the given input (case-insensitive).
 * Returns up to 8 matches.
 */
export function suggestStates(input: string): string[] {
  if (!input) return INDIA_STATES.slice(0, 8);
  const q = input.toLowerCase();
  return INDIA_STATES.filter((s) => s.toLowerCase().includes(q)).slice(0, 8);
}

/**
 * Suggest districts for a given state whose names include the given input (case-insensitive).
 * Returns up to 8 matches.
 */
export function suggestDistricts(state: string, input: string): string[] {
  const districts = DISTRICTS_BY_STATE[state] ?? [];
  if (!input) return districts.slice(0, 8);
  const q = input.toLowerCase();
  return districts.filter((d) => d.toLowerCase().includes(q)).slice(0, 8);
}

/** Cities for a given state (alias for districts, used in city dropdowns). */
export function getCitiesForState(state: string): string[] {
  return DISTRICTS_BY_STATE[state] ?? [];
}

/** World countries — India listed first, then alphabetical. */
export const WORLD_COUNTRIES: string[] = [
  'India',
  'Afghanistan', 'Australia', 'Bangladesh', 'Bhutan', 'Canada', 'China',
  'France', 'Germany', 'Indonesia', 'Iran', 'Iraq', 'Italy', 'Japan',
  'Kenya', 'Malaysia', 'Maldives', 'Mexico', 'Myanmar', 'Nepal',
  'New Zealand', 'Nigeria', 'Oman', 'Pakistan', 'Philippines',
  'Qatar', 'Russia', 'Saudi Arabia', 'Singapore', 'South Africa',
  'South Korea', 'Spain', 'Sri Lanka', 'Thailand', 'Turkey',
  'United Arab Emirates', 'United Kingdom', 'United States', 'Vietnam',
];

export interface PincodeLookupResult {
  state: string;
  district: string;
  city: string;
}

/**
 * Real-time pincode lookup using India Post API.
 * Returns state, district, and city/post-office name for the given 6-digit pincode.
 * Free API, no auth required.
 */
export async function lookupPincode(pincode: string): Promise<PincodeLookupResult | null> {
  if (!/^\d{6}$/.test(pincode)) return null;
  try {
    const res = await fetch(`https://api.postalpincode.in/pincode/${pincode}`);
    if (!res.ok) return null;
    const data = await res.json();
    const entry = data?.[0];
    if (entry?.Status !== 'Success' || !entry?.PostOffice?.length) return null;
    // Pick first delivery post office, fallback to first
    const po = entry.PostOffice.find(
      (p: { DeliveryStatus: string }) => p.DeliveryStatus === 'Delivery'
    ) ?? entry.PostOffice[0];
    return {
      state: po.State ?? '',
      district: po.District ?? '',
      city: po.Name ?? '',
    };
  } catch {
    return null;
  }
}
