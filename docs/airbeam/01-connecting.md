# AirBeam Communication — Feature 1: Connecting

> **What this is.** A behavioral specification for the *first* feature of the rewrite:
> **discovering and connecting to an AirBeam**. It describes *what must happen* and *why* —
> not which library, class, or platform API does it. The new app is free to choose any
> architecture, transport library, or pattern, as long as the behavior below holds.
>
> **Scope.** Only establishing a connection and reaching a known ready state. Configuring a
> session, recording, and syncing are separate features (later docs). Reconnection during an
> active session is noted where it touches connect logic but is otherwise out of scope here.
>
> **Sibling reference.** The exact V2 wire protocol (opcodes, byte layouts) lives in
> [`.claude/ble_mobile_app_guide.md`](../../.claude/ble_mobile_app_guide.md). This doc stays
> at the behavioral level and points there for byte-level detail.

---

## 1. Goal

Take the user from *"I want to use this AirBeam"* to *"the app has a live link to a specific
device and that device is in a known, ready state"* — for any of the four supported devices,
through one uniform connection concept.

**Definition of "connected / ready":** the app has an open communication link, has confirmed
the device is a supported AirBeam of a known type, has completed whatever handshake that
device requires, and has learned the device's current state (idle, or already running a
session). Only then may session configuration/recording proceed.

---

## 2. The four devices (connection-relevant differences)

There are **two transport families** and **two device generations**. Connection logic must
absorb these differences behind one concept.

| Device | Transport | How it's identified | Handshake on connect | Initial state known from |
|--------|-----------|---------------------|----------------------|--------------------------|
| **AirBeam 2** | Classic Bluetooth serial link | Advertised name contains `airbeam2` | Send device identity, pause, send auth credential | none — assumed idle |
| **AirBeam 3** | Bluetooth Low Energy | Name contains `airbeam3` | Send identity then auth credential (ordered) | none — assumed idle |
| **AirBeam Mini (gen 1)** | Bluetooth Low Energy | Name contains `airbeammini` **+** exposes the legacy service | Send identity then auth credential | none — assumed idle |
| **AirBeam Mini (gen 2)** | Bluetooth Low Energy | Name contains `airbeammini` **+** exposes the V2 service | **None** — device identity is carried later in the session config | device announces its state right after connect (idle / has-saved-session / running) |

Critical connection facts:

- **AirBeam 2 uses classic Bluetooth serial, not BLE.** It is a fundamentally different
  transport. Some target platforms have no equivalent for it — whether AirBeam 2 is supported
  at all is an open product decision (see §9). Connection logic must not assume every device
  is BLE.
- **Both Mini generations advertise the same name.** They can only be told apart by which
  service they expose. This distinction must be resolved as early as possible — ideally at
  discovery time from the advertised service identifier, so the app knows *before* opening a
  link which generation (and therefore which protocol) it is dealing with. The legacy app
  resolved this late (connect assuming gen-2, fall back to gen-1 on failure); that late
  fallback is fragile and should not be carried forward if discovery-time detection is
  possible.
- **Gen-2 needs no auth handshake.** Older devices do. The connection concept must allow a
  device to declare "I require a handshake" vs "I don't."
