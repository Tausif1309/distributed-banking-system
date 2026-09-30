
const token = sessionStorage.getItem("accessToken");
const role = sessionStorage.getItem("role");

if (!token || role !== "ADMIN") {
    window.location.href = "index.html";
}

// Logout
document.getElementById("logoutButton").addEventListener("click", function () {
    sessionStorage.clear();
    window.location.href = "index.html";
});

// Show messages
function showMessage(elementId, message, color) {
    const element = document.getElementById(elementId);
    element.textContent = message;
    element.style.color = color;
}

// Create User
document.getElementById("createUserForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const userData = {
        fullName: document.getElementById("newName").value.trim(),
        username: document.getElementById("newUsername").value.trim(),
        email: document.getElementById("newEmail").value.trim(),
        phone: document.getElementById("newPhone").value.trim(),
        address: document.getElementById("newAddress").value.trim(),
        password: document.getElementById("newPassword").value,
        initialBalance: Number(document.getElementById("initialBalance").value)
    };

    const button = this.querySelector("button[type='submit']");
    button.disabled = true;
    button.textContent = "Creating...";

    try {
        const response = await fetch(`${API_CONFIG.USER_URL}/api/v1/users`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify(userData)
        });

        const responseText = await response.text();
        let data = {};

        if (responseText) {
            try {
                data = JSON.parse(responseText);
            } catch (error) {
                console.log("Response:", responseText);
            }
        }

        if (!response.ok) {
            throw new Error(data.message || `Request failed: ${response.status}`);
        }

        showMessage(
            "createUserMessage",
            `User created successfully! User ID: ${data.id}`,
            "lightgreen"
        );

        this.reset();

        loadUsers();

    } catch (error) {
        console.error("Create User Error:", error);

        showMessage(
            "createUserMessage",
            error.message || "Could not connect to server.",
            "#ff6b78"
        );

    } finally {
        button.disabled = false;
        button.textContent = "Create User";
    }
});

// Load All Users
async function loadUsers() {

    const tableBody = document.getElementById("usersTableBody");

    try {
        const response = await fetch(`${API_CONFIG.USER_URL}/api/v1/users`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error(`Failed to load users: ${response.status}`);
        }

        const users = await response.json();

        tableBody.innerHTML = "";

        if (users.length === 0) {
            tableBody.innerHTML =
                '<tr><td colspan="5">No users found.</td></tr>';
            return;
        }

        users.forEach(function (user) {

            const row = document.createElement("tr");

            const idCell = document.createElement("td");
            idCell.textContent = user.id;

            const nameCell = document.createElement("td");
            nameCell.textContent = user.fullName;

            const emailCell = document.createElement("td");
            emailCell.textContent = user.email;

            const statusCell = document.createElement("td");
            statusCell.textContent = user.status;

            const actionCell = document.createElement("td");

            const actionButton = document.createElement("button");

            if (user.status === "ACTIVE") {

                actionButton.textContent = "Deactivate";
                actionButton.className = "reject-button";

                actionButton.addEventListener("click", function () {
                    changeUserStatus(user.id, "deactivate");
                });

            } else if (user.status === "DISABLED") {

                actionButton.textContent = "Activate";
                actionButton.className = "approve-button";

                actionButton.addEventListener("click", function () {
                    changeUserStatus(user.id, "activate");
                });

            } else {

                actionButton.textContent = "Blocked";
                actionButton.disabled = true;
            }

            actionCell.appendChild(actionButton);

            row.append(
                idCell,
                nameCell,
                emailCell,
                statusCell,
                actionCell
            );

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error("Load Users Error:", error);

        tableBody.innerHTML =
            '<tr><td colspan="5">Could not load users.</td></tr>';
    }
}

// Activate or Deactivate User
async function changeUserStatus(userId, action) {

    const isDeactivating = action === "deactivate";

    const confirmation = confirm(
        `Are you sure you want to ${action} user #${userId}?`
    );

    if (!confirmation) {
        return;
    }

    try {

        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/users/${userId}/${action}`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            const errorText = await response.text();

            throw new Error(
                errorText || `Request failed: ${response.status}`
            );
        }

        alert(
            `User #${userId} ${isDeactivating ? "deactivated" : "activated"} successfully!`
        );

        await loadUsers();

    } catch (error) {

        console.error("Change User Status Error:", error);

        alert(error.message || "Unable to update user status.");
    }
}

// Load Pending Change Requests
async function loadChangeRequests() {
    const tableBody = document.getElementById("requestsTableBody");

    tableBody.innerHTML = `
        <tr>
            <td colspan="7">Loading requests...</td>
        </tr>
    `;

    try {
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/change-requests/pending`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Failed to load requests: ${response.status}`);
        }

        const requests = await response.json();

        tableBody.innerHTML = "";

        if (requests.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="7">No pending requests.</td>
                </tr>
            `;
            return;
        }

        requests.forEach(function (request) {
            const row = document.createElement("tr");

            const idCell = document.createElement("td");
            idCell.textContent = request.id;

            const userCell = document.createElement("td");
            userCell.textContent = request.userId;

            const fieldCell = document.createElement("td");
            fieldCell.textContent = request.fieldName;

            const oldValueCell = document.createElement("td");
            oldValueCell.textContent = request.oldValue || "-";

            const newValueCell = document.createElement("td");
            newValueCell.textContent = request.newValue || "-";

            const statusCell = document.createElement("td");
            statusCell.textContent = request.status;

            const actionCell = document.createElement("td");

            const approveButton = document.createElement("button");
            approveButton.textContent = "Approve";
            approveButton.className = "approve-button";
            approveButton.addEventListener("click", function () {
                reviewRequest(request.id, true);
            });

            const rejectButton = document.createElement("button");
            rejectButton.textContent = "Reject";
            rejectButton.className = "reject-button";
            rejectButton.addEventListener("click", function () {
                reviewRequest(request.id, false);
            });

            actionCell.append(approveButton, rejectButton);

            row.append(
                idCell,
                userCell,
                fieldCell,
                oldValueCell,
                newValueCell,
                statusCell,
                actionCell
            );

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error("Load Change Requests Error:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="7">Could not load change requests.</td>
            </tr>
        `;
    }
}

