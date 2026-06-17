export type UserRole =
  | "PATIENT"
  | "PHYSICIAN"
  | "NURSE"
  | "RECEPTIONIST"
  | "CLINIC_ADMIN"
  | "SUPER_ADMIN";

// The refresh token is delivered in an httpOnly cookie, never in this body — only the
// short-lived access token reaches JS. (Backend DTO: AuthTokens.)
export interface AuthTokens {
  accessToken: string;
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
