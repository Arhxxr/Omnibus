import { render, screen } from "@/test/test-utils"
import { SettingsPage } from "./SettingsPage"

const mockUser = {
    userId: "u-123",
    username: "testuser",
    email: "test@example.com",
    accounts: [
        {
            id: "a-1",
            userId: "u-123",
            accountNumber: "1234567890",
            balance: 5000,
            currency: "USD",
            status: "ACTIVE",
            createdAt: "2026-01-01T00:00:00Z",
        },
    ],
}

vi.mock("@/contexts/AuthContext", () => ({
    useAuth: vi.fn(() => ({
        user: mockUser,
        token: "fake-token",
        isAuthenticated: true,
        isLoading: false,
        login: vi.fn(),
        logout: vi.fn(),
        refreshProfile: vi.fn(),
    })),
}))

vi.mock("@/hooks/useTheme", () => ({
    useTheme: vi.fn(() => ({
        theme: "light" as const,
        toggleTheme: vi.fn(),
    })),
}))

describe("SettingsPage", () => {
    it("renders the settings heading", () => {
        render(<SettingsPage />)
        expect(
            screen.getByRole("heading", { name: /settings/i })
        ).toBeInTheDocument()
    })

    it("displays user profile info", () => {
        render(<SettingsPage />)
        expect(screen.getByText("testuser")).toBeInTheDocument()
        expect(screen.getByText("test@example.com")).toBeInTheDocument()
        expect(screen.getByText("u-123")).toBeInTheDocument()
    })

    it("displays account details", () => {
        render(<SettingsPage />)
        expect(screen.getByText("1234567890")).toBeInTheDocument()
        expect(screen.getByText(/5,000/)).toBeInTheDocument()
        // Check the account status specifically in the capitalize class element
        const statusEl = document.querySelector(".capitalize")
        expect(statusEl).toHaveTextContent("active")
    })

    it("shows theme toggle button", () => {
        render(<SettingsPage />)
        expect(
            screen.getByRole("button", { name: /dark mode/i })
        ).toBeInTheDocument()
    })

    it("displays security information", () => {
        render(<SettingsPage />)
        expect(
            screen.getByText(/jwt authentication active/i)
        ).toBeInTheDocument()
        expect(
            screen.getByText(/idempotency protection/i)
        ).toBeInTheDocument()
        expect(
            screen.getByText(/account ownership verified/i)
        ).toBeInTheDocument()
    })

    it("shows user initials in avatar", () => {
        render(<SettingsPage />)
        // getInitials("testuser") returns "T" for single-word names
        expect(screen.getByText("T")).toBeInTheDocument()
    })

    it("renders preference section with current theme", () => {
        render(<SettingsPage />)
        expect(screen.getByText(/currently using/i)).toBeInTheDocument()
        expect(
            screen.getByText((content) => /light/i.test(content))
        ).toBeInTheDocument()
    })
})
