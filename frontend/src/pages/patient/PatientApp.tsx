import { Home, PlusCircle, ClipboardList, Pill, FlaskConical, Bell } from "lucide-react";
import { AppShell, type NavItem } from "@/components/AppShell";

const patientNav: NavItem[] = [
  { label: "Home", to: "/app", icon: Home, end: true },
  { label: "New symptom", to: "/app/new", icon: PlusCircle },
  { label: "Care plans", to: "/app/care-plans", icon: ClipboardList },
  { label: "Prescriptions", to: "/app/prescriptions", icon: Pill },
  { label: "Test results", to: "/app/results", icon: FlaskConical },
  { label: "Notifications", to: "/app/notifications", icon: Bell },
];

export function PatientApp() {
  return <AppShell nav={patientNav} title="MedSimple" />;
}
