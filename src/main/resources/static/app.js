let actionInProgress = false
let idInputIssue
let amountInputIssue
let issueButton
let numberInputBulk
let amountInputBulk
let bulkButton
let idInputRedeem
let amountInputRedeem
let redeemButton
let notification
let notificationText
let notificationButton
let tableSource

function setDomElements() {
    idInputIssue = document.getElementById("id-input-issue")
    amountInputIssue = document.getElementById("amount-input-issue")
    issueButton = document.getElementById("issue-button")
    numberInputBulk = document.getElementById("number-input-bulk")
    amountInputBulk = document.getElementById("amount-input-bulk")
    bulkButton = document.getElementById("bulk-button")
    idInputRedeem = document.getElementById("id-input-redeem")
    amountInputRedeem = document.getElementById("amount-input-redeem")
    redeemButton = document.getElementById("redeem-button")
    notification = document.getElementById("notification")
    notificationText = document.getElementById("notification-text")
    notificationButton = document.getElementById("notification-button")
}

function maybeSwitchIssueState() {
    if (issueButton.disabled && !actionInProgress && idInputIssue.value !== "" && amountInputIssue.value !== "") {
        issueButton.disabled = false
    } else if (idInputIssue.value === "" || amountInputIssue.value === "") {
        issueButton.disabled = true
    }
}

function maybeSwitchBulkState() {
    if (bulkButton.disabled && !actionInProgress && numberInputBulk.value !== "" && amountInputBulk.value !== "") {
        bulkButton.disabled = false
    } else if (numberInputBulk.value === "" && amountInputBulk.value === "") {
        bulkButton.disabled = true
    }
}

function maybeSwitchRedeemState() {
    if (redeemButton.disabled && !actionInProgress && idInputRedeem.value !== "" && amountInputRedeem.value !== "") {
        redeemButton.disabled = false
    } else if (idInputRedeem.value === "" || amountInputRedeem.value === "") {
        redeemButton.disabled = true
    }
}

function maybeSwitchAll(){
    maybeSwitchIssueState()
    maybeSwitchBulkState()
    maybeSwitchRedeemState()
}

function disableAllButtons() {
    issueButton.disabled = true
    bulkButton.disabled = true
    redeemButton.disabled = true
}

function hideNotification() {
    notification.style.visibility = "hidden"
}

function removeColorClassesFromNotification() {
    notification.classList.remove("is-success", "is-info", "is-danger")
}

async function handleResult(result) {
    actionInProgress = false
    hideNotification()
    removeColorClassesFromNotification()
    if (result["isSuccess"] === true) {
        notification.classList.add("is-success")
        notificationText.innerHTML = "Success"
    } else {
        notification.classList.add("is-danger")
        notificationText.innerHTML = result["error"]
    }
    notification.style.visibility = ""
    maybeSwitchAll()
}


async function issueCard() {
    actionInProgress = true
    disableAllButtons()
    const value = idInputIssue.value
    const amount = amountInputIssue.value
    idInputIssue.value = ""
    amountInputIssue.value = ""
    const response = await fetch("/giftcard/issue/id/" + value + "/amount/" + amount, {
        method: "POST"
    });
    response.json().then(result => handleResult(result))
}

async function bulkCard() {
    actionInProgress = true
    disableAllButtons()
    const number = numberInputBulk.value
    const amount = amountInputBulk.value
    numberInputBulk.value = ""
    amountInputBulk.value = ""
    removeColorClassesFromNotification()
    notification.classList.add("is-info")
    const progressBar = document.createElement('progress')
    notificationText.innerHTML = ""
    notificationText.appendChild(progressBar)
    progressBar["value"] = 0
    progressBar["max"] = number
    progressBar.classList.add("progress")
    notificationButton.style.display = "none"
    notification.style.visibility = ""
    let successCount = 0
    let failedCount = 0
    const source = new EventSource("/giftcard/bulkissue/number/" + number + "/amount/" + amount)
    source.onmessage = function (event) {
        const result = JSON.parse(event.data)
        if (result["isSuccess"] === true) {
            successCount++
        } else {
            failedCount++
            console.log(result["error"])
        }
        const total = successCount + failedCount
        progressBar["value"] = total
        if (total >= number) {
            source.close()
            notificationButton.style.display = ""
            if (failedCount === 0) {
                handleResult({"isSuccess": true})
            } else {
                handleResult({"isSuccess": false, "error": failedCount + " of the " + number + " ended in failure."})
            }
        }
    };
    source.onerror = function (error) {
        source.close()
        console.log(error)
        notificationButton.style.display = ""
        handleResult({"isSuccess": false, "error": "Failed to connect to the server"})
    }
}