- **Only gen-2 reports its live state on connect.** For older devices the app assumes the
  device is idle and drives it explicitly. Gen-2 may already be mid-session (e.g. the app was
  killed, or it's a fixed device that resumed on its own) — so the app must *read* gen-2 state
  rather than assume it.

---

## 3. Preconditions

Before any connection attempt, the environment must be ready. These are behavioral gates, not
tied to any OS:

- **Permissions granted** for: scanning for and connecting to nearby wireless devices, and
  (because measurements are geo-tagged) location access. The connection flow must detect
  missing permissions and route the user to grant them rather than failing opaquely.
- **Bluetooth (and, where relevant, location services) enabled** on the device. If off, prompt
  to enable.
- If a permission or radio prerequisite is missing, connection cannot start — surface a clear,
  actionable state, not a silent failure.

---

## 4. Connection flow (device-agnostic)

The same six phases apply to every device; per-device behavior plugs into phases 3–5.

1. **Discover.** Scan for nearby AirBeams. Produce a list of candidates, each carrying at
   minimum: a display name, a stable identifier, the device *type*, and — for Mini — the
   *generation*, determined from what the device advertises. The user picks one. (For an
   automatic reconnect, the target is already known and discovery may be skipped or filtered
   to that identifier.)

2. **Identify & select.** From the chosen candidate, determine the device type and generation.
   This selection determines which protocol and handshake will be used. Selection must be
   settled here, before opening the link, whenever the advertised data allows it.

3. **Open the link.** Establish the underlying communication channel appropriate to the
   device's transport family (serial link for AirBeam 2, BLE connection otherwise). Apply a
   **connection timeout** — if the link is not established within a bounded time, treat it as a
   failure (see §6). The legacy timeout was ~30 seconds; the exact value is tunable but a bound
   must exist.

4. **Verify capability.** Once the link is open, confirm the device actually is the AirBeam
   type expected — i.e. it exposes the communication surface that type requires. If it does
   not, this is *not* a ready device: fail the attempt (and, for a mis-identified Mini, this is
   the signal that it's the other generation).

5. **Handshake & learn state.** Bring the device to a known ready state:
   - Devices that require it: send the device identity, then the auth credential, in the
     required order (older devices are order- and timing-sensitive between these two steps).
   - Establish a shared clock where the device needs one (time synchronization on connect).
   - Read the device's current state where the device reports it (gen-2): is it idle, does it
     hold a saved/unsynced session, or is it actively running one? This state drives every
     subsequent decision and must be captured, not assumed.

6. **Ready.** Mark the connection established and publish a connection status the rest of the
   app can observe. The device is now available for the next feature (configuration/recording).

---

## 5. State the app must model

Connection is not binary. The app must represent, at minimum, an observable connection status
covering:

- **Disconnected / idle** — no link.
- **Connecting** — attempt in progress (discovery, linking, or handshake).
- **Connected / ready** — link open, handshake done, device state known.
- **Connection failed** — attempt ended without reaching ready (with a reason).
- **Disconnected unexpectedly** — a previously-ready link dropped (distinct from a user-driven
  disconnect; relevant to reconnection, a later feature).

Separately, for gen-2 devices, the app must model the **device-reported state** learned in
phase 5 (idle / has-saved-session / running), because it changes what the app does next. For
older devices this device-state is implicit (assumed idle).

The connection status should be exposed as something the UI and other components can *observe*
continuously, rather than a one-shot callback — connecting, dropping, and reconnecting are
ongoing conditions, not single events.

---

## 6. Failure & edge cases the flow must handle

- **No device found / discovery timeout.** Nothing matching appeared within a bounded time →
  surface "no AirBeam found," let the user retry.
- **Link fails to open or times out.** Bounded wait exceeded, or the transport refused →
  connection-failed state with a retry path.
- **Wrong / unsupported communication surface.** The device connected but is not the expected
  AirBeam type. Fail cleanly. For a Mini, this is specifically the cue that it is the *other*
  generation — the app should then be able to try the correct protocol without the user
  re-selecting. (Best avoided entirely by resolving generation at discovery, §2.)
- **Mini generation ambiguity.** If generation cannot be determined before linking, the app
  must have a deterministic strategy to discover it and settle on the right protocol, without
  leaving the connection half-initialized or leaking a partial link. Any transitional link
  opened while probing must be fully torn down before the correct one is opened.
- **Drop mid-handshake.** A disconnect before "ready" must not be mistaken for a ready
  connection, and must clean up any partial state.
- **Device already in a session (gen-2).** Not an error — a normal outcome the app learns in
  phase 5 and carries into later features.

---

## 7. What the app learns from a connection

By the time a connection is "ready," the app should hold:

- The device **type and generation** (drives protocol, capabilities, later config shape).
- A **stable device identifier** (used to attribute measurements and to target reconnects).
- Which **sensor metrics** the device offers, or a way to ask for them (particulate matter
  variants, and on some devices temperature/humidity). Older devices reveal this as data
  starts flowing; gen-2 can be *asked* what it supports.
- Whether **battery level** is reported by this device, and by what means (some report it
  continuously, some on request, some not at all).
- For gen-2: the device's **current session state**.

These become inputs to the configuration/recording feature — the connection feature's job is
to make them available, not to act on them.

---

## 8. Acceptance criteria for Feature 1

The connecting feature is done when:

1. A user can discover nearby AirBeams and see each one's type (and Mini generation).
2. The app can open a link to each supported device type and reach the "ready" state, applying
   the correct per-device handshake.
3. Gen-2 device state (idle / has-saved-session / running) is read and available after connect.
4. All failure cases in §6 produce a clear, observable status and a retry path — never a silent
   hang or a false "connected."
5. Connection status is exposed as an observable stream the rest of the app can react to.
6. Mini gen-1 vs gen-2 is resolved correctly, ideally at discovery, with a clean deterministic
   fallback if not.

---

## 9. Open questions (resolve while designing Feature 1)

- **Is AirBeam 2 in scope?** Its classic-serial transport has no equivalent on some target
  platforms. Support it (transport-gated to platforms that can), or drop it?
- **Discovery-time generation detection for Mini** — confirm the advertised data exposes the
  distinguishing service identifier on the target platforms, so late fallback can be avoided.
- **How is the auth credential obtained** for the devices that need one, and does the modern
  backend/account model still require it in the same form?
- **Connection ownership & lifetime** — connections must survive the app being backgrounded
  during a session (a recording concern), which influences where the connection is *held*.
  Decide the owning component early even though recording is a later feature.
- **Automatic vs manual connect** — reconnection reuses most of this flow with a known target;
  design the connect flow so it can be driven both by user selection and by an automatic
  retry.
