import { Outlet } from "react-router-dom"
import { Sidebar } from "@/components/layout/Sidebar"

export function DashboardLayout() {
    return (
        <div className="flex min-h-screen bg-background">
            <Sidebar />
            <main className="flex-1 overflow-y-auto pt-14 md:pt-0">
                <div className="container max-w-6xl py-8 px-4 md:px-8">
                    <Outlet />
                </div>
            </main>
        </div>
    )
}
