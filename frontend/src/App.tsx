import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { SignupPage } from "@/pages/SignupPage";
import { PatientApp } from "@/pages/patient/PatientApp";
import { PatientHome } from "@/pages/patient/PatientHome";

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* Patient app (authenticated, PATIENT role) */}
          <Route
            path="/app"
            element={
              <ProtectedRoute allow={["PATIENT"]}>
                <PatientApp />
              </ProtectedRoute>
            }
          >
            <Route index element={<PatientHome />} />
            {/* /app/new, /app/care-plans, … added as those screens are built */}
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
