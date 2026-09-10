import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Siren,
  Map as MapIcon,
  Users,
  History,
  Settings,
  PanelLeftClose,
  PanelLeftOpen,
} from "lucide-react";
import "../styles/sidebar.css";

const NAV_ITEMS = [
  { to: "/", label: "Overview", icon: LayoutDashboard, end: true },
  { to: "/incidents", label: "Active Incidents", icon: Siren },
  { to: "/map", label: "Rescue Map", icon: MapIcon },
  { to: "/people", label: "People / Safety Circle", icon: Users },
  { to: "/history", label: "Detection History", icon: History },
  { to: "/settings", label: "Settings", icon: Settings },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export default function Sidebar({ collapsed, onToggle }: SidebarProps) {
  return (
    <aside className={`sidebar ${collapsed ? "sidebar--collapsed" : ""}`}>
      <nav className="sidebar__nav" aria-label="Primary">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `sidebar__link ${isActive ? "sidebar__link--active" : ""}`
            }
            title={collapsed ? label : undefined}
          >
            <Icon size={17} strokeWidth={2} />
            <span className="sidebar__label">{label}</span>
          </NavLink>
        ))}
      </nav>

      <button
        type="button"
        className="sidebar__collapse"
        onClick={onToggle}
        aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        aria-expanded={!collapsed}
      >
        {collapsed ? <PanelLeftOpen size={16} /> : <PanelLeftClose size={16} />}
        <span className="sidebar__label">Collapse</span>
      </button>
    </aside>
  );
}
