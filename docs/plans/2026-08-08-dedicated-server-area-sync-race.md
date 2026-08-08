# Dedicated Server Area Sync Race Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Ensure dedicated-server area data is persisted and loaded only after the client has selected the final server world folder.

**Architecture:** Buffer the latest area payload for each supported dimension while `ClientWorldFolderManager` is not initialized, with the cache bound to the active `ClientPlayNetworkHandler`. Replay buffered payloads through the existing processing path after final world-folder initialization, invalidate stale asynchronous detection results, publish the current area synchronously, and clear connection-owned state on disconnect.

**Tech Stack:** Java 17, Fabric Networking API, Gradle

---

### Task 1: Extract reusable client area-data processing

**Files:**
- Modify: `src/client/java/areahint/network/ClientNetworking.java`

**Step 1:** Add an insertion-ordered map containing the latest pending payload for each dimension.

**Step 2:** Change `handleAreaData` so an uninitialized world folder caches the payload instead of writing it to the fallback path.

**Step 3:** Reject unsupported dimensions before caching and discard queued tasks whose network handler is no longer current.

**Step 4:** Extract the existing save, Xaero refresh, current-dimension detector reload, and BoundViz reload operations into one private method used by both immediate processing and replay.

**Step 5:** Add methods to replay pending data for the matching handler and invalidate data owned by a disconnected handler.

### Task 2: Replay data after final world-folder initialization

**Files:**
- Modify: `src/client/java/areahint/network/ClientWorldNetworking.java`

**Step 1:** After `finalizeWorldInitialization`, replay all pending area payloads.

**Step 2:** Ignore world-information tasks whose handler no longer matches the active client connection.

**Step 3:** Keep the existing current-area reload after replay so title and detection state are recalculated from the final directory.

### Task 3: Remove initialization-order gaps

**Files:**
- Modify: `src/client/java/areahint/world/ClientWorldFolderManager.java`
- Modify: `src/client/java/areahint/AreashintClient.java`
- Modify: `src/client/java/areahint/detection/AsyncAreaDetector.java`

**Step 1:** Establish the temporary folder state before requesting world information.

**Step 2:** Invalidate pending payloads immediately on disconnect, then run all remaining client-state cleanup on the client thread.

**Step 3:** Add an asynchronous detection generation so reset results cannot be overwritten by an older in-flight task.

**Step 4:** Publish the synchronous forced-redetection result through `AreaChangeTracker` after network synchronization.

### Task 4: Verify

**Files:**
- Inspect: all modified files

**Step 1:** Run `.\gradlew.bat build` and require exit code 0.

**Step 2:** Run `git diff --check` and require no whitespace errors.

**Step 3:** Inspect `git diff` and confirm only the approved synchronization behavior and plan documents changed.
