import Ajv from "ajv/dist/2020";
import addFormats from "ajv-formats";

const ajv = new Ajv({ allErrors: true, strict: false });
addFormats(ajv);

const schemaInput = document.getElementById("schema-input");
const payloadInput = document.getElementById("payload-input");
const validateBtn = document.getElementById("validate-btn");
const resultOutput = document.getElementById("result-output");
const statusIndicator = document.querySelector(".status-indicator");

// Modal Logic
const btnIntegrate = document.getElementById("btn-integrate");
const modalOverlay = document.getElementById("integration-modal");
const btnCloseModal = document.getElementById("close-modal");

function openModal() {
    modalOverlay.classList.remove("hidden");
}

function closeModal() {
    modalOverlay.classList.add("hidden");
}

btnIntegrate.addEventListener("click", openModal);
btnCloseModal.addEventListener("click", closeModal);
modalOverlay.addEventListener("click", (e) => {
    if (e.target === modalOverlay) closeModal();
});

// Theme Logic
const themeToggle = document.getElementById("theme-toggle");
const htmlEl = document.documentElement;

themeToggle.addEventListener("click", () => {
    const currentTheme = htmlEl.getAttribute("data-theme");
    const newTheme = currentTheme === "dark" ? "light" : "dark";
    htmlEl.setAttribute("data-theme", newTheme);
});

function safeParse(jsonString) {
    try {
        // Strip single-line comments that are preceded by whitespace
        // This avoids stripping URLs like https:// while allowing // comments
        let processed = jsonString.replace(/\s\/\/.*/g, '');
        // Strip lines that start with // (at the beginning of the line)
        processed = processed.replace(/^\/\/.*/gm, '');
        // Strip trailing commas before closing braces/brackets (common in dev scratchpads)
        processed = processed.replace(/,(\s*[\]}])/g, '$1');
        
        return { data: JSON.parse(processed), error: null };
    } catch (e) {
        return { data: null, error: e.message };
    }
}

validateBtn.addEventListener("click", () => {
    resultOutput.className = "result-console__content";
    statusIndicator.className = "status-indicator";
    resultOutput.textContent = "Intercepting request...\n";

    const schemaRes = safeParse(schemaInput.value);
    const payloadRes = safeParse(payloadInput.value);

    if (schemaRes.error) {
        showError(`Failed to parse JSON Schema: ${schemaRes.error}`);
        return;
    }
    if (payloadRes.error) {
        showError(`Failed to parse Payload: ${payloadRes.error}`);
        return;
    }

    try {
        const validate = ajv.compile(schemaRes.data);
        const valid = validate(payloadRes.data);

        if (valid) {
            showSuccess("✅ VALIDATION PASSED: Event successfully published to Kafka!");
        } else {
            const formattedErrors = validate.errors.map(err => {
                if (err.keyword === 'required') {
                    return `[MissingField]: '${err.params.missingProperty}' is required`;
                }
                return `[SchemaValidationError]: '${err.instancePath}' ${err.message}`;
            }).join("\n");
            
            showError(`🚫 POISON PILL BLOCKED by Schema Guard!\n\nDetails:\n${formattedErrors}`);
        }
    } catch (e) {
        showError(`Schema compilation error: ${e.message}`);
    }
});

function showSuccess(msg) {
    resultOutput.textContent = msg;
    resultOutput.classList.add("status-success");
    statusIndicator.classList.add("status-success-indicator");
}

function showError(msg) {
    resultOutput.textContent = msg;
    resultOutput.classList.add("status-error");
    statusIndicator.classList.add("status-error-indicator");
    // Trigger reflow for animation
    resultOutput.parentElement.classList.remove("animate-error");
    void resultOutput.parentElement.offsetWidth;
    resultOutput.parentElement.classList.add("animate-error");
}
