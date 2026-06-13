export type UserRole =
  | "PATIENT"
  | "PHYSICIAN"
  | "NURSE"
  | "RECEPTIONIST"
  | "CLINIC_ADMIN"
  | "SUPER_ADMIN";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  role: UserRole;
}

export interface CurrentUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

// Clinic staff use the console; patients use the patient app.
export function homePathForRole(role: UserRole): string {
  return role === "PATIENT" ? "/app" : "/console";
}
