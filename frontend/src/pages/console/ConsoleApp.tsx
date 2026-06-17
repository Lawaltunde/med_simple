import { ListChecks } from "lucide-react";
import { AppShell, type NavItem } from "@/components/AppShell";

const consoleNav: NavItem[] = [
  { label: "Queue", to: "/console", icon: ListChecks, end: true },
  // Patients, care plans, prescriptions… added as the clinician tools are built.
];

export function ConsoleApp() {
  return <AppShell nav={consoleNav} title="Clinician console" />;
}
