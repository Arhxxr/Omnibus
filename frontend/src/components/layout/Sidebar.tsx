import { useState } from "react"
import { NavLink, useNavigate } from "react-router-dom"
import { useAuth } from "@/contexts/AuthContext"
import { useTheme } from "@/hooks/useTheme"
import { cn, getInitials, formatCurrency } from "@/lib/utils"
import {
    LayoutDashboard,
    ArrowLeftRight,
    Clock,
    Settings,
    LogOut,
    Shield,
    Moon,
    Sun,
    Menu,
    X,
} from "lucide-react"

const navItems = [
    { to: "/dashboard", icon: LayoutDashboard, label: "Dashboard" },
    { to: "/send", icon: ArrowLeftRight, label: "Send Money" },
    { to: "/activity", icon: Clock, label: "Activity" },
    { to: "/settings", icon: Settings, label: "Settings" },
]

export function Sidebar() {
    const { user, logout } = useAuth()
    const { theme, toggleTheme } = useTheme()
    const navigate = useNavigate()
    const primaryAccount = user?.accounts?.[0]
    const [mobileOpen, setMobileOpen] = useState(false)

    const handleLogout = () => {
        logout()
        navigate("/login")
    }

    const sidebarContent = (
        <>
            {/* Brand */}
            <div className="flex items-center gap-3 px-6 py-5 border-b border-border">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-primary-foreground font-bold text-lg shadow-md">
                    <Shield className="h-5 w-5" />
                </div>
                <div>
                    <h1 className="text-lg font-bold tracking-tight">Omnibus</h1>
                    <p className="text-xs text-muted-foreground">Secure Banking</p>
                </div>
            </div>

            {/* User Card */}
            {user && (
                <div className="px-4 py-4">
                    <div className="rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 p-4 border border-primary/10">
                        <div className="flex items-center gap-3 mb-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold shadow-sm">
                                {getInitials(user.username)}
                            </div>
                            <div className="min-w-0">
                                <p className="font-semibold truncate">{user.username}</p>
                                <p className="text-xs text-muted-foreground truncate">
                                    {user.email}
                                </p>
                            </div>
                        </div>
                        {primaryAccount && (
                            <div className="pt-3 border-t border-primary/10">
                                <p className="text-xs text-muted-foreground mb-0.5">
                                    Available Balance
                                </p>
                                <p className="text-xl font-bold tracking-tight">
                                    {formatCurrency(
                                        primaryAccount.balance,
                                        primaryAccount.currency
                                    )}
                                </p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* Navigation */}
            <nav className="flex-1 px-3 py-2 space-y-1">
                {navItems.map(({ to, icon: Icon, label }) => (
                    <NavLink
                        key={to}
                        to={to}
                        onClick={() => setMobileOpen(false)}
                        className={({ isActive }) =>
                            cn(
                                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200",
                                isActive
                                    ? "bg-primary text-primary-foreground shadow-md shadow-primary/20"
                                    : "text-muted-foreground hover:text-foreground hover:bg-accent"
                            )
                        }
                    >
                        <Icon className="h-[18px] w-[18px]" />
                        {label}
                    </NavLink>
                ))}
            </nav>

            {/* Footer */}
            <div className="p-3 border-t border-border space-y-1">
                <button
                    onClick={toggleTheme}
                    className="flex w-full items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
                >
                    {theme === "dark" ? (
                        <Sun className="h-[18px] w-[18px]" />
                    ) : (
                        <Moon className="h-[18px] w-[18px]" />
                    )}
                    {theme === "dark" ? "Light Mode" : "Dark Mode"}
                </button>
                <button
                    onClick={handleLogout}
                    className="flex w-full items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                >
                    <LogOut className="h-[18px] w-[18px]" />
                    Sign Out
                </button>
            </div>
        </>
    )

    return (
        <>
            {/* Mobile Header */}
            <div className="md:hidden fixed top-0 left-0 right-0 z-50 flex items-center justify-between px-4 py-3 bg-card border-b border-border">
                <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                        <Shield className="h-4 w-4" />
                    </div>
                    <span className="font-bold text-sm">Omnibus</span>
                </div>
                <button
                    onClick={() => setMobileOpen(!mobileOpen)}
                    className="h-9 w-9 flex items-center justify-center rounded-lg hover:bg-accent transition-colors"
                    aria-label="Toggle menu"
                >
                    {mobileOpen ? (
                        <X className="h-5 w-5" />
                    ) : (
                        <Menu className="h-5 w-5" />
                    )}
                </button>
            </div>

            {/* Mobile Overlay */}
            {mobileOpen && (
                <div
                    className="md:hidden fixed inset-0 z-40 bg-black/50 backdrop-blur-sm"
                    onClick={() => setMobileOpen(false)}
                />
            )}

            {/* Mobile Drawer */}
            <aside
                className={cn(
                    "md:hidden fixed top-0 left-0 bottom-0 z-50 w-72 bg-card border-r border-border flex flex-col transition-transform duration-300 ease-in-out",
                    mobileOpen ? "translate-x-0" : "-translate-x-full"
                )}
            >
                {sidebarContent}
            </aside>

            {/* Desktop Sidebar */}
            <aside className="hidden md:flex md:w-72 flex-col border-r border-border bg-card shrink-0">
                {sidebarContent}
            </aside>
        </>
    )
}