// Approve or Reject Request
async function reviewRequest(requestId, approved) {

    const action = approved ? "approve" : "reject";

    if (!confirm(`Are you sure you want to ${action} request #${requestId}?`)) {
        return;
    }

    try {
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/change-requests/${requestId}/review`,
            {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    approved: approved
                })
            }
        );

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Review failed: ${response.status}`);
        }

        alert(`Request #${requestId} ${approved ? "approved" : "rejected"} successfully!`);

        await loadChangeRequests();

        // Refresh user list in case approved changes affect user details
        await loadUsers();

    } catch (error) {
        console.error("Review Request Error:", error);

        alert("Unable to review request. Please check the console.");
    }
}

async function loadAdminTransactions() {
    const tableBody =
        document.getElementById("transactionsTableBody");

    tableBody.innerHTML = `
        <tr>
            <td colspan="5">Loading transactions...</td>
        </tr>
    `;

    try {
        const response = await fetch(
            `${API_CONFIG.TRANSACTION_URL}/api/v1/transactions/admin`,
            {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Failed to load transactions: ${response.status}`);
        }

        const data = await response.json();

        // Spring Page response contains transactions inside content
        const transactions = data.content || [];

        tableBody.innerHTML = "";

        if (transactions.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5">No transactions found.</td>
                </tr>
            `;
            return;
        }

        transactions.forEach(transaction => {
            const row = document.createElement("tr");

            const referenceCell = document.createElement("td");
            referenceCell.textContent =
                transaction.referenceId || transaction.transactionId;

            const senderCell = document.createElement("td");
            senderCell.textContent = transaction.senderUserId;

            const receiverCell = document.createElement("td");
            receiverCell.textContent = transaction.receiverUserId;

            const amountCell = document.createElement("td");
            amountCell.textContent =
                `₹ ${Number(transaction.amount).toLocaleString("en-IN", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                })}`;

            const statusCell = document.createElement("td");
            statusCell.textContent = transaction.status;

            row.append(
                referenceCell,
                senderCell,
                receiverCell,
                amountCell,
                statusCell
            );

            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error("Admin Transaction Error:", error);

        tableBody.innerHTML = `
            <tr>
                <td colspan="5">Unable to load transactions.</td>
            </tr>
        `;
    }
}

// Load dashboard data
loadUsers();
loadChangeRequests();
loadAdminTransactions();