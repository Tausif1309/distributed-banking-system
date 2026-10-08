
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

function handleSessionExpired() {
    ["accessToken", "tokenType", "role", "userId", "username"]
        .forEach(key => sessionStorage.removeItem(key));
    window.location.href = "index.html";
}

function displayProfileValue(value, fallback = "Not provided") {
    if (value === null || value === undefined || String(value).trim() === "") {
        return fallback;
    }
    return String(value);
}

// Load the authenticated user's profile
async function loadProfile() {
    const message = document.getElementById("profileMessage");
    message.textContent = "Loading personal information...";

    try {
        const sessionUserId = sessionStorage.getItem("userId");
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/users/${encodeURIComponent(sessionUserId || "")}`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (response.status === 401) {
            handleSessionExpired();
            return;
        }
        if (!response.ok) {
            throw new Error(`Profile request failed (HTTP ${response.status}).`);
        }

        const profile = await response.json();
        if (!profile || typeof profile !== "object") {
            throw new Error("Profile information is unavailable.");
        }

        document.getElementById("userId").textContent =
            displayProfileValue(profile.id ?? profile.userId ?? sessionUserId, "Not available");
        document.getElementById("profileFullName").textContent =
            displayProfileValue(profile.fullName, "Not available");
        document.getElementById("profileEmail").textContent =
            displayProfileValue(profile.email, "Not available");
        document.getElementById("profilePhone").textContent =
            displayProfileValue(profile.phone);
        document.getElementById("profileAddress").textContent =
            displayProfileValue(profile.address);
        document.getElementById("profileStatus").textContent =
            displayProfileValue(profile.status, "Not available");
        message.textContent = "Personal information loaded.";
    } catch (error) {
        console.error("Load Profile Error:", error);
        document.getElementById("userId").textContent =
            displayProfileValue(sessionStorage.getItem("userId"), "Not available");
        for (const id of ["profileFullName", "profileEmail", "profilePhone", "profileAddress", "profileStatus"]) {
            document.getElementById(id).textContent = "Unavailable";
        }
        message.textContent = "Unable to load personal information.";
    }
}

// Load account information
async function loadAccount() {
    const accountMessage = document.getElementById("accountMessage");
    accountMessage.textContent = "Loading account information...";

    try {
        const response = await fetch(
            `${API_CONFIG.USER_URL}/api/v1/accounts/me`,
            {
                headers: {
                    "Authorization": `Bearer ${token}`
                }
            }
        );

        if (response.status === 401) {
            handleSessionExpired();
            return;
        }
        if (!response.ok) {
            throw new Error(`Account request failed (HTTP ${response.status}).`);
        }

        const account = await response.json();
        if (!account || typeof account !== "object") {
            throw new Error("Account information is unavailable.");
        }

        const accountId = account.accountId ?? account.id;
        const balance = Number(account.balance);
        document.getElementById("accountId").textContent =
            displayProfileValue(accountId, "Not available");
        document.getElementById("accountCurrency").textContent =
            displayProfileValue(account.currency, "Not available");
        document.getElementById("accountStatus").textContent =
            displayProfileValue(account.status, "Not available");
        document.getElementById("accountBalance").textContent =
            account.balance === null || account.balance === undefined || !Number.isFinite(balance)
                ? "Not available"
                : `₹ ${balance.toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            })}`;
        accountMessage.textContent = "Account information loaded.";

    } catch (error) {
        console.error("Load Account Error:", error);
        document.getElementById("accountId").textContent = "Unavailable";
        document.getElementById("accountCurrency").textContent = "Unavailable";
        document.getElementById("accountStatus").textContent = "Unavailable";
        document.getElementById("accountBalance").textContent =
            "Unavailable";
        accountMessage.textContent = "Unable to load account information.";
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

// Transfer attempt state is kept for this signed-in browser tab so an
// uncertain request can be retried with the exact same key and payload.
const transferForm = document.getElementById("transferForm");
const receiverInput = document.getElementById("receiverId");
const amountInput = document.getElementById("transferAmount");
const descriptionInput = document.getElementById("description");
const transferMessage = document.getElementById("transferMessage");
const transferSubmitButton =
    document.getElementById("transferSubmitButton");
const newTransferButton =
    document.getElementById("newTransferButton");
const transferAttemptStorageKey = `pendingTransferAttempt:${userId}`;
const transferTimeoutMs = 20000;
const definitiveRejectionMessage =
    "The account service rejected the transfer; no account movement was applied.";

let transferInProgress = false;
let transferAttempt = restoreTransferAttempt();

function restoreTransferAttempt() {
    try {
        const savedAttempt =
            sessionStorage.getItem(transferAttemptStorageKey);

        if (!savedAttempt) {
            return null;
        }

        const attempt = JSON.parse(savedAttempt);
        const request = attempt.request;

        if (attempt.senderUserId !== userId
                || typeof attempt.idempotencyKey !== "string"
                || !request
                || !Number.isSafeInteger(request.receiverUserId)
                || !Number.isFinite(request.amount)
                || typeof request.description !== "string") {
            sessionStorage.removeItem(transferAttemptStorageKey);
            return null;
        }

        receiverInput.value = String(request.receiverUserId);
        amountInput.value = String(request.amount);
        descriptionInput.value = request.description;
        setTransferFieldsLocked(true);
        return attempt;
    } catch (error) {
        console.error("Could not restore transfer retry information:", error);
        return null;
    }
}

function saveTransferAttempt() {
    try {
        sessionStorage.setItem(
            transferAttemptStorageKey,
            JSON.stringify(transferAttempt)
        );
        return true;
    } catch (error) {
        console.error("Could not save transfer retry information:", error);
        return false;
    }
}

function clearTransferAttempt() {
    transferAttempt = null;
    try {
        sessionStorage.removeItem(transferAttemptStorageKey);
    } catch (error) {
        console.error("Could not clear transfer retry information:", error);
    }
}

function setTransferFieldsLocked(locked) {
    receiverInput.disabled = locked;
    amountInput.disabled = locked;
    descriptionInput.disabled = locked;
}

function updateTransferControls() {
    transferSubmitButton.disabled = transferInProgress
        || (transferAttempt && transferAttempt.status === "FAILED");

    if (transferInProgress) {
        transferSubmitButton.textContent = "Sending...";
    } else if (transferAttempt) {
        transferSubmitButton.textContent = transferAttempt.status === "FAILED"
            ? "Transfer failed"
            : "Retry same transfer";
    } else {
        transferSubmitButton.textContent = "Send Money";
    }

    newTransferButton.hidden = !transferAttempt
        || transferAttempt.status !== "FAILED";
}

function showTransferMessage(message, type) {
    transferMessage.textContent = message;
    transferMessage.style.color = type === "success"
        ? "#18794e"
        : type === "error"
            ? "#b42318"
            : "#8a5a00";
}

function createIdempotencyKey() {
    if (window.crypto && typeof window.crypto.randomUUID === "function") {
        return window.crypto.randomUUID();
    }

    if (window.crypto && typeof window.crypto.getRandomValues === "function") {
        const bytes = new Uint8Array(16);
        window.crypto.getRandomValues(bytes);
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;
        const hex = Array.from(bytes, byte =>
            byte.toString(16).padStart(2, "0")
        ).join("");

        return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
    }

    throw new Error("Secure idempotency-key generation is unavailable in this browser.");
}

function readTransferRequestFromForm() {
    if (!transferForm.reportValidity()) {
        return null;
    }

    const receiverUserId = Number(receiverInput.value);
    const amount = Number(amountInput.value);
    const description = descriptionInput.value.trim();

    if (!Number.isSafeInteger(receiverUserId) || receiverUserId <= 0) {
        showTransferMessage("Enter a valid positive receiver user ID.", "error");
        return null;
    }

    if (receiverUserId === userId) {
        showTransferMessage("You cannot transfer money to your own user ID.", "error");
        return null;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
        showTransferMessage("Enter a valid amount greater than zero.", "error");
        return null;
    }

    if (description.length > 255) {
        showTransferMessage("Description cannot exceed 255 characters.", "error");
        return null;
    }

    return { receiverUserId, amount, description };
}

async function readTransferResponse(response) {
    const responseText = await response.text();
    if (!responseText) {
        return null;
    }

    try {
        return JSON.parse(responseText);
    } catch (error) {
        return null;
    }
}

function markTransferFailed(message) {
    transferAttempt.status = "FAILED";
    saveTransferAttempt();
    setTransferFieldsLocked(true);
    showTransferMessage(message, "error");
    updateTransferControls();
}

function markTransferOutcomeUnknown(message) {
    transferAttempt.status = "PENDING";
    saveTransferAttempt();
    setTransferFieldsLocked(true);
    showTransferMessage(
        `${message} Retry the same request; its idempotency key will be reused.`,
        "pending"
    );
    updateTransferControls();
}

transferForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    if (transferInProgress || (transferAttempt && transferAttempt.status === "FAILED")) {
        return;
    }

    if (!transferAttempt) {
        const request = readTransferRequestFromForm();
        if (!request) {
            return;
        }

        try {
            transferAttempt = {
                senderUserId: userId,
                idempotencyKey: createIdempotencyKey(),
                request,
                status: "PENDING"
            };
        } catch (error) {
            showTransferMessage(error.message, "error");
            return;
        }

        if (!saveTransferAttempt()) {
            transferAttempt = null;
            showTransferMessage(
                "Transfer was not sent because retry information could not be saved.",
                "error"
            );
            return;
        }
    }

    transferInProgress = true;
    setTransferFieldsLocked(true);
    updateTransferControls();
    showTransferMessage("Sending transfer...", "pending");

    const controller = new AbortController();
    let requestTimedOut = false;
    const timeoutId = window.setTimeout(() => {
        requestTimedOut = true;
        controller.abort();
    }, transferTimeoutMs);

    try {
        const response = await fetch(
            `${API_CONFIG.TRANSACTION_URL}/api/v1/transactions/transfer`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json",
                    "Idempotency-Key": transferAttempt.idempotencyKey
                },
                body: JSON.stringify(transferAttempt.request),
                signal: controller.signal
            }
        );

        const responseBody = await readTransferResponse(response);

        if (response.status === 401) {
            handleSessionExpired();
            return;
        }

        const serverMessage = typeof responseBody?.message === "string"
            ? responseBody.message
            : "";

        if (response.status === 403) {
            markTransferFailed(serverMessage
                || "You do not have permission to transfer. Your account may be disabled.");
            return;
        }

        if (response.status === 400) {
            markTransferFailed(serverMessage || "The server rejected the transfer details.");
            return;
        }

        if (!response.ok) {
            if (response.status === 409
                    && serverMessage === definitiveRejectionMessage) {
                markTransferFailed(serverMessage);
                return;
            }

            const errorText = serverMessage
                ? `Server response: ${serverMessage}.`
                : `The server returned HTTP ${response.status}.`;
            markTransferOutcomeUnknown(errorText);
            return;
        }

        const status = typeof responseBody?.status === "string"
            ? responseBody.status.toUpperCase()
            : "";

        if (status === "SUCCESS") {
            const reference = responseBody.referenceId
                ? ` Reference: ${responseBody.referenceId}.`
                : " The response did not include a transaction reference.";

            clearTransferAttempt();
            transferForm.reset();
            setTransferFieldsLocked(false);
            updateTransferControls();
            showTransferMessage(`Transfer successful.${reference}`, "success");

            await Promise.all([loadAccount(), loadTransactions()]);
            return;
        }

        if (status === "FAILED") {
            const reference = responseBody.referenceId
                ? ` Reference: ${responseBody.referenceId}.`
                : "";
            markTransferFailed(
                `The server marked this transfer FAILED.${reference} No failure reason was included in the transfer response.`
            );
            return;
        }

        if (status === "PENDING") {
            markTransferOutcomeUnknown(
                "The transfer is still PENDING; its final result is not confirmed."
            );
            return;
        }

        markTransferOutcomeUnknown(
            "The server response did not confirm whether the transfer succeeded."
        );
    } catch (error) {
        if (requestTimedOut) {
            markTransferOutcomeUnknown(
                "The request timed out, so the transfer outcome is unknown."
            );
        } else {
            markTransferOutcomeUnknown(
                "The server could not be reached or its response could not be read."
            );
        }
    } finally {
        window.clearTimeout(timeoutId);
        transferInProgress = false;
        updateTransferControls();
    }
});

newTransferButton.addEventListener("click", function () {
    if (transferInProgress || !transferAttempt || transferAttempt.status !== "FAILED") {
        return;
    }

    clearTransferAttempt();
    transferForm.reset();
    setTransferFieldsLocked(false);
    showTransferMessage("Ready to start a new transfer.", "pending");
    updateTransferControls();
});

if (transferAttempt) {
    if (transferAttempt.status === "FAILED") {
        setTransferFieldsLocked(true);
        showTransferMessage(
            "The previous transfer was confirmed FAILED. Start a new transfer to use a new idempotency key.",
            "error"
        );
    } else {
        transferAttempt.status = "PENDING";
        saveTransferAttempt();
        setTransferFieldsLocked(true);
        showTransferMessage(
            "A previous transfer has no confirmed result. Retry it to reuse the same idempotency key.",
            "pending"
        );
    }
    updateTransferControls();
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

// Notification API calls always use the logged-in user's verified JWT.
const notificationCountBadge =
    document.getElementById("notificationUnreadCount");
const notificationToggleButton =
    document.getElementById("toggleNotificationsButton");
const notificationsPanel =
    document.getElementById("notificationsPanel");
const notificationMessage =
    document.getElementById("notificationMessage");
const notificationsList =
    document.getElementById("notificationsList");
const markAllNotificationsReadButton =
    document.getElementById("markAllNotificationsReadButton");

async function requestNotificationApi(path, method = "GET") {
    const response = await fetch(
        `${API_CONFIG.NOTIFICATION_URL}${path}`,
        {
            method,
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        }
    );

    const responseText = await response.text();
    let responseBody = null;
    if (responseText) {
        try {
            responseBody = JSON.parse(responseText);
        } catch (error) {
            responseBody = null;
        }
    }

    if (response.status === 401) {
        handleSessionExpired();
        throw new Error("Your session expired. Please sign in again.");
    }

    if (!response.ok) {
        const message = responseBody?.message || responseBody?.detail;
        throw new Error(message || `Notification request failed (HTTP ${response.status}).`);
    }

    return responseBody;
}

function showNotificationMessage(message, type = "info") {
    notificationMessage.textContent = message;
    notificationMessage.style.color = type === "error"
        ? "#ff6b78"
        : type === "success"
            ? "#19c994"
            : "#93a4b8";
}

async function refreshUnreadNotificationCount() {
    try {
        const response = await requestNotificationApi(
            "/api/v1/notifications/my/unread-count"
        );
        const unreadCount = Number(response?.unreadCount);

        if (!Number.isInteger(unreadCount) || unreadCount < 0) {
            throw new Error("Notification Service returned an invalid unread count.");
        }

        notificationCountBadge.textContent = String(unreadCount);
        markAllNotificationsReadButton.disabled = unreadCount === 0;
        return unreadCount;
    } catch (error) {
        if (error.message !== "Your session expired. Please sign in again.") {
            notificationCountBadge.textContent = "!";
            markAllNotificationsReadButton.disabled = true;
            if (!notificationsPanel.hidden) {
                showNotificationMessage(
                    "Unable to load the unread notification count.",
                    "error"
                );
            }
        }
        return null;
    }
}

function createNotificationListItem(notification) {
    const isRead = notification.read === true;
    const listItem = document.createElement("li");
    listItem.className = isRead
        ? "notification-item"
        : "notification-item notification-item--unread";

    const header = document.createElement("div");
    header.className = "notification-item-header";

    const title = document.createElement("h3");
    title.textContent = notification.title || "Notification";

    const readStatus = document.createElement("span");
    readStatus.className = "notification-read-status";
    readStatus.textContent = isRead ? "Read" : "Unread";
    header.append(title, readStatus);

    const message = document.createElement("p");
    message.className = "notification-item-message";
    message.textContent = notification.message || "";

    const date = document.createElement("time");
    date.className = "notification-item-date";
    date.dateTime = notification.createdAt || "";
    date.textContent = notification.createdAt
        ? new Date(notification.createdAt).toLocaleString("en-IN")
        : "Date unavailable";

    listItem.append(header, message, date);

    if (!isRead) {
        const markReadButton = document.createElement("button");
        markReadButton.type = "button";
        markReadButton.textContent = "Mark as read";
        markReadButton.addEventListener("click", async function () {
            markReadButton.disabled = true;
            try {
                await requestNotificationApi(
                    `/api/v1/notifications/${encodeURIComponent(notification.id)}/read`,
                    "PATCH"
                );
                await Promise.all([
                    loadMyNotifications(),
                    refreshUnreadNotificationCount()
                ]);
                showNotificationMessage("Notification marked as read.", "success");
            } catch (error) {
                showNotificationMessage(
                    error.message || "Unable to mark this notification as read.",
                    "error"
                );
                markReadButton.disabled = false;
            }
        });
        listItem.appendChild(markReadButton);
    }

    return listItem;
}

async function loadMyNotifications() {
    showNotificationMessage("Loading notifications...");
    notificationsList.replaceChildren();
    markAllNotificationsReadButton.disabled = true;

    try {
        const notifications = await requestNotificationApi(
            "/api/v1/notifications/my"
        );

        if (!Array.isArray(notifications)) {
            throw new Error("Notification Service returned an invalid notification list.");
        }

        if (notifications.length === 0) {
            showNotificationMessage("You do not have any notifications yet.");
            return;
        }

        notifications.forEach(notification => {
            notificationsList.appendChild(
                createNotificationListItem(notification)
            );
        });
        showNotificationMessage("Notifications loaded.", "success");
    } catch (error) {
        notificationsList.replaceChildren();
        if (error.message !== "Your session expired. Please sign in again.") {
            showNotificationMessage(
                error instanceof TypeError
                    ? "Could not connect to Notification Service."
                    : error.message || "Unable to load notifications.",
                "error"
            );
        }
    }
}

notificationToggleButton.addEventListener("click", async function () {
    const isOpening = notificationsPanel.hidden;
    notificationsPanel.hidden = !isOpening;
    notificationToggleButton.setAttribute("aria-expanded", String(isOpening));
    notificationToggleButton.textContent = isOpening
        ? "Close notifications"
        : "Open notifications";

    if (isOpening) {
        await Promise.all([
            loadMyNotifications(),
            refreshUnreadNotificationCount()
        ]);
    }
});

markAllNotificationsReadButton.addEventListener("click", async function () {
    markAllNotificationsReadButton.disabled = true;
    try {
        const response = await requestNotificationApi(
            "/api/v1/notifications/my/read-all",
            "PATCH"
        );
        await Promise.all([
            loadMyNotifications(),
            refreshUnreadNotificationCount()
        ]);
        showNotificationMessage(
            `Marked ${Number(response?.updatedCount) || 0} notification(s) as read.`,
            "success"
        );
    } catch (error) {
        if (error.message !== "Your session expired. Please sign in again.") {
            showNotificationMessage(
                error.message || "Unable to mark notifications as read.",
                "error"
            );
        }
        markAllNotificationsReadButton.disabled = false;
    }
});

// Load dashboard data
loadProfile();
loadAccount();
loadTransactions();
loadChangeRequests();
refreshUnreadNotificationCount();
