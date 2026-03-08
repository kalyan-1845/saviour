type OtpRecord = {
  otp: string;
  expiresAt: number;
  attempts: number;
};

const OTP_TTL_MS = 5 * 60 * 1000;
const MAX_ATTEMPTS = 5;
const otpStore = new Map<string, OtpRecord>();

export function createOtp(phone: string): string {
  const otp = String(Math.floor(100000 + Math.random() * 900000));
  otpStore.set(phone, {
    otp,
    expiresAt: Date.now() + OTP_TTL_MS,
    attempts: 0,
  });
  return otp;
}

export function verifyOtp(phone: string, enteredOtp: string): { ok: boolean; error?: string } {
  const record = otpStore.get(phone);
  if (!record) {
    return { ok: false, error: 'OTP not found. Please request a new OTP.' };
  }

  if (Date.now() > record.expiresAt) {
    otpStore.delete(phone);
    return { ok: false, error: 'OTP expired. Please request a new OTP.' };
  }

  if (record.attempts >= MAX_ATTEMPTS) {
    otpStore.delete(phone);
    return { ok: false, error: 'Too many attempts. Request a new OTP.' };
  }

  if (record.otp !== enteredOtp) {
    record.attempts += 1;
    otpStore.set(phone, record);
    return { ok: false, error: 'Invalid OTP.' };
  }

  otpStore.delete(phone);
  return { ok: true };
}
