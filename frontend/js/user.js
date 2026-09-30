
const token = sessionStorage.getItem("accessToken");
const role = sessionStorage.getItem("role");
const userId = Number(sessionStorage.getItem("userId"));

// Check login and role
if (!token || role !== "USER") {
    window.location.href = "index.html";
}

// Display user details
document.getElementById("username").textContent =
    sessionStorage.getItem("username") || "-";

document.getElementById("userId").textContent =
    sessionStorage.getItem("userId") || "-";

document.getElementById("welcomeMessage").textContent =
    "Welcome, " + (sessionStorage.getItem("username") || "User");

// Logout
document.getElementById("logoutButton").addEventListener("click", function () {
    sessionStorage.clear();
    window.location.href = "index.html";
});

// Load account balance
async function loadAccount() {
    try {
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/accounts/me`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load account details");
        }

        const account = await response.json();

        document.getElementById("accountBalance").textContent =
            `₹ ${Number(account.balance).toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })}`;

    } catch (error) {
        console.error("Load Account Error:", error);
        document.getElementById("accountBalance").textContent =
            "Unable to load balance";
    }
}

// Load transaction history
async function loadTransactions() {
    const tableBody =
        document.getElementById("transactionsTableBody");

    tableBody.innerHTML = `
        <tr>
            <td colspan="5">Loading transactions...</td>
        </tr>
    `;

    try {
        const response = await fetch(
            `${API_CONFIG.TRANSACTION_URL}/api/v1/transactions/my`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load transactions");
        }

        const transactions = await response.json();

        if (transactions.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5">No transactions yet</td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = "";

        transactions.forEach(transaction => {
            const row = document.createElement("tr");

            const type =
                transaction.senderUserId === userId
                    ? "Sent"
                    : "Received";

            const amount = Number(transaction.amount).toLocaleString(
                "en-IN",
                {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                }
            );

            const date = transaction.createdAt
                ? new Date(transaction.createdAt).toLocaleString("en-IN")
                : "-";

            row.innerHTML = `
                <td>${transaction.transactionId}</td>
                <td>${type}</td>
                <td>₹ ${amount}</td>
                <td>${transaction.status}</td>
                <td>${date}</td>
            `;

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error("Transaction History Error:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="5">Unable to load transactions</td>
            </tr>
        `;
    }
}

// Submit profile change request
document.getElementById("changeRequestForm")
    .addEventListener("submit", async function (event) {

        event.preventDefault();

        const fieldName =
            document.getElementById("changeField").value;

        const newValue =
            document.getElementById("newValue").value.trim();

        const message =
            document.getElementById("changeRequestMessage");

        if (!fieldName || !newValue) {
            message.textContent = "Please fill all fields.";
            return;
        }

        message.textContent = "Submitting request...";

        try {
            const response = await fetch(
                `${API_CONFIG.USER_URL}/api/v1/change-requests`,
                {
                    method: "POST",
                    headers: {
                        "Authorization": `Bearer ${token}`,
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        fieldName: fieldName,
                        newValue: newValue
                    })
                }
            );

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Request failed");
            }

            message.textContent =
                "Change request submitted successfully!";

            document.getElementById("changeRequestForm").reset();

            await loadChangeRequests();

        } catch (error) {
            console.error("Change Request Error:", error);

            message.textContent =
                "Unable to submit request. Please try again.";
        }
    });

// Load user's change request history
async function loadChangeRequests() {
    const tableBody =
        document.getElementById("changeRequestsTableBody");

    tableBody.innerHTML = `
        <tr>
            <td colspan="5">Loading requests...</td>
        </tr>
    `;

    try {
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/change-requests/my`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error("Failed to load change requests");
        }

        const requests = await response.json();

        if (requests.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5">No change requests yet</td>
                </tr>
            `;
            return;
        }

        tableBody.innerHTML = "";

        requests.forEach(request => {
            const row = document.createElement("tr");

            const date = request.createdAt
                ? new Date(request.createdAt).toLocaleString("en-IN")
                : "-";

            row.innerHTML = `
                <td>${request.fieldName}</td>
                <td></td>
                <td></td>
                <td>${request.status}</td>
                <td>${date}</td>
            `;

            row.children[1].textContent = request.oldValue || "-";
            row.children[2].textContent = request.newValue || "-";

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error("Change Request History Error:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="5">Unable to load requests</td>
            </tr>
        `;
    }
}

// Load dashboard data
loadAccount();
loadTransactions();
loadChangeRequests();