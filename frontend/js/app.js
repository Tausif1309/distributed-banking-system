
const loginForm = document.getElementById("loginForm");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");

const loginButton = document.getElementById("loginButton");
const errorMessage = document.getElementById("errorMessage");
const togglePassword = document.getElementById("togglePassword");

// Show or hide password
togglePassword.addEventListener("click", function () {

    if (passwordInput.type === "password") {
        passwordInput.type = "text";
        togglePassword.textContent = "Hide";
    } else {
        passwordInput.type = "password";
        togglePassword.textContent = "Show";
    }

});

// Login form
loginForm.addEventListener("submit", async function (event) {

    event.preventDefault();

    errorMessage.textContent = "";
    errorMessage.style.color = "#ff6b78";

    loginButton.disabled = true;
    loginButton.textContent = "Logging in...";

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    try {

        const response = await fetch(
            `${API_CONFIG.AUTH_URL}/api/v1/auth/login`,
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    username: username,
                    password: password
                })
            }
        );

        // Safely read response
        const responseText = await response.text();

        let data = {};

        if (responseText) {
            try {
                data = JSON.parse(responseText);
            } catch (error) {
                console.error("Invalid server response:", responseText);
            }
        }

        // Handle errors
        if (!response.ok) {

            if (response.status === 401) {
                throw new Error(
                    data.message || "Invalid username or password."
                );
            }

            if (response.status === 403) {
                throw new Error(
                    data.message || "Your account is disabled. Please contact the administrator."
                );
            }

            if (response.status === 400) {
                throw new Error(
                    data.message || "Please enter valid login details."
                );
            }

            if (response.status >= 500) {
                throw new Error(
                    "Something went wrong on the server. Please try again later."
                );
            }

            throw new Error(
                data.message || `Login failed. Error code: ${response.status}`
            );
        }

        // Validate successful response
        if (!data.accessToken || !data.role || !data.userId) {
            throw new Error("Invalid login response from server.");
        }

        const role = data.role.replace("ROLE_", "").toUpperCase();

        if (role !== "ADMIN" && role !== "USER") {
            throw new Error("Unknown user role.");
        }

        // Store authentication information
        sessionStorage.setItem("accessToken", data.accessToken);
        sessionStorage.setItem("tokenType", data.tokenType || "Bearer");
        sessionStorage.setItem("userId", data.userId);
        sessionStorage.setItem("username", data.username);
        sessionStorage.setItem("role", role);

        // Redirect based on role
        if (role === "ADMIN") {
            window.location.href = "admin.html";
        } else {
            window.location.href = "user.html";
        }

    } catch (error) {

        console.error("Login error:", error);

        if (error instanceof TypeError) {

            errorMessage.textContent =
                "Cannot connect to server. Please check your internet connection or backend.";

        } else {

            errorMessage.textContent = error.message;

        }

    } finally {

        loginButton.disabled = false;
        loginButton.textContent = "Login";

    }

});