/* ============================================================
 * MCA Form DIR-12 – client-side behaviour
 * Handles: segment navigation, add/remove particulars,
 * CIN pre-fill (mock), DIN/PAN verify (mock), validation,
 * save draft (localStorage) and submit (JSON preview).
 * ============================================================ */
(function () {
  "use strict";

  const form = document.getElementById("dir12Form");
  const msgBox = document.getElementById("formMessage");
  const STORAGE_KEY = "dir12-draft";

  /* ---------- Validation patterns ---------- */
  const PATTERNS = {
    // CIN: 1 letter + 5 digits + 2 letters + 4 digits + 3 letters + 6 digits
    cin: /^[LUlu]\d{5}[A-Za-z]{2}\d{4}[A-Za-z]{3}\d{6}$/,
    din: /^\d{8}$/,
    pan: /^[A-Za-z]{5}\d{4}[A-Za-z]$/,
    email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  };

  /* ============================================================
   * 1. Segment navigation (scroll + active state)
   * ============================================================ */
  const segLinks = document.querySelectorAll(".seg-link");
  segLinks.forEach((link) => {
    link.addEventListener("click", () => {
      const target = document.getElementById(link.dataset.seg);
      if (!target) return;
      segLinks.forEach((l) => l.classList.remove("is-active"));
      link.classList.add("is-active");
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });

  /* ============================================================
   * 2. Repeatable particular entries (appointment/cessation/change)
   * ============================================================ */
  const lists = {
    appointment: document.getElementById("appointmentList"),
    cessation: document.getElementById("cessationList"),
    change: document.getElementById("changeList"),
  };
  const templates = {
    appointment: document.getElementById("tpl-appointment"),
    cessation: document.getElementById("tpl-cessation"),
    change: document.getElementById("tpl-change"),
  };
  const counters = { appointment: 0, cessation: 0, change: 0 };

  function addEntry(type) {
    const tpl = templates[type];
    const list = lists[type];
    if (!tpl || !list) return null;

    counters[type] += 1;
    const uid = `${type}-${counters[type]}-${Date.now()}`;
    const node = tpl.content.firstElementChild.cloneNode(true);

    // Give radio groups unique names so entries don't collide
    node.querySelectorAll("input[type=radio]").forEach((r) => {
      r.name = r.name.replace("__IDX", "-" + uid);
    });
    node.querySelector(".entry__idx").textContent = list.children.length + 1;

    // Remove handler
    node.querySelector("[data-remove]").addEventListener("click", () => {
      node.remove();
      renumber(list, type);
      syncCounts();
    });
    // Verify (mock) handler
    const verifyBtn = node.querySelector("[data-verify]");
    if (verifyBtn) verifyBtn.addEventListener("click", () => verifyDinPan(node));

    list.appendChild(node);
    syncCounts();
    return node;
  }

  function renumber(list, type) {
    Array.from(list.children).forEach((child, i) => {
      const idx = child.querySelector(".entry__idx");
      if (idx) idx.textContent = i + 1;
    });
  }

  document.querySelectorAll("[data-add]").forEach((btn) => {
    btn.addEventListener("click", () => addEntry(btn.dataset.add));
  });

  /* Keep the "number of..." fields in sync with actual entries */
  function syncCounts() {
    document.getElementById("numAppointment").value = lists.appointment.children.length;
    document.getElementById("numCessation").value = lists.cessation.children.length;
    document.getElementById("numChange").value = lists.change.children.length;
  }

  /* ============================================================
   * 3. Mock Pre-fill (CIN) and DIN/PAN verify
   *    (replace fetch stubs with real API calls to your backend)
   * ============================================================ */
  const prefillBtn = document.getElementById("prefillBtn");
  prefillBtn.addEventListener("click", () => {
    const cin = document.getElementById("cin").value.trim();
    if (!PATTERNS.cin.test(cin)) {
      flash("Enter a valid 21-character CIN before pre-filling.", "error");
      markField("cin", false);
      return;
    }
    markField("cin", true);
    // --- Mock data. Swap for: fetch(`/api/company?cin=${cin}`) ---
    const mock = {
      companyName: "EKMICRO TECHNOLOGIES PRIVATE LIMITED",
      regdOffice: "3rd Floor, Tower B, Cyber Park, Sector 62, Gurugram, Haryana - 122001, India",
      companyEmail: "compliance@ekmicro.example",
    };
    document.getElementById("companyName").value = mock.companyName;
    document.getElementById("regdOffice").value = mock.regdOffice;
    if (!document.getElementById("companyEmail").value)
      document.getElementById("companyEmail").value = mock.companyEmail;
    flash("Company details pre-filled from CIN " + cin + ".", "success");
  });

  function verifyDinPan(entry) {
    const input = entry.querySelector('input[name$="_din"]');
    const nameInput = entry.querySelector('input[name$="_name"]');
    if (!input) return;
    const val = input.value.trim();
    const ok = PATTERNS.din.test(val) || PATTERNS.pan.test(val);
    if (!ok) {
      flash("Enter a valid 8-digit DIN or a valid PAN to verify.", "error");
      setInvalid(input, true);
      return;
    }
    setInvalid(input, false);
    // --- Mock verification. Swap for: fetch(`/api/verify?id=${val}`) ---
    if (nameInput && !nameInput.value) nameInput.value = "VERIFIED HOLDER NAME";
    flash("DIN/PAN " + val + " verified successfully (mock).", "success");
  }

  /* ============================================================
   * 4. Validation helpers
   * ============================================================ */
  function fieldWrap(el) { return el.closest(".field"); }

  function setInvalid(el, invalid, message) {
    const wrap = fieldWrap(el);
    if (!wrap) return;
    wrap.classList.toggle("invalid", invalid);
    if (message) {
      let em = wrap.querySelector(".error-msg");
      if (!em) {
        em = document.createElement("span");
        em.className = "error-msg";
        wrap.appendChild(em);
      }
      em.textContent = message;
    }
  }

  function markField(id, valid) {
    const el = document.getElementById(id);
    if (el) setInvalid(el, !valid);
  }

  function validateForm() {
    const errors = [];
    clearAllInvalid();

    // Company details
    const cin = document.getElementById("cin");
    if (!PATTERNS.cin.test(cin.value.trim())) {
      errors.push("Valid CIN is required.");
      setInvalid(cin, true, "Enter a valid 21-character CIN.");
    }
    const email = document.getElementById("companyEmail");
    if (!PATTERNS.email.test(email.value.trim())) {
      errors.push("Valid company e-mail is required.");
      setInvalid(email, true, "Enter a valid e-mail address.");
    }

    // At least one particular
    const total = lists.appointment.children.length +
                  lists.cessation.children.length +
                  lists.change.children.length;
    if (total === 0) {
      errors.push("Add at least one appointment, cessation or change entry.");
    }

    // Validate each entry's required fields
    validateEntries(lists.appointment, ["ap_category", "ap_din", "ap_name", "ap_date"], errors);
    validateEntries(lists.cessation, ["ce_din", "ce_name", "ce_date", "ce_reason"], errors);
    validateEntries(lists.change, ["ch_din", "ch_name", "ch_newDesignation", "ch_date"], errors);

    // Signatory
    const signerId = document.getElementById("signerId");
    if (!(PATTERNS.din.test(signerId.value.trim()) || PATTERNS.pan.test(signerId.value.trim()))) {
      errors.push("Signatory DIN/PAN is required.");
      setInvalid(signerId, true, "Enter a valid DIN (8 digits) or PAN.");
    }
    const signerName = document.getElementById("signerName");
    if (!signerName.value.trim()) {
      errors.push("Signatory name is required.");
      setInvalid(signerName, true, "Required.");
    }

    return errors;
  }

  function validateEntries(list, requiredNames, errors) {
    Array.from(list.children).forEach((entry, i) => {
      requiredNames.forEach((base) => {
        const el = entry.querySelector(`[name="${base}"]`);
        if (!el) return;
        let bad = !el.value.trim();
        if (!bad && base.endsWith("_din")) {
          bad = !(PATTERNS.din.test(el.value.trim()) || PATTERNS.pan.test(el.value.trim()));
        }
        if (bad) {
          setInvalid(el, true, "Required / invalid.");
          errors.push(`Entry #${i + 1}: "${base}" is required or invalid.`);
        }
      });
    });
  }

  function clearAllInvalid() {
    form.querySelectorAll(".field.invalid").forEach((f) => f.classList.remove("invalid"));
  }

  /* ============================================================
   * 5. Messaging
   * ============================================================ */
  function flash(text, type) {
    msgBox.textContent = text;
    msgBox.className = "form-message show " + (type || "");
    msgBox.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  /* ============================================================
   * 6. Serialise the form to a plain object
   * ============================================================ */
  function collectData() {
    const data = {
      company: {
        cin: val("cin"),
        gln: val("gln"),
        name: val("companyName"),
        regdOffice: val("regdOffice"),
        email: val("companyEmail"),
      },
      purpose: Array.from(form.querySelectorAll('input[name="purpose"]:checked')).map((c) => c.value),
      counts: {
        appointments: lists.appointment.children.length,
        cessations: lists.cessation.children.length,
        changes: lists.change.children.length,
      },
      appointments: readEntries(lists.appointment),
      cessations: readEntries(lists.cessation),
      changes: readEntries(lists.change),
      declaration: {
        resolutionNo: val("resNo"),
        resolutionDate: val("resDate"),
        signerType: radio("signerType"),
        signerId: val("signerId"),
        signerName: val("signerName"),
      },
      certification: {
        professionalType: radio("proType"),
        membershipClass: radio("proMember"),
        membershipNumber: val("proMembership"),
      },
    };
    return data;
  }

  function readEntries(list) {
    return Array.from(list.children).map((entry) => {
      const obj = {};
      entry.querySelectorAll("input, select, textarea").forEach((el) => {
        if (el.type === "radio") {
          if (el.checked) obj[stripUid(el.name)] = el.value;
        } else {
          obj[el.name] = el.value;
        }
      });
      return obj;
    });
  }

  function stripUid(name) { return name.replace(/-[a-z]+-\d+-\d+$/, ""); }
  function val(id) { const e = document.getElementById(id); return e ? e.value.trim() : ""; }
  function radio(name) {
    const c = form.querySelector(`input[name="${name}"]:checked`);
    return c ? c.value : "";
  }

  /* ============================================================
   * 7. Buttons: check / save / submit / reset
   * ============================================================ */
  document.getElementById("checkBtn").addEventListener("click", () => {
    const errors = validateForm();
    if (errors.length) flash("Form has " + errors.length + " issue(s). Fields highlighted in red.", "error");
    else flash("Form check passed. You can submit now.", "success");
  });

  document.getElementById("saveBtn").addEventListener("click", () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(collectData()));
      flash("Draft saved locally in this browser.", "success");
    } catch (e) {
      flash("Could not save draft: " + e.message, "error");
    }
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const errors = validateForm();
    if (errors.length) {
      flash("Cannot submit — " + errors.length + " issue(s) found. Please correct highlighted fields.", "error");
      const firstBad = form.querySelector(".field.invalid");
      if (firstBad) firstBad.scrollIntoView({ behavior: "smooth", block: "center" });
      return;
    }
    const data = collectData();
    document.getElementById("jsonOutput").textContent = JSON.stringify(data, null, 2);
    document.getElementById("jsonPreview").hidden = false;
    flash("Form submitted successfully. Review the generated JSON below.", "success");
    document.getElementById("jsonPreview").scrollIntoView({ behavior: "smooth", block: "start" });
    // To send to backend:
    // fetch("/api/dir12", { method: "POST", headers: {"Content-Type":"application/json"}, body: JSON.stringify(data) });
  });

  form.addEventListener("reset", () => {
    setTimeout(() => {
      Object.values(lists).forEach((l) => (l.innerHTML = ""));
      syncCounts();
      clearAllInvalid();
      document.getElementById("jsonPreview").hidden = true;
      msgBox.className = "form-message";
      msgBox.textContent = "";
    }, 0);
  });

  /* ============================================================
   * 8. Restore draft on load (if present)
   * ============================================================ */
  function restoreDraft() {
    let saved;
    try { saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null"); } catch (e) { return; }
    if (!saved) return;
    const c = saved.company || {};
    setVal("cin", c.cin); setVal("gln", c.gln); setVal("companyName", c.name);
    setVal("regdOffice", c.regdOffice); setVal("companyEmail", c.email);
    (saved.appointments || []).forEach((row) => fillEntry(addEntry("appointment"), row));
    (saved.cessations || []).forEach((row) => fillEntry(addEntry("cessation"), row));
    (saved.changes || []).forEach((row) => fillEntry(addEntry("change"), row));
    const d = saved.declaration || {};
    setVal("resNo", d.resolutionNo); setVal("resDate", d.resolutionDate);
    setVal("signerId", d.signerId); setVal("signerName", d.signerName);
    if (saved.appointments || saved.cessations || saved.changes) syncCounts();
  }

  function fillEntry(node, row) {
    if (!node || !row) return;
    Object.keys(row).forEach((key) => {
      const el = node.querySelector(`[name="${key}"]`) ||
                 node.querySelector(`[name^="${key}-"]`);
      if (!el) return;
      if (el.type === "radio") {
        const r = node.querySelector(`[name^="${key}-"][value="${row[key]}"]`);
        if (r) r.checked = true;
      } else { el.value = row[key]; }
    });
  }

  function setVal(id, v) { const e = document.getElementById(id); if (e && v != null) e.value = v; }

  // Restore a saved draft if present; otherwise seed one empty appointment row
  restoreDraft();
  const hasAny = lists.appointment.children.length +
                 lists.cessation.children.length +
                 lists.change.children.length;
  if (hasAny === 0) addEntry("appointment");
})();
