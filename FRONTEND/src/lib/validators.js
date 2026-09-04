// Mirrors the backend's validation rules (BACKEND/.../validation/*.java) so the
// signup form can give immediate feedback instead of a round-trip error. The
// backend remains the source of truth - these are UX conveniences, not the
// actual security boundary.

const WEAK_PINS = new Set([
  "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
  "1234", "2345", "3456", "4567", "5678", "6789", "9876", "8765", "7654", "6543", "5432", "4321",
  "0123", "1230", "2580", "1212", "1122", "1004", "2001", "2000",
]);

export function isWeakPin(pin) {
  return WEAK_PINS.has(pin);
}

export function isValidName(value) {
  return /^[A-Za-z '-]{2,60}$/.test(value);
}

// South African 13-digit ID: YYMMDD SSSS C A Z (date of birth, gender sequence,
// citizenship, historical race marker, Luhn-style check digit).
export function isValidSaId(id) {
  if (!/^[0-9]{13}$/.test(id)) return false;

  const yy = Number(id.slice(0, 2));
  const mm = Number(id.slice(2, 4));
  const dd = Number(id.slice(4, 6));
  if (mm < 1 || mm > 12) return false;
  const currentYearTwoDigit = new Date().getFullYear() % 100;
  const fullYear = yy <= currentYearTwoDigit ? 2000 + yy : 1900 + yy;
  const daysInMonth = new Date(fullYear, mm, 0).getDate();
  if (dd < 1 || dd > daysInMonth) return false;

  const citizenship = id[10];
  if (citizenship !== "0" && citizenship !== "1") return false;

  let oddSum = 0;
  for (let i = 0; i < 12; i += 2) oddSum += Number(id[i]);
  let evenDigits = "";
  for (let i = 1; i < 12; i += 2) evenDigits += id[i];
  const doubled = Number(evenDigits) * 2;
  const evenSum = String(doubled)
    .split("")
    .reduce((sum, c) => sum + Number(c), 0);
  const expected = (10 - ((oddSum + evenSum) % 10)) % 10;
  return expected === Number(id[12]);
}

// SA number plates aren't one uniform format - modern provincial plates look
// like "DX 45 FG MP" while pre-1998 city plates (Cape Town, Durban) still on
// the road look like "CA 123-456". This matches the shared shape: letters,
// then digits, then optionally more letters - rather than guessing at an
// exhaustive province-code list.
export function isValidVehicleReg(value) {
  const normalized = normalizeVehicleReg(value);
  return /^[A-Z]{2,3}[0-9]{2,6}[A-Z]{0,4}$/.test(normalized);
}

export function normalizeVehicleReg(value) {
  return (value || "").toUpperCase().replace(/[\s-]/g, "");
}
