WAIS Project Development Guide (AGENTS.md)

You are an expert Wear OS developer assisting with the WAIS (Wearable Audio Interest Study) app. You must prioritize correctness over speed.

CRITICAL INSTRUCTION: We are **restarting development to ensure stability**.

You must **modify or add ONLY ONE function at a time**. After generating code, you must **verify it matches the "Known Working Patterns" below (taken from the `health-samples` repo)** before proceeding.

---

## 1. The “Source of Truth”

For all Health Services API implementation details (Passive Data, Bootstrapping, Permissions, Versioning, Passive Listener), you must strictly follow the patterns found in this repository:

- **Primary reference repo (MANDATORY):**  
  [`health-samples/health-services`](https://github.com/JimmyRaoUF/health-samples/tree/main/health-services)

- **Primary module/example (MANDATORY):**  
  [`health-services/PassiveDataCompose`](https://github.com/JimmyRaoUF/health-samples/tree/main/health-services/PassiveDataCompose)

This repo is **definitely correct** for:
- Health Services API version checks
- Passive listener configuration and lifecycle
- Permissions and capabilities flow
- Use of `PassiveListenerService` and related APIs

### 1.1. How to use the sample as a template

Before suggesting or modifying any WAIS code related to Health Services:

1. **Locate the equivalent class in `PassiveDataCompose`**, such as:
    - `HealthServicesRepository` (or similarly named repository)
    - `PassiveDataService` / `PassiveListenerService` implementation
    - Permissions / capability checking classes
    - Version / availability checking utilities

2. **Mirror the structure and flow**, not just the API calls:
    - How the sample:
        - Checks Health Services availability and version
        - Requests permissions
        - Registers/unregisters passive listeners
        - Handles capabilities (what data types are supported)
        - Manages `PassiveListenerConfig` and its flags

3. **Do NOT invent API usages.**
    - If a method/class is **not present** in the `health-samples` repo, you must:
        - Treat it as **suspicious**, even if you saw it in other docs.
        - Check if it is tied to a newer API level or Play Services version.
        - Prefer **only** patterns that appear in `health-samples`.

4. **API versioning and capability checks must follow the sample exactly.**
    - Use the same:
        - `HealthServicesClient` acquisition pattern
        - Capability / availability checks
        - Version- or feature-gating logic

---

## 2. “Never Again” Technical Rules (Strict Mandates)

### 2.A. AndroidManifest.xml Correctness

You must ensure the Manifest follows the same patterns as the `PassiveDataCompose` sample:

1. **Passive Listener Service registration**
    - Always declare a service similar to the sample’s `PassiveListenerService` (name can vary, pattern must not).
    - Use **short component names** (e.g., `.PassiveDataService`, `.PassiveListenerService`) to avoid package path typos.

2. **Required `<intent-filter>`**
    - The service **must** include the same intent filters used in `PassiveDataCompose` to receive background updates from Health Services.
    - Do **not** invent custom actions; copy the pattern from the sample and only adjust the package/service name.

3. **Exported / permission attributes**
    - Match the sample’s values for `exported`, `permission`, and any Health Services-specific attributes.
    - If unsure, open the corresponding service declaration in `PassiveDataCompose` and replicate its flags.

### 2.B. Health Services API & Dependencies

1. **Mandatory dependencies**

Before writing or modifying any code that uses:
- `ListenableFuture`
- `.await()` extensions for futures
- `callbackFlow` wrappers around futures

you must verify `build.gradle.kts` (module-level) includes the **same dependencies** as `PassiveDataCompose`, including (but not limited to):

- `androidx.concurrent:concurrent-futures-ktx`
- `com.google.guava:guava`

If WAIS uses additional versions, **they must not conflict** with the versions used in the sample.

2. **User Activity State**

- **Do NOT request `DataType.USER_ACTIVITY_STATE` as a direct data type.**  
  It does **not** exist as a direct data type.
- Instead, follow the sample’s correct pattern:
    - Enable via `PassiveListenerConfig` (or its equivalent) using the flag:
        - `shouldUserActivityInfoBeRequested = true`
    - Do **not** introduce any other undocumented flags or experimental fields.

3. **Repository pattern**

- `HealthServicesRepository`-like components:
    - **MUST be `class`es, not `object` singletons**, when they have dependencies (e.g., `Context`, `HealthServicesClient`, coroutine scopes).
    - Initialization must follow the **injectable** or **factory** pattern seen in the sample:
        - No hard-coded global singletons with implicit contexts.
    - Do **not** instantiate repositories in a way that diverges from the sample:
        - Follow the same construction pattern and lifecycle scope (e.g., ViewModel, service, etc.).

### 2.C. File Management & Safety

1. **No duplicate classes or filenames**

Before creating any new file (e.g., `L2DecisionEngine`, `HealthServicesRepository`, or any new service):

- You **must** search the entire project for an existing file with:
    - The same class name, or
    - The same conceptual responsibility.
- If a file/class already exists:
    - **Modify the existing one** instead of creating a competing copy.
    - Avoid “Redeclaration” compiler errors and split-logic confusion.

2. **Verify imports and package structure**

- Imports must match the **actual package paths** used in `health-samples`.
- Do **not**:
    - Import classes from guessed or wrong packages.
    - Introduce phantom imports (classes that don’t exist).
- If an import path differs from the sample, treat it as suspicious and re-check.

---

## 3. Core Project Constraints (Privacy-by-Design)

1. **Operation mode: Offline-first**

- WAIS must **not**:
    - Perform HTTP requests.
    - Call cloud APIs.
    - Use external Wi‑Fi/Bluetooth connections for data transfer beyond what’s needed for sensor context.
- All inference and storage is **on-device**.

2. **Target hardware**

- Target device: Samsung Galaxy Watch FE or Galaxy Watch 7 (Wear OS).
- Ensure:
    - APIs used are compatible with these devices.
    - Health Services usage matches what is supported on these watches, as demonstrated by the sample repo.

---

## 4. Sensor & Trigger Architecture (The Logic)

We use a **hybrid motion/context trigger** design:

1. **Motion (primary trigger)**

- Use F-ratio on 15-second windows of accelerometer/gyroscope:
    - $$F = S_B / S_W$$
    - Where:
        - $S_B$ = between-class variance
        - $S_W$ = within-class variance
- Sampling:
    - Use `SensorManager` with `SENSOR_DELAY_NORMAL`.
    - Use `conflate()` for raw flows to reduce battery usage and avoid backpressure.

2. **Context (secondary / refinement)**

- BLE scanning with Intersection-over-Union (IoU) metric for device sets.
- Ambient light sensor.
- Barometer deltas for altitude/pressure changes.

3. **Adaptive threshold**

- The F-ratio threshold $Th$ is **adaptive**:
    - When BLE context **changes significantly**:
        - Lower $Th$ (higher sensitivity).
    - When context is **stable**:
        - Raise $Th$ (lower sensitivity).

4. **Background monitoring via Health Services**

- Long-running background sensing must be handled via Health Services passive monitoring:
    - **Passive listener** is the primary mechanism for background updates.
    - Do not build your own long-running, high-rate foreground sensor loop when the sample uses passive monitoring.

---

## 5. UI/UX Guidelines

1. **Haptics**

- 1 haptic buzz for **start**.
- 2 haptic buzzes for **stop**.

2. **Privacy**

- Immediately after recording, show a **non-blocking “Discard” prompt**.
    - User can discard without being blocked from other actions.

3. **Framework**

- Use **Jetpack Compose for Wear OS**.
- Follow composable and state management patterns similar to `PassiveDataCompose` where relevant (e.g., observing flows/state from repositories).

---

## 6. Workflow Rules (Development Process)

### 6.1. Atomic Changes (MOST IMPORTANT)

- You must **modify or add ONLY ONE function at a time**.
- This applies whether:
    - You are editing existing WAIS code, or
    - You are copying/adapting code from `PassiveDataCompose`.

**Examples of “ONE function at a time”:**

- Allowed:
    - Add a new function `registerPassiveListener()` in a WAIS repository file, with no other changes.
    - Modify the body of `onNewData()` in a listener, but nothing else in the file.
- Not allowed:
    - Adding or changing multiple unrelated functions in a single edit.
    - Changing a function and also adding new classes/services in the same step.

### 6.2. Verification after each change

After each single-function change:

1. **Compile** the project.
2. **Compare the modified function’s structure and API usage** to the closest equivalent in `PassiveDataCompose`:
    - Health Services client usage.
    - Handling of versions and capabilities.
    - Cancellation, lifecycle, and coroutine patterns (if applicable).
3. If there is any deviation without a strong reason and explicit commentary, **revert** or **align it with the sample**.

### 6.3. Pre-PR Checklist

Before proposing a PR or major merge:

1. **Manifest / Component Naming**
    - Passive listener service present, correct, and following the sample’s pattern.
    - Short component names used where possible (e.g., `.PassiveDataService`).
    - Required intent-filters present and properly configured for Passive Listener.

2. **Dependencies**
    - `androidx.concurrent:concurrent-futures-ktx` present.
    - `com.google.guava:guava` present.
    - Versions do not conflict with those in `health-samples`.

3. **Files and Classes**
    - No duplicate filenames or class declarations.
    - No “Redeclaration” or naming collisions.
    - Imports are valid and resolve to real classes, matching the package structure of the sample (where applicable).

4. **Privacy & Storage**
    - Persistent data is encrypted, using Keystore-backed solutions where appropriate.
    - No network/cloud dependencies introduced.

---

## 7. Quick Checklist (Short)

Before approving or continuing from any new code step, verify:

1. **Passive Listener**
    - Passive listener service declared in `AndroidManifest.xml`.
    - Intent-filters match those in `PassiveDataCompose` (adjusted for package/class name).

2. **Health Services & Dependencies**
    - Required `concurrent-futures-ktx` and `guava` dependencies are present.
    - Health Services API usage (clients, capabilities, version checks, registration, config) matches patterns in `health-samples/PassiveDataCompose`.
    - `DataType.USER_ACTIVITY_STATE` is **not** used directly.
    - `shouldUserActivityInfoBeRequested = true` is used when user activity info is needed.

3. **Structure & Safety**
    - Repository classes with dependencies are `class`, not `object` singletons.
    - No duplicate/malformed filenames or classes.
    - Imports map to real, existing types; no guessed packages.

4. **Privacy & Storage**
    - Persistent data is encrypted via Keystore (or equivalent secure storage).
    - No HTTP/cloud APIs or external data transfer beyond minimal sensor context.

5. **Development Process**
    - You have modified or added **only one function** in the last step.
    - The new/changed function is verified against the sample’s pattern.
    - The project compiles.

---

## 8. Summary for Future Agents (Gemini in Android Studio)

When working on WAIS:

- **Always start** by examining the relevant file in:  
  [`health-samples/health-services/PassiveDataCompose`](https://github.com/JimmyRaoUF/health-samples/tree/main/health-services/PassiveDataCompose)

- Treat that repo as the **single source of truth** for:
    - Health Services API versioning
    - Passive listener configuration and registration
    - Capability and permission handling
    - Background update patterns

- **Never**:
    - Introduce Health Services usages that are not demonstrated in that repo.
    - Skip the manifest service + intent-filter pattern required for passive listeners.
    - Modify more than one function per step.

- **Always**:
    - Make one function-level change at a time.
    - Compile and compare to the sample before proceeding.
    - Preserve offline-first, privacy-by-design constraints.