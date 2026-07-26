import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { renderWithProviders } from "@/test-utils";
import { LoginPage } from "./LoginPage";

const loginMock = vi.fn();

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ login: loginMock, logout: vi.fn(), employee: null, isLoading: false }),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    loginMock.mockReset();
  });

  it("shows validation errors when submitted empty", async () => {
    renderWithProviders(<LoginPage />);
    await userEvent.click(screen.getByRole("button", { name: /log in/i }));

    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("calls login with the entered credentials on valid submit", async () => {
    loginMock.mockResolvedValueOnce(undefined);
    renderWithProviders(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/email/i), "alice@example.com");
    await userEvent.type(screen.getByLabelText(/password/i), "secret123");
    await userEvent.click(screen.getByRole("button", { name: /log in/i }));

    await waitFor(() => expect(loginMock).toHaveBeenCalledWith("alice@example.com", "secret123"));
  });

  it("shows an error message when login fails", async () => {
    loginMock.mockRejectedValueOnce(new Error("nope"));
    renderWithProviders(<LoginPage />);

    await userEvent.type(screen.getByLabelText(/email/i), "alice@example.com");
    await userEvent.type(screen.getByLabelText(/password/i), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: /log in/i }));

    expect(await screen.findByText(/unable to log in/i)).toBeInTheDocument();
  });
});
