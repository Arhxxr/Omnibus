import { Link } from "react-router-dom"
import { FileQuestion, ArrowLeft } from "lucide-react"

export function NotFoundPage() {
    return (
        <div className="flex min-h-screen items-center justify-center bg-background px-4">
            <div className="max-w-md w-full text-center animate-fade-in">
                <div className="mx-auto h-20 w-20 rounded-full bg-muted flex items-center justify-center mb-6">
                    <FileQuestion className="h-10 w-10 text-muted-foreground" />
                </div>
                <h1 className="text-6xl font-extrabold tracking-tight mb-2">404</h1>
                <p className="text-xl text-muted-foreground mb-8">
                    Page not found
                </p>
                <Link
                    to="/dashboard"
                    className="inline-flex items-center justify-center gap-2 h-11 px-6 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:bg-primary/90 transition-colors"
                >
                    <ArrowLeft className="h-4 w-4" />
                    Back to Dashboard
                </Link>
            </div>
        </div>
    )
}
