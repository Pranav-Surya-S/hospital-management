const API = "http://localhost:8080/api";
let userRole = "";
let userId = "";

function togglePass() {
  const i = document.getElementById("password");
  const btn = document.querySelector(".toggle-pass"); // Get the icon element
  if (i.type === "password") {
    i.type = "text";
    btn.innerText = "visibility_off"; // Switch icon
  } else {
    i.type = "password";
    btn.innerText = "visibility"; // Switch icon back
  }
}

// ... handleLogin remains the same ...
async function handleLogin(e) {
  e.preventDefault();
  const u = document.getElementById("username").value;
  const p = document.getElementById("password").value;

  const res = await fetch(`${API}/login`, {
    method: "POST",
    body: `username=${u}&password=${p}`,
  });

  if (res.ok) {
    const data = await res.json();
    userRole = data.role;
    userId = data.id;
    document.getElementById("login-screen").classList.add("hidden");
    document.getElementById("user-badge").innerText =
      `ID: ${userId} | Role: ${userRole}`;

    if (userRole === "ROOT") {
      document.getElementById("nav-admin").style.display = "block";
    }

    if (userRole === "DOCTOR") {
      document.getElementById("btn-add-doc").style.display = "none";
      document.getElementById("btn-add-appt").style.display = "none";
      if (document.getElementById("btn-add-rx"))
        document.getElementById("btn-add-rx").style.display = "block";
    } else {
      if (document.getElementById("btn-add-rx"))
        document.getElementById("btn-add-rx").style.display = "none";
    }

    refreshAll();
  } else {
    document.getElementById("login-err").innerText = "Invalid Credentials";
  }
}

async function loadTable(endpoint, tableId, allowDelete) {
  const res = await fetch(`${API}/${endpoint}`, {
    headers: { "X-Role": userRole },
  });
  const data = await res.json();
  const thead = document.querySelector(`#${tableId} thead`);
  const tbody = document.querySelector(`#${tableId} tbody`);
  tbody.innerHTML = "";

  if (data.length > 0) {
    const allKeys = Object.keys(data[0]);
    const hiddenCols = ["patient_id", "doctor_id", "age", "gender"];
    const visibleCols = allKeys.filter((k) => !hiddenCols.includes(k));

    const isAdmin = userRole === "ADMIN" || userRole === "ROOT";
    const showEdit = isAdmin;
    const showPrint = endpoint === "treatments";

    thead.innerHTML = `<tr>
        ${visibleCols.map((c) => `<th>${c.toUpperCase()}</th>`).join("")}
        ${showPrint ? "<th>PRINT</th>" : ""}
        ${showEdit ? "<th>EDIT</th>" : ""}
        ${allowDelete ? "<th>ACTION</th>" : ""}
    </tr>`;

    tbody.innerHTML = data
      .map(
        (item) => `
            <tr>
                ${visibleCols
                  .map((key) => {
                    // Password Masking Logic with Material Icon
                    if (key === "password") {
                      return `<td class="pass-cell">
                            <span class="pass-mask">••••••••</span>
                            <span class="pass-real" style="display:none">${item[key]}</span>
                            <span class="material-icons table-eye" onclick="toggleRowPass(this)">visibility</span>
                        </td>`;
                    }
                    return `<td>${item[key]}</td>`;
                  })
                  .join("")}
                
                ${showPrint ? `<td><button class="action-btn" style="background:#4b5563" onclick='printPrescription(${JSON.stringify(item)})'><span class="material-icons" style="font-size:16px;">print</span> Print</button></td>` : ""}
                ${showEdit ? `<td><button class="action-btn" onclick='openEditModalGeneral("${endpoint}", ${JSON.stringify(item)})'><span class="material-icons" style="font-size:16px;">edit</span> Edit</button></td>` : ""}
                ${allowDelete ? `<td><button class="del-btn" onclick="deleteItem('${endpoint}', '${item.id}')">Delete</button></td>` : ""}
            </tr>
        `,
      )
      .join("");
  } else {
    thead.innerHTML = "";
    tbody.innerHTML =
      "<tr><td style='padding:20px; text-align:center'>No Records Found</td></tr>";
  }
}

// ... openEditModalGeneral and Edit Modals remain the same ...
function openEditModalGeneral(endpoint, item) {
  if (endpoint === "doctors") openEditDoctorModal(item);
  if (endpoint === "patients") openEditPatientModal(item);
  if (endpoint === "admins") openEditAdminModal(item);
  if (endpoint === "treatments") openEditTreatmentModal(item);
  if (endpoint === "appointments") openEditApptModal(item);
}