async function redeemCard() {
    actionInProgress = true
    disableAllButtons()
    const id = idInputRedeem.value
    const amount = amountInputRedeem.value
    idInputRedeem.value = ""
    amountInputRedeem.value = ""
    const response = await fetch("/giftcard/redeem/id/" + id + "/amount/" + amount, {
        method: "POST"
    });
    response.json().then(result => {
        handleResult(result)
    })
}

async function updateCount() {
    const count = document.getElementById("count")
    const lastEvent = document.getElementById("last-event")
    const source = new EventSource("/giftcard/subscribe-count")
    source.onmessage = function (event) {
        const cardData = JSON.parse(event.data)
        count.innerHTML = cardData["count"]
        lastEvent.innerHTML = new Date(cardData["lastEvent"]).toLocaleString()
    };
}

async function updateTable(maxRows) {
    if (tableSource !== undefined){
        tableSource.close()
    }
    let tableIds = []
    const tableBody = document.getElementById("table-body")
    tableBody.innerHTML = ""
    tableSource = new EventSource("/giftcard/subscribe/limit/" + maxRows)
    tableSource.onmessage = function (event) {
        const cardData = JSON.parse(event.data)
        const cardId = "card_" + cardData["id"]
        if (tableIds.includes(cardId)) {
            document.getElementById(cardId).remove()
            tableIds = tableIds.filter(item => item !== cardId)
        } else if (tableIds.length >= maxRows) {
            const toRemove = tableIds.pop()
            document.getElementById(toRemove).remove()
        }
        tableIds.unshift(cardId)
        const row = tableBody.insertRow(0)
        row.id = cardId
        const cell1 = row.insertCell(0)
        const cell2 = row.insertCell(1)
        const cell3 = row.insertCell(2)
        const cell4 = row.insertCell(3)
        const cell5 = row.insertCell(4)
        cell1.innerHTML = cardData["id"]
        cell2.innerHTML = cardData["initialValue"]
        cell3.innerHTML = cardData["remainingValue"]
        cell4.innerHTML = new Date(cardData["issued"]).toLocaleString()
        cell5.innerHTML = new Date(cardData["lastUpdated"]).toLocaleString()
    };
}

function setListeners() {
    notificationButton.addEventListener("click", () => {
        void hideNotification()
    });
    issueButton.addEventListener("click", () => {
        void issueCard();
    });
    bulkButton.addEventListener("click", () => {
        void bulkCard();
    });
    redeemButton.addEventListener("click", () => {
        void redeemCard();
    });
    idInputIssue.addEventListener("keyup", () => {
        maybeSwitchIssueState()
    })
    amountInputIssue.addEventListener("keyup", () => {
        maybeSwitchIssueState()
    })
    numberInputBulk.addEventListener("keyup", () => {
        maybeSwitchBulkState()
    })
    amountInputBulk.addEventListener("keyup", () => {
        maybeSwitchBulkState()
    })
    idInputRedeem.addEventListener("keyup", () => {
        maybeSwitchRedeemState()
    })
    amountInputRedeem.addEventListener("keyup", () => {
        maybeSwitchRedeemState()
    })
    addEventListener("paste", () => {
        setTimeout(maybeSwitchAll, 10)
    })
    document.getElementById("table-size-select").addEventListener("change", event => {
        void updateTable(event.target.value)
    })
}

window.addEventListener("load", () => {
    setDomElements()
    setListeners()
    void updateCount()
    void updateTable(20)
})