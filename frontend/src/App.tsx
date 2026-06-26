import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/auth/AuthProvider";
import { ProtectedRoute } from "@/auth/ProtectedRoute";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { SignupPage } from "@/pages/SignupPage";
import { PatientApp } from "@/pages/patient/PatientApp";
import { PatientHome } from "@/pages/patient/PatientHome";
import { NewSymptomCheck } from "@/pages/patient/NewSymptomCheck";
import { SymptomChat } from "@/pages/patient/SymptomChat";
import { EpisodeDetail } from "@/pages/patient/EpisodeDetail";
import { CarePlansPage } from "@/pages/patient/CarePlansPage";
import { CarePlanDetail } from "@/pages/patient/CarePlanDetail";
import { NotificationsPage } from "@/pages/patient/NotificationsPage";
import { PrescriptionsPage } from "@/pages/patient/PrescriptionsPage";
import { ResultsPage } from "@/pages/patient/ResultsPage";
import { VitalsPage } from "@/pages/patient/VitalsPage";
import { ReferralsPage } from "@/pages/patient/ReferralsPage";
import { PrivacyPage } from "@/pages/patient/PrivacyPage";
import { AppointmentsPage } from "@/pages/patient/AppointmentsPage";
import { ComingSoon } from "@/pages/patient/ComingSoon";
import { ConsoleApp } from "@/pages/console/ConsoleApp";
import { QueuePage } from "@/pages/console/QueuePage";
import { EpisodeReview } from "@/pages/console/EpisodeReview";
import { ConsoleAppointmentsPage } from "@/pages/console/ConsoleAppointmentsPage";
import { AdminApp } from "@/pages/admin/AdminApp";
import { StaffPage } from "@/pages/admin/StaffPage";
import { IntegrationsPage } from "@/pages/admin/IntegrationsPage";

// Clinical staff use the console; clinic administrators use the admin console.
const CLINICAL_ROLES = ["PHYSICIAN", "NURSE", "RECEPTIONIST"] as const;
const ADMIN_ROLES = ["CLINIC_ADMIN", "SUPER_ADMIN"] as const;

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
            <Route path="assistant" element={<SymptomChat />} />
            <Route path="new" element={<NewSymptomCheck />} />
            <Route path="episodes/:id" element={<EpisodeDetail />} />
            <Route path="care-plans" element={<CarePlansPage />} />
            <Route path="care-plans/:id" element={<CarePlanDetail />} />
            <Route path="appointments" element={<AppointmentsPage />} />
            {/* Feature pages — stubs land first via the shell-contract scaffold, then each
                workstream replaces its page body on its own branch. */}
            <Route path="prescriptions" element={<PrescriptionsPage />} />
            <Route path="results" element={<ResultsPage />} />
            <Route path="vitals" element={<VitalsPage />} />
            <Route path="referrals" element={<ReferralsPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="privacy" element={<PrivacyPage />} />
            {/* Unknown /app/* path stays in the shell rather than bouncing to the landing page. */}
            <Route path="*" element={<ComingSoon title="This page" description="That section isn't available yet." />} />
          </Route>

          {/* Clinician console (authenticated, clinic staff) */}
          <Route
            path="/console"
            element={
              <ProtectedRoute allow={[...CLINICAL_ROLES]}>
                <ConsoleApp />
              </ProtectedRoute>
            }
          >
            <Route index element={<QueuePage />} />
            <Route path="episodes/:id" element={<EpisodeReview />} />
            <Route path="appointments" element={<ConsoleAppointmentsPage />} />
            <Route path="*" element={<ComingSoon title="This page" description="That section isn't available yet." />} />
          </Route>

          {/* Clinic admin console (authenticated, CLINIC_ADMIN / SUPER_ADMIN) */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute allow={[...ADMIN_ROLES]}>
                <AdminApp />
              </ProtectedRoute>
            }
          >
            <Route index element={<StaffPage />} />
            <Route path="integrations" element={<IntegrationsPage />} />
            <Route path="*" element={<ComingSoon title="This page" description="That section isn't available yet." />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
