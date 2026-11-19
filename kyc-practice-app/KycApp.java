import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KYC Practice API - a small banking KYC onboarding backend for TESTING PRACTICE.
 * It contains planted defects. Do not open SEEDED_BUGS.md until you have finished testing.
 * Run:  java KycApp.java        (needs JDK 11 or newer, no other install)
 */
public class KycApp {

    static class Doc { String type, number, name; LocalDate expiry; }
    static class Customer {
        int id; String owner, fullName, email, mobile, pan; LocalDate dob;
        String status = "PENDING"; int attempts = 0;
        List<String> reasons = new ArrayList<>();
        Map<String, Doc> docs = new LinkedHashMap<>();
    }

    static final Map<Integer, Customer> customers = new LinkedHashMap<>();
    static final Map<String, String[]> users = new LinkedHashMap<>();
    static final Map<String, String> tokens = new HashMap<>();
    static int nextId = 1001, nextTransfer = 9001;

    static {
        users.put("customer1", new String[]{"Pass@123", "customer"});
        users.put("customer2", new String[]{"Pass@123", "customer"});
        users.put("officer1", new String[]{"Pass@123", "officer"});
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> {
            try { route(ex); }
            catch (Exception e) { send(ex, 500, map("error", "Server error: " + e)); }
        });
        server.start();
        System.out.println("KYC practice API running on http://localhost:" + port);
        System.out.println("Users: customer1 / customer2 / officer1, password Pass@123");
    }

    // ---------- routing ----------
    static synchronized void route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] p = ex.getRequestURI().getPath().replaceAll("^/|/$", "").split("/");
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        if (p.length < 2 || !p[0].equals("api")) { send(ex, 404, map("error", "Not found")); return; }

        if (method.equals("GET") && p[1].equals("health")) { send(ex, 200, map("status", "UP")); return; }
        if (method.equals("POST") && p[1].equals("reset")) {
            customers.clear(); tokens.clear(); nextId = 1001; nextTransfer = 9001;
            send(ex, 200, map("message", "Data cleared")); return;
        }
        if (method.equals("POST") && p[1].equals("login")) { login(ex, body); return; }

        String user = authUser(ex);
        if (user == null) { send(ex, 401, map("error", "Missing or invalid token")); return; }
        String role = users.get(user)[1];

        if (p[1].equals("transfers") && method.equals("POST") && p.length == 2) { transfer(ex, body, user); return; }

        if (p[1].equals("customers")) {
            if (p.length == 2 && method.equals("POST")) { register(ex, body, user, role); return; }
            if (p.length >= 3) {
                Integer id = parseIntOrNull(p[2]);
                Customer c = id == null ? null : customers.get(id);
                if (c == null) { send(ex, 404, map("error", "Customer not found")); return; }
                if (p.length == 3 && method.equals("GET")) { profile(ex, c); return; }
                if (p.length == 4) {
                    String action = p[3];
                    if (action.equals("documents") && method.equals("POST")) { submitDoc(ex, body, c, user); return; }
                    if (action.equals("verify") && method.equals("POST")) { verify(ex, c, user); return; }
                    if (action.equals("status") && method.equals("GET")) { status(ex, c, user, role); return; }
                    if (action.equals("review") && method.equals("POST")) { review(ex, body, c, role); return; }
                }
            }
        }
        send(ex, 404, map("error", "Not found"));
    }

    // ---------- handlers ----------
    static void login(HttpExchange ex, String body) throws IOException {
        String u = str(body, "username"), pw = str(body, "password");
        String[] rec = u == null ? null : users.get(u);
        if (rec == null || pw == null || !rec[0].equals(pw)) { send(ex, 401, map("error", "Invalid credentials")); return; }
        String token = UUID.randomUUID().toString();
        tokens.put(token, u);
        send(ex, 200, map("token", token, "role", rec[1]));
    }

    static void register(HttpExchange ex, String body, String user, String role) throws IOException {
        if (!role.equals("customer")) { send(ex, 403, map("error", "Only customers can register")); return; }
        String name = str(body, "fullName"), dob = str(body, "dob"), mobile = str(body, "mobile"),
               email = str(body, "email"), pan = str(body, "pan");
        if (isBlank(name) || isBlank(dob) || isBlank(mobile) || isBlank(email) || isBlank(pan)) {
            send(ex, 400, map("error", "fullName, dob, mobile, email and pan are required")); return; }
        LocalDate d;
        try { d = LocalDate.parse(dob); } catch (Exception e) { send(ex, 400, map("error", "dob must be yyyy-MM-dd")); return; }
        if (Period.between(d, LocalDate.now()).getYears() < 18) { send(ex, 400, map("error", "Customer must be at least 18")); return; }
        if (!mobile.matches("[6-9][0-9]{9}")) { send(ex, 400, map("error", "Invalid mobile number")); return; }
        if (!email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) { send(ex, 400, map("error", "Invalid email")); return; }
        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) { send(ex, 400, map("error", "Invalid PAN format")); return; }

        String emailKey = email.trim().toLowerCase();
        for (Customer o : customers.values()) {
            if (o.pan.equals(pan)) { send(ex, 409, map("error", "Duplicate PAN")); return; }
            if (o.mobile.equals(mobile)) { send(ex, 409, map("error", "Duplicate mobile number")); return; }
            if (o.email.equals(emailKey)) { send(ex, 409, map("error", "Duplicate email")); return; }
        }
        Customer c = new Customer();
        c.id = nextId++; c.owner = user; c.fullName = name.trim(); c.dob = d;
        c.mobile = mobile; c.email = emailKey; c.pan = pan;
        customers.put(c.id, c);
        send(ex, 201, map("id", c.id, "status", c.status, "panMasked", mask(pan)));
    }

    static void submitDoc(HttpExchange ex, String body, Customer c, String user) throws IOException {
        if (!c.owner.equals(user)) { send(ex, 403, map("error", "Not your record")); return; }
        if (c.status.equals("LOCKED")) { send(ex, 423, map("error", "Account locked after too many failed attempts")); return; }
        if (c.status.equals("APPROVED")) { send(ex, 409, map("error", "Customer already approved")); return; }

        String type = str(body, "type"), number = str(body, "number"),
               name = str(body, "nameOnDocument"), expiry = str(body, "expiry");
        if (type == null || !(type.equals("PAN") || type.equals("AADHAAR") || type.equals("PASSPORT"))) {
            send(ex, 400, map("error", "type must be PAN, AADHAAR or PASSPORT")); return; }
        if (isBlank(number) || isBlank(name)) { send(ex, 400, map("error", "number and nameOnDocument are required")); return; }
        number = number.replaceAll("\\s", "");

        if (type.equals("PAN")) {
            if (!number.matches("[A-Z]{5}[0-9]{4}[A-Z]")) { send(ex, 400, map("error", "Invalid PAN format")); return; }
            if (!number.equals(c.pan)) { send(ex, 400, map("error", "PAN does not match the registered PAN")); return; }
        } else if (type.equals("AADHAAR")) {
            if (!number.matches("[2-9][0-9]{10,11}")) { send(ex, 400, map("error", "Invalid Aadhaar format")); return; }
        } else {
            if (!number.matches("[A-PR-WY][0-9]{7}")) { send(ex, 400, map("error", "Invalid passport format")); return; }
        }

        LocalDate exp = null;
        if (type.equals("PASSPORT")) {
            if (isBlank(expiry)) { send(ex, 400, map("error", "expiry is required for PASSPORT")); return; }
            try { exp = LocalDate.parse(expiry); } catch (Exception e) { send(ex, 400, map("error", "expiry must be yyyy-MM-dd")); return; }
            if (isExpired(exp)) { send(ex, 400, map("error", "Document expired")); return; }
        }

        Doc d = new Doc();
        d.type = type; d.number = number; d.name = name.trim(); d.expiry = exp;
        boolean resubmission = c.status.equals("REJECTED");
        if (resubmission) c.status = "PENDING";
        if (resubmission && c.docs.containsKey(type)) { }
        else c.docs.put(type, d);
        send(ex, 200, map("type", type, "numberMasked", mask(number), "customerStatus", c.status));
    }

    static void verify(HttpExchange ex, Customer c, String user) throws IOException {
        if (!c.owner.equals(user)) { send(ex, 403, map("error", "Not your record")); return; }
        if (c.status.equals("LOCKED")) { send(ex, 423, map("error", "Account locked after too many failed attempts")); return; }
        if (c.status.equals("APPROVED")) { send(ex, 409, map("error", "Customer already approved")); return; }
        if (c.status.equals("REJECTED")) { send(ex, 409, map("error", "Resubmit documents before verifying again")); return; }
        if (!c.docs.containsKey("PAN")) { send(ex, 400, map("error", "PAN document is required")); return; }

        List<String> fails = new ArrayList<>();
        for (Doc d : c.docs.values())
            if (!nameMatches(c.fullName, d.name)) fails.add(d.type + ": NAME_MISMATCH");
        if (fails.isEmpty()) {
            c.status = "APPROVED"; c.reasons = new ArrayList<>();
            send(ex, 200, map("status", c.status, "tier", tier(c))); return;
        }
        c.attempts++;
        c.reasons = fails;
        c.status = "REJECTED";
        if (c.attempts > 3) c.status = "LOCKED";
        send(ex, 200, map("status", c.status, "attempts", c.attempts, "reasons", fails));
    }

    static void status(HttpExchange ex, Customer c, String user, String role) throws IOException {
        if (!role.equals("officer") && !c.owner.equals(user)) { send(ex, 403, map("error", "Not your record")); return; }
        send(ex, 200, map("id", c.id, "status", c.status, "attempts", c.attempts,
                "tier", c.status.equals("APPROVED") ? tier(c) : null, "reasons", c.reasons));
    }

    static void profile(HttpExchange ex, Customer c) throws IOException {
        List<Object> docs = new ArrayList<>();
        for (Doc d : c.docs.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", d.type);
            m.put("number", d.type.equals("AADHAAR") ? d.number : mask(d.number));
            m.put("nameOnDocument", d.name);
            if (d.expiry != null) m.put("expiry", d.expiry.toString());
            docs.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", c.id); out.put("fullName", c.fullName); out.put("dob", c.dob.toString());
        out.put("mobile", c.mobile); out.put("email", c.email); out.put("pan", mask(c.pan));
        out.put("status", c.status); out.put("documents", docs);
        send(ex, 200, out);
    }

    static void review(HttpExchange ex, String body, Customer c, String role) throws IOException {
        if (!role.equals("officer")) { send(ex, 403, map("error", "Officers only")); return; }
        String decision = str(body, "decision");
        if (decision == null || !(decision.equals("APPROVE") || decision.equals("REJECT"))) {
            send(ex, 400, map("error", "decision must be APPROVE or REJECT")); return; }
        if (c.status.equals("LOCKED")) { send(ex, 423, map("error", "Account locked")); return; }
        if (c.status.equals("APPROVED")) { send(ex, 409, map("error", "Customer already approved")); return; }
        c.status = decision.equals("APPROVE") ? "APPROVED" : "REJECTED";
        send(ex, 200, map("id", c.id, "status", c.status));
    }

    static void transfer(HttpExchange ex, String body, String user) throws IOException {
        Integer cid = parseIntOrNull(num(body, "customerId"));
        Customer c = cid == null ? null : customers.get(cid);
        if (c == null) { send(ex, 404, map("error", "Customer not found")); return; }
        if (!c.owner.equals(user)) { send(ex, 403, map("error", "Not your record")); return; }
        String to = str(body, "toAccount"); String amt = num(body, "amount");
        double amount;
        try { amount = Double.parseDouble(amt); } catch (Exception e) { send(ex, 400, map("error", "amount is required")); return; }
        if (amount <= 0) { send(ex, 400, map("error", "amount must be greater than 0")); return; }
        if (to == null || !to.matches("[0-9]{9,18}")) { send(ex, 400, map("error", "Invalid toAccount")); return; }
        if (!c.status.equals("APPROVED")) { send(ex, 403, map("error", "Customer is not approved")); return; }
        double limit = tier(c).equals("TIER2") ? 200000 : 50000;
        if (amount > limit) { send(ex, 422, map("error", "Amount exceeds tier limit", "tier", tier(c), "limit", (long) limit)); return; }
        send(ex, 201, map("transferId", nextTransfer++, "amount", amount, "status", "COMPLETED"));
    }

    // ---------- rules ----------
    static boolean nameMatches(String registered, String onDoc) {
        String[] a = norm(registered).split(" "), b = norm(onDoc).split(" ");
        return a[0].equals(b[0]);
    }
    static boolean isExpired(LocalDate expiry) { return !expiry.isAfter(LocalDate.now()); }
    static String tier(Customer c) { return c.docs.containsKey("AADHAAR") ? "TIER2" : "TIER1"; }
    static String norm(String s) { return s.trim().replaceAll("\\s+", " ").toUpperCase(); }
    static String mask(String s) {
        if (s.length() <= 4) return s;
        return "X".repeat(s.length() - 4) + s.substring(s.length() - 4);
    }

    // ---------- helpers ----------
    static String authUser(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return tokens.get(h.substring(7).trim());
    }
    static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    static Integer parseIntOrNull(String s) { try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; } }
    static String str(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
    static String num(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9.]+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }
    static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
    static String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{"); boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(","); first = false;
                sb.append(toJson(String.valueOf(e.getKey()))).append(":").append(toJson(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (o instanceof List) {
            StringBuilder sb = new StringBuilder("["); boolean first = true;
            for (Object x : (List<?>) o) { if (!first) sb.append(","); first = false; sb.append(toJson(x)); }
            return sb.append("]").toString();
        }
        return "\"" + o.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    static void send(HttpExchange ex, int code, Object body) throws IOException {
        byte[] b = toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