function openEditDoctorModal(item) {
  document.getElementById("edit-doc-id").value = item.id;
  document.getElementById("edit-doc-name").value = item.name;
  document.getElementById("edit-doc-spec").value = item.specialization;
  document.getElementById("edit-doc-pass").value = item.password;
  document.getElementById("modal-edit-doc").classList.add("open");
}
async function updateDoctor(e) {
  e.preventDefault();
  await postUpdate("doctors", {
    id: document.getElementById("edit-doc-id").value,
    name: document.getElementById("edit-doc-name").value,
    specialization: document.getElementById("edit-doc-spec").value,
    password: document.getElementById("edit-doc-pass").value,
  });
}

function openEditPatientModal(item) {
  document.getElementById("edit-pat-id").value = item.id;
  document.getElementById("edit-pat-name").value = item.name;
  document.getElementById("edit-pat-age").value = item.age;
  document.getElementById("edit-pat-gen").value = item.gender;
  document.getElementById("modal-edit-pat").classList.add("open");
}
async function updatePatient(e) {
  e.preventDefault();
  await postUpdate("patients", {
    id: document.getElementById("edit-pat-id").value,
    name: document.getElementById("edit-pat-name").value,
    age: document.getElementById("edit-pat-age").value,
    gender: document.getElementById("edit-pat-gen").value,
  });
}

function openEditAdminModal(item) {
  document.getElementById("edit-admin-id").value = item.id;
  document.getElementById("edit-admin-user").value = item.username;
  document.getElementById("edit-admin-pass").value = item.password;
  document.getElementById("modal-edit-admin").classList.add("open");
}
async function updateAdmin(e) {
  e.preventDefault();
  await postUpdate("admins", {
    id: document.getElementById("edit-admin-id").value,
    username: document.getElementById("edit-admin-user").value,
    password: document.getElementById("edit-admin-pass").value,
  });
}

function openEditApptModal(item) {
  loadDropdowns().then(() => {
    const docSelect = document.getElementById("edit-appt-doc");
    const patSelect = document.getElementById("edit-appt-pat");
    if (docSelect && patSelect) {
      docSelect.value = item.doctor_id;
      patSelect.value = item.patient_id;
    }
  });
  document.getElementById("edit-appt-id").value = item.id;
  document.getElementById("edit-appt-date").value = item.appointment_date;
  document.getElementById("modal-edit-appt").classList.add("open");
}
async function updateAppointment(e) {
  e.preventDefault();
  await postUpdate("appointments", {
    id: document.getElementById("edit-appt-id").value,
    doctor_id: document.getElementById("edit-appt-doc").value,
    patient_id: document.getElementById("edit-appt-pat").value,
    appointment_date: document.getElementById("edit-appt-date").value,
  });
}

function openEditTreatmentModal(item) {
  document.getElementById("edit-rx-id").value = item.id;
  document.getElementById("edit-rx-diag").value = item.diagnosis;
  document.getElementById("edit-rx-desc").value = item.prescription;
  document.getElementById("edit-rx-date").value = item.visit_date
    .replace(" ", "T")
    .substring(0, 16);
  document.getElementById("modal-edit-rx").classList.add("open");
}
async function updateTreatment(e) {
  e.preventDefault();
  await postUpdate("treatments", {
    id: document.getElementById("edit-rx-id").value,
    diagnosis: document.getElementById("edit-rx-diag").value,
    prescription: document.getElementById("edit-rx-desc").value,
    visit_date: document.getElementById("edit-rx-date").value,
  });
}

async function postUpdate(endpoint, body) {
  const res = await fetch(`${API}/${endpoint}/update`, {
    method: "POST",
    headers: { "X-Role": userRole },
    body: new URLSearchParams(body),
  });
  const data = await res.json();
  if (data.status === "error") alert(data.message);
  closeModals();
  refreshAll();
}

// ... printPrescription remains the same ...
function printPrescription(item) {
  const printWindow = window.open("", "", "width=800,height=600");
  const html = `<html><head><title>Prescription - ${item.id}</title><style>body{font-family:'Times New Roman';padding:40px;}.header{text-align:center;border-bottom:2px solid #333;margin-bottom:30px;}.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:30px;}.box{border:1px solid #ddd;padding:15px;}.rx-section{border:1px solid #333;padding:20px;min-height:200px;margin-top:20px;}.footer{margin-top:50px;text-align:right;}</style></head><body><div class="header"><h1>CITY GENERAL HOSPITAL</h1></div><div class="info-grid"><div class="box"><h3>DOCTOR</h3><p>${item.doctor_name}</p></div><div class="box"><h3>PATIENT</h3><p>${item.patient_name}</p></div></div><div class="rx-section"><h3>Rx:</h3><pre>${item.prescription}</pre></div><div class="footer"><p>Doctor's Signature</p></div><script>window.onload=function(){window.print();}</script></body></html>`;
  printWindow.document.write(html);
  printWindow.document.close();
}

