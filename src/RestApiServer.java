import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

public class RestApiServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/api/login", RestApiServer::handleLogin);
        
        // DOCTORS
        server.createContext("/api/doctors", RestApiServer::handleDoctors);
        server.createContext("/api/doctors/add", RestApiServer::handleAddDoctor);
        server.createContext("/api/doctors/update", RestApiServer::handleUpdateDoctor); // NEW
        server.createContext("/api/doctors/delete", RestApiServer::handleDelete); 
        
        // PATIENTS
        server.createContext("/api/patients", RestApiServer::handlePatients);
        server.createContext("/api/patients/add", RestApiServer::handleAddPatient);
        server.createContext("/api/patients/update", RestApiServer::handleUpdatePatient); // NEW
        server.createContext("/api/patients/delete", RestApiServer::handleDelete);
        
        // APPOINTMENTS
        server.createContext("/api/appointments", RestApiServer::handleAppointments);
        server.createContext("/api/appointments/add", RestApiServer::handleAddAppointment);
        server.createContext("/api/appointments/update", RestApiServer::handleUpdateAppointment);
        server.createContext("/api/appointments/delete", RestApiServer::handleDelete);
        
        // TREATMENTS
        server.createContext("/api/treatments", RestApiServer::handleTreatments);
        server.createContext("/api/treatments/add", RestApiServer::handleAddTreatment);
        server.createContext("/api/treatments/update", RestApiServer::handleUpdateTreatment);
        server.createContext("/api/treatments/delete", RestApiServer::handleDelete);
        
        // ADMINS
        server.createContext("/api/admins", RestApiServer::handleAdmins);
        server.createContext("/api/admins/add", RestApiServer::handleAddAdmin);
        server.createContext("/api/admins/update", RestApiServer::handleUpdateAdmin); // NEW
        server.createContext("/api/admins/delete", RestApiServer::handleDelete);

        server.setExecutor(null);
        System.out.println("Server started at http://localhost:8080");
        server.start();
    }

    // --- UPDATE HANDLERS (NEW) ---

    private static void handleUpdateDoctor(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> p = parseRequestBody(ex);
            try (Connection conn = DbConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE doctors SET name=?, specialization=?, password=? WHERE id=?");
                ps.setString(1, p.get("name"));
                ps.setString(2, p.get("specialization"));
                ps.setString(3, p.get("password"));
                ps.setString(4, p.get("id"));
                ps.executeUpdate();
                sendResponse(ex, "{\"status\":\"success\"}", 200);
            } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); }
        } else handleCORS(ex);
    }

    private static void handleUpdatePatient(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> p = parseRequestBody(ex);
            try (Connection conn = DbConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE patients SET name=?, age=?, gender=? WHERE id=?");
                ps.setString(1, p.get("name"));
                ps.setInt(2, Integer.parseInt(p.get("age")));
                ps.setString(3, p.get("gender"));
                ps.setString(4, p.get("id"));
                ps.executeUpdate();
                sendResponse(ex, "{\"status\":\"success\"}", 200);
            } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); }
        } else handleCORS(ex);
    }

    private static void handleUpdateAdmin(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> p = parseRequestBody(ex);
            try (Connection conn = DbConnection.getConnection()) {
                // Prevent editing Root via this API (Safety)
                PreparedStatement ps = conn.prepareStatement("UPDATE users SET username=?, password=? WHERE id=? AND role != 'ROOT'");
                ps.setString(1, p.get("username"));
                ps.setString(2, p.get("password"));
                ps.setString(3, p.get("id"));
                int rows = ps.executeUpdate();
                if(rows > 0) sendResponse(ex, "{\"status\":\"success\"}", 200);
                else sendResponse(ex, "{\"status\":\"error\", \"message\":\"Cannot edit Root or ID not found\"}", 403);
            } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); }
        } else handleCORS(ex);
    }

    // --- EXISTING HANDLERS ---

    private static void handleAppointments(HttpExchange ex) throws IOException {
        if ("GET".equals(ex.getRequestMethod())) {
            String q = "SELECT a.id, a.appointment_date, a.patient_id, p.name as patient, a.doctor_id, d.name as doctor FROM appointments a JOIN patients p ON a.patient_id = p.id JOIN doctors d ON a.doctor_id = d.id ORDER BY a.appointment_date DESC";
            sendTableJson(ex, q);
        } else handleCORS(ex);
    }
    private static void handleUpdateAppointment(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> p = parseRequestBody(ex);
            try (Connection conn = DbConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET patient_id=?, doctor_id=?, appointment_date=? WHERE id=?");
                ps.setString(1, p.get("patient_id")); ps.setString(2, p.get("doctor_id")); ps.setString(3, p.get("appointment_date")); ps.setString(4, p.get("id"));
                ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200);
            } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); }
        } else handleCORS(ex);
    }
    private static void handleLogin(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            Map<String, String> p = parseRequestBody(ex);
            String user = p.get("username"); String pass = p.get("password");
            try (Connection conn = DbConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE username=? AND password=?");
                ps.setString(1, user); ps.setString(2, pass); ResultSet rs = ps.executeQuery();
                if (rs.next()) { sendResponse(ex, String.format("{\"status\":\"success\", \"role\":\"%s\", \"id\":\"%s\"}", rs.getString("role"), user), 200); return; }
                PreparedStatement psDoc = conn.prepareStatement("SELECT name FROM doctors WHERE id=? AND password=?");
                psDoc.setString(1, user); psDoc.setString(2, pass); ResultSet rsDoc = psDoc.executeQuery();
                if (rsDoc.next()) { sendResponse(ex, String.format("{\"status\":\"success\", \"role\":\"DOCTOR\", \"id\":\"%s\"}", user), 200); return; }
            } catch (SQLException e) { e.printStackTrace(); } sendResponse(ex, "{\"status\":\"error\", \"message\":\"Invalid Credentials\"}", 401);
        } else handleCORS(ex);
    }
    private static void handleTreatments(HttpExchange ex) throws IOException { if ("GET".equals(ex.getRequestMethod())) { String q = "SELECT t.id, t.visit_date, t.patient_id, p.name as patient_name, p.age, p.gender, t.doctor_id, d.name as doctor_name, t.diagnosis, t.prescription FROM treatments t JOIN patients p ON t.patient_id = p.id JOIN doctors d ON t.doctor_id = d.id ORDER BY t.visit_date DESC"; sendTableJson(ex, q); } else handleCORS(ex); }
    private static void handleAddTreatment(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection()) { String newId = generateId(conn, "TR", "treatments"); PreparedStatement ps = conn.prepareStatement("INSERT INTO treatments (id, patient_id, doctor_id, diagnosis, prescription, visit_date) VALUES (?, ?, ?, ?, ?, NOW())"); ps.setString(1, newId); ps.setString(2, p.get("patient_id")); ps.setString(3, p.get("doctor_id")); ps.setString(4, p.get("diagnosis")); ps.setString(5, p.get("prescription")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); } } else handleCORS(ex); }
    private static void handleUpdateTreatment(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection()) { PreparedStatement ps = conn.prepareStatement("UPDATE treatments SET diagnosis=?, prescription=?, visit_date=? WHERE id=?"); ps.setString(1, p.get("diagnosis")); ps.setString(2, p.get("prescription")); ps.setString(3, p.get("visit_date").replace("T", " ")); ps.setString(4, p.get("id")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); } } else handleCORS(ex); }
    private static void handleDoctors(HttpExchange ex) throws IOException { if ("GET".equals(ex.getRequestMethod())) { boolean showPass = !ex.getRequestHeaders().getFirst("X-Role").equals("DOCTOR"); sendTableJson(ex, showPass ? "SELECT * FROM doctors" : "SELECT id, name, specialization FROM doctors"); } else handleCORS(ex); }
    private static void handleAddDoctor(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection()) { String newId = generateId(conn, "DR", "doctors"); PreparedStatement ps = conn.prepareStatement("INSERT INTO doctors VALUES(?, ?, ?, ?)"); ps.setString(1, newId); ps.setString(2, p.get("name")); ps.setString(3, p.get("specialization")); ps.setString(4, p.get("password")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); } } else handleCORS(ex); }
    private static void handlePatients(HttpExchange ex) throws IOException { if ("GET".equals(ex.getRequestMethod())) sendTableJson(ex, "SELECT * FROM patients"); else handleCORS(ex); }
    private static void handleAddPatient(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection()) { String newId = generateId(conn, "PT", "patients"); PreparedStatement ps = conn.prepareStatement("INSERT INTO patients VALUES(?, ?, ?, ?)"); ps.setString(1, newId); ps.setString(2, p.get("name")); ps.setInt(3, Integer.parseInt(p.get("age"))); ps.setString(4, p.get("gender")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); } } else handleCORS(ex); }
    private static void handleAddAppointment(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection()) { PreparedStatement psCheck = conn.prepareStatement("SELECT count(*) FROM appointments WHERE doctor_id=? AND appointment_date=?"); psCheck.setString(1, p.get("doctor_id")); psCheck.setString(2, p.get("appointment_date")); ResultSet rs = psCheck.executeQuery(); if (rs.next() && rs.getInt(1) > 0) { sendResponse(ex, "{\"status\":\"error\", \"message\":\"Busy\"}", 400); return; } String newId = generateId(conn, "APT", "appointments"); PreparedStatement ps = conn.prepareStatement("INSERT INTO appointments VALUES(?, ?, ?, ?)"); ps.setString(1, newId); ps.setString(2, p.get("patient_id")); ps.setString(3, p.get("doctor_id")); ps.setString(4, p.get("appointment_date")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{}", 500); } } else handleCORS(ex); }
    private static void handleAdmins(HttpExchange ex) throws IOException { if ("GET".equals(ex.getRequestMethod())) { String q = "ROOT".equals(ex.getRequestHeaders().getFirst("X-Role")) ? "SELECT * FROM users" : "SELECT id, username, role FROM users"; sendTableJson(ex, q); } else handleCORS(ex); }
    private static void handleAddAdmin(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { Map<String, String> p = parseRequestBody(ex); try (Connection conn = DbConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username, password, role) VALUES(?, ?, 'ADMIN')")) { ps.setString(1, p.get("username")); ps.setString(2, p.get("password")); ps.executeUpdate(); sendResponse(ex, "{\"status\":\"success\"}", 200); } catch(Exception e) {e.printStackTrace(); sendResponse(ex, "{}", 500);} } else handleCORS(ex); }
    private static void handleDelete(HttpExchange ex) throws IOException { if ("POST".equals(ex.getRequestMethod())) { String id = parseRequestBody(ex).get("id"); String path = ex.getRequestURI().getPath(); String table = ""; if(path.contains("doctors")) table = "doctors"; else if(path.contains("patients")) table = "patients"; else if(path.contains("admins")) table = "users"; else if(path.contains("treatments")) table = "treatments"; else if(path.contains("appointments")) table = "appointments"; try (Connection conn = DbConnection.getConnection()) { if (table.equals("users")) { PreparedStatement checkStmt = conn.prepareStatement("SELECT username, role FROM users WHERE id = ?"); checkStmt.setString(1, id); ResultSet rs = checkStmt.executeQuery(); if (rs.next()) { String rUser = rs.getString("username"); String rRole = rs.getString("role"); if ("root".equalsIgnoreCase(rUser) || "ROOT".equalsIgnoreCase(rRole)) { sendResponse(ex, "{\"status\":\"error\", \"message\":\"Cannot delete ROOT account!\"}", 403); return; } } } PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE id=?"); ps.setString(1, id); int rows = ps.executeUpdate(); if (rows > 0) sendResponse(ex, "{\"status\":\"success\"}", 200); else sendResponse(ex, "{\"status\":\"error\", \"message\":\"Record not found\"}", 404); } catch (Exception e) { e.printStackTrace(); sendResponse(ex, "{\"status\":\"error\", \"message\":\"Database Error: " + e.getMessage() + "\"}", 500); } } else handleCORS(ex); }

    // Utils
    private static String escapeJson(String input) { if (input == null) return ""; StringBuilder sb = new StringBuilder(); for (char c : input.toCharArray()) { switch (c) { case '"': sb.append("\\\""); break; case '\\': sb.append("\\\\"); break; case '\b': sb.append("\\b"); break; case '\f': sb.append("\\f"); break; case '\n': sb.append("\\n"); break; case '\r': sb.append("\\r"); break; case '\t': sb.append("\\t"); break; default: sb.append(c); } } return sb.toString(); }
    private static void sendTableJson(HttpExchange ex, String query) throws IOException { StringBuilder json = new StringBuilder("["); try (Connection conn = DbConnection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) { ResultSetMetaData meta = rs.getMetaData(); int cols = meta.getColumnCount(); while (rs.next()) { json.append("{"); for (int i = 1; i <= cols; i++) { String val = rs.getString(i); json.append("\"").append(meta.getColumnLabel(i)).append("\":\"").append(escapeJson(val)).append("\""); if (i < cols) json.append(","); } json.append("},"); } if (json.length() > 1) json.setLength(json.length() - 1); } catch (Exception e) { e.printStackTrace(); } json.append("]"); sendResponse(ex, json.toString(), 200); }
    private static String generateId(Connection conn, String prefix, String table) throws SQLException { String query = "SELECT id FROM " + table + " WHERE id LIKE '" + prefix + "-%' ORDER BY id DESC LIMIT 1"; try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) { if (rs.next()) { String lastId = rs.getString("id"); int num = Integer.parseInt(lastId.split("-")[1]); return String.format("%s-%03d", prefix, num + 1); } } return prefix + "-001"; }
    private static Map<String, String> parseRequestBody(HttpExchange ex) throws IOException { InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8); BufferedReader br = new BufferedReader(isr); StringBuilder b = new StringBuilder(); String line; while ((line = br.readLine()) != null) b.append(line); return parseQuery(b.toString()); }
    private static Map<String, String> parseQuery(String q) { Map<String, String> res = new HashMap<>(); if (q == null || q.isEmpty()) return res; for (String p : q.split("&")) { String[] pair = p.split("="); if (pair.length > 1) { try { res.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8")); } catch (Exception e) {} } } return res; }
    private static void handleCORS(HttpExchange ex) throws IOException { ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Role"); ex.sendResponseHeaders(204, -1); }
    private static void sendResponse(HttpExchange ex, String resp, int code) throws IOException { ex.getResponseHeaders().set("Content-Type", "application/json"); ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); ex.sendResponseHeaders(code, resp.getBytes().length); try (OutputStream os = ex.getResponseBody()) { os.write(resp.getBytes()); } }
}