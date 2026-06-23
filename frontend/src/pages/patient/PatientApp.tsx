import { Home, PlusCircle, ClipboardList, Pill, FlaskConical, Send, Bell } from "lucide-react";
import { AppShell, type NavItem } from "@/components/AppShell";
import { useAuth } from "@/auth/AuthProvider";
import { useUnreadCount } from "@/features/notifications/hooks";

const patientNav: NavItem[] = [
  { label: "Home", to: "/app", icon: Home, end: true },
  { label: "New symptom", to: "/app/new", icon: PlusCircle },
  { label: "Care plans", to: "/app/care-plans", icon: ClipboardList },
  { label: "Prescriptions", to: "/app/prescriptions", icon: Pill },
  { label: "Test results", to: "/app/results", icon: FlaskConical },
  { label: "Referrals", to: "/app/referrals", icon: Send },
  { label: "Notifications", to: "/app/notifications", icon: Bell },
];

export function PatientApp() {
  const { user } = useAuth();
  const { data: unread } = useUnreadCount(user?.id);

  // Surface the live unread count on the Notifications item.
  const nav = patientNav.map((item) =>
    item.to === "/app/notifications" ? { ...item, badge: unread } : item
  );

  return <AppShell nav={nav} title="MedSimple" />;
}