// Updated Table Password Toggle logic
function toggleRowPass(btn) {
  const cell = btn.parentElement;
  const mask = cell.querySelector(".pass-mask");
  const real = cell.querySelector(".pass-real");

  if (real.style.display === "none") {
    real.style.display = "inline";
    mask.style.display = "none";
    btn.innerText = "visibility_off";
  } else {
    real.style.display = "none";
    mask.style.display = "inline";
    btn.innerText = "visibility";
  }
}

// ... refreshAll, postData, deleteItem, add functions, openModal, closeModals, loadDropdowns remain the same ...
function refreshAll() {
  loadTable(
    "treatments",
    "table-history",
    userRole === "ADMIN" || userRole === "ROOT",
  );
  if (userRole === "DOCTOR") {
    loadTable("appointments", "table-appt", false);
    loadTable("patients", "table-pat", false);
    loadTable("doctors", "table-doc", false);
  } else {
    loadTable("doctors", "table-doc", true);
    loadTable("patients", "table-pat", true);
    loadTable("appointments", "table-appt", true);
    if (userRole === "ROOT") loadTable("admins", "table-admin", true);
  }
}

async function postData(endpoint, body) {
  await fetch(`${API}/${endpoint}`, {
    method: "POST",
    body: new URLSearchParams(body),
  });
  closeModals();
  refreshAll();
}

async function deleteItem(endpoint, id) {
  if (!confirm("Delete this record?")) return;
  const res = await fetch(`${API}/${endpoint}/delete`, {
    method: "POST",
    body: new URLSearchParams({ id: id }),
  });
  const data = await res.json();
  if (data.status === "error") {
    alert("❌ Error: " + data.message);
  } else {
    refreshAll();
  }
}

function addDoctor(e) {
  e.preventDefault();
  postData("doctors/add", {
    name: document.getElementById("d-name").value,
    specialization: document.getElementById("d-spec").value,
    password: document.getElementById("d-pass").value,
  });
}
function addPatient(e) {
  e.preventDefault();
  postData("patients/add", {
    name: document.getElementById("p-name").value,
    age: document.getElementById("p-age").value,
    gender: document.getElementById("p-gen").value,
  });
}
function addAdmin(e) {
  e.preventDefault();
  postData("admins/add", {
    username: document.getElementById("a-user").value,
    password: document.getElementById("a-pass").value,
  });
}
async function bookAppt(e) {
  e.preventDefault();
  postData("appointments/add", {
    doctor_id: document.getElementById("appt-doc").value,
    patient_id: document.getElementById("appt-pat").value,
    appointment_date: document.getElementById("appt-date").value,
  });
}

function addTreatment(e) {
  e.preventDefault();
  postData("treatments/add", {
    patient_id: document.getElementById("rx-pat").value,
    doctor_id: userId,
    diagnosis: document.getElementById("rx-diag").value,
    prescription: document.getElementById("rx-desc").value,
  });
}

function switchTab(id) {
  document
    .querySelectorAll(".tab-content")
    .forEach((e) => e.classList.remove("active"));
  document.getElementById(`tab-${id}`).classList.add("active");
}
function openModal(id) {
  document.getElementById(id).classList.add("open");
  if (id === "modal-appt" || id === "modal-rx") loadDropdowns();
}
function closeModals() {
  document
    .querySelectorAll(".modal")
    .forEach((e) => e.classList.remove("open"));
}
async function loadDropdowns() {
  const [docs, pats] = await Promise.all([
    fetch(`${API}/doctors`, { headers: { "X-Role": "PUBLIC" } }).then((r) =>
      r.json(),
    ),
    fetch(`${API}/patients`).then((r) => r.json()),
  ]);

  const docOptions = docs
    .map((d) => `<option value="${d.id}">${d.name}</option>`)
    .join("");
  const patOptions = pats
    .map((p) => `<option value="${p.id}">${p.name} (${p.id})</option>`)
    .join("");

  if (document.getElementById("appt-doc")) {
    document.getElementById("appt-doc").innerHTML = docOptions;
    document.getElementById("appt-pat").innerHTML = patOptions;
    if (document.getElementById("edit-appt-doc")) {
      document.getElementById("edit-appt-doc").innerHTML = docOptions;
      document.getElementById("edit-appt-pat").innerHTML = patOptions;
    }
  }
  if (document.getElementById("rx-pat")) {
    document.getElementById("rx-pat").innerHTML = patOptions;
  }
}
