import { Users, Plug } from "lucide-react";
import { AppShell, type NavItem } from "@/components/AppShell";

const adminNav: NavItem[] = [
  { label: "Staff", to: "/admin", icon: Users, end: true },
  { label: "Integrations", to: "/admin/integrations", icon: Plug },
];

export function AdminApp() {
  return <AppShell nav={adminNav} title="Clinic admin" />;
}
