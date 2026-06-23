import { ListChecks, CalendarDays } from "lucide-react";
import { AppShell, type NavItem } from "@/components/AppShell";

const consoleNav: NavItem[] = [
  { label: "Queue", to: "/console", icon: ListChecks, end: true },
  { label: "Appointments", to: "/console/appointments", icon: CalendarDays },
];

export function ConsoleApp() {
  return <AppShell nav={consoleNav} title="Clinician console" />;
}
